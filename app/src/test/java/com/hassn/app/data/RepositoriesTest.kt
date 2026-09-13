package com.hassn.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class RepositoriesTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var scope: CoroutineScope

    @Before
    fun setup() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val file = File.createTempFile("hassn_ds_test", ".preferences_pb")
        file.delete()
        dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { file }
        )
    }

    @After
    fun teardown() {
        scope.cancel()
    }

    // --- SettingsRepository ---

    @Test
    fun `protection defaults to false and can be enabled`() = runBlocking {
        val repo = SettingsRepository(dataStore)
        assertFalse(repo.protectionEnabled.first())
        repo.setProtectionEnabled(true)
        assertTrue(repo.protectionEnabled.first())
    }

    @Test
    fun `selected behaviors default to redirect only`() = runBlocking {
        val repo = SettingsRepository(dataStore)
        assertEquals(setOf("redirect"), repo.selectedBehaviors.first())
    }

    @Test
    fun `behavior order is persisted`() = runBlocking {
        val repo = SettingsRepository(dataStore)
        assertEquals(
            listOf("show_message", "show_challenge", "redirect"),
            repo.behaviorOrder.first()
        )
        repo.setBehaviorOrder(listOf("redirect", "show_message"))
        assertEquals(listOf("redirect", "show_message"), repo.behaviorOrder.first())
    }

    @Test
    fun `message settings roundtrip`() = runBlocking {
        val repo = SettingsRepository(dataStore)
        assertEquals("توقف! ركز على هدفك 💪", repo.messageSettings.first().text)
        repo.updateMessageSettings(
            MessageSettings(text = "اختبر", duration = 5, textSize = 20f, backgroundOpacity = 0.5f)
        )
        val saved = repo.messageSettings.first()
        assertEquals("اختبر", saved.text)
        assertEquals(5, saved.duration)
        assertEquals(20f, saved.textSize)
        assertEquals(0.5f, saved.backgroundOpacity)
    }

    @Test
    fun `challenge settings default to medium without morse`() = runBlocking {
        val repo = SettingsRepository(dataStore)
        val settings = repo.challengeSettings.first()
        assertEquals(Difficulty.MEDIUM, settings.difficulty)
        assertTrue(ChallengeType.MORSE_CODE !in settings.enabledChallenges)
        assertEquals(8, settings.enabledChallenges.size)
    }

    @Test
    fun `redirect settings default to no destination`() = runBlocking {
        val repo = SettingsRepository(dataStore)
        val settings = repo.redirectSettings.first()
        assertEquals(null, settings.destinationPackage)
        assertEquals(350L, settings.delayMs)
    }

    @Test
    fun `locale defaults to arabic`() = runBlocking {
        val repo = SettingsRepository(dataStore)
        assertEquals("ar", repo.locale.first())
        repo.setLocale("en")
        assertEquals("en", repo.locale.first())
    }

    // --- MonitoredAppsRepository ---

    @Test
    fun `monitored apps crud operations`() = runBlocking {
        val repo = MonitoredAppsRepository(dataStore)
        assertTrue(repo.getAllMonitoredApps().first().isEmpty())

        val app = MonitoredApp(
            packageName = "com.example.distraction",
            appName = "Distraction",
            mode = MonitorMode.PRIVATE_ONLY,
            customKeywords = listOf("خاص")
        )
        repo.addMonitoredApp(app)
        assertEquals(1, repo.getAllMonitoredApps().first().size)

        val loaded = repo.getMonitoredApp("com.example.distraction")!!
        assertEquals(MonitorMode.PRIVATE_ONLY, loaded.mode)
        assertEquals(listOf("خاص"), loaded.customKeywords)

        repo.updateMonitoredApp(loaded.copy(enabled = false))
        assertFalse(repo.getMonitoredApp("com.example.distraction")!!.enabled)

        repo.removeMonitoredApp("com.example.distraction")
        assertTrue(repo.getAllMonitoredApps().first().isEmpty())
    }

    @Test
    fun `adding duplicate package does not duplicate entry`() = runBlocking {
        val repo = MonitoredAppsRepository(dataStore)
        val app = MonitoredApp(packageName = "com.example.app", appName = "App")
        repo.addMonitoredApp(app)
        repo.addMonitoredApp(app.copy(appName = "App2"))
        assertEquals(1, repo.getAllMonitoredApps().first().size)
    }

    // --- ChallengeRepository ---

    @Test
    fun `stats initialize is idempotent and counters increment`() = runBlocking {
        val repo = StatsRepository(dataStore)
        repo.initialize()
        repo.initialize() // should not overwrite install date
        repo.recordRedirection()
        repo.recordRedirection()
        repo.recordChallengeResult(success = true)
        repo.recordChallengeResult(success = false)

        val stats = repo.getStats().first()
        assertEquals(2, stats.totalRedirections)
        assertEquals(1, stats.challengesCompleted)
        assertEquals(1, stats.challengesFailed)
        assertEquals(0, stats.daysUsed)
        assertEquals(0, stats.hoursSaved)
    }

    @Test
    fun `disable attempts and lock lifecycle`() = runBlocking {
        val repo = ChallengeRepository(dataStore)
        assertEquals(0, repo.getAttemptCount().first())
        assertFalse(repo.isLocked().first())

        repo.incrementAttemptCount()
        repo.incrementAttemptCount()
        assertEquals(2, repo.getAttemptCount().first())

        repo.resetAttemptCount()
        assertEquals(0, repo.getAttemptCount().first())

        repo.setLock(15)
        assertTrue(repo.isLocked().first())
    }
}
