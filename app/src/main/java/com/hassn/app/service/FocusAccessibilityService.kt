package com.hassn.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.hassn.app.HassnApp
import com.hassn.app.data.MonitoredApp
import com.hassn.app.data.SettingsDataStore
import com.hassn.app.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HassnAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isMonitoringEnabled = false
    private var targetAppPackage: String? = null
    private var monitoredApps: List<MonitoredApp> = emptyList()
    // Fast lookup
    private var alwaysPackages: Set<String> = emptySet()
    private var privateOnlyPackages: Set<String> = emptySet()

    private var lastRedirectTimestamp = 0L
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var settingsDataStore: SettingsDataStore

    override fun onCreate() {
        super.onCreate()
        settingsDataStore = HassnApp.instance.settingsDataStore
        Log.d(TAG, "Service created")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 200
        }
        serviceScope.launch {
            settingsDataStore.monitoringEnabled.collectLatest { enabled ->
                isMonitoringEnabled = enabled
                Log.d(TAG, "Monitoring enabled = $enabled")
            }
        }
        serviceScope.launch {
            settingsDataStore.targetAppPackage.collectLatest { pkg ->
                targetAppPackage = pkg
                Log.d(TAG, "Target app = $pkg")
            }
        }
        serviceScope.launch {
            settingsDataStore.monitoredApps.collectLatest { apps ->
                monitoredApps = apps
                alwaysPackages = apps.filter { it.isAlways }.map { it.packageName }.toSet()
                privateOnlyPackages = apps.filter { it.isPrivateOnly }.map { it.packageName }.toSet()
                Log.d(TAG, "Monitored apps: always=$alwaysPackages privateOnly=$privateOnlyPackages")
            }
        }
        Log.d(TAG, "Service connected and configured")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!isMonitoringEnabled || targetAppPackage.isNullOrBlank()) return
        val packageName = event.packageName?.toString() ?: return
        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return

        Log.v(TAG, "Event type=$eventType package=$packageName")

        // Don't redirect from destination app itself
        if (packageName == targetAppPackage) return

        // Check custom monitored apps + built-in fallback
        when {
            alwaysPackages.contains(packageName) -> {
                if (shouldProceed(eventType)) {
                    Log.i(TAG, "Always-redirect app detected: $packageName")
                    redirectToChosenApp()
                }
            }
            privateOnlyPackages.contains(packageName) || privateOnlyPackages.any { packageName.startsWith(it) } -> {
                if (shouldProceed(eventType) && isIncognitoModeActive()) {
                    Log.i(TAG, "Private-only app incognito detected: $packageName")
                    redirectToChosenApp()
                }
            }
            // Built-in fallback: ريديت/كروم/بريف يعمل حتى بدون إضافتهم يدوياً
            packageName == com.hassn.app.util.Constants.REDDIT_PACKAGE ||
            packageName.startsWith(com.hassn.app.util.Constants.CHROME_PACKAGE) ||
            packageName == com.hassn.app.util.Constants.BRAVE_PACKAGE -> {
                if (shouldProceed(eventType) && isIncognitoModeActive()) {
                    Log.i(TAG, "Built-in private app detected: $packageName")
                    redirectToChosenApp()
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "onInterrupt() called")
    }

    override fun onDestroy() {
        serviceScope.cancel()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
    }

    private fun shouldProceed(eventType: Int): Boolean {
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return true
        val now = System.currentTimeMillis()
        return (now - lastRedirectTimestamp) > Constants.DEBOUNCE_MS
    }

    private fun isIncognitoModeActive(): Boolean {
        val root = rootInActiveWindow ?: run {
            Log.v(TAG, "No active window root")
            return false
        }
        return try {
            searchNodeForIncognito(root)
        } finally {
            root.recycle()
        }
    }

    private fun searchNodeForIncognito(node: AccessibilityNodeInfo): Boolean {
        fun String?.safeLower(): String = this?.lowercase(java.util.Locale.ROOT).orEmpty()
        val text = node.text?.toString().safeLower()
        val contentDesc = node.contentDescription?.toString().safeLower()
        val viewId = node.viewIdResourceName.safeLower()
        val className = node.className?.toString().safeLower()
        val combined = "$text $contentDesc $viewId $className"
        for (keyword in Constants.INCOGNITO_KEYWORDS) {
            if (keyword.lowercase(java.util.Locale.ROOT) in combined) {
                Log.d(TAG, "Incognito keyword '$keyword' matched")
                return true
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = try { searchNodeForIncognito(child) } finally { child.recycle() }
            if (found) return true
        }
        return false
    }

    private fun redirectToChosenApp() {
        val now = System.currentTimeMillis()
        if (now - lastRedirectTimestamp < Constants.DEBOUNCE_MS) {
            Log.v(TAG, "Redirect suppressed by debounce")
            return
        }
        lastRedirectTimestamp = now
        val targetPkg = targetAppPackage ?: return
        val homeSuccess = performGlobalAction(GLOBAL_ACTION_HOME)
        Log.d(TAG, "GLOBAL_ACTION_HOME success = $homeSuccess")
        handler.postDelayed({
            try {
                val launchIntent = packageManager.getLaunchIntentForPackage(targetPkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    )
                    startActivity(launchIntent)
                    Log.i(TAG, "Launched destination app: $targetPkg")
                } else {
                    Log.w(TAG, "Could not find launch intent for $targetPkg")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch destination app", e)
            }
        }, Constants.REDIRECT_DELAY_MS)
    }

    companion object {
        private const val TAG = "HassnAccessibilitySvc"
    }
}

// Keep alias for manifest compatibility
typealias FocusAccessibilityService = HassnAccessibilityService
