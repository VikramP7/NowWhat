package com.example.nowwhat

suspend fun saveTrimNote(
    epochDay: Long,
    text: String,
    noteDao: NoteDao
){
    val textTrim = text.trim()
    if(textTrim.isNotEmpty()){
        noteDao.upsert(Note(epochDay,textTrim))
    }else{
        noteDao.deleteNote(epochDay)
    }
}