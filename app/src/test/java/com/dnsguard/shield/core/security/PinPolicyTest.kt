package com.dnsguard.shield.core.security

import com.dnsguard.shield.core.security.PinPolicy.AttemptOutcome
import com.dnsguard.shield.core.security.PinPolicy.SetOutcome
import com.dnsguard.shield.core.security.PinPolicy.VaultState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PinPolicyTest {
    private val state = VaultState("real-hash", 0, 0L)
    private val verify: (String, String) -> Boolean = { pin, hash -> pin == "1234" && hash == "real-hash" }

    @Test fun `PIN setup requires strength and confirmation`() {
        assertEquals(SetOutcome.Invalid(PinPolicy.Strength.TOO_SHORT), PinPolicy.setup("abc", "abc"))
        assertEquals(SetOutcome.Invalid(PinPolicy.Strength.BAD_CHARS), PinPolicy.setup("a b c d", "a b c d"))
        assertEquals(SetOutcome.Invalid(PinPolicy.Strength.TOO_LONG), PinPolicy.setup("12345678901234567", "12345678901234567"))
        assertEquals(SetOutcome.Mismatch, PinPolicy.setup("1234", "1235"))
        assertEquals(SetOutcome.Ok, PinPolicy.setup("1234", "1234"))
    }

    @Test fun `no PIN gives no unlock grant`() {
        val (outcome, _) = PinPolicy.attemptUnlock(VaultState.EMPTY, "1234", 1000L, verify)
        assertEquals(AttemptOutcome.NoPin, outcome)
    }

    @Test fun `correct PIN resets failure budget`() {
        val (outcome, next) = PinPolicy.attemptUnlock(state.copy(fails = 4), "1234", 1000L, verify)
        assertEquals(AttemptOutcome.Success, outcome)
        assertEquals(0, next.fails)
    }

    @Test fun `five wrong attempts lock the vault`() {
        var s = state
        for (i in 1..5) {
            val (outcome, next) = PinPolicy.attemptUnlock(s, "wrong", 1000L, verify)
            assertTrue(outcome is AttemptOutcome.Failure)
            assertEquals(i, next.fails)
            s = next
        }
        assertEquals(31_000L, s.lockedUntilMs)
    }

    @Test fun `lockout rejects attempts without calling hasher`() {
        var calls = 0
        val (outcome, _) = PinPolicy.attemptUnlock(state.copy(fails = 5, lockedUntilMs = 31_000L),
            "1234", 1000L) { _, _ -> calls++; true }
        assertEquals(AttemptOutcome.Locked(30_000L), outcome)
        assertEquals(0, calls)
    }

    @Test fun `lockout escalates and caps at five minutes`() {
        assertEquals(30_000L, PinPolicy.lockDurationMs(5))
        assertEquals(60_000L, PinPolicy.lockDurationMs(6))
        assertEquals(120_000L, PinPolicy.lockDurationMs(7))
        assertEquals(300_000L, PinPolicy.lockDurationMs(100))
    }

    @Test fun `change needs current PIN and shares lockout budget`() {
        val (wrong, afterWrong) = PinPolicy.change(state, "bad", "9876", "9876", 1000L, verify)
        assertEquals(SetOutcome.WrongCurrent, wrong)
        assertEquals(1, afterWrong.fails)
        val (ok, afterOk) = PinPolicy.change(afterWrong, "1234", "9876", "9876", 1000L, verify)
        assertEquals(SetOutcome.Ok, ok)
        assertEquals(0, afterOk.fails)
    }

    @Test fun `change locked vault never verifies`() {
        var calls = 0
        val (outcome, _) = PinPolicy.change(state.copy(lockedUntilMs = 10000L), "1234",
            "9876", "9876", 1000L) { _, _ -> calls++; true }
        assertEquals(SetOutcome.WrongCurrent, outcome)
        assertEquals(0, calls)
    }

    @Test fun `real BCrypt hashes and verifies without storing plaintext`() {
        val hashed = PinHasher.hash("1a2b3c4d")
        assertNotEquals("1a2b3c4d", hashed)
        assertTrue(hashed.startsWith("\$2a\$10\$") || hashed.startsWith("\$2b\$10\$"))
        assertTrue(PinHasher.verify("1a2b3c4d", hashed))
        assertFalse(PinHasher.verify("wrong", hashed))
        assertFalse(PinHasher.verify("1a2b3c4d", "corrupt-hash"))
    }
}
