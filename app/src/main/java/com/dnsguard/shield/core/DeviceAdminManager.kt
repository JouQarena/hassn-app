package com.dnsguard.shield.core

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.dnsguard.shield.receiver.AppDeviceAdminReceiver

/**
 * Thin wrapper over [DevicePolicyManager] for the uninstall-protection layer.
 *
 * While the admin component is active, Android itself refuses a normal
 * uninstall from the launcher / Settings ("first deactivate device admin") —
 * that is the platform's native behaviour, nothing here writes any system
 * setting.
 *
 * Note: `DeviceAdminReceiver` is deprecated since API 35 in favour of
 * device-owner provisioning, but it remains fully functional for the
 * prevent-uninstall use case on every supported version (minSdk 28 → 35),
 * and device-owner mode is impossible without factory provisioning — hence
 * the deliberate `@Suppress("DEPRECATION")`.
 */
@Suppress("DEPRECATION")
object DeviceAdminManager {

    fun componentName(context: Context): ComponentName =
        AppDeviceAdminReceiver.componentName(context)

    /** True while the user has the admin switch ON for this app. */
    fun isAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(DevicePolicyManager::class.java) ?: return false
        return runCatching { dpm.isAdminActive(componentName(context)) }.getOrDefault(false)
    }

    /**
     * Intent for Android's native "Activate device admin?" consent screen —
     * the only way an admin ever becomes active (user tap required).
     */
    fun activationIntent(context: Context, explanation: String): Intent =
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName(context))
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, explanation)
        }

    /**
     * Asks the platform to remove the admin (async; the receiver's
     * `onDisabled` fires when it lands). The UI must only call this behind a
     * successful Master-PIN challenge.
     */
    fun deactivate(context: Context) {
        val dpm = context.getSystemService(DevicePolicyManager::class.java) ?: return
        runCatching { dpm.removeActiveAdmin(componentName(context)) }
    }
}
