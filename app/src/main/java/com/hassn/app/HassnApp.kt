package com.hassn.app

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.hassn.app.data.ChallengeRepository
import com.hassn.app.data.MonitoredAppsRepository
import com.hassn.app.data.SettingsRepository
import com.hassn.app.data.StatsRepository
import com.hassn.app.detection.PrivacyDetectionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.jvm.Volatile

val Context.dataStore: DataStore<Preferences> by lazy {
    PreferenceDataStoreFactory.create(
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        produceFile = { preferencesDataStoreFile("hassn_prefs") }
    )
}

class HassnApp : Application() {

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(dataStore) }
    val monitoredAppsRepository: MonitoredAppsRepository by lazy {
        MonitoredAppsRepository(dataStore)
    }
    val challengeRepository: ChallengeRepository by lazy { ChallengeRepository(dataStore) }
    val statsRepository: StatsRepository by lazy { StatsRepository(dataStore) }
    val privacyDetectionEngine: PrivacyDetectionEngine by lazy { PrivacyDetectionEngine() }

    /** Current app locale; read from DataStore at startup (ar is primary). */
    @Volatile
    var appLocale: String = "ar"

    override fun onCreate() {
        super.onCreate()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            appLocale = settingsRepository.locale.first()
            statsRepository.initialize()
        }
    }
}
