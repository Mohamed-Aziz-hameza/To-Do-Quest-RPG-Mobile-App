package com.example.todoquest.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "player_profile")
public class PlayerProfile {

    @PrimaryKey
    public int id = 1;

    public int xp;
    public int level;
    public int coins;
    public int streakDays;
    public long lastCompletedDate;
    public int tasksCompletedToday;
    public long todayDate;

    public String heroName;
    public int avatarChoice;
    public String characterClass;

    public int baseStrength;
    public int baseSpeed;
    public int baseHealth;
    public int currentHealth;

    public int effectiveAttack;
    public int effectiveDefense;
    public int effectiveSpeed;

    public boolean ownsSword;
    public boolean ownsShield;
    public boolean equippedSword;
    public boolean equippedShield;

    public int dungeonKeys;
    public long lastDailyReset;
    public boolean dailyTaskCreated;
    public boolean dailyTaskCompleted;

    public int dungeonWins;
    public int freeStatPoints;

    public void recalculateStats() {
        effectiveAttack = baseStrength + (equippedSword ? 18 : 0);
        effectiveDefense = 5 + (equippedShield ? 22 : 0);
        effectiveSpeed = baseSpeed + (equippedSword ? 3 : 0) - (equippedShield ? 1 : 0);
    }
}
