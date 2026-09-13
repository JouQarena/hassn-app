package com.hassn.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class ChallengeRepository(private val dataStore: DataStore<Preferences>) {

    /** Never throws: falls back to empty prefs if the file is corrupted. */
    private val safeData: Flow<Preferences> =
        dataStore.data.catch { emit(emptyPreferences()) }

    fun getAttemptCount(): Flow<Int> = safeData.map { prefs ->
        prefs[KEY_ATTEMPTS] ?: 0
    }

    suspend fun incrementAttemptCount() {
        dataStore.edit { prefs ->
            prefs[KEY_ATTEMPTS] = (prefs[KEY_ATTEMPTS] ?: 0) + 1
        }
    }

    suspend fun resetAttemptCount() {
        dataStore.edit { prefs ->
            prefs[KEY_ATTEMPTS] = 0
        }
    }

    fun isLocked(): Flow<Boolean> = safeData.map { prefs ->
        val expiry = prefs[KEY_LOCK_EXPIRY] ?: 0L
        System.currentTimeMillis() < expiry
    }

    suspend fun setLock(durationMinutes: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_LOCK_EXPIRY] = System.currentTimeMillis() + durationMinutes * 60_000L
        }
    }

    companion object {
        private val KEY_ATTEMPTS = intPreferencesKey("disable_attempts_count")
        private val KEY_LOCK_EXPIRY = longPreferencesKey("disable_lock_expiry")
    }
}
