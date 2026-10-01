package com.dnsguard.shield.core

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.dnsguard.shield.util.DnsHostname
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One restoration attempt; the foreground service owns the IO CoroutineScope
 * and serializes calls from its observer and polling fallback.
 *
 * WRITE_SECURE_SETTINGS must have been explicitly granted with ADB. Without
 * it, no ordinary app can change Settings.Global. A false return means the
 * value is not confirmed restored; never report success based on putString
 * alone. This is best-effort (the device/user can revoke permission).
 */
object DnsEnforcer {
    private const val TAG = "DnsEnforcer"

    /** Pure comparison shared by observer, writer and tests. */
    fun matches(status: DnsManager.DnsStatus, target: String): Boolean =
        status.mode == DnsManager.MODE_HOSTNAME && status.specifier == target

    /** Does not write on the main thread, even if called there by mistake. */
    suspend fun enforce(context: Context, hostname: String): Boolean = withContext(Dispatchers.IO) {
        if (!DnsHostname.isValid(hostname)) return@withContext false
        try {
            if (!DnsManager.canWriteSecureSettings(context)) return@withContext false
            val resolver = context.contentResolver
            if (matches(DnsManager.currentStatus(context), hostname)) {
                return@withContext true
            }
            // Write the hostname before switching to strict mode, then verify
            // BOTH values. Two Settings.Global writes are not an atomic OS
            // transaction; the observer and periodic poll handle later changes.
            val specifierWritten = Settings.Global.putString(
                resolver, DnsManager.KEY_PRIVATE_DNS_SPECIFIER, hostname
            )
            val modeWritten = Settings.Global.putString(
                resolver, DnsManager.KEY_PRIVATE_DNS_MODE, DnsManager.MODE_HOSTNAME
            )
            val restored = specifierWritten && modeWritten &&
                matches(DnsManager.currentStatus(context), hostname)
            if (!restored) Log.w(TAG, "Private DNS write did not restore both values")
            restored
        } catch (e: SecurityException) {
            Log.w(TAG, "WRITE_SECURE_SETTINGS not granted or revoked", e)
            false
        } catch (e: RuntimeException) {
            Log.w(TAG, "Private DNS read/write failed", e)
            false
        }
    }
}
