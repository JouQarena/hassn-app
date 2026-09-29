package com.dnsguard.shield.core.security

/**
 * Pure decision engine for the Master PIN system (unit-tested in
 * `PinPolicyTest`). No Android imports: the [PinVault] persists state and
 * supplies the hashing primitive, every *decision* is made here so the
 * adversarial matrix (wrong PIN, lockout, replay, single-use burn, expiry)
 * runs on the JVM.
 */
object PinPolicy {

    // ── Constants (single source of truth) ─────────────────────────────────

    const val MIN_LENGTH = 4
    const val MAX_LENGTH = 16

    /** Failures allowed before the first lockout. */
    const val MAX_FAILS_BEFORE_LOCK = 5

    const val BASE_LOCK_MS = 30_000L
    const val MAX_LOCK_MS = 300_000L

    /** A granted unlock token stays valid this long. */
    const val TOKEN_TTL_MS = 60_000L

    /** Characters accepted in a Master PIN/password. */
    private val ALLOWED = Regex("^[0-9a-zA-Z]+$")

    // ── Validation ─────────────────────────────────────────────────────────

    enum class Strength { OK, TOO_SHORT, TOO_LONG, BAD_CHARS }

    fun validate(pin: String): Strength = when {
        pin.length < MIN_LENGTH -> Strength.TOO_SHORT
        pin.length > MAX_LENGTH -> Strength.TOO_LONG
        !ALLOWED.matches(pin) -> Strength.BAD_CHARS
        else -> Strength.OK
    }

    // ── Vault state + unlock attempts ──────────────────────────────────────

    /** Persisted slice of the vault the decisions need. */
    data class VaultState(
        val hash: String?,
        val fails: Int,
        val lockedUntilMs: Long
    ) {
        companion object {
            val EMPTY = VaultState(hash = null, fails = 0, lockedUntilMs = 0L)
        }
    }

    sealed class AttemptOutcome {
        /** No PIN has been set — nothing to unlock. */
        object NoPin : AttemptOutcome()

        /** Caller is locked out; [retryAfterMs] until the next attempt. */
        data class Locked(val retryAfterMs: Long) : AttemptOutcome()

        /** PIN matched. */
        object Success : AttemptOutcome()

        /** PIN wrong; [remainingAttempts] before the next lockout,
         *  [lockedUntilMs] > 0 when this failure triggered a lock. */
        data class Failure(
            val fails: Int,
            val remainingAttempts: Int,
            val lockedUntilMs: Long
        ) : AttemptOutcome()
    }

    /** Escalating lock: 30 s, 60 s, 120 s, 240 s, capped at 5 min. */
    fun lockDurationMs(fails: Int): Long {
        if (fails < MAX_FAILS_BEFORE_LOCK) return 0L
        val exponent = fails - MAX_FAILS_BEFORE_LOCK
        val duration = BASE_LOCK_MS shl exponent.coerceAtMost(30)
        return duration.coerceAtMost(MAX_LOCK_MS)
    }

    /**
     * One unlock attempt. [verify] is injected (real BCrypt in production, a
     * counting stub in tests) so the policy decides *around* the hash without
     * owning the algorithm. Returns the outcome and the state to persist.
     */
    fun attemptUnlock(
        state: VaultState,
        pin: String,
        nowMs: Long,
        verify: (pin: String, hash: String) -> Boolean
    ): Pair<AttemptOutcome, VaultState> {
        val hash = state.hash ?: return Pair(AttemptOutcome.NoPin, state)

        // A locked vault never consults the hash — the attempt does not even
        // count (stops both brute force and hash-timing probes).
        if (nowMs < state.lockedUntilMs) {
            return Pair(
                AttemptOutcome.Locked(retryAfterMs = state.lockedUntilMs - nowMs),
                state
            )
        }

        return if (verify(pin, hash)) {
            // Success clears the failure budget entirely.
            Pair(AttemptOutcome.Success, state.copy(fails = 0, lockedUntilMs = 0L))
        } else {
            val fails = state.fails + 1
            val lockFor = lockDurationMs(fails)
            val lockedUntil = if (lockFor > 0) nowMs + lockFor else 0L
            Pair(
                AttemptOutcome.Failure(
                    fails = fails,
                    remainingAttempts = (MAX_FAILS_BEFORE_LOCK - fails).coerceAtLeast(0),
                    lockedUntilMs = lockedUntil
                ),
                state.copy(fails = fails, lockedUntilMs = lockedUntil)
            )
        }
    }

    // ── Set / change flows ─────────────────────────────────────────────────

    sealed class SetOutcome {
        object Ok : SetOutcome()
        data class Invalid(val strength: Strength) : SetOutcome()
        object Mismatch : SetOutcome()
        object WrongCurrent : SetOutcome()
        object StorageError : SetOutcome()
    }

    /** First-time setup: strength + confirmation equality. */
    fun setup(newPin: String, confirmPin: String): SetOutcome {
        val strength = validate(newPin)
        if (strength != Strength.OK) return SetOutcome.Invalid(strength)
        if (newPin != confirmPin) return SetOutcome.Mismatch
        return SetOutcome.Ok
    }

    /** Change: the current PIN must verify *before* the new one is judged. */
    fun change(
        state: VaultState,
        currentPin: String,
        newPin: String,
        confirmPin: String,
        nowMs: Long,
        verify: (pin: String, hash: String) -> Boolean
    ): Pair<SetOutcome, VaultState> {
        val hash = state.hash ?: return Pair(SetOutcome.WrongCurrent, state)
        if (nowMs < state.lockedUntilMs) {
            return Pair(SetOutcome.WrongCurrent, state)
        }
        if (!verify(currentPin, hash)) {
            // A wrong "current PIN" in the change dialog is an unlock failure
            // too — it shares the same budget and lockout.
            val (outcome, nextState) = attemptUnlock(state, currentPin, nowMs, verify)
            return when (outcome) {
                is AttemptOutcome.Failure -> Pair(SetOutcome.WrongCurrent, nextState)
                is AttemptOutcome.Locked -> Pair(SetOutcome.WrongCurrent, nextState)
                else -> Pair(SetOutcome.WrongCurrent, nextState)
            }
        }
        return when (val outcome = setup(newPin, confirmPin)) {
            SetOutcome.Ok -> Pair(SetOutcome.Ok, state.copy(fails = 0, lockedUntilMs = 0L))
            else -> Pair(outcome, state)
        }
    }

}
