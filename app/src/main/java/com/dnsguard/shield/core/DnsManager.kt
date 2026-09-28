package com.dnsguard.shield.core

import android.content.Context
import android.provider.Settings
import com.dnsguard.shield.util.DnsHostname

/**
 * Read/write access to Android's system-wide *Private DNS* setting.
 *
 * Android stores two `Settings.Global` keys (they are hidden from the public
 * SDK, hence the literal names):
 *  - `private_dns_mode`        → "off" | "opportunistic" | "hostname"
 *  - `private_dns_specifier`   → the DoT hostname when mode == "hostname"
 *
 * Writing them requires `WRITE_SECURE_SETTINGS` (granted over adb — see the
 * in-app ADB Setup Guide).
 */
object DnsManager {

    const val KEY_PRIVATE_DNS_MODE = "private_dns_mode"
    const val KEY_PRIVATE_DNS_SPECIFIER = "private_dns_specifier"

    const val MODE_OFF = "off"
    const val MODE_OPPORTUNISTIC = "opportunistic"
    const val MODE_HOSTNAME = "hostname"

    /** A single preset offered on the DNS settings screen. */
    data class DnsPreset(
        val id: String,
        val name: String,
        val description: String,
        /** Null → applies the "Automatic" (opportunistic) mode. */
        val hostname: String?
    )

    /** The presets rendered on the settings screen, in display order. */
    fun presets(
        familyName: String, familyDesc: String,
        adguardName: String, adguardDesc: String,
        unfilteredName: String, unfilteredDesc: String,
        cloudflareName: String, cloudflareDesc: String,
        cloudflareFamilyName: String, cloudflareFamilyDesc: String,
        quad9Name: String, quad9Desc: String,
        automaticName: String, automaticDesc: String
    ): List<DnsPreset> = listOf(
        DnsPreset("adguard_family", familyName, familyDesc, "family.adguard-dns.com"),
        DnsPreset("adguard", adguardName, adguardDesc, "dns.adguard-dns.com"),
        DnsPreset("adguard_unfiltered", unfilteredName, unfilteredDesc, "unfiltered.adguard-dns.com"),
        DnsPreset("cloudflare", cloudflareName, cloudflareDesc, "one.one.one.one"),
        DnsPreset("cloudflare_family", cloudflareFamilyName, cloudflareFamilyDesc, "security.cloudflare-dns.com"),
        DnsPreset("quad9", quad9Name, quad9Desc, "dns.quad9.net"),
        DnsPreset("automatic", automaticName, automaticDesc, null)
    )

    /** Snapshot of the current system Private DNS configuration. */
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
     * Applies an explicit Private DNS hostname.
     *
     * @return true when both settings were written successfully, false when the
     *         app lacks `WRITE_SECURE_SETTINGS` or the value was rejected.
     */
    fun applyHostname(context: Context, hostname: String): Boolean {
        val normalized = DnsHostname.normalize(hostname)
        if (!DnsHostname.isValid(normalized)) return false
        val resolver = context.contentResolver
        val specifierWritten = Settings.Global.putString(
            resolver, KEY_PRIVATE_DNS_SPECIFIER, normalized
        )
        val modeWritten = Settings.Global.putString(
            resolver, KEY_PRIVATE_DNS_MODE, MODE_HOSTNAME
        )
        return specifierWritten && modeWritten
    }

    /**
     * Switches back to Android's automatic (opportunistic) Private DNS mode.
     *
     * @return true when the mode was written successfully.
     */
    fun applyAutomatic(context: Context): Boolean {
        val resolver = context.contentResolver
        return Settings.Global.putString(resolver, KEY_PRIVATE_DNS_MODE, MODE_OPPORTUNISTIC)
    }
}
