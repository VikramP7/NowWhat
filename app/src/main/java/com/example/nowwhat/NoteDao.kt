package com.example.nowwhat

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Upsert
    suspend fun upsert(note: Note)

    @Insert
    suspend fun insertAll(notes: List<Note>)

    @Query("SELECT * FROM notes WHERE epochDay = :epochDay LIMIT 1")
    suspend fun getByEpochDay(epochDay: Long): Note?

    @Query("SELECT * FROM notes ORDER BY epochDay DESC")
    fun getAll(): Flow<List<Note>>

    @Query("DELETE FROM notes WHERE epochDay = :epochDay")
    suspend fun deleteNote(epochDay: Long)

    @Query("DELETE FROM notes")
    suspend fun deleteAll()

}