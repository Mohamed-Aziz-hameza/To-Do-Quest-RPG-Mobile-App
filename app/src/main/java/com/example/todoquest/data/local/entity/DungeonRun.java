package com.example.todoquest.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "dungeon_runs")
public class DungeonRun {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public long completedAt;
    public int xpEarned;
    public int coinsEarned;
    public boolean bossDefeated;
}

