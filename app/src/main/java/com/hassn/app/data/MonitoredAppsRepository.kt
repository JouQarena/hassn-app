package com.hassn.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MonitoredAppsRepository(private val dataStore: DataStore<Preferences>) {

    fun getAllMonitoredApps(): Flow<List<MonitoredApp>> = dataStore.data.map { prefs ->
        prefs[KEY_MONITORED_APPS]?.let { json -> decodeList(json) } ?: emptyList()
    }

    suspend fun getMonitoredApp(packageName: String): MonitoredApp? =
        getAllMonitoredApps().first().find { it.packageName == packageName }

    suspend fun addMonitoredApp(app: MonitoredApp) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_MONITORED_APPS]?.let { decodeList(it) } ?: emptyList()
            if (current.none { it.packageName == app.packageName }) {
                prefs[KEY_MONITORED_APPS] = Json.encodeToString(current + app)
            }
        }
    }

    suspend fun updateMonitoredApp(app: MonitoredApp) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_MONITORED_APPS]?.let { decodeList(it) } ?: emptyList()
            val index = current.indexOfFirst { it.packageName == app.packageName }
            if (index >= 0) {
                val updated = current.toMutableList().also { it[index] = app }
                prefs[KEY_MONITORED_APPS] = Json.encodeToString(updated)
            }
        }
    }

    suspend fun removeMonitoredApp(packageName: String) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_MONITORED_APPS]?.let { decodeList(it) } ?: emptyList()
            prefs[KEY_MONITORED_APPS] =
                Json.encodeToString(current.filterNot { it.packageName == packageName })
        }
    }

    private fun decodeList(json: String): List<MonitoredApp> =
        try {
            Json.decodeFromString<List<MonitoredApp>>(json)
        } catch (e: Exception) {
            emptyList()
        }

    companion object {
        private val KEY_MONITORED_APPS = stringPreferencesKey("monitored_apps")
    }
}
