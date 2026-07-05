package com.example.nowwhat

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSlot(entry: ScheduleEntry)

    @Query("SELECT * FROM schedule_entries")
    fun getAll(): Flow<List<ScheduleEntry>>

    @Query("DELETE FROM schedule_entries WHERE dayOfWeek = :dayOfWeek AND hourOfDay = :hourOfDay")
    suspend fun clearSlot(dayOfWeek: Int, hourOfDay: Int)

    @Query("DELETE FROM schedule_entries")
    suspend fun deleteAll()
}