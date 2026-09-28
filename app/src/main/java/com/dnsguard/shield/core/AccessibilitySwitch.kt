package com.dnsguard.shield.core

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import com.dnsguard.shield.service.ShieldAccessibilityService

/**
 * The single authority for flipping Android's global accessibility master
 * switch — the heart of the product's safety guarantee:
 *
 * ```
 *   default state ................ ACCESSIBILITY_ENABLED = 0   (service OFF)
 *   Reddit (com.reddit.frontpage)
 *     in foreground ............. ACCESSIBILITY_ENABLED = 1   (service ON)
 *   Reddit leaves foreground .... ACCESSIBILITY_ENABLED = 0   (service OFF, immediate)
 * ```
 *
 * All writes require `WRITE_SECURE_SETTINGS`. Every method degrades to a safe
 * no-op when the permission is missing (the shield simply stays disabled, which
 * is the safe direction).
 */
object AccessibilitySwitch {

    /** Safety rule: the shield must be off whenever Reddit is not in front. */
    const val REDDIT_PACKAGE = "com.reddit.frontpage"

    fun canWrite(context: Context): Boolean = Permissions.hasWriteSecureSettings(context)

    /**
     * Ensures our service is listed in `enabled_accessibility_services`
     * (preserving every other service the user may have enabled) and flips the
     * global master switch to 1.
     *
     * Callers must have already verified that Reddit is the foreground app.
     *
     * @return true when the switch ended up enabled.
     */
    fun enableForReddit(context: Context): Boolean {
        if (!canWrite(context)) return false
        val resolver = context.contentResolver

        val component = ShieldAccessibilityService.componentName(context).flattenToString()
        val existing = Settings.Secure.getString(
            resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        val merged = mergeEnabledServices(existing, component)
        if (merged != existing) {
            Settings.Secure.putString(
                resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, merged
            )
        }

        Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1)
        return Settings.Secure.getInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1
    }

    /**
     * Forces the global master switch back to 0. Idempotent and cheap; safe to
     * call from every layer (watchdog poll, accessibility event, screen-off
     * broadcast, service teardown).
     *
     * @return true when the switch now reads 0 (or could not be written at all,
     *         in which case the service was never enabled by us anyway).
     */
    fun forceDisable(context: Context): Boolean {
        if (!canWrite(context)) return true
        val resolver = context.contentResolver
        val current = Settings.Secure.getInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0)
        if (current != 0) {
            Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0)
        }
        return Settings.Secure.getInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 0
    }

    /**
     * Adds [ours] to the colon-separated `enabled_accessibility_services` list
     * without disturbing any existing entries.
     */
    internal fun mergeEnabledServices(existing: String?, ours: String): String {
        if (existing.isNullOrBlank()) return ours
        val parts = existing.split(':')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (parts.any { it.equals(ours, ignoreCase = true) }) return existing
        return (parts + ours).joinToString(":")
    }

    /** ComponentName of our accessibility service, for manifest-independent references. */
    fun serviceComponent(context: Context): ComponentName =
        ShieldAccessibilityService.componentName(context)
}
