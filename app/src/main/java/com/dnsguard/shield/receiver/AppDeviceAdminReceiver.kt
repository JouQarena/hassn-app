package com.dnsguard.shield.receiver

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.dnsguard.shield.core.ProtectionRuntime
import com.dnsguard.shield.ui.i18n.AppLanguage
import com.dnsguard.shield.ui.i18n.Strings
import com.dnsguard.shield.ui.i18n.stringsFor

/**
 * Device Administrator receiver — layer 1 of the anti-uninstall protection.
 *
 * While this admin is **active**, Android itself blocks the normal uninstall
 * paths (launcher long-press → Uninstall, Settings → Apps → Uninstall) with
 * its own "you must first deactivate device administrator" dialog. Nothing
 * here writes a system setting; the platform enforces the rule.
 *
 * Activation happens only through the user tapping **Activate** on Android's
 * native consent screen ([DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN],
 * launched from the dashboard / setup guide). Deactivation is offered in-app
 * behind the Master-PIN gate; Android's own confirmation dialog additionally
 * shows the warning returned by [onDisableRequested].
 *
 * Manifest registration (see AndroidManifest.xml):
 *  - `android:permission="android.permission.BIND_DEVICE_ADMIN"` (only the
 *    system may deliver these broadcasts)
 *  - `meta-data android.app.device_admin → @xml/device_admin_policies`
 *
 * `DeviceAdminReceiver` is deprecated in API 35 but fully functional on
 * minSdk 28 → 35; see DeviceAdminManager for the rationale.
 */
@Suppress("DEPRECATION")
class AppDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        ProtectionRuntime.refresh(context)
        Toast.makeText(context, strings(context).adminActivatedToast, Toast.LENGTH_LONG).show()
    }

    /**
     * Shown by Android's own "Deactivate device admin?" confirmation dialog —
     * the platform-level friction point we get for free.
     */
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return strings(context).adminDisableWarning
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        ProtectionRuntime.refresh(context)
        Toast.makeText(context, strings(context).adminDeactivatedToast, Toast.LENGTH_LONG).show()
    }

    private fun strings(context: Context): Strings {
        val language = runCatching {
            com.dnsguard.shield.DnsGuardApplication.prefs().language
        }.getOrDefault(AppLanguage.EN)
        return stringsFor(language)
    }

    companion object {
        fun componentName(context: Context) =
            android.content.ComponentName(context, AppDeviceAdminReceiver::class.java)
    }
}
