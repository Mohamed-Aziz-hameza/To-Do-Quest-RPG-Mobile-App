package com.example.todoquest.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.todoquest.data.local.dao.AchievementDao;
import com.example.todoquest.data.local.dao.DungeonRunDao;
import com.example.todoquest.data.local.dao.PlayerDao;
import com.example.todoquest.data.local.dao.TaskDao;
import com.example.todoquest.data.local.entity.Achievement;
import com.example.todoquest.data.local.entity.DungeonRun;
import com.example.todoquest.data.local.entity.PlayerProfile;
import com.example.todoquest.data.local.entity.Task;

@Database(
        entities = {Task.class, PlayerProfile.class, Achievement.class, DungeonRun.class},
        version = 5,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DB_NAME = "todoquest.db";
    private static volatile AppDatabase instance;

    /**
     * Returns the task DAO.
     *
     * @return task DAO
     */
    public abstract TaskDao taskDao();

    /**
     * Returns the player DAO.
     *
     * @return player DAO
     */
    public abstract PlayerDao playerDao();

    /**
     * Returns the achievement DAO.
     *
     * @return achievement DAO
     */
    public abstract AchievementDao achievementDao();

    public abstract DungeonRunDao dungeonRunDao();

    /**
     * Returns the singleton Room database instance.
     *
     * @param context application context
     * @return database singleton
     */
    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DB_NAME
                            )
                            // Defensive startup: if an old local schema is incompatible, rebuild DB.
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}
