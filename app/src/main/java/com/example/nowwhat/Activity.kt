package com.example.nowwhat

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activities")
data class Activity(
    val name: String,
    val colour: Int, // Hexadecimal (0xAARRGGBB) as Int
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
)