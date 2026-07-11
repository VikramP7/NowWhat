package com.example.nowwhat

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HourEntryDao {
    @Insert
    suspend fun insert(entry: HourEntry)

    @Query("SELECT * FROM hour_entries WHERE timestamp = :timestamp LIMIT 1")
    suspend fun getByTimestamp(timestamp: Long): HourEntry?

    @Query("UPDATE hour_entries SET plannedActivityId = :activityId WHERE timestamp = :timestamp")
    suspend fun updatePlanned(timestamp: Long, activityId: Long)

    @Query("UPDATE hour_entries SET actualActivityId = :activityId WHERE timestamp = :timestamp")
    suspend fun updateActual(timestamp: Long, activityId: Long)

    @Query("SELECT * FROM hour_entries ORDER BY timestamp DESC")
    fun getAll(): Flow<List<HourEntry>>

    @Query("SELECT * FROM hour_entries WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getSince(since: Long): Flow<List<HourEntry>>

    @Query("DELETE FROM hour_entries")
    suspend fun deleteAll()
}