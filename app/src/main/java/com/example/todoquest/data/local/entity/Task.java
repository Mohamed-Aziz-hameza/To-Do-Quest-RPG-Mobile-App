package com.example.todoquest.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class Task {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String description;
    public String priority;
    public boolean completed;
    public long createdAt;
    public long dueDate;
    public String subTasks;
    public int bossHpTotal;
    public int bossHpRemaining;

    public String remoteId;
    public String ownerUid;
    public String assignedTo;
    public String roomCode;
    public boolean raidTask;
    public int raidBossHpContribution;
    public long updatedAt;
}

