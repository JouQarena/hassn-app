package com.hassn.app.viewmodel

import android.content.Intent
import android.content.pm.PackageManager
import com.hassn.app.HassnApp
import com.hassn.app.data.ChallengeSettings
import com.hassn.app.data.MessageSettings
import com.hassn.app.data.MonitoredAppsRepository
import com.hassn.app.data.RedirectSettings
import com.hassn.app.data.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.just
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

class MainViewModelTest {

    private val settingsRepo = mockk<SettingsRepository>()
    private val appsRepo = mockk<MonitoredAppsRepository>()
    private val app = mockk<HassnApp>()
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        every { app.settingsRepository } returns settingsRepo
        every { app.monitoredAppsRepository } returns appsRepo
        every { app.appLocale = any() } just Runs

        val pm = mockk<PackageManager>()
        every { app.packageManager } returns pm
        every { pm.queryIntentActivities(any<Intent>(), 0) } returns emptyList()

        every { settingsRepo.protectionEnabled } returns flowOf(false)
        every { settingsRepo.selectedBehaviors } returns flowOf(setOf("redirect"))
        every { settingsRepo.behaviorOrder } returns
            flowOf(listOf("show_message", "show_challenge", "redirect"))
        every { settingsRepo.messageSettings } returns flowOf(MessageSettings.DEFAULT)
        every { settingsRepo.challengeSettings } returns flowOf(ChallengeSettings())
        every { settingsRepo.redirectSettings } returns flowOf(RedirectSettings())
        every { settingsRepo.locale } returns flowOf("ar")
        every { appsRepo.getAllMonitoredApps() } returns flowOf(emptyList())

        coEvery { settingsRepo.setProtectionEnabled(any()) } just Runs
        coEvery { settingsRepo.setSelectedBehaviors(any()) } just Runs
        coEvery { settingsRepo.setBehaviorOrder(any()) } just Runs
        coEvery { settingsRepo.updateRedirectSettings(any()) } just Runs
        coEvery { settingsRepo.setLocale(any()) } just Runs

        viewModel = MainViewModel(app)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggle protection writes inverse of current state`() = runTest {
        viewModel.toggleProtection()
        coVerify { settingsRepo.setProtectionEnabled(true) }
    }

    @Test
    fun `update behaviors persists selection`() = runTest {
        viewModel.updateBehaviors(setOf("show_message", "redirect"))
        coVerify { settingsRepo.setSelectedBehaviors(setOf("show_message", "redirect")) }
    }

    @Test
    fun `move behavior reorders the list`() = runTest {
        viewModel.moveBehavior("show_message", 1)
        coVerify {
            settingsRepo.setBehaviorOrder(
                listOf("show_challenge", "show_message", "redirect")
            )
        }
    }

    @Test
    fun `move behavior with invalid direction is a no-op`() = runTest {
        viewModel.moveBehavior("show_message", -1)
        coVerify(exactly = 0) { settingsRepo.setBehaviorOrder(any<List<String>>()) }
    }

    @Test
    fun `select destination app updates redirect settings`() = runTest {
        viewModel.selectDestinationApp("com.example.study")
        coVerify {
            settingsRepo.updateRedirectSettings(match { it.destinationPackage == "com.example.study" })
        }
    }

    @Test
    fun `toggle locale flips between arabic and english`() = runTest {
        viewModel.toggleLocale { }
        coVerify { settingsRepo.setLocale("en") }
    }
}
