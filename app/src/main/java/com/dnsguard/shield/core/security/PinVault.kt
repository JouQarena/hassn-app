package com.dnsguard.shield.core.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.dnsguard.shield.core.TamperGuardPolicy

/**
 * Master-PIN storage facade. The *decisions* live in the pure [PinPolicy];
 * this object only persists state and supplies the real [PinHasher] (BCrypt).
 *
 * Storage layers:
 *  1. `EncryptedSharedPreferences` (AES-256-SIV keys / AES-256-GCM values,
 *     master key in the Android Keystore) — the normal path.
 *  2. If the Keystore is broken on a particular device, creation can throw;
 *     the vault then falls back to app-private plain `SharedPreferences`
 *     holding **only the BCrypt hash** (still never the PIN itself) and sets
 *     [storageDegraded] so the UI can be honest about it.
 *
 * Failure direction: if neither store is readable the vault reports
 * "no PIN set" — protection degrades to the Device-Admin layer instead of
 * locking the legitimate owner out of their own app.
 */
object PinVault {

    private const val TAG = "PinVault"
    private const val FILE_ENCRYPTED = "dnsguard_pin_vault"
    private const val FILE_FALLBACK = "dnsguard_pin_vault_fallback"

    private const val KEY_HASH = "pin_hash"
    private const val KEY_RECOVERY_HASH = "recovery_hash"
    private const val KEY_FAILS = "pin_fails"
    private const val KEY_LOCKED_UNTIL = "pin_locked_until"
    private const val KEY_SUSPENDED_UNTIL = "guard_suspended_until"

    @Volatile
    private var prefs: SharedPreferences? = null

    @Volatile
    var storageDegraded: Boolean = false
        private set

    @Volatile
    private var storageFault: Boolean = false

