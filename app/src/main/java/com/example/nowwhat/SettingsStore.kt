package com.example.nowwhat

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    private object Keys {
        val IS_24_HOUR = booleanPreferencesKey("is_24_hour")
        val DAY_START_HOUR = intPreferencesKey("day_start_hour")
    }

    //READ
    val is24Hour: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.IS_24_HOUR] ?: false
    }

    val dayStartHour: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.DAY_START_HOUR] ?: 6
    }

    // WRITE
    suspend fun setIs24Hour(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_24_HOUR] = value
        }
    }

    suspend fun setDayStartHour(value: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DAY_START_HOUR] = value
        }
    }
}