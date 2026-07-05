package com.example.nowwhat

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "schedule_entries",
    indices = [Index(value = ["dayOfWeek", "hourOfDay"], unique = true)]
)
data class ScheduleEntry(
    val dayOfWeek: Int,           // 1–7, matches java.time DayOfWeek.value (Mon=1 … Sun=7)
    val hourOfDay: Int,           // 0–23, raw clock hour
    val plannedActivityId: Long?, // Activity ID
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
)

