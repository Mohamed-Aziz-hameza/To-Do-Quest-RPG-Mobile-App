package com.example.todoquest.domain;

import com.example.todoquest.data.local.entity.PlayerProfile;
import com.example.todoquest.data.repository.TaskRepository;

import java.time.LocalDate;

public class DailyQuestManager {

    private static final int REWARD_XP_CREATED = 15;
    private static final int REWARD_COINS_CREATED = 10;
    private static final int REWARD_XP_COMPLETED = 25;
    private static final int REWARD_COINS_COMPLETED = 15;
    private static final int BONUS_XP_DUNGEON_READY = 30;
    private static final int BONUS_COINS_DUNGEON_READY = 20;

    private static volatile DailyQuestManager instance;

    private final TaskRepository repository;
    private final XPEngine xpEngine = new XPEngine();

    private DailyQuestManager(TaskRepository repository) {
        this.repository = repository;
    }

    public static DailyQuestManager getInstance(TaskRepository repository) {
        if (instance == null) {
            synchronized (DailyQuestManager.class) {
                if (instance == null) {
                    instance = new DailyQuestManager(repository);
                }
            }
        }
        return instance;
    }

    public void onTaskCreated() {
        repository.runOnDiskIo(() -> {
            PlayerProfile profile = repository.getOrCreateProfileSync();
            if (applyTaskCreatedRewards(profile)) {
                repository.updateProfileSync(profile);
            }
        });
    }

    public void onTaskCompleted() {
        repository.runOnDiskIo(() -> {
            PlayerProfile profile = repository.getOrCreateProfileSync();
            if (applyTaskCompletedRewards(profile)) {
                repository.updateProfileSync(profile);
            }
        });
    }

    // Use this from existing background work to avoid race conditions.
    public boolean applyTaskCreatedRewards(PlayerProfile profile) {
        resetIfNewDay(profile);
        if (profile.dailyTaskCreated) {
            return false;
        }

        profile.dailyTaskCreated = true;
        profile.dungeonKeys += 1;
        int oldLevel = Math.max(1, profile.level);
        profile.xp += REWARD_XP_CREATED;
        profile.coins += REWARD_COINS_CREATED;

        if (profile.dailyTaskCompleted) {
            profile.dungeonKeys += 1;
            profile.xp += BONUS_XP_DUNGEON_READY;
            profile.coins += BONUS_COINS_DUNGEON_READY;
        }
        int newLevel = xpEngine.calculateLevel(profile.xp);
        if (newLevel > oldLevel) {
            profile.freeStatPoints += (newLevel - oldLevel) * 5;
        }
        profile.level = newLevel;
        return true;
    }

    // Use this from existing background work to avoid race conditions.
    public boolean applyTaskCompletedRewards(PlayerProfile profile) {
        resetIfNewDay(profile);
        if (profile.dailyTaskCompleted) {
            return false;
        }

        profile.dailyTaskCompleted = true;
        profile.dungeonKeys += 1;
        int oldLevel = Math.max(1, profile.level);
        profile.xp += REWARD_XP_COMPLETED;
        profile.coins += REWARD_COINS_COMPLETED;

        if (profile.dailyTaskCreated) {
            profile.dungeonKeys += 1;
            profile.xp += BONUS_XP_DUNGEON_READY;
            profile.coins += BONUS_COINS_DUNGEON_READY;
        }
        int newLevel = xpEngine.calculateLevel(profile.xp);
        if (newLevel > oldLevel) {
            profile.freeStatPoints += (newLevel - oldLevel) * 5;
        }
        profile.level = newLevel;
        return true;
    }

    public void resetDailyIfNeeded() {
        repository.runOnDiskIo(() -> {
            PlayerProfile profile = repository.getOrCreateProfileSync();
            if (resetIfNewDay(profile)) {
                repository.updateProfileSync(profile);
            }
        });
    }

    private boolean resetIfNewDay(PlayerProfile profile) {
        long today = LocalDate.now().toEpochDay();
        if (profile.lastDailyReset == today) {
            return false;
        }
        profile.lastDailyReset = today;
        profile.dailyTaskCreated = false;
        profile.dailyTaskCompleted = false;
        profile.tasksCompletedToday = 0;
        profile.todayDate = today;
        return true;
    }
}
