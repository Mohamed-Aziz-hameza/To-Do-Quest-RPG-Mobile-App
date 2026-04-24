package com.example.todoquest.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.todoquest.data.local.entity.Achievement;
import com.example.todoquest.data.local.entity.PlayerProfile;
import com.example.todoquest.data.local.entity.Task;
import com.example.todoquest.data.repository.TaskRepository;
import com.example.todoquest.domain.DailyQuestManager;
import com.example.todoquest.domain.StreakTracker;
import com.example.todoquest.domain.XPEngine;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

public class TaskViewModel extends AndroidViewModel {

    private static final int EARLY_BIRD_CUTOFF_HOUR = 8;

    private final TaskRepository repository;
    private final XPEngine xpEngine;
    private final StreakTracker streakTracker;
    private final DailyQuestManager dailyQuestManager;
    private final LiveData<List<Task>> allTasks;

    public TaskViewModel(@NonNull Application application) {
        super(application);
        this.repository = new TaskRepository(application);
        this.xpEngine = new XPEngine();
        this.streakTracker = new StreakTracker();
        this.dailyQuestManager = DailyQuestManager.getInstance(repository);
        this.allTasks = repository.getAllTasks();
    }

    /**
     * Returns all tasks for list rendering.
     *
     * @return observable task list
     */
    public LiveData<List<Task>> getAllTasks() {
        return allTasks;
    }

    /**
     * Adds a task and applies defaults for timestamps and boss HP.
     *
     * @param task task to add
     */
    public void addTask(Task task) {
        if (task == null) {
            return;
        }

        if (task.createdAt == 0L) {
            task.createdAt = System.currentTimeMillis();
        }

        if (task.priority == null) {
            task.priority = "EASY";
        }

        repository.insertTask(task);
        dailyQuestManager.onTaskCreated();
    }

    public void addRaidTask(Task task, TaskRepository.OnActionResult callback) {
        if (task == null) {
            if (callback != null) {
                callback.onResult(false, "Invalid task.");
            }
            return;
        }
        if (task.createdAt == 0L) {
            task.createdAt = System.currentTimeMillis();
        }
        if (task.priority == null) {
            task.priority = "BOSS";
        }
        repository.addRaidTask(task, callback);
        dailyQuestManager.onTaskCreated();
    }

    public String generateRaidRoomCode() {
        return repository.generateRaidRoomCode();
    }

    public void createRaidRoom(String roomCode, String bossTitle, TaskRepository.OnActionResult callback) {
        repository.createRaidRoomIfAbsent(roomCode, bossTitle, callback);
    }

    public void joinRaidRoom(String roomCode, TaskRepository.OnActionResult callback) {
        repository.joinRaidRoom(roomCode, callback);
    }

    /**
     * Completes a task and applies XP, coins, streak, and achievement updates.
     *
     * @param task completed task
     */
    public void completeTask(Task task) {
        if (task == null || task.completed) {
            return;
        }

        if (task.raidTask) {
            repository.completeRaidTask(task);
        }

        repository.runOnDiskIo(() -> {
            task.completed = true;
            repository.updateTask(task);

            PlayerProfile profile = repository.getOrCreateProfileSync();
            resetDailyCounterIfNeeded(profile);
            dailyQuestManager.applyTaskCompletedRewards(profile);

            int previousLevel = Math.max(1, profile.level);
            profile.xp += xpEngine.getXpForPriority(task.priority);
            profile.coins += xpEngine.getCoinsForPriority(task.priority);
            int newLevel = xpEngine.calculateLevel(profile.xp);
            if (newLevel > previousLevel) {
                profile.freeStatPoints += (newLevel - previousLevel) * 5;
            }
            profile.level = newLevel;
            streakTracker.updateStreak(profile);
            profile.tasksCompletedToday += 1;
            profile.todayDate = LocalDate.now().toEpochDay();

            repository.updateProfileSync(profile);
            unlockAchievementsIfEligible(profile);
        });
    }

    /**
     * Deletes a task.
     *
     * @param task task to delete
     */
    public void deleteTask(Task task) {
        if (task == null) {
            return;
        }
        repository.deleteTask(task);
    }

    private void resetDailyCounterIfNeeded(PlayerProfile profile) {
        long currentEpochDay = LocalDate.now().toEpochDay();
        if (profile.todayDate != currentEpochDay) {
            profile.tasksCompletedToday = 0;
            profile.todayDate = currentEpochDay;
        }
    }

    private void unlockAchievementsIfEligible(PlayerProfile profile) {
        List<Achievement> achievements = repository.getAchievementsSync();
        if (achievements == null || achievements.isEmpty()) {
            return;
        }

        for (Achievement achievement : achievements) {
            if (achievement.unlocked || achievement.key == null) {
                continue;
            }

            if (!shouldUnlockAchievement(achievement.key, profile)) {
                continue;
            }

            achievement.unlocked = true;
            achievement.unlockedAt = System.currentTimeMillis();
            repository.updateAchievement(achievement);
        }
    }

    private boolean shouldUnlockAchievement(String key, PlayerProfile profile) {
        switch (key.toUpperCase(Locale.US)) {
            case TaskRepository.ACH_FIRST_BLOOD:
                return profile.xp > 0;
            case TaskRepository.ACH_ON_FIRE:
                return profile.tasksCompletedToday >= 5;
            case TaskRepository.ACH_PERFECTIONIST:
                return repository.getIncompleteTaskCountSync() == 0;
            case TaskRepository.ACH_EARLY_BIRD:
                return LocalDateTime.now().getHour() < EARLY_BIRD_CUTOFF_HOUR;
            case TaskRepository.ACH_DAILY_CHAMPION:
                return profile.dailyTaskCreated && profile.dailyTaskCompleted;
            case TaskRepository.ACH_DUNGEON_HUNTER:
                return profile.dungeonWins >= 1;
            case TaskRepository.ACH_TREASURE_HUNTER:
                return profile.coins >= 250;
            default:
                return false;
        }
    }
}
