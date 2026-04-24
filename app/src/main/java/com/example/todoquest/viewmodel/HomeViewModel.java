package com.example.todoquest.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.todoquest.data.local.entity.PlayerProfile;
import com.example.todoquest.data.repository.TaskRepository;
import com.example.todoquest.domain.DailyQuestManager;

public class HomeViewModel extends AndroidViewModel {

    private final TaskRepository repository;
    private final DailyQuestManager dailyQuestManager;
    private final LiveData<PlayerProfile> profile;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        this.repository = new TaskRepository(application);
        this.dailyQuestManager = DailyQuestManager.getInstance(repository);
        this.profile = repository.getProfile();
    }

    /**
     * Returns the observable player profile for home UI stats.
     *
     * @return observable profile row
     */
    public LiveData<PlayerProfile> getProfile() {
        return profile;
    }

    public void refreshDailyState() {
        dailyQuestManager.resetDailyIfNeeded();
    }

    public void toggleSword(TaskRepository.OnActionResult callback) {
        repository.buyOrEquipSword(callback);
    }

    public void toggleShield(TaskRepository.OnActionResult callback) {
        repository.buyOrEquipShield(callback);
    }

    public void allocateStats(int strengthPoints, int speedPoints, int healthPoints,
                              TaskRepository.OnActionResult callback) {
        repository.allocateStatPoints(strengthPoints, speedPoints, healthPoints, callback);
    }
}
