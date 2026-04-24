package com.example.todoquest.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.todoquest.data.local.entity.Task;

import java.util.List;

@Dao
public interface TaskDao {

    /**
     * Inserts a new task row.
     *
     * @param task task to insert
     * @return inserted row id
     */
    @Insert
    long insertTask(Task task);

    /**
     * Updates an existing task row.
     *
     * @param task task to update
     */
    @Update
    void updateTask(Task task);

    /**
     * Deletes a task row.
     *
     * @param task task to delete
     */
    @Delete
    void deleteTask(Task task);

    /**
     * Returns all tasks sorted by priority groups and latest-first creation time.
     *
     * @return observable task list
     */
    @Query("SELECT * FROM tasks ORDER BY CASE priority WHEN 'BOSS' THEN 0 WHEN 'HARD' THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END, createdAt DESC")
    LiveData<List<Task>> getAllTasks();

    @Query("SELECT * FROM tasks")
    List<Task> getAllTasksSync();

    @Query("SELECT * FROM tasks WHERE remoteId = :remoteId LIMIT 1")
    Task getTaskByRemoteIdSync(String remoteId);

    /**
     * Returns one task by id.
     *
     * @param taskId task id
     * @return observable task row
     */
    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    LiveData<Task> getTaskById(int taskId);

    /**
     * Counts completed tasks for the provided time window.
     *
     * @param dayStartMs inclusive range start epoch millis
     * @param dayEndMs inclusive range end epoch millis
     * @return number of completed tasks
     */
    @Query("SELECT COUNT(*) FROM tasks WHERE completed = 1 AND createdAt BETWEEN :dayStartMs AND :dayEndMs")
    int getCompletedTasksToday(long dayStartMs, long dayEndMs);

    /**
     * Counts tasks that are not completed.
     *
     * @return number of incomplete tasks
     */
    @Query("SELECT COUNT(*) FROM tasks WHERE completed = 0")
    int getIncompleteTaskCount();

    @Query("SELECT COUNT(*) FROM tasks WHERE completed = 1")
    int getCompletedTaskCount();
}

