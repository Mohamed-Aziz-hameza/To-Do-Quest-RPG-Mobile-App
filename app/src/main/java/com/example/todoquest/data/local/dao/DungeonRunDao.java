package com.example.todoquest.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.todoquest.data.local.entity.DungeonRun;

import java.util.List;

@Dao
public interface DungeonRunDao {

    @Insert
    void insert(DungeonRun run);

    @Query("SELECT * FROM dungeon_runs ORDER BY completedAt DESC")
    LiveData<List<DungeonRun>> getAllRuns();

    @Query("SELECT COUNT(*) FROM dungeon_runs WHERE bossDefeated = 1")
    int getBossWinCountSync();
}

