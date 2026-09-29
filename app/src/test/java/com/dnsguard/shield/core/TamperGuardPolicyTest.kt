package com.dnsguard.shield.core

import com.dnsguard.shield.core.TamperGuardPolicy.Action
import com.dnsguard.shield.core.TamperGuardPolicy.GuardState
import com.dnsguard.shield.core.TamperGuardPolicy.Kind
import com.dnsguard.shield.core.TamperGuardPolicy.Observation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TamperGuardPolicyTest {
    private val settings = setOf("com.android.settings", "com.oem.settings")
    private val active = GuardState(
        protectionActive = true, pinSet = true, suspendedUntilMs = 0L,
        challengeVisible = false, lastBackAtMs = 0L, challengedSignature = null
    )
    private fun obs(
        pkg: String? = "com.android.settings", kind: Kind = Kind.WINDOW_STATE,
        clazz: String? = "com.android.settings.applications.InstalledAppDetails",
        texts: List<String> = listOf("DNS Guard & Shield", "Uninstall"),
        now: Long = 1_000L
    ) = Observation(kind, pkg, clazz, texts, now)

    @Test fun `app details raises PIN challenge`() {
        val d = TamperGuardPolicy.onObservation(active, obs(), settings)
        assertEquals(Action.SHOW_CHALLENGE, d.action)
        assertTrue(d.state.challengeVisible)
    }

    @Test fun `no PIN exits directly`() {
        val d = TamperGuardPolicy.onObservation(active.copy(pinSet = false), obs(), settings)
        assertEquals(Action.EXIT_BACK, d.action)
    }

    @Test fun `dormant guard never intercepts`() {
        val d = TamperGuardPolicy.onObservation(GuardState.DORMANT, obs(), settings)
        assertEquals(Action.IGNORE, d.action)
    }

    @Test fun `arabic and english destructive keywords normalize whitespace`() {
        val list = TamperGuardPolicy.matchKeywords(listOf(
            " إلغاء التثبيت ", " Uninstall ", "Force  Stop", "مسح البيانات", "Deactivate device admin",
            "إيقاف إجباري", "Clear Data", "مسؤولو الجهاز"
        ))
        for (keyword in listOf("uninstall", "force stop", "clear data", "deactivate device admin",
            "إلغاء التثبيت", "إيقاف إجباري", "مسح البيانات", "مسؤولو الجهاز")) {
            assertTrue(keyword in list, "missing $keyword")
        }
    }

    @Test fun `clicked destructive control exits immediately`() {
        val d = TamperGuardPolicy.onObservation(active, obs(kind = Kind.CLICK), settings)
        assertEquals(Action.EXIT_BACK, d.action)
    }

    @Test fun `benign click on app details is not intercepted`() {
        val d = TamperGuardPolicy.onObservation(active,
            obs(kind = Kind.CLICK, texts = listOf("DNS Guard & Shield")), settings)
        assertEquals(Action.IGNORE, d.action)
    }

    @Test fun `duplicate click within back cooldown is ignored`() {
        val first = TamperGuardPolicy.onObservation(active, obs(kind = Kind.CLICK), settings)
        val duplicate = TamperGuardPolicy.onObservation(first.state,
            obs(kind = Kind.CLICK, now = 1_100L), settings)
        assertEquals(Action.IGNORE, duplicate.action)
    }

    @Test fun `successful PIN suspension allows owner to leave or uninstall`() {
        val suspended = active.copy(suspendedUntilMs = 121_000L)
        val d = TamperGuardPolicy.onObservation(suspended, obs(now = 15_000L), settings)
        assertEquals(Action.IGNORE, d.action)
        assertEquals(Action.SHOW_CHALLENGE,
            TamperGuardPolicy.onObservation(suspended, obs(now = 121_000L), settings).action)
    }

    @Test fun `challenge repeated content event not duplicated`() {
        val first = TamperGuardPolicy.onObservation(active, obs(), settings)
        val second = TamperGuardPolicy.onObservation(first.state, obs(kind = Kind.CONTENT), settings)
        assertEquals(Action.IGNORE, second.action)
    }

    @Test fun `leaving settings dismisses challenge and does not read another app`() {
        val first = TamperGuardPolicy.onObservation(active, obs(), settings)
        val second = TamperGuardPolicy.onObservation(first.state,
            obs(pkg = "com.bank.example", texts = listOf("Uninstall")), settings)
        assertEquals(Action.DISMISS_CHALLENGE, second.action)
        assertFalse(second.state.challengeVisible)
    }

    @Test fun `unrelated app settings and generic admin list stay reachable`() {
        for ((clazz, texts) in listOf(
            "com.android.settings.SubSettings" to listOf("Other app", "Uninstall"),
            "com.android.settings.applications.InstalledAppDetails" to listOf("Other app", "Force stop", "Uninstall"),
            "com.android.settings.DeviceAdminSettings" to listOf("Device admin apps"),
            "com.android.settings.Settings" to listOf("Wi-Fi", "Bluetooth")
        )) {
            val d = TamperGuardPolicy.onObservation(active, obs(clazz = clazz, texts = texts), settings)
            assertEquals(Action.IGNORE, d.action, "class=$clazz")
        }
    }

    @Test fun `reddit accessibility entry is not mistaken for full app label`() {
        assertFalse(TamperGuardPolicy.matchesIdentity(listOf("DNS Guard Reddit Shield")))
    }

    @Test fun `OEM settings and package installer are in scope`() {
        assertEquals(Action.SHOW_CHALLENGE, TamperGuardPolicy.onObservation(active,
            obs(pkg = "com.oem.settings"), settings).action)
        assertEquals(Action.SHOW_CHALLENGE, TamperGuardPolicy.onObservation(active,
            obs(pkg = "com.google.android.packageinstaller", clazz = "UninstallerActivity"), settings).action)
    }

    @Test fun `wrong package with our label is ignored`() {
        val d = TamperGuardPolicy.onObservation(active,
            obs(pkg = "com.bank.example"), settings)
        assertEquals(Action.IGNORE, d.action)
    }

    @Test fun `in-app window dismisses stale challenge`() {
        val first = TamperGuardPolicy.onObservation(active, obs(), settings)
        assertEquals(Action.DISMISS_CHALLENGE, TamperGuardPolicy.onObservation(first.state,
            obs(pkg = TamperGuardPolicy.OWN_PACKAGE), settings).action)
    }

    @Test fun `release durations are monotonic and bounded to minutes`() {
        val now = 1000L
        assertEquals(now + 120_000L, TamperGuardPolicy.suspendUntil(now,
            TamperGuardPolicy.UNINSTALL_RELEASE_MINUTES))
        assertEquals(now + 600_000L, TamperGuardPolicy.suspendUntil(now,
            TamperGuardPolicy.DEACTIVATE_RELEASE_MINUTES))
    }
}
