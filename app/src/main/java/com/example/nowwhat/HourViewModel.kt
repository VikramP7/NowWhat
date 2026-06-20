package com.example.nowwhat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class HourViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).hourEntryDao()
    public val entries: Flow<List<HourEntry>> = dao.getAll()

    public fun addEntry(didWhat: String, nowWhat: String) {
        viewModelScope.launch {
            val entry = HourEntry(
                timestamp = System.currentTimeMillis(),
                didWhat=didWhat,
                nowWhat=nowWhat
            )
            dao.insert(entry)
        }
    }
}