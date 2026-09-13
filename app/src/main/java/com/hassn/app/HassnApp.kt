package com.hassn.app

import android.app.Application
import android.util.Log
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
        installCrashLogger()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            try {
                appLocale = settingsRepository.locale.first()
                statsRepository.initialize()
            } catch (e: Exception) {
                // Never crash the process on a startup read failure
                Log.e("HassnApp", "Startup init failed", e)
            }
        }
    }

    /**
     * Writes uncaught exception traces to filesDir/hassn_crash.log so the
     * crash can be inspected without a debugger, then defers to the default
     * handler (system crash dialog).
     */
    private fun installCrashLogger() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val writer = java.io.StringWriter()
                throwable.printStackTrace(java.io.PrintWriter(writer))
                File(filesDir, "hassn_crash.log").writeText(
                    "Thread: ${thread.name}\n" + writer
                )
            } catch (_: Exception) {
                // best effort only
            }
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
                throw throwable
            }
        }
    }
}
