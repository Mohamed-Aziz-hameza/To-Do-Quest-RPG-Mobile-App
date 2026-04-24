package com.example.todoquest.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.todoquest.data.local.AppDatabase;
import com.example.todoquest.data.local.dao.AchievementDao;
import com.example.todoquest.data.local.dao.DungeonRunDao;
import com.example.todoquest.data.local.dao.PlayerDao;
import com.example.todoquest.data.local.dao.TaskDao;
import com.example.todoquest.data.local.entity.Achievement;
import com.example.todoquest.data.local.entity.DungeonRun;
import com.example.todoquest.data.local.entity.PlayerProfile;
import com.example.todoquest.data.local.entity.Task;
import com.example.todoquest.data.remote.FirebaseSyncManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskRepository {

    public static final String ACH_FIRST_BLOOD = "FIRST_BLOOD";
    public static final String ACH_ON_FIRE = "ON_FIRE";
    public static final String ACH_PERFECTIONIST = "PERFECTIONIST";
    public static final String ACH_EARLY_BIRD = "EARLY_BIRD";
    public static final String ACH_DAILY_CHAMPION = "DAILY_CHAMPION";
    public static final String ACH_DUNGEON_HUNTER = "DUNGEON_HUNTER";
    public static final String ACH_TREASURE_HUNTER = "TREASURE_HUNTER";

    public static final int SWORD_COST = 120;
    public static final int SHIELD_COST = 90;

    private final TaskDao taskDao;
    private final PlayerDao playerDao;
    private final AchievementDao achievementDao;
    private final DungeonRunDao dungeonRunDao;
    private final LiveData<List<Task>> allTasks;
    private final LiveData<PlayerProfile> profile;
    private final LiveData<List<Achievement>> allAchievements;
    private final LiveData<List<DungeonRun>> dungeonRuns;
    private final ExecutorService diskExecutor;
    private final FirebaseSyncManager firebaseSyncManager;

    public TaskRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        this.taskDao = database.taskDao();
        this.playerDao = database.playerDao();
        this.achievementDao = database.achievementDao();
        this.dungeonRunDao = database.dungeonRunDao();
        this.allTasks = taskDao.getAllTasks();
        this.profile = playerDao.getProfile();
        this.allAchievements = achievementDao.getAllAchievements();
        this.dungeonRuns = dungeonRunDao.getAllRuns();
        this.diskExecutor = Executors.newFixedThreadPool(4);
        this.firebaseSyncManager = FirebaseSyncManager.getInstance(context.getApplicationContext());
        this.firebaseSyncManager.startSync(taskDao, playerDao, diskExecutor);
        seedDefaultsIfNeeded();
    }

    /**
     * Returns all tasks as observable data.
     *
     * @return observable task list
     */
    public LiveData<List<Task>> getAllTasks() {
        return allTasks;
    }

    /**
     * Returns the single player profile as observable data.
     *
     * @return observable profile row
     */
    public LiveData<PlayerProfile> getProfile() {
        return profile;
    }

    /**
     * Returns all achievements as observable data.
     *
     * @return observable achievements
     */
    public LiveData<List<Achievement>> getAllAchievements() {
        return allAchievements;
    }

    public LiveData<List<DungeonRun>> getDungeonRuns() {
        return dungeonRuns;
    }

    /**
     * Adds a task asynchronously.
     *
     * @param task task to insert
     */
    public void insertTask(Task task) {
        runOnDiskIo(() -> {
            task.updatedAt = System.currentTimeMillis();
            taskDao.insertTask(task);
            firebaseSyncManager.pushTask(task, taskDao);
        });
    }

    /**
     * Updates a task asynchronously.
     *
     * @param task task to update
     */
    public void updateTask(Task task) {
        runOnDiskIo(() -> {
            task.updatedAt = System.currentTimeMillis();
            taskDao.updateTask(task);
            firebaseSyncManager.pushTask(task, taskDao);
        });
    }

    /**
     * Deletes a task asynchronously.
     *
     * @param task task to delete
     */
    public void deleteTask(Task task) {
        runOnDiskIo(() -> {
            taskDao.deleteTask(task);
            firebaseSyncManager.removeTask(task);
        });
    }

    /**
     * Inserts or replaces the profile asynchronously.
     *
     * @param playerProfile profile to write
     */
    public void insertProfile(PlayerProfile playerProfile) {
        runOnDiskIo(() -> {
            playerDao.insertProfile(playerProfile);
            firebaseSyncManager.pushProfile(playerProfile);
        });
    }

    /**
     * Updates the profile asynchronously.
     *
     * @param playerProfile profile to update
     */
    public void updateProfile(PlayerProfile playerProfile) {
        runOnDiskIo(() -> {
            playerDao.updateProfile(playerProfile);
            firebaseSyncManager.pushProfile(playerProfile);
        });
    }

    /**
     * Updates an achievement asynchronously.
     *
     * @param achievement achievement to update
     */
    public void updateAchievement(Achievement achievement) {
        runOnDiskIo(() -> achievementDao.updateAchievement(achievement));
    }

    /**
     * Returns a blocking profile snapshot.
     *
     * @return profile row or null
     */
    public PlayerProfile getProfileSync() {
        return playerDao.getProfileSync();
    }

    public PlayerProfile getOrCreateProfileSync() {
        PlayerProfile playerProfile = playerDao.getProfileSync();
        if (playerProfile == null) {
            playerProfile = createDefaultProfile();
            playerDao.insertProfile(playerProfile);
        }
        if (playerProfile.heroName == null) {
            playerProfile.heroName = "";
        }
        if (playerProfile.characterClass == null || playerProfile.characterClass.trim().isEmpty()) {
            playerProfile.characterClass = "ROOKIE_ADVENTURER";
        }

        // Backfill older profiles to the new unified baseline stats.
        if (playerProfile.baseStrength <= 0) {
            playerProfile.baseStrength = 10;
        }
        if (playerProfile.baseSpeed <= 0) {
            playerProfile.baseSpeed = 10;
        }
        if (playerProfile.baseHealth <= 0) {
            playerProfile.baseHealth = 100;
        }
        if (playerProfile.currentHealth <= 0) {
            playerProfile.currentHealth = playerProfile.baseHealth;
        }
        if (playerProfile.freeStatPoints < 0) {
            playerProfile.freeStatPoints = 0;
        }
        playerProfile.recalculateStats();
        return playerProfile;
    }

    public void updateProfileSync(PlayerProfile playerProfile) {
        playerDao.updateProfile(playerProfile);
        firebaseSyncManager.pushProfile(playerProfile);
    }

    public boolean isUserSignedIn() {
        return firebaseSyncManager.isSignedIn();
    }

    public void loginWithEmail(String email, String password, OnActionResult callback) {
        firebaseSyncManager.loginWithEmail(email, password, (success, message) -> {
            if (callback != null) {
                callback.onResult(success, message == null ? "" : message);
            }
        });
    }

    public void registerWithEmail(String email, String password, OnActionResult callback) {
        firebaseSyncManager.registerWithEmail(email, password, (success, message) -> {
            if (callback != null) {
                callback.onResult(success, message == null ? "" : message);
            }
        });
    }

    public void loginWithGoogleToken(String idToken, OnActionResult callback) {
        firebaseSyncManager.loginWithGoogleIdToken(idToken, (success, message) -> {
            if (callback != null) {
                callback.onResult(success, message == null ? "" : message);
            }
        });
    }

    public String generateRaidRoomCode() {
        return firebaseSyncManager.generateRoomCode();
    }

    public void createRaidRoomIfAbsent(String roomCode, String bossTitle, OnActionResult callback) {
        firebaseSyncManager.createRaidRoomIfAbsent(roomCode, bossTitle, (success, message) -> {
            if (callback != null) {
                callback.onResult(success, message == null ? "" : message);
            }
        });
    }

    public void joinRaidRoom(String roomCode, OnActionResult callback) {
        firebaseSyncManager.joinRaidRoom(roomCode, (success, message) -> {
            if (callback != null) {
                callback.onResult(success, message == null ? "" : message);
            }
        });
    }

    public void addRaidTask(Task task, OnActionResult callback) {
        runOnDiskIo(() -> {
            task.updatedAt = System.currentTimeMillis();
            task.raidTask = true;
            task.raidBossHpContribution = task.raidBossHpContribution <= 0 ? 1 : task.raidBossHpContribution;
            taskDao.insertTask(task);
            firebaseSyncManager.pushRaidTask(task, taskDao, (success, message) -> {
                if (callback != null) {
                    callback.onResult(success, message == null ? "" : message);
                }
            });
        });
    }

    public void completeRaidTask(Task task) {
        firebaseSyncManager.markRaidTaskCompleted(task);
    }

    /**
     * Returns a blocking achievement snapshot.
     *
     * @return achievement list
     */
    public List<Achievement> getAchievementsSync() {
        return achievementDao.getAllAchievementsSync();
    }

    /**
     * Returns the number of incomplete tasks.
     *
     * @return incomplete task count
     */
    public int getIncompleteTaskCountSync() {
        return taskDao.getIncompleteTaskCount();
    }

    public int getCompletedTaskCountSync() {
        return taskDao.getCompletedTaskCount();
    }

    public int getDungeonBossWinsSync() {
        return dungeonRunDao.getBossWinCountSync();
    }

    /**
     * Executes database work on the shared disk executor.
     *
     * @param runnable work to execute
     */
    public void runOnDiskIo(Runnable runnable) {
        diskExecutor.execute(runnable);
    }

    public void createHeroProfile(String heroName, int avatarChoice, String characterClass) {
        runOnDiskIo(() -> {
            PlayerProfile profile = getOrCreateProfileSync();
            profile.heroName = heroName == null ? "" : heroName.trim();
            profile.avatarChoice = avatarChoice;
            profile.characterClass = "ROOKIE_ADVENTURER";

            // Avatar choice is visual only; all profiles share the same base stats.
            profile.baseStrength = 10;
            profile.baseSpeed = 10;
            profile.baseHealth = 100;

            profile.currentHealth = profile.baseHealth;
            profile.recalculateStats();
            playerDao.updateProfile(profile);
            firebaseSyncManager.pushProfile(profile);
        });
    }

    public void awardBattleRewards(int xp, int coins, boolean healToFull) {
        runOnDiskIo(() -> {
            PlayerProfile profile = getOrCreateProfileSync();
            int oldLevel = Math.max(1, profile.level);
            profile.xp += Math.max(0, xp);
            profile.coins += Math.max(0, coins);
            int newLevel = new com.example.todoquest.domain.XPEngine().calculateLevel(profile.xp);
            if (newLevel > oldLevel) {
                profile.freeStatPoints += (newLevel - oldLevel) * 5;
            }
            profile.level = newLevel;
            if (healToFull) {
                profile.currentHealth = Math.max(1, profile.baseHealth);
            }
            playerDao.updateProfile(profile);
            firebaseSyncManager.pushProfile(profile);
        });
    }

    public void allocateStatPoints(int strengthPoints, int speedPoints, int healthPoints, OnActionResult callback) {
        runOnDiskIo(() -> {
            int str = Math.max(0, strengthPoints);
            int spd = Math.max(0, speedPoints);
            int hp = Math.max(0, healthPoints);
            int total = str + spd + hp;

            PlayerProfile profile = getOrCreateProfileSync();
            if (total <= 0) {
                if (callback != null) {
                    callback.onResult(false, "Assign at least 1 point.");
                }
                return;
            }
            if (total > profile.freeStatPoints) {
                if (callback != null) {
                    callback.onResult(false, "Not enough free stat points.");
                }
                return;
            }

            profile.baseStrength += str;
            profile.baseSpeed += spd;
            profile.baseHealth += hp * 5;
            profile.freeStatPoints -= total;
            profile.currentHealth = Math.min(profile.currentHealth + (hp * 5), profile.baseHealth);
            profile.recalculateStats();
            playerDao.updateProfile(profile);
            firebaseSyncManager.pushProfile(profile);

            if (callback != null) {
                callback.onResult(true, "Stats updated.");
            }
        });
    }

    public void awardDungeonVictory(int xp, int coins, int keys) {
        runOnDiskIo(() -> {
            PlayerProfile profile = getOrCreateProfileSync();
            profile.xp += Math.max(0, xp);
            profile.coins += Math.max(0, coins);
            profile.dungeonKeys += Math.max(0, keys);
            profile.dungeonWins += 1;
            playerDao.updateProfile(profile);
            firebaseSyncManager.pushProfile(profile);
        });
    }

    public void addDungeonRun(int xp, int coins, boolean bossDefeated) {
        runOnDiskIo(() -> {
            DungeonRun run = new DungeonRun();
            run.completedAt = System.currentTimeMillis();
            run.xpEarned = Math.max(0, xp);
            run.coinsEarned = Math.max(0, coins);
            run.bossDefeated = bossDefeated;
            dungeonRunDao.insert(run);
        });
    }

    public void addDungeonKeys(int amount) {
        runOnDiskIo(() -> {
            playerDao.addDungeonKeys(amount);
            firebaseSyncManager.pushProfile(getOrCreateProfileSync());
        });
    }

    public void consumeDungeonKey(OnBooleanResult callback) {
        runOnDiskIo(() -> {
            boolean consumed = playerDao.consumeDungeonKey() > 0;
            if (consumed) {
                firebaseSyncManager.pushProfile(getOrCreateProfileSync());
            }
            if (callback != null) {
                callback.onResult(consumed);
            }
        });
    }

    public void buyOrEquipSword(OnActionResult callback) {
        runOnDiskIo(() -> {
            PlayerProfile profile = getOrCreateProfileSync();
            boolean success;
            String message;
            if (!profile.ownsSword) {
                if (profile.coins < SWORD_COST) {
                    success = false;
                    message = "Not enough coins.";
                } else {
                    profile.coins -= SWORD_COST;
                    profile.ownsSword = true;
                    profile.equippedSword = true;
                    profile.recalculateStats();
                    playerDao.updateProfile(profile);
                    firebaseSyncManager.pushProfile(profile);
                    success = true;
                    message = "Fire Sword purchased.";
                }
            } else {
                profile.equippedSword = !profile.equippedSword;
                profile.recalculateStats();
                playerDao.updateProfile(profile);
                firebaseSyncManager.pushProfile(profile);
                success = true;
                message = profile.equippedSword ? "Fire Sword equipped." : "Fire Sword unequipped.";
            }

            if (callback != null) {
                callback.onResult(success, message);
            }
        });
    }

    public void buyOrEquipShield(OnActionResult callback) {
        runOnDiskIo(() -> {
            PlayerProfile profile = getOrCreateProfileSync();
            boolean success;
            String message;
            if (!profile.ownsShield) {
                if (profile.coins < SHIELD_COST) {
                    success = false;
                    message = "Not enough coins.";
                } else {
                    profile.coins -= SHIELD_COST;
                    profile.ownsShield = true;
                    profile.equippedShield = true;
                    profile.recalculateStats();
                    playerDao.updateProfile(profile);
                    firebaseSyncManager.pushProfile(profile);
                    success = true;
                    message = "Iron Shield purchased.";
                }
            } else {
                profile.equippedShield = !profile.equippedShield;
                profile.recalculateStats();
                playerDao.updateProfile(profile);
                firebaseSyncManager.pushProfile(profile);
                success = true;
                message = profile.equippedShield ? "Iron Shield equipped." : "Iron Shield unequipped.";
            }

            if (callback != null) {
                callback.onResult(success, message);
            }
        });
    }

    private void seedDefaultsIfNeeded() {
        runOnDiskIo(() -> {
            PlayerProfile existingProfile = playerDao.getProfileSync();
            if (existingProfile == null) {
                playerDao.insertProfile(createDefaultProfile());
            }

            List<Achievement> existingAchievements = achievementDao.getAllAchievementsSync();
            if (existingAchievements == null || existingAchievements.isEmpty()) {
                achievementDao.insertAll(buildDefaultAchievements());
                return;
            }

            Set<String> keys = new HashSet<>();
            for (Achievement achievement : existingAchievements) {
                if (achievement != null && achievement.key != null) {
                    keys.add(achievement.key);
                }
            }

            List<Achievement> missing = new ArrayList<>();
            for (Achievement candidate : buildDefaultAchievements()) {
                if (!keys.contains(candidate.key)) {
                    missing.add(candidate);
                }
            }

            if (!missing.isEmpty()) {
                achievementDao.insertAll(missing);
            }
        });
    }

    private PlayerProfile createDefaultProfile() {
        PlayerProfile initialProfile = new PlayerProfile();
        initialProfile.id = 1;
        initialProfile.xp = 0;
        initialProfile.level = 1;
        initialProfile.coins = 0;
        initialProfile.streakDays = 0;
        initialProfile.lastCompletedDate = 0L;
        initialProfile.tasksCompletedToday = 0;
        initialProfile.todayDate = LocalDate.now().toEpochDay();
        initialProfile.heroName = "";
        initialProfile.avatarChoice = 0;
        initialProfile.characterClass = "ROOKIE_ADVENTURER";
        initialProfile.baseStrength = 10;
        initialProfile.baseSpeed = 10;
        initialProfile.baseHealth = 100;
        initialProfile.currentHealth = 100;
        initialProfile.ownsSword = false;
        initialProfile.ownsShield = false;
        initialProfile.equippedSword = false;
        initialProfile.equippedShield = false;
        initialProfile.dungeonKeys = 0;
        initialProfile.lastDailyReset = 0L;
        initialProfile.dailyTaskCreated = false;
        initialProfile.dailyTaskCompleted = false;
        initialProfile.dungeonWins = 0;
        initialProfile.freeStatPoints = 0;
        initialProfile.recalculateStats();
        return initialProfile;
    }

    private List<Achievement> buildDefaultAchievements() {
        List<Achievement> defaults = new ArrayList<>();
        defaults.add(createAchievement(1, ACH_FIRST_BLOOD));
        defaults.add(createAchievement(2, ACH_ON_FIRE));
        defaults.add(createAchievement(3, ACH_PERFECTIONIST));
        defaults.add(createAchievement(4, ACH_EARLY_BIRD));
        defaults.add(createAchievement(5, ACH_DAILY_CHAMPION));
        defaults.add(createAchievement(6, ACH_DUNGEON_HUNTER));
        defaults.add(createAchievement(7, ACH_TREASURE_HUNTER));
        return defaults;
    }

    private Achievement createAchievement(int id, String key) {
        Achievement achievement = new Achievement();
        achievement.id = id;
        achievement.key = key;
        achievement.unlocked = false;
        achievement.unlockedAt = 0L;
        return achievement;
    }

    public interface OnActionResult {
        void onResult(boolean success, String message);
    }

    public interface OnBooleanResult {
        void onResult(boolean value);
    }
}
