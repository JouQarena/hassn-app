package com.hassn.app.viewmodel

import android.content.pm.PackageManager
import com.hassn.app.HassnApp
import com.hassn.app.data.ChallengeSettings
import com.hassn.app.data.MessageSettings
import com.hassn.app.data.MonitoredAppsRepository
import com.hassn.app.data.RedirectSettings
import com.hassn.app.data.SettingsRepository
import io.mockk.any
import io.mockk.coVerify
import io.mockk.every
import io.mockk.match
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestWatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settingsRepo = mockk<SettingsRepository>()
    private val appsRepo = mockk<MonitoredAppsRepository>()
    private val app = mockk<HassnApp>()
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        every { app.settingsRepository } returns settingsRepo
        every { app.monitoredAppsRepository } returns appsRepo

        val pm = mockk<PackageManager>()
        every { app.packageManager } returns pm
        every { pm.queryIntentActivities(any(), any()) } returns emptyList()

        every { settingsRepo.protectionEnabled } returns flowOf(false)
        every { settingsRepo.selectedBehaviors } returns flowOf(setOf("redirect"))
        every { settingsRepo.behaviorOrder } returns
            flowOf(listOf("show_message", "show_challenge", "redirect"))
        every { settingsRepo.messageSettings } returns flowOf(MessageSettings.DEFAULT)
        every { settingsRepo.challengeSettings } returns flowOf(ChallengeSettings())
        every { settingsRepo.redirectSettings } returns flowOf(RedirectSettings())
        every { settingsRepo.locale } returns flowOf("ar")
        every { appsRepo.getAllMonitoredApps() } returns flowOf(emptyList())

        viewModel = MainViewModel(app)
    }

    @Test
    fun `toggle protection writes inverse of current state`() {
        viewModel.toggleProtection()
        coVerify { settingsRepo.setProtectionEnabled(true) }
    }

    @Test
    fun `update behaviors persists selection`() {
        viewModel.updateBehaviors(setOf("show_message", "redirect"))
        coVerify { settingsRepo.setSelectedBehaviors(setOf("show_message", "redirect")) }
    }

    @Test
    fun `move behavior reorders the list`() {
        viewModel.moveBehavior("show_message", 1)
        coVerify {
            settingsRepo.setBehaviorOrder(
                listOf("show_challenge", "show_message", "redirect")
            )
        }
    }

    @Test
    fun `move behavior with invalid direction is a no-op`() {
        viewModel.moveBehavior("show_message", -1)
        verify(exactly = 0) { settingsRepo.setBehaviorOrder(any()) }
    }

    @Test
    fun `select destination app updates redirect settings`() {
        viewModel.selectDestinationApp("com.example.study")
        coVerify {
            settingsRepo.updateRedirectSettings(match { it.destinationPackage == "com.example.study" })
        }
    }

    @Test
    fun `toggle locale flips between arabic and english`() {
        viewModel.toggleLocale { }
        coVerify { settingsRepo.setLocale("en") }
    }

    private class MainDispatcherRule : TestWatcher() {
        @Before
        fun setMainDispatcher() {
            Dispatchers.setMain(UnconfinedTestDispatcher())
        }

        @After
        fun resetMainDispatcher() {
            Dispatchers.resetMain()
        }
    }
}
