package com.dnsguard.shield.core

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.dnsguard.shield.service.ShieldAccessibilityService
import com.dnsguard.shield.service.ShieldWatchdogService

/**
 * Centralised, side-effect-free permission checks used by the dashboard.
 */
object Permissions {

    /** True when `WRITE_SECURE_SETTINGS` was granted (normally via adb). */
    fun hasWriteSecureSettings(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_SECURE_SETTINGS
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * True when Usage Access (`PACKAGE_USAGE_STATS` app-op) is allowed.
     *
     * The String overload of checkOpNoThrow is deprecated but is the only one
     * available on every supported API level (26+), hence the suppression.
     */
    @Suppress("DEPRECATION")
    fun hasUsageStats(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** True when the watchdog foreground service is currently running. */
    fun isWatchdogRunning(): Boolean = ShieldWatchdogService.isRunning

    /**
     * True when the accessibility master switch currently reads 1.
     * Per the product safety rule this should only ever be true while
     * `com.reddit.frontpage` is the foreground application.
     */
    fun isAccessibilityMasterEnabled(context: Context): Boolean =
        Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0
        ) == 1

    /**
     * True when our own service is listed in the enabled accessibility services
     * (being *listed* is harmless — the global master switch decides whether it
     * actually runs).
     */
    fun isShieldListedAsEnabledService(context: Context): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val wanted = ShieldAccessibilityService.componentName(context).flattenToString()
        return flat.split(':').any { it.equals(wanted, ignoreCase = true) }
    }

    /** Convenience: every prerequisite for the Reddit shield being armed. */
    fun shieldPrerequisitesMet(context: Context): Boolean =
        hasWriteSecureSettings(context) &&
            hasUsageStats(context) &&
            ShieldWatchdogService.isRunning
}
