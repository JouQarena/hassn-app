package com.dnsguard.shield.core

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import com.dnsguard.shield.util.DnsHostname

/**
 * Reads Android Private DNS and, after a one-time ADB grant of
 * WRITE_SECURE_SETTINGS, restores the hostname selected by the user.
 */
object DnsManager {

    private const val TAG = "DnsManager"

    const val KEY_PRIVATE_DNS_MODE = "private_dns_mode"
    const val KEY_PRIVATE_DNS_SPECIFIER = "private_dns_specifier"

    const val MODE_OPPORTUNISTIC = "opportunistic"
    const val MODE_HOSTNAME = "hostname"

    /** Undocumented-but-stable action that opens the Private DNS dialog. */
    const val ACTION_PRIVATE_DNS_SETTINGS = "android.settings.PRIVATE_DNS_SETTINGS"

    /** A single preset offered on the DNS settings screen. */
    data class DnsPreset(
        val id: String,
        val name: String,
        val description: String,
        val hostname: String
    )

    /** The presets rendered on the settings screen, in display order. */
    fun presets(
        familyName: String, familyDesc: String,
        adguardName: String, adguardDesc: String,
        unfilteredName: String, unfilteredDesc: String,
        cloudflareName: String, cloudflareDesc: String,
        cloudflareFamilyName: String, cloudflareFamilyDesc: String,
        quad9Name: String, quad9Desc: String
    ): List<DnsPreset> = listOf(
        DnsPreset("adguard_family", familyName, familyDesc, "family.adguard-dns.com"),
        DnsPreset("adguard", adguardName, adguardDesc, "dns.adguard-dns.com"),
        DnsPreset("adguard_unfiltered", unfilteredName, unfilteredDesc, "unfiltered.adguard-dns.com"),
        DnsPreset("cloudflare", cloudflareName, cloudflareDesc, "one.one.one.one"),
        DnsPreset("cloudflare_family", cloudflareFamilyName, cloudflareFamilyDesc, "security.cloudflare-dns.com"),
        DnsPreset("quad9", quad9Name, quad9Desc, "dns.quad9.net")
    )

    /** Snapshot of the current system Private DNS configuration (read-only). */
    data class DnsStatus(
        val mode: String,
        val specifier: String?
    ) {
        /** Active means an explicit, enforceable DoT hostname is configured. */
        val isActive: Boolean
            get() = mode == MODE_HOSTNAME && !specifier.isNullOrBlank()

        /** Human-readable hostname to show on cards, or null when automatic. */
        val displayHostname: String?
            get() = if (isActive) specifier else null
    }

    fun currentStatus(context: Context): DnsStatus {
        val resolver = context.contentResolver
        val mode = Settings.Global.getString(resolver, KEY_PRIVATE_DNS_MODE)
            ?: MODE_OPPORTUNISTIC
        val specifier = Settings.Global.getString(resolver, KEY_PRIVATE_DNS_SPECIFIER)
        return DnsStatus(mode = mode, specifier = specifier)
    }

    /**
     * Opens Android's Private DNS screen (preferred) or, on OEM skins that do
     * not expose it, the wireless/network settings page where Private DNS can
     * still be reached. The caller is expected to have copied the hostname
     * to the clipboard first.
     *
     * @return the action label that was actually opened, for diagnostics.
     */
    fun openPrivateDnsSettings(context: Context): String = try {
        context.startActivity(Intent(ACTION_PRIVATE_DNS_SETTINGS))
        "PRIVATE_DNS_SETTINGS"
    } catch (e: ActivityNotFoundException) {
        Log.w(TAG, "PRIVATE_DNS_SETTINGS not exposed, falling back to wireless settings", e)
        try {
            context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
            "WIRELESS_SETTINGS"
        } catch (e2: ActivityNotFoundException) {
            Log.e(TAG, "No settings screen available", e2)
            "NONE"
        }
    }

    /** True after WRITE_SECURE_SETTINGS has been granted once through ADB. */
    fun canWriteSecureSettings(context: Context): Boolean =
        context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    /** Suspended compatibility entry point; always performs writes on IO. */
    suspend fun enforceProtectedHostname(context: Context, hostname: String): Boolean =
        DnsEnforcer.enforce(context, hostname)

    /** Validates a user-supplied hostname before it goes on the clipboard. */
    fun isValidHostname(candidate: String): Boolean = DnsHostname.isValid(candidate)
}
