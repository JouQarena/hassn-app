package com.dnsguard.shield.core.security

import at.favre.lib.crypto.bcrypt.BCrypt

/**
 * BCrypt hashing for the Master PIN, backed by the real
 * `at.favre.lib:bcrypt` library (pure Java — the very same code runs in JVM
 * unit tests, see `PinHasherTest`).
 *
 * Cost 10 keeps an on-device verify around ~50–150 ms on mid-range hardware
 * (run off the main thread) while making offline brute force of a recovered
 * hash expensive; at-rest exposure is further reduced because the hash itself
 * lives inside EncryptedSharedPreferences (AES-256, Android Keystore).
 */
object PinHasher {

    const val COST = 10

    /** Produces a salted `$2a$10$…` hash; never returns the PIN itself. */
    fun hash(pin: String): String =
        BCrypt.withDefaults().hashToString(COST, pin.toCharArray())

    /** Constant-time verification; any malformed hash or error ⇒ false. */
    fun verify(pin: String, hash: String): Boolean =
        runCatching {
            BCrypt.verifyer().verify(pin.toCharArray(), hash).verified
        }.getOrDefault(false)
}
