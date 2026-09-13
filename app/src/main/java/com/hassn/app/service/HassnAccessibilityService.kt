package com.hassn.app.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityService
import com.hassn.app.HassnApp
import com.hassn.app.data.MonitoredApp
import com.hassn.app.data.MonitorMode
import com.hassn.app.data.SettingsRepository
import com.hassn.app.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.jvm.Volatile

class HassnAccessibilityService : AccessibilityService() {

    private lateinit var settingsRepo: SettingsRepository
    private lateinit var privacyEngine: com.hassn.app.detection.PrivacyDetectionEngine
    private lateinit var responseExecutor: ResponseExecutor

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val monitoredAppsCache = mutableMapOf<String, MonitoredApp>()

    @Volatile
    private var protectionEnabled = false
    @Volatile
    private var lastTriggerTime = 0L
    private var keepAliveView: View? = null

    override fun onServiceConnected() {
        super.onServiceConnected()

        val app = applicationContext as HassnApp
        settingsRepo = app.settingsRepository
        privacyEngine = app.privacyDetectionEngine
        responseExecutor = ResponseExecutor(this, settingsRepo, app.statsRepository)

        // Keep the monitored apps cache in sync with DataStore
        scope.launch {
            app.monitoredAppsRepository.getAllMonitoredApps().collect { apps ->
                monitoredAppsCache.clear()
                apps.forEach { app ->
                    monitoredAppsCache[app.packageName] = app
                    if (app.mode == MonitorMode.PRIVATE_ONLY && app.customKeywords.isNotEmpty()) {
                        privacyEngine.registerCustomDetector(app.packageName, app.customKeywords)
                    }
                }
            }
        }

        // Cache protection state (avoids blocking reads on every event)
        scope.launch {
            settingsRepo.protectionEnabled.collect { enabled ->
                protectionEnabled = enabled
                if (enabled) showKeepAliveOverlay() else hideKeepAliveOverlay()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName == Constants.APP_PACKAGE || !protectionEnabled) return

        val monitoredApp = monitoredAppsCache[packageName] ?: return
        if (!monitoredApp.enabled) return

        // Debounce rapid triggers
        val now = System.currentTimeMillis()
        if (now - lastTriggerTime < Constants.DEBOUNCE_MS) return
        lastTriggerTime = now

        when (monitoredApp.mode) {
            MonitorMode.ALWAYS -> triggerResponse()
            MonitorMode.PRIVATE_ONLY -> {
                val rootNode = try {
                    rootInActiveWindow
                } catch (e: Exception) {
                    null
                } ?: return
                try {
                    if (privacyEngine.detect(packageName, rootNode).isPrivate) {
                        triggerResponse()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Detection failed", e)
                } finally {
                    try {
                        rootNode.recycle()
                    } catch (e: Exception) {
                        // node may be already recycled
                    }
                }
            }
        }
    }

    private fun triggerResponse() {
        scope.launch {
            try {
                val behaviors = settingsRepo.selectedBehaviors.first()
                val order = settingsRepo.behaviorOrder.first()
                responseExecutor.execute(behaviors, order)
            } catch (e: Exception) {
                Log.e(TAG, "Response execution failed", e)
            }
        }
    }

    /**
     * A tiny transparent overlay window kept visible while protection is on.
     * It grants the background-activity-start exemption needed to launch
     * the destination app on Android 10+.
     */
    private fun showKeepAliveOverlay() {
        try {
            if (keepAliveView != null || !Settings.canDrawOverlays(this)) return
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val view = View(this)
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }
            val params = WindowManager.LayoutParams(
                1,
                1,
                type,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.TOP or Gravity.START
            wm.addView(view, params)
            keepAliveView = view
        } catch (e: Exception) {
            Log.w(TAG, "Keep-alive overlay failed", e)
        }
    }

    private fun hideKeepAliveOverlay() {
        val view = keepAliveView ?: return
        try {
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(view)
        } catch (e: Exception) {
            Log.w(TAG, "Keep-alive removal failed", e)
        }
        keepAliveView = null
    }

    override fun onInterrupt() {
        Log.w(TAG, "Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        hideKeepAliveOverlay()
        scope.cancel()
    }

    companion object {
        private const val TAG = "HassnService"
    }
}
