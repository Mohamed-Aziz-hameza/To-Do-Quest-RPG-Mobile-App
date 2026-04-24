package com.example.todoquest.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.todoquest.data.local.entity.Achievement;
import com.example.todoquest.data.repository.TaskRepository;

import java.util.List;

public class AchievementsViewModel extends AndroidViewModel {

    private final LiveData<List<Achievement>> achievements;

    /**
     * Creates a view model that exposes achievement data.
     *
     * @param application application context
     */
    public AchievementsViewModel(@NonNull Application application) {
        super(application);
        TaskRepository repository = new TaskRepository(application);
        this.achievements = repository.getAllAchievements();
    }

    /** Returns all achievements as observable data. */
    public LiveData<List<Achievement>> getAchievements() {
        return achievements;
    }
}

