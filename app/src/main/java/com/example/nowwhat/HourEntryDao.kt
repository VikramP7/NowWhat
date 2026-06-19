package com.example.nowwhat

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HourEntryDao {
    @Insert
    suspend fun insert(entry: HourEntry)

    @Query("SELECT * FROM hour_entries ORDER BY timestamp DESC")
    fun getAll(): Flow<List<HourEntry>>

    @Query("SELECT * FROM hour_entries WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getSince(since: Long): Flow<List<HourEntry>>
}