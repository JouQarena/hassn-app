package com.hassn.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.hassn.app.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    // Protection enabled
    val protectionEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_PROTECTION] ?: false
    }

    suspend fun setProtectionEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_PROTECTION] = enabled }
    }

    // Selected behaviors (default: redirect only)
    val selectedBehaviors: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[KEY_SELECTED_BEHAVIORS] ?: Constants.DEFAULT_BEHAVIORS
    }

    suspend fun setSelectedBehaviors(behaviors: Set<String>) {
        dataStore.edit { prefs -> prefs[KEY_SELECTED_BEHAVIORS] = behaviors }
    }

    // Custom execution order (JSON list)
    val behaviorOrder: Flow<List<String>> = dataStore.data.map { prefs ->
        prefs[KEY_BEHAVIOR_ORDER]?.let { json ->
            tryDecodeList(json)
        } ?: Constants.DEFAULT_BEHAVIOR_ORDER
    }

    suspend fun setBehaviorOrder(order: List<String>) {
        dataStore.edit { prefs -> prefs[KEY_BEHAVIOR_ORDER] = Json.encodeToString(order) }
    }

    // Message settings
    val messageSettings: Flow<MessageSettings> = dataStore.data.map { prefs ->
        prefs[KEY_MESSAGE_SETTINGS]?.let { json ->
            tryDecode<MessageSettings>(json)
        } ?: MessageSettings.DEFAULT
    }

    suspend fun updateMessageSettings(settings: MessageSettings) {
        dataStore.edit { prefs ->
            prefs[KEY_MESSAGE_SETTINGS] = Json.encodeToString(settings)
        }
    }

    // Challenge settings
    val challengeSettings: Flow<ChallengeSettings> = dataStore.data.map { prefs ->
        prefs[KEY_CHALLENGE_SETTINGS]?.let { json ->
            tryDecode<ChallengeSettings>(json)
        } ?: ChallengeSettings()
    }

    suspend fun updateChallengeSettings(settings: ChallengeSettings) {
        dataStore.edit { prefs ->
            prefs[KEY_CHALLENGE_SETTINGS] = Json.encodeToString(settings)
        }
    }

    // Redirect settings
    val redirectSettings: Flow<RedirectSettings> = dataStore.data.map { prefs ->
        prefs[KEY_REDIRECT_SETTINGS]?.let { json ->
            tryDecode<RedirectSettings>(json)
        } ?: RedirectSettings()
    }

    suspend fun updateRedirectSettings(settings: RedirectSettings) {
        dataStore.edit { prefs ->
            prefs[KEY_REDIRECT_SETTINGS] = Json.encodeToString(settings)
        }
    }

    // Locale (ar is primary)
    val locale: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_LOCALE] ?: "ar"
    }

    suspend fun setLocale(locale: String) {
        dataStore.edit { prefs -> prefs[KEY_LOCALE] = locale }
    }

    companion object {
        private val KEY_PROTECTION = booleanPreferencesKey("protection_enabled")
        private val KEY_SELECTED_BEHAVIORS = stringSetPreferencesKey("selected_behaviors")
        private val KEY_BEHAVIOR_ORDER = stringPreferencesKey("behavior_order")
        private val KEY_MESSAGE_SETTINGS = stringPreferencesKey("message_settings_json")
        private val KEY_CHALLENGE_SETTINGS = stringPreferencesKey("challenge_settings_json")
        private val KEY_REDIRECT_SETTINGS = stringPreferencesKey("redirect_settings_json")
        private val KEY_LOCALE = stringPreferencesKey("locale")

        private fun tryDecodeList(json: String): List<String> =
            try {
                Json.decodeFromString<List<String>>(json)
            } catch (e: Exception) {
                Constants.DEFAULT_BEHAVIOR_ORDER
            }

        private fun <T> tryDecode(json: String): T? =
            try {
                Json.decodeFromString<T>(json)
            } catch (e: Exception) {
                null
            }
    }
}
