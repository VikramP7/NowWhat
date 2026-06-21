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

    @Query("SELECT * FROM activities")
    fun getAll(): Flow<List<Activity>>

    @Delete
    suspend fun delete(activity: Activity)
}