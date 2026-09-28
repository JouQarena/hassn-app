package com.dnsguard.shield.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.dnsguard.shield.DnsGuardApplication
import com.dnsguard.shield.R
import com.dnsguard.shield.core.AccessibilitySwitch
import com.dnsguard.shield.core.Permissions
import com.dnsguard.shield.core.ShieldRuntime
import com.dnsguard.shield.ui.i18n.AppLanguage
import com.dnsguard.shield.ui.i18n.stringsFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager

/**
 * Foreground watchdog — the *only* component in the app that ever turns the
 * global accessibility switch ON, and it does so exclusively while
 * `com.reddit.frontpage` is the foreground application.
 *
 * ```
 * poll every 750 ms ──► foreground == com.reddit.frontpage (screen on)
 *                        │
 *                        ├─ yes → ENABLED_ACCESSIBILITY_SERVICES += our service
 *                        │        ACCESSIBILITY_ENABLED = 1
 *                        │
 *                        └─ no  → ACCESSIBILITY_ENABLED = 0   (immediately)
 * ```
 *
 * Screen-off is treated as "not in the foreground": the receiver below forces
 * the switch to 0 without waiting for the next poll, and polls never re-enable
 * while the display is off.
 *
 * If `PACKAGE_USAGE_STATS` is missing the watchdog *cannot* prove Reddit is in
 * front, so it keeps the switch at 0 forever — the failure direction is always
 * the safe one.
 */
class ShieldWatchdogService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollJob: Job? = null

    @Volatile
    private var screenInteractive = true

    @Volatile
    private var redditForeground = false

    private var powerReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        registerPowerReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Foreground notification must be posted within the startup window.
        startAsForeground()

        if (pollJob == null) {
            pollJob = scope.launch {
                while (isActive) {
                    runCatching { evaluateOnce() }.onFailure {
                        Log.w(TAG, "Watchdog evaluation failed", it)
                    }
                    delay(POLL_INTERVAL_MS)
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Core state machine ─────────────────────────────────────────────────

    private fun evaluateOnce() {
        // Without Usage Access we can never *prove* Reddit is in front → stay off.
        if (!Permissions.hasUsageStats(this)) {
            forceOffIfNecessary()
            ShieldRuntime.markIncomplete()
            return
        }

        // Display off ⇒ Reddit is not "in the foreground" for our purposes.
        if (!screenInteractive) {
            forceOffIfNecessary()
            redditForeground = false
            updateRuntimePhase()
            return
        }

        val foreground = queryForegroundPackage()
        if (foreground == AccessibilitySwitch.REDDIT_PACKAGE) {
            if (!redditForeground || !isMasterEnabled()) {
                val enabled = AccessibilitySwitch.enableForReddit(this)
                Log.i(TAG, "Reddit detected → ACCESSIBILITY_ENABLED=${if (enabled) 1 else "0 (WRITE_SECURE_SETTINGS missing)"}")
            }
            redditForeground = true
        } else {
            if (redditForeground || isMasterEnabled()) {
                Log.i(TAG, "Foreground is ${foreground ?: "unknown"} → forcing ACCESSIBILITY_ENABLED=0")
            }
            forceOffIfNecessary()
            redditForeground = false
        }
        updateRuntimePhase()
    }

    private fun updateRuntimePhase() {
        if (!Permissions.shieldPrerequisitesMet(this)) {
            ShieldRuntime.markIncomplete()
            return
        }
        val redditShieldLive = redditForeground && isMasterEnabled()
        if (!redditShieldLive) {
            ShieldRuntime.markStandby()
        }
        // When the shield IS live we deliberately leave the flow untouched:
        // the accessibility service flips it to ACTIVE_IN_REDDIT on connect,
        // and only it can flip it back when Reddit goes away.
    }

    private fun forceOffIfNecessary() {
        AccessibilitySwitch.forceDisable(this)
    }

    private fun isMasterEnabled(): Boolean =
        Settings.Secure.getInt(
            contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0
        ) == 1

    /**
     * Reconstructs the current foreground package from the usage-events ring
     * buffer (last 10 seconds). Returns null when nothing conclusive is found.
     *
     * MOVE_TO_FOREGROUND/BACKGROUND are deprecated in favour of
     * ACTIVITY_RESUMED/PAUSED, but those constants only exist on API 29+ while
     * this app supports API 26 — both pairs share the same values.
     */
    @Suppress("DEPRECATION")
    private fun queryForegroundPackage(): String? {
        val manager = getSystemService(UsageStatsManager::class.java) ?: return null
        val end = System.currentTimeMillis()
        val start = end - LOOKBACK_MS
        val events = manager.queryEvents(start, end) ?: return null

        var current: String? = null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND,
                UsageEvents.Event.ACTIVITY_RESUMED -> current = pkg
                UsageEvents.Event.MOVE_TO_BACKGROUND,
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> if (current == pkg) current = null
                else -> Unit
            }
        }
        return current
    }

    // ── Power / screen transitions ─────────────────────────────────────────

    private fun registerPowerReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        screenInteractive = false
                        redditForeground = false
                        forceOffIfNecessary()
                        ShieldRuntime.markStandby()
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        screenInteractive = true
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        runCatching {
            registerReceiver(receiver, filter)
            powerReceiver = receiver
        }
    }

    // ── Foreground notification ────────────────────────────────────────────

    private fun startAsForeground() {
        val notification = buildNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Unable to promote to foreground service", e)
        } catch (e: IllegalStateException) {
            // ForegroundServiceStartNotAllowedException subclasses surface here
            // when the system refuses a background start; the poll loop simply
            // does not run until the user opens the app again.
            Log.e(TAG, "Foreground start refused", e)
        }
    }

    private fun buildNotification(): Notification {
        val language = runCatching { DnsGuardApplication.prefs().language }
            .getOrDefault(AppLanguage.EN)
        val strings = stringsFor(language)

        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            DnsGuardApplication.CHANNEL_WATCHDOG,
            strings.notifChannelName,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = strings.notifChannelDesc
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)

        return Notification.Builder(this, DnsGuardApplication.CHANNEL_WATCHDOG)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(strings.notifTitle)
            .setContentText(strings.notifText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .build()
    }

    // ── Teardown ───────────────────────────────────────────────────────────

    override fun onDestroy() {
        isRunning = false
        pollJob?.cancel()
        scope.cancel()
        powerReceiver?.let { runCatching { unregisterReceiver(it) } }
        powerReceiver = null
        // Watchdog gone ⇒ nobody can vouch for Reddit being in front ⇒ off.
        AccessibilitySwitch.forceDisable(this)
        redditForeground = false
        ShieldRuntime.markIncomplete()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "ShieldWatchdog"
        private const val NOTIFICATION_ID = 4711
        private const val POLL_INTERVAL_MS = 750L
        private const val LOOKBACK_MS = 10_000L

        @Volatile
        var isRunning: Boolean = false
            private set

        /** Safe to call from activities, receivers and boot. */
        fun start(context: Context) {
            val intent = Intent(context, ShieldWatchdogService::class.java)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: SecurityException) {
                Log.w(TAG, "startForegroundService denied", e)
            } catch (e: IllegalStateException) {
                Log.w(TAG, "startForegroundService not allowed right now", e)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ShieldWatchdogService::class.java))
        }
    }
}
