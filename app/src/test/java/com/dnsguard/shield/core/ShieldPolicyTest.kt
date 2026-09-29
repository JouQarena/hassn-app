package com.dnsguard.shield.core

import com.dnsguard.shield.core.ShieldPolicy.Action
import com.dnsguard.shield.core.ShieldPolicy.Phase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Adversarial matrix (playbook V08) for the shield state machine.
 * Every row is a named scenario; [ShieldPolicy.onEvent] is the pure function
 * under test, so a regression anywhere fails these without an emulator.
 */
class ShieldPolicyTest {

    private val neutral = setOf("com.android.settings", "com.example.launcher")
    private val own = ShieldPolicy.OWN_PACKAGE
    private val reddit = ShieldPolicy.REDDIT_PACKAGE

    // ── GRACE: the arm-and-switch ritual ───────────────────────────────────

    @Test
    fun `grace + settings window stays alive`() {
        val d = ShieldPolicy.onEvent(Phase.GRACE, "com.android.settings", neutral)
        assertEquals(Action.IGNORE, d.action)
        assertEquals(Phase.GRACE, d.nextPhase)
    }

    @Test
    fun `grace + launcher window stays alive`() {
        val d = ShieldPolicy.onEvent(Phase.GRACE, "com.example.launcher", neutral)
        assertEquals(Action.IGNORE, d.action)
        assertEquals(Phase.GRACE, d.nextPhase)
    }

    @Test
    fun `grace + our own window stays alive`() {
        val d = ShieldPolicy.onEvent(Phase.GRACE, own, neutral)
        assertEquals(Action.IGNORE, d.action)
        assertEquals(Phase.GRACE, d.nextPhase)
    }

    @Test
    fun `grace + unresolvable window stays alive`() {
        val d = ShieldPolicy.onEvent(Phase.GRACE, null, neutral)
        assertEquals(Action.IGNORE, d.action)
        assertEquals(Phase.GRACE, d.nextPhase)
    }

    @Test
    fun `grace + reddit activates the shield`() {
        val d = ShieldPolicy.onEvent(Phase.GRACE, reddit, neutral)
        assertEquals(Action.ACTIVATE, d.action)
        assertEquals(Phase.ACTIVE, d.nextPhase)
    }

    @Test
    fun `grace + any other real app dies immediately`() {
        for (pkg in listOf("com.bank.app", "com.whatsapp", "tv.twitch")) {
            val d = ShieldPolicy.onEvent(Phase.GRACE, pkg, neutral)
            assertEquals(Action.DISABLE, d.action, "expected DISABLE for $pkg")
            assertEquals(Phase.DEAD, d.nextPhase, "expected DEAD for $pkg")
        }
    }

    @Test
    fun `grace timeout dies but later timeouts are no-ops`() {
        val d = ShieldPolicy.onGraceTimeout(Phase.GRACE)
        assertEquals(Action.DISABLE, d.action)
        assertEquals(Phase.DEAD, d.nextPhase)

        assertEquals(Action.IGNORE, ShieldPolicy.onGraceTimeout(Phase.DEAD).action)
        assertEquals(Action.IGNORE, ShieldPolicy.onGraceTimeout(Phase.ACTIVE).action)
    }

    // ── ACTIVE: the strict Reddit-only window ──────────────────────────────

    @Test
    fun `active + reddit keeps scanning`() {
        val d = ShieldPolicy.onEvent(Phase.ACTIVE, reddit, neutral)
        assertEquals(Action.SCAN, d.action)
        assertEquals(Phase.ACTIVE, d.nextPhase)
    }

    @Test
    fun `active + own overlay is ignored not fatal`() {
        val d = ShieldPolicy.onEvent(Phase.ACTIVE, own, neutral)
        assertEquals(Action.IGNORE, d.action)
        assertEquals(Phase.ACTIVE, d.nextPhase)
    }

    @Test
    fun `active + banking app dies immediately`() {
        val d = ShieldPolicy.onEvent(Phase.ACTIVE, "com.bank.app", neutral)
        assertEquals(Action.DISABLE, d.action)
        assertEquals(Phase.DEAD, d.nextPhase)
    }

    @Test
    fun `active + neutral package still dies - neutral only applies in grace`() {
        // Settings must NOT keep an ACTIVE shield alive (e.g. user opens
        // Settings from Reddit): leaving Reddit = off, no exceptions.
        val d = ShieldPolicy.onEvent(Phase.ACTIVE, "com.android.settings", neutral)
        assertEquals(Action.DISABLE, d.action)
        assertEquals(Phase.DEAD, d.nextPhase)
    }

    @Test
    fun `active + unresolvable window fails safe`() {
        val d = ShieldPolicy.onEvent(Phase.ACTIVE, null, neutral)
        assertEquals(Action.DISABLE, d.action)
        assertEquals(Phase.DEAD, d.nextPhase)
    }

    @Test
    fun `root lost while active dies`() {
        val d = ShieldPolicy.onRootLost(Phase.ACTIVE)
        assertEquals(Action.DISABLE, d.action)
        assertEquals(Action.IGNORE, ShieldPolicy.onRootLost(Phase.GRACE).action)
    }

    // ── DEAD: terminal ─────────────────────────────────────────────────────

    @Test
    fun `dead phase never revives from any observation`() {
        for (pkg in listOf(reddit, "com.bank.app", own, null)) {
            val d = ShieldPolicy.onEvent(Phase.DEAD, pkg, neutral)
            assertEquals(Action.IGNORE, d.action, "DEAD must ignore $pkg")
            assertEquals(Phase.DEAD, d.nextPhase)
        }
        assertEquals(Action.IGNORE, ShieldPolicy.onScreenOff(Phase.DEAD).action)
        assertEquals(Phase.DEAD, ShieldPolicy.onScreenOff(Phase.DEAD).nextPhase)
    }

    // ── Screen off ─────────────────────────────────────────────────────────

    @Test
    fun `screen off kills grace and active alike`() {
        for (phase in listOf(Phase.GRACE, Phase.ACTIVE)) {
            val d = ShieldPolicy.onScreenOff(phase)
            assertEquals(Action.DISABLE, d.action, "screen off in $phase")
            assertEquals(Phase.DEAD, d.nextPhase)
        }
    }

    // ── Structural invariants ──────────────────────────────────────────────

    @Test
    fun `every transition out of alive states is either ignore or disable-safe`() {
        // V08 invariant: no path from GRACE/ACTIVE to "keep running over a
        // non-reddit app": any unknown package must map to DISABLE.
        val unknownPkgs = listOf("com.bank.app", "org.mozilla.firefox", "com.adobe.reader")
        for (phase in listOf(Phase.GRACE, Phase.ACTIVE)) {
            for (pkg in unknownPkgs) {
                assertNotEquals(
                    Action.ACTIVATE,
                    ShieldPolicy.onEvent(phase, pkg, neutral).action
                )
                assertEquals(
                    Action.DISABLE,
                    ShieldPolicy.onEvent(phase, pkg, neutral).action,
                    "$phase + $pkg must DISABLE"
                )
            }
        }
    }

    @Test
    fun `policy constants stay pinned to the product spec`() {
        assertEquals("com.reddit.frontpage", ShieldPolicy.REDDIT_PACKAGE)
        assertEquals(30_000L, ShieldPolicy.GRACE_PERIOD_MS)
    }
}
