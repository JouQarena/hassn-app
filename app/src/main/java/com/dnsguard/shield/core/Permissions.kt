package com.dnsguard.shield.core

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import com.dnsguard.shield.service.ShieldAccessibilityService
import com.dnsguard.shield.service.ShieldWatchdogService
import com.dnsguard.shield.service.TamperGuardAccessibilityService

/**
 * Centralised, side-effect-free permission/state checks used by the
 * dashboard and the setup guide — **all of them answerable through normal
 * Android screens, no ADB**.
 */
object Permissions {

    /** True when Usage Access (`PACKAGE_USAGE_STATS` app-op) is allowed.
     *  Granted through Android's normal "Usage access" settings screen. */
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

    /**
     * True when the user has toggled OUR accessibility service ON in
     * Android's Accessibility settings (the normal per-app flow).
     *
     * Uses the public [AccessibilityManager] API — no special permission and
     * no reading of Settings.Secure required.
     */
    fun isShieldServiceEnabled(context: Context): Boolean {
        val manager = context.getSystemService(AccessibilityManager::class.java) ?: return false
        if (!manager.isEnabled) return false
        val target = ShieldAccessibilityService::class.java.name
        return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { info -> info.id?.contains(target) == true }
    }

    /** True when the watchdog foreground service is currently running. */
    fun isWatchdogRunning(): Boolean = ShieldWatchdogService.isRunning

    /**
     * True when the user has toggled the Settings anti-tamper guard ON in
     * Android's Accessibility settings. Same public-API check as
     * [isShieldServiceEnabled] — the two services are independent toggles.
     */
    fun isTamperGuardServiceEnabled(context: Context): Boolean {
        val manager = context.getSystemService(AccessibilityManager::class.java) ?: return false
        if (!manager.isEnabled) return false
        val target = TamperGuardAccessibilityService::class.java.name
        return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { info -> info.id?.contains(target) == true }
    }

    /**
     * True when POST_NOTIFICATIONS is granted; below API 33 the permission does
     * not exist and is treated as granted. The [android.Manifest] constant is a
     * compile-time inlined String, so referencing it below API 33 is safe —
     * hence the benign [android.annotation.SuppressLint] below.
     */
    @SuppressLint("InlinedApi")
    fun hasNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    /**
     * Convenience: everything the shield needs to run — armed accessibility
     * service + live observer. DNS and notifications are enhancements whose
     * state is surfaced separately.
     */
    fun shieldPrerequisitesMet(context: Context): Boolean =
        isShieldServiceEnabled(context) && ShieldWatchdogService.isRunning
}
