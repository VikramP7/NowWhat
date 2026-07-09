package com.example.nowwhat

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Insert
    suspend fun insert(activity: Activity)

    @Query("UPDATE activities SET name = :name, colour = :colour WHERE id = :activityId")
    suspend fun updateActivity(name: String, colour: Int, activityId: Long)

    @Query("SELECT * FROM activities")
    fun getAll(): Flow<List<Activity>>

    @Query("SELECT * FROM activities WHERE id = :activityId LIMIT 1")
    suspend fun getById(activityId: Long): Activity?

    @Delete
    suspend fun delete(activity: Activity)
}