package com.hassn.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hassn.app.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SettingsDataStore(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = "hassn_settings"
        )
        val KEY_MONITORING_ENABLED = booleanPreferencesKey(Constants.PREF_MONITORING_ENABLED)
        val KEY_TARGET_APP_PACKAGE = stringPreferencesKey(Constants.PREF_TARGET_APP_PACKAGE)
        val KEY_TARGET_APP_NAME = stringPreferencesKey(Constants.PREF_TARGET_APP_NAME)
        val KEY_DISABLE_ATTEMPTS = longPreferencesKey(Constants.PREF_DISABLE_ATTEMPTS)
        val KEY_DISABLE_FAILS = longPreferencesKey(Constants.PREF_DISABLE_FAILS)
        val KEY_LAST_DISABLE_TS = longPreferencesKey(Constants.PREF_LAST_DISABLE_TS)
        val KEY_LOCKOUT_UNTIL = longPreferencesKey(Constants.PREF_LOCKOUT_UNTIL)
        val KEY_MONITORED_APPS = stringPreferencesKey(Constants.PREF_MONITORED_APPS)
        val KEY_APP_LANGUAGE = stringPreferencesKey(Constants.PREF_APP_LANGUAGE)
        val KEY_FIRST_LAUNCH = booleanPreferencesKey(Constants.PREF_FIRST_LAUNCH)
    }

    val monitoringEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_MONITORING_ENABLED] ?: false }
    val targetAppPackage: Flow<String?> = context.dataStore.data.map { it[KEY_TARGET_APP_PACKAGE] }
    val targetAppName: Flow<String?> = context.dataStore.data.map { it[KEY_TARGET_APP_NAME] }
    val disableAttempts: Flow<Long> = context.dataStore.data.map { it[KEY_DISABLE_ATTEMPTS] ?: 0L }
    val disableFails: Flow<Long> = context.dataStore.data.map { it[KEY_DISABLE_FAILS] ?: 0L }
    val lastDisableTs: Flow<Long> = context.dataStore.data.map { it[KEY_LAST_DISABLE_TS] ?: 0L }
    val lockoutUntil: Flow<Long> = context.dataStore.data.map { it[KEY_LOCKOUT_UNTIL] ?: 0L }

    val monitoredApps: Flow<List<MonitoredApp>> = context.dataStore.data.map { prefs ->
        MonitoredApp.listFromJson(prefs[KEY_MONITORED_APPS])
    }

    val appLanguage: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_APP_LANGUAGE] ?: "ar"
    }

    val firstLaunchDone: Flow<Boolean> = context.dataStore.data.map { it[KEY_FIRST_LAUNCH] ?: false }

    suspend fun setMonitoringEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_MONITORING_ENABLED] = enabled }
    }

    suspend fun setTargetApp(packageName: String, appName: String) {
        context.dataStore.edit {
            it[KEY_TARGET_APP_PACKAGE] = packageName
            it[KEY_TARGET_APP_NAME] = appName
        }
    }

    suspend fun clearTargetApp() {
        context.dataStore.edit {
            it.remove(KEY_TARGET_APP_PACKAGE)
            it.remove(KEY_TARGET_APP_NAME)
        }
    }

    suspend fun setMonitoredApps(apps: List<MonitoredApp>) {
        context.dataStore.edit {
            it[KEY_MONITORED_APPS] = MonitoredApp.listToJson(apps)
        }
    }

    suspend fun addMonitoredApp(app: MonitoredApp) {
        context.dataStore.edit { prefs ->
            val current = MonitoredApp.listFromJson(prefs[KEY_MONITORED_APPS]).toMutableList()
            if (current.none { it.packageName == app.packageName }) {
                current.add(app)
                prefs[KEY_MONITORED_APPS] = MonitoredApp.listToJson(current)
            }
        }
    }

    suspend fun removeMonitoredApp(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = MonitoredApp.listFromJson(prefs[KEY_MONITORED_APPS])
            val filtered = current.filterNot { it.packageName == packageName }
            prefs[KEY_MONITORED_APPS] = MonitoredApp.listToJson(filtered)
        }
    }

    suspend fun updateMonitoredAppMode(packageName: String, mode: String) {
        context.dataStore.edit { prefs ->
            val current = MonitoredApp.listFromJson(prefs[KEY_MONITORED_APPS])
            val updated = current.map { if (it.packageName == packageName) it.copy(mode = mode) else it }
            prefs[KEY_MONITORED_APPS] = MonitoredApp.listToJson(updated)
        }
    }

    suspend fun setAppLanguage(lang: String) {
        context.dataStore.edit { it[KEY_APP_LANGUAGE] = lang }
    }

    suspend fun setFirstLaunchDone(done: Boolean) {
        context.dataStore.edit { it[KEY_FIRST_LAUNCH] = done }
    }

    suspend fun recordDisableAttempt() {
        context.dataStore.edit {
            it[KEY_DISABLE_ATTEMPTS] = (it[KEY_DISABLE_ATTEMPTS] ?: 0L) + 1L
            it[KEY_LAST_DISABLE_TS] = System.currentTimeMillis()
        }
    }

    suspend fun recordDisableFail() {
        context.dataStore.edit { it[KEY_DISABLE_FAILS] = (it[KEY_DISABLE_FAILS] ?: 0L) + 1L }
    }

    suspend fun setLockoutUntil(untilMillis: Long) {
        context.dataStore.edit {
            if (untilMillis <= 0L) it.remove(KEY_LOCKOUT_UNTIL)
            else it[KEY_LOCKOUT_UNTIL] = untilMillis
        }
    }

    suspend fun maybeResetStaleFailCounter(nowMillis: Long = System.currentTimeMillis()) {
        context.dataStore.edit { prefs ->
            val last = prefs[KEY_LAST_DISABLE_TS] ?: 0L
            val stale = last == 0L || (nowMillis - last) > Constants.LOCKOUT_RESET_AFTER_SECONDS * 1000L
            if (stale && (prefs[KEY_DISABLE_FAILS] ?: 0L) > 0L) {
                prefs[KEY_DISABLE_FAILS] = 0L
                prefs.remove(KEY_LOCKOUT_UNTIL)
            }
        }
    }

    suspend fun clearFailures() {
        context.dataStore.edit {
            it[KEY_DISABLE_FAILS] = 0L
            it.remove(KEY_LOCKOUT_UNTIL)
        }
    }

    fun disableAttemptsAsSnapshot(): Long = runBlockingRead { it[KEY_DISABLE_ATTEMPTS] ?: 0L }
    fun disableFailsAsSnapshot(): Long = runBlockingRead { it[KEY_DISABLE_FAILS] ?: 0L }
    fun lockoutUntilAsSnapshot(): Long = runBlockingRead { it[KEY_LOCKOUT_UNTIL] ?: 0L }
    fun monitoredAppsSnapshot(): List<MonitoredApp> = runBlockingRead { MonitoredApp.listFromJson(it[KEY_MONITORED_APPS]) }

    private fun <T> runBlockingRead(block: (Preferences) -> T): T =
        kotlinx.coroutines.runBlocking { context.dataStore.data.first() }.let(block)
}
