package com.example.todoquest.domain;

import com.example.todoquest.data.local.entity.PlayerProfile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public class StreakTracker {

    /**
     * Updates streak based on the profile's last completion date.
     *
     * @param profile player profile to mutate
     */
    public void updateStreak(PlayerProfile profile) {
        if (profile == null) {
            return;
        }

        long nowMs = System.currentTimeMillis();
        LocalDate today = LocalDate.now();

        if (profile.lastCompletedDate <= 0L) {
            profile.streakDays = 1;
            profile.lastCompletedDate = nowMs;
            return;
        }

        LocalDate lastDate = Instant.ofEpochMilli(profile.lastCompletedDate)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        if (lastDate.isEqual(today)) {
            profile.lastCompletedDate = nowMs;
            return;
        }

        if (lastDate.plusDays(1).isEqual(today)) {
            profile.streakDays = Math.max(1, profile.streakDays + 1);
        } else {
            profile.streakDays = 1;
        }

        profile.lastCompletedDate = nowMs;
    }
}

