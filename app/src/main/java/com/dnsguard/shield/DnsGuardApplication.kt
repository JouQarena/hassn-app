package com.dnsguard.shield

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.dnsguard.shield.core.Prefs
import com.dnsguard.shield.ui.i18n.stringsFor

/**
 * Application entry point. Creates the notification channel used by the
 * foreground watchdog as soon as the process comes up, and exposes a process-wide
 * [Prefs] instance so the accessibility service and the watchdog can read the
 * user's language preference without a UI context.
 */
class DnsGuardApplication : Application() {

    lateinit var prefs: Prefs
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = Prefs(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val strings = stringsFor(prefs.language)
        val channel = NotificationChannel(
            CHANNEL_WATCHDOG,
            strings.notifChannelName,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = strings.notifChannelDesc
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_WATCHDOG = "dns_guard_watchdog"

        @Volatile
        private var instance: DnsGuardApplication? = null

        fun getInstance(): DnsGuardApplication =
            instance ?: error("DnsGuardApplication has not been created yet")

        fun prefs(): Prefs =
            instance?.prefs ?: error("DnsGuardApplication has not been created yet")
    }
}
