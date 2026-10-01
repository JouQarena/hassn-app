package com.dnsguard.shield.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.dnsguard.shield.DnsGuardApplication
import com.dnsguard.shield.R
import com.dnsguard.shield.core.DnsManager
import com.dnsguard.shield.core.DnsEnforcer
import com.dnsguard.shield.core.Permissions
import com.dnsguard.shield.core.ShieldPolicy
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Foreground watchdog. It observes the foreground package for dashboard state
 * and continuously restores the user-selected Private DNS hostname whenever
 * WRITE_SECURE_SETTINGS has been granted once through ADB.
 */
class ShieldWatchdogService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollJob: Job? = null

    // Every Settings.Global read/write runs off the UI thread. The mutex
    // serializes observer events with periodic fallback checks.
    private val dnsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dnsMutex = Mutex()
    private val observerHandler = Handler(Looper.getMainLooper())
    private var dnsObserver: ContentObserver? = null
    private var pendingDnsCheck: Runnable? = null

    @Volatile
    private var screenInteractive = true

    private var powerReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        registerPowerReceiver()
        registerDnsObserver()
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

    // ── Core observation loop ─────────────────────────────────────────────

    private fun evaluateOnce() {
        val canObserve = screenInteractive && Permissions.hasUsageStats(this)
        val foreground = if (canObserve) queryForegroundPackage() else null
        val redditVisible = foreground == ShieldPolicy.REDDIT_PACKAGE

        // Feeds the dashboard ("Reddit open now") and demotes a stale ACTIVE.
        ShieldRuntime.setRedditVisible(redditVisible && canObserve)

        // Fallback poll in case an OEM does not deliver ContentObserver
        // callbacks. Do not block the usage-stats loop with a binder write.
        launchDnsCheck()
    }

    /**
     * Reconstructs the current foreground package from the usage-events ring
     * buffer (last 10 seconds). Returns null when nothing conclusive is found.
     *
     * MOVE_TO_FOREGROUND/BACKGROUND are deprecated in favour of
     * ACTIVITY_RESUMED/PAUSED, but those constants only exist on API 29+ while
     * this app supports API 28 — both pairs share the same values.
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

    // ── Private DNS change observer ───────────────────────────────────────

    private fun registerDnsObserver() {
        val observer = object : ContentObserver(observerHandler) {
            override fun onChange(selfChange: Boolean) {
                // Android can report our own writes twice (mode + specifier).
                // Debounce the burst, then read and compare both values on IO;
                // a mismatch enforces the *latest saved* target immediately.
                scheduleDnsCheck(immediate = false)
            }
        }
        runCatching {
            contentResolver.registerContentObserver(
                Settings.Global.getUriFor(DnsManager.KEY_PRIVATE_DNS_MODE), false, observer
            )
            contentResolver.registerContentObserver(
                Settings.Global.getUriFor(DnsManager.KEY_PRIVATE_DNS_SPECIFIER), false, observer
            )
            dnsObserver = observer
            scheduleDnsCheck(immediate = true)
        }.onFailure {
            // Avoid leaving the first URI registered if registering the
            // second failed. The 750 ms poll remains as a fallback.
            runCatching { contentResolver.unregisterContentObserver(observer) }
            Log.w(TAG, "Private DNS observer unavailable; poll remains active", it)
        }
    }

    private fun scheduleDnsCheck(immediate: Boolean) {
        if (immediate) {
            launchDnsCheck()
        } else {
            pendingDnsCheck?.let(observerHandler::removeCallbacks)
            val task = Runnable {
                pendingDnsCheck = null
                launchDnsCheck()
            }
            pendingDnsCheck = task
            observerHandler.postDelayed(task, DNS_OBSERVER_DEBOUNCE_MS)
        }
    }

    private fun launchDnsCheck() {
        dnsScope.launch(Dispatchers.IO) {
            dnsMutex.withLock {
                val target = runCatching { DnsGuardApplication.prefs().protectedDnsHostname }
                    .getOrNull() ?: return@withLock
                // Re-read inside the mutex. An observer event caused by our
                // own write should become a no-op; changes while a write is
                // in flight are checked by the next observer/poll iteration.
                val restored = DnsEnforcer.enforce(this@ShieldWatchdogService, target)
                if (!restored) Log.w(TAG, "Private DNS mismatch; restore failed (check ADB grant)")
            }
        }
    }

    // ── Power / screen transitions ────────────────────────────────────────

    private fun registerPowerReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        screenInteractive = false
                        ShieldRuntime.setRedditVisible(false)
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

    // ── Foreground notification ───────────────────────────────────────────

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

    // ── Teardown ──────────────────────────────────────────────────────────

    override fun onDestroy() {
        isRunning = false
        pollJob?.cancel()
        pendingDnsCheck?.let(observerHandler::removeCallbacks)
        pendingDnsCheck = null
        dnsObserver?.let { runCatching { contentResolver.unregisterContentObserver(it) } }
        dnsObserver = null
        scope.cancel()
        dnsScope.cancel()
        powerReceiver?.let { runCatching { unregisterReceiver(it) } }
        powerReceiver = null
        ShieldRuntime.setRedditVisible(false)
        super.onDestroy()
    }

    companion object {
        private const val TAG = "ShieldWatchdog"
        private const val NOTIFICATION_ID = 4711
        private const val POLL_INTERVAL_MS = 750L
        private const val DNS_OBSERVER_DEBOUNCE_MS = 100L
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
