package com.pck.nex.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "nex_prefs")

object PrefsKeys {
    // Minutes since midnight local, e.g., 21:30 = 21*60 + 30
    val PLAN_REMINDER_MIN = intPreferencesKey("plan_reminder_min")
    // Enable/disable the daily planning reminder
    val PLAN_REMINDER_ENABLED = intPreferencesKey("plan_reminder_enabled")
}

class PrefsDataStore(private val context: Context) {
    val reminderMinuteOfDay = context.dataStore.data.map { prefs ->
        prefs[PrefsKeys.PLAN_REMINDER_MIN] ?: (21 * 60) // default 9:00 PM
    }
    val reminderEnabled = context.dataStore.data.map { prefs ->
        prefs[PrefsKeys.PLAN_REMINDER_ENABLED] ?: 1 // 1 = enabled, 0 = disabled
    }

    suspend fun setReminderMinuteOfDay(minute: Int) {
        context.dataStore.edit { it[PrefsKeys.PLAN_REMINDER_MIN] = minute }
    }
    suspend fun setReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PrefsKeys.PLAN_REMINDER_ENABLED] = if (enabled) 1 else 0 }
    }
}
