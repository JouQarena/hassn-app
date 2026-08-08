package com.hassn.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log

/**
 * BroadcastReceiver that fires after the device finishes booting.
 *
 * Its only job is to check whether the Accessibility Service has been
 * enabled by the user.  If it has, nothing happens (the service is
 * already running).  If it hasn't, we open the system accessibility
 * settings so the user can turn it on — because after a reboot it's
 * easy to forget that the service needs to be re-enabled (it restarts
 * automatically once enabled).
 *
 * NOTE: On Android 12+ the system may delay BOOT_COMPLETED broadcasts
 * for apps that are not in the "active" bucket.  This receiver is a
 * best-effort convenience; the user can always open the app manually.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED != intent.action) return

        Log.d(TAG, "Boot completed — checking accessibility service status")

        if (!isAccessibilityServiceEnabled(context)) {
            Log.i(TAG, "Accessibility Service is NOT enabled after boot")
            // We intentionally do NOT auto-launch settings — Android
            // blocks background activity launches.  The user will see
            // the "not enabled" state next time they open the app.
        } else {
            Log.d(TAG, "Accessibility Service is already enabled")
        }
    }

    /**
     * Returns `true` if the Focus Redirect accessibility service is
     * currently enabled in the device's accessibility settings.
     */
    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val serviceId = "${context.packageName}/.service.FocusAccessibilityService"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.split(':').any { it.equals(serviceId, ignoreCase = true) }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
