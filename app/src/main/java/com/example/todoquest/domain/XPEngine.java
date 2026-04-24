package com.example.todoquest.domain;

import java.util.Locale;

public class XPEngine {

    /**
     * Returns XP earned for a task priority tier.
     *
     * @param priority task priority label
     * @return XP reward
     */
    public int getXpForPriority(String priority) {
        if (priority == null) {
            return 0;
        }
        switch (priority.toUpperCase(Locale.US)) {
            case "EASY":
                return 10;
            case "MEDIUM":
                return 25;
            case "HARD":
                return 50;
            case "BOSS":
                return 100;
            default:
                return 0;
        }
    }

    /**
     * Returns coins earned for a task priority tier.
     *
     * @param priority task priority label
     * @return coin reward
     */
    public int getCoinsForPriority(String priority) {
        if (priority == null) {
            return 0;
        }
        switch (priority.toUpperCase(Locale.US)) {
            case "EASY":
                return 5;
            case "MEDIUM":
                return 12;
            case "HARD":
                return 25;
            case "BOSS":
                return 50;
            default:
                return 0;
        }
    }

    /**
     * Calculates player level from total XP.
     *
     * @param totalXp cumulative XP
     * @return level, at least 1
     */
    public int calculateLevel(int totalXp) {
        int safeXp = Math.max(0, totalXp);
        int level = 1;
        int remainingXp = safeXp;

        while (remainingXp >= getXpForNextLevel(level)) {
            remainingXp -= getXpForNextLevel(level);
            level++;
        }

        return level;
    }

    /**
     * Returns XP needed to advance from the given level.
     *
     * @param level current level
     * @return XP requirement for next level
     */
    public int getXpForNextLevel(int level) {
        return Math.max(1, level) * 100;
    }

    /**
     * Returns XP progress within the player's current level.
     *
     * @param totalXp cumulative XP
     * @return XP earned inside current level band
     */
    public int getXpProgressInCurrentLevel(int totalXp) {
        int safeXp = Math.max(0, totalXp);
        int level = 1;
        int remainingXp = safeXp;

        while (remainingXp >= getXpForNextLevel(level)) {
            remainingXp -= getXpForNextLevel(level);
            level++;
        }

        return remainingXp;
    }
}

