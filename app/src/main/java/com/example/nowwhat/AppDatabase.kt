package com.example.nowwhat

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [HourEntry::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hourEntryDao(): HourEntryDao
}