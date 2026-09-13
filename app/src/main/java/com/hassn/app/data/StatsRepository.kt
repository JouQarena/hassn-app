package com.hassn.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Lightweight usage stats — shown on the "remove Hassn" review screen.
 * (Part of the optional uninstall-protection feature, implemented without
 * Device Admin to avoid Play policy friction.)
 */
class StatsRepository(private val dataStore: DataStore<Preferences>) {

    companion object {
        private val INSTALL_DATE_KEY = longPreferencesKey("install_date")
        private val TOTAL_REDIRECTIONS_KEY = intPreferencesKey("total_redirections")
        private val CHALLENGES_COMPLETED_KEY = intPreferencesKey("challenges_completed")
        private val CHALLENGES_FAILED_KEY = intPreferencesKey("challenges_failed")
    }

    suspend fun initialize() {
        dataStore.edit { prefs ->
            if (!prefs.contains(INSTALL_DATE_KEY)) {
                prefs[INSTALL_DATE_KEY] = System.currentTimeMillis()
            }
        }
    }

    suspend fun recordRedirection() {
        dataStore.edit { prefs ->
            val current = prefs[TOTAL_REDIRECTIONS_KEY] ?: 0
            prefs[TOTAL_REDIRECTIONS_KEY] = current + 1
        }
    }

    suspend fun recordChallengeResult(success: Boolean) {
        dataStore.edit { prefs ->
            val key = if (success) CHALLENGES_COMPLETED_KEY else CHALLENGES_FAILED_KEY
            val current = prefs[key] ?: 0
            prefs[key] = current + 1
        }
    }

    fun getStats(): Flow<UsageStats> = dataStore.data.map { prefs ->
        val installDate = prefs[INSTALL_DATE_KEY] ?: System.currentTimeMillis()
        val daysUsed = ((System.currentTimeMillis() - installDate) / (1000L * 60 * 60 * 24)).toInt()
        val totalRedirections = prefs[TOTAL_REDIRECTIONS_KEY] ?: 0
        val challengesCompleted = prefs[CHALLENGES_COMPLETED_KEY] ?: 0
        val challengesFailed = prefs[CHALLENGES_FAILED_KEY] ?: 0
        val hoursSaved = (totalRedirections * 5) / 60 // Assume ~5 min saved per redirection

        UsageStats(
            daysUsed = daysUsed.coerceAtLeast(0),
            totalRedirections = totalRedirections,
            challengesCompleted = challengesCompleted,
            challengesFailed = challengesFailed,
            hoursSaved = hoursSaved
        )
    }

    data class UsageStats(
        val daysUsed: Int,
        val totalRedirections: Int,
        val challengesCompleted: Int,
        val challengesFailed: Int,
        val hoursSaved: Int
    )
}
