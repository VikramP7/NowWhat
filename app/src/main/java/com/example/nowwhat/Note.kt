package com.example.nowwhat

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey val epochDay: Long,
    val text: String,
)

