package com.example.todoquest.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.todoquest.data.local.entity.Achievement;

import java.util.List;

@Dao
public interface AchievementDao {

    /**
     * Returns all achievements ordered by id.
     *
     * @return observable achievement list
     */
    @Query("SELECT * FROM achievements ORDER BY id ASC")
    LiveData<List<Achievement>> getAllAchievements();

    /**
     * Returns all achievements as a blocking snapshot.
     *
     * @return achievement list snapshot
     */
    @Query("SELECT * FROM achievements ORDER BY id ASC")
    List<Achievement> getAllAchievementsSync();

    /**
     * Updates one achievement row.
     *
     * @param achievement achievement to update
     */
    @Update
    void updateAchievement(Achievement achievement);

    /**
     * Inserts all achievement rows.
     *
     * @param achievements achievement rows to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Achievement> achievements);
}

