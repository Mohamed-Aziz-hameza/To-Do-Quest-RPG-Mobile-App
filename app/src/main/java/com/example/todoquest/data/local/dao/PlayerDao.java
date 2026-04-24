package com.example.todoquest.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.todoquest.data.local.entity.PlayerProfile;

@Dao
public interface PlayerDao {

    /**
     * Returns the single player profile row.
     *
     * @return observable profile
     */
    @Query("SELECT * FROM player_profile WHERE id = 1 LIMIT 1")
    LiveData<PlayerProfile> getProfile();

    /**
     * Returns the current profile snapshot.
     *
     * @return profile row or null
     */
    @Query("SELECT * FROM player_profile WHERE id = 1 LIMIT 1")
    PlayerProfile getProfileSync();

    /**
     * Updates the profile row.
     *
     * @param profile profile to update
     */
    @Update
    void updateProfile(PlayerProfile profile);

    /**
     * Inserts the single profile row.
     *
     * @param profile profile to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertProfile(PlayerProfile profile);

    /**
     * Adds the specified amount of dungeon keys to the player's profile.
     *
     * @param amount the amount of dungeon keys to add
     */
    @Query("UPDATE player_profile SET dungeonKeys = dungeonKeys + :amount WHERE id = 1")
    void addDungeonKeys(int amount);

    /**
     * Spends the specified amount of coins from the player's profile if enough coins are available.
     *
     * @param cost the amount of coins to spend
     * @return the number of rows updated (1 if successful, 0 if not enough coins)
     */
    @Query("UPDATE player_profile SET coins = coins - :cost WHERE id = 1 AND coins >= :cost")
    int spendCoinsIfEnough(int cost);

    /**
     * Consumes a dungeon key from the player's profile if available.
     *
     * @return the number of rows updated (1 if a key was consumed, 0 if no keys were available)
     */
    @Query("UPDATE player_profile SET dungeonKeys = dungeonKeys - 1 WHERE id = 1 AND dungeonKeys > 0")
    int consumeDungeonKey();
}