    /** Idempotent; call from `Application.onCreate`. */
    fun init(context: Context) {
        if (prefs != null) return
        val app = context.applicationContext
        prefs = try {
            val masterKey = MasterKey.Builder(app)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                app,
                FILE_ENCRYPTED,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (t: Throwable) {
            // Broken Keystore / first-boot races on some OEM builds.
            Log.w(TAG, "EncryptedSharedPreferences unavailable, using hashed fallback", t)
            storageDegraded = true
            runCatching {
                app.getSharedPreferences(FILE_FALLBACK, Context.MODE_PRIVATE)
            }.getOrNull()
        }
    }

    private fun prefs(): SharedPreferences =
        prefs ?: error("PinVault.init(context) must run first (Application.onCreate)")

    private fun state(): PinPolicy.VaultState {
        val p = prefs ?: return PinPolicy.VaultState.EMPTY
        return PinPolicy.VaultState(
            hash = runCatching { p.getString(KEY_HASH, null) }.getOrNull(),
            fails = runCatching { p.getInt(KEY_FAILS, 0) }.getOrDefault(0),
            lockedUntilMs = runCatching { p.getLong(KEY_LOCKED_UNTIL, 0L) }.getOrDefault(0L)
        )
    }

    private fun persist(state: PinPolicy.VaultState): Boolean =
        runCatching {
            prefs().edit()
                .putString(KEY_HASH, state.hash)
                .putInt(KEY_FAILS, state.fails)
                .putLong(KEY_LOCKED_UNTIL, state.lockedUntilMs)
                .commit()
        }.onFailure { Log.e(TAG, "persist failed", it) }.getOrDefault(false)

    /** True when a Master PIN has been enrolled. Safe before init (false). */
    fun isPinSet(): Boolean =
        runCatching { prefs()?.getString(KEY_HASH, null) != null }.getOrDefault(false)

    /**
     * Verifies [pin] with real BCrypt. Callers should invoke this off the
     * main thread (≈50–150 ms). Lockout bookkeeping is [PinPolicy]'s job.
     */
    @Synchronized
    fun attemptUnlock(pin: String, nowMs: Long = System.currentTimeMillis()): PinPolicy.AttemptOutcome {
        if (storageFault) return PinPolicy.AttemptOutcome.Locked(PinPolicy.MAX_LOCK_MS)
        val current = state()
        val (outcome, next) = PinPolicy.attemptUnlock(current, pin, nowMs, PinHasher::verify)
        if (next != current && !persist(next)) {
            // A failure count MUST persist before further attempts are
            // allowed; a successful unlock MUST clear it before granting.
            storageFault = true
            return PinPolicy.AttemptOutcome.Locked(PinPolicy.MAX_LOCK_MS)
        }
        return outcome
    }

    /** First-time enrollment; strength/confirmation checked by [PinPolicy.setup]. */
    @Synchronized
    fun setPin(newPin: String, confirmPin: String): PinPolicy.SetOutcome {
        val outcome = PinPolicy.setup(newPin, confirmPin)
        if (outcome == PinPolicy.SetOutcome.Ok) {
            if (!persist(PinPolicy.VaultState(hash = PinHasher.hash(newPin), fails = 0, lockedUntilMs = 0L))) {
                return PinPolicy.SetOutcome.StorageError
            }
        }
        return outcome
    }

    /** Change flow; verifies the current PIN through the same lockout budget. */
    @Synchronized
    fun changePin(
        currentPin: String,
        newPin: String,
        confirmPin: String,
        nowMs: Long = System.currentTimeMillis()
    ): PinPolicy.SetOutcome {
        val (outcome, next) = PinPolicy.change(state(), currentPin, newPin, confirmPin, nowMs, PinHasher::verify)
        val finalState = if (outcome == PinPolicy.SetOutcome.Ok) {
            next.copy(hash = PinHasher.hash(newPin))
        } else {
            next
        }
        if (finalState != state() && !persist(finalState)) {
            return PinPolicy.SetOutcome.StorageError
        }
        return outcome
    }

    /**
     * Creates a one-time recovery code and stores only its BCrypt hash. Call
     * immediately after first PIN enrollment and show the returned code once.
     */
    @Synchronized
    fun createRecoveryCode(): String? {
        val bytes = ByteArray(10)
        java.security.SecureRandom().nextBytes(bytes)
        val code = bytes.joinToString("") { "%02X".format(it) }.chunked(5).joinToString("-")
        val saved = runCatching {
            prefs().edit().putString(KEY_RECOVERY_HASH, PinHasher.hash(code)).commit()
        }.getOrDefault(false)
        return code.takeIf { saved }
    }

    /** Single-use recovery: replaces the PIN and burns the recovery code. */
    @Synchronized
    fun resetWithRecoveryCode(code: String, newPin: String, confirmPin: String): PinPolicy.SetOutcome {
        val validation = PinPolicy.setup(newPin, confirmPin)
        if (validation != PinPolicy.SetOutcome.Ok) return validation
        val recoveryHash = runCatching { prefs().getString(KEY_RECOVERY_HASH, null) }.getOrNull()
            ?: return PinPolicy.SetOutcome.WrongCurrent
        if (!PinHasher.verify(code.trim().uppercase(), recoveryHash)) return PinPolicy.SetOutcome.WrongCurrent
        val saved = runCatching {
            prefs().edit()
                .putString(KEY_HASH, PinHasher.hash(newPin))
                .remove(KEY_RECOVERY_HASH)
                .putInt(KEY_FAILS, 0)
                .putLong(KEY_LOCKED_UNTIL, 0L)
                .commit()
        }.getOrDefault(false)
        return if (saved) PinPolicy.SetOutcome.Ok else PinPolicy.SetOutcome.StorageError
    }

    /** Remaining milliseconds of an active lockout (0 when unlocked). */
    fun lockRemainingMs(nowMs: Long = System.currentTimeMillis()): Long =
        (state().lockedUntilMs - nowMs).coerceAtLeast(0L)

    // ── Guard suspension (owner authenticated / protection deactivated) ────

    fun suspendGuard(minutes: Int, nowMs: Long = System.currentTimeMillis()) {
        runCatching {
            prefs().edit()
                .putLong(KEY_SUSPENDED_UNTIL, TamperGuardPolicy.suspendUntil(nowMs, minutes))
                .apply()
        }.onFailure { Log.e(TAG, "suspend failed", it) }
    }

    fun suspendedUntilMs(): Long =
        runCatching { prefs()?.getLong(KEY_SUSPENDED_UNTIL, 0L) ?: 0L }.getOrDefault(0L)

    fun isGuardSuspended(nowMs: Long = System.currentTimeMillis()): Boolean =
        nowMs < suspendedUntilMs()
}
