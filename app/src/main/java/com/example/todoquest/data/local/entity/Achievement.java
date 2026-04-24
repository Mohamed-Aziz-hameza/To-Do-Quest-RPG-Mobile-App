package com.example.todoquest.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "achievements")
public class Achievement {

    @PrimaryKey
    public int id;

    public String key;
    public boolean unlocked;
    public long unlockedAt;
}

