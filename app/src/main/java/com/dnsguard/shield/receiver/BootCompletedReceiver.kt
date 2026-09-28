package com.dnsguard.shield.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dnsguard.shield.service.ShieldWatchdogService

/**
 * Restarts the watchdog after a reboot so the guarantee
 * "accessibility is 0 unless Reddit is in the foreground" holds from the very
 * first boot after install. (The accessibility master switch itself persists
 * across reboots as 0, so the safe state is the default.)
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            ShieldWatchdogService.start(context)
        }
    }
}
