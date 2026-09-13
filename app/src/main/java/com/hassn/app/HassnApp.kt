package com.hassn.app

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
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
import java.io.File
import kotlin.jvm.Volatile

class HassnApp : Application() {

    val dataStore: DataStore<Preferences> by lazy {
        val dir = File(filesDir, "datastore").apply { mkdirs() }
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { File(dir, "hassn_prefs.preferences_pb") }
        )
    }

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
