package com.hassn.app.service

import android.content.Intent
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityService
import com.hassn.app.data.ChallengeSettings
import com.hassn.app.data.MessageSettings
import com.hassn.app.data.RedirectSettings
import com.hassn.app.data.SettingsRepository
import com.hassn.app.util.Constants
import io.mockk.Runs
import io.mockk.any
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ResponseExecutorTest {

    @Test
    fun `redirect behavior presses home and launches destination app`() = runBlocking {
        val pm = mockk<PackageManager>()
        val service = mockk<AccessibilityService>()
        every { service.packageManager } returns pm
        every { service.performGlobalAction(any()) } returns true
        every { service.startActivity(any()) } just Runs
        every { pm.getLaunchIntentForPackage("com.example.product") } returns Intent()

        val repo = mockk<SettingsRepository>()
        every { repo.redirectSettings } returns
            flowOf(RedirectSettings(destinationPackage = "com.example.product", delayMs = 10L))

        val executor = ResponseExecutor(service, repo)
        executor.execute(
            behaviors = setOf(Constants.BEHAVIOR_REDIRECT),
            order = listOf(Constants.BEHAVIOR_REDIRECT)
        )

        verify(exactly = 1) {
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
        }
        verify { service.startActivity(any()) }
    }

    @Test
    fun `redirect behavior without destination does not crash and presses home`() = runBlocking {
        val pm = mockk<PackageManager>()
        val service = mockk<AccessibilityService>()
        every { service.packageManager } returns pm
        every { service.performGlobalAction(any()) } returns true
        every { service.startActivity(any()) } just Runs

        val repo = mockk<SettingsRepository>()
        every { repo.redirectSettings } returns
            flowOf(RedirectSettings(destinationPackage = null, delayMs = 10L))

        val executor = ResponseExecutor(service, repo)
        executor.execute(
            behaviors = setOf(Constants.BEHAVIOR_REDIRECT),
            order = listOf(Constants.BEHAVIOR_REDIRECT)
        )

        verify(exactly = 1) {
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
        }
        verify(exactly = 0) { service.startActivity(any()) }
    }

    @Test
    fun `challenge failure goes home and stops execution`() = runBlocking {
        val service = mockk<AccessibilityService>()
        every { service.performGlobalAction(any()) } returns true

        val repo = mockk<SettingsRepository>()
        every { repo.challengeSettings } returns flowOf(ChallengeSettings())
        // On the JVM, Settings.canDrawOverlays returns false by default,
        // so the challenge cannot be presented and counts as a failure.

        val executor = ResponseExecutor(service, repo)
        executor.execute(
            behaviors = setOf(Constants.BEHAVIOR_CHALLENGE),
            order = listOf(Constants.BEHAVIOR_CHALLENGE)
        )

        verify(exactly = 1) {
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
        }
    }

    @Test
    fun `message-only behavior does not press home`() = runBlocking {
        val service = mockk<AccessibilityService>()

        val repo = mockk<SettingsRepository>()
        every { repo.messageSettings } returns flowOf(MessageSettings(text = "توقف"))
        // Overlay not drawable on JVM -> no-op, but no home action expected either.

        val executor = ResponseExecutor(service, repo)
        executor.execute(
            behaviors = setOf(Constants.BEHAVIOR_MESSAGE),
            order = listOf(Constants.BEHAVIOR_MESSAGE)
        )

        verify(exactly = 0) { service.performGlobalAction(any()) }
    }

    @Test
    fun `behaviors not in order are ignored`() = runBlocking {
        val service = mockk<AccessibilityService>()

        val repo = mockk<SettingsRepository>()
        every { repo.messageSettings } returns flowOf(MessageSettings(text = "توقف"))

        val executor = ResponseExecutor(service, repo)
        executor.execute(
            behaviors = setOf(Constants.BEHAVIOR_MESSAGE),
            order = listOf(Constants.BEHAVIOR_REDIRECT) // message not in order
        )

        verify(exactly = 0) { service.performGlobalAction(any()) }
    }
}
