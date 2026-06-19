package com.example.nowwhat

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hour_entries")
data class HourEntry(
    val timestamp: Long,
    val didWhat: String,
    val nowWhat: String,
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
)