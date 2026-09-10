package com.example.nowwhat

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [HourEntry::class, Activity::class, ScheduleEntry::class, Note::class],
    version = 4, exportSchema = true,
    autoMigrations = [AutoMigration(from = 3, to = 4)]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hourEntryDao(): HourEntryDao
    abstract fun activityDao(): ActivityDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun noteDao(): NoteDao
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nowwhat.db"
                ).fallbackToDestructiveMigration(true)
                    .build().also { INSTANCE = it }
            }
        }
    }
}