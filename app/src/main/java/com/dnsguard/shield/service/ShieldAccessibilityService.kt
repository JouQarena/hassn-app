package com.dnsguard.shield.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.dnsguard.shield.core.AccessibilitySwitch
import com.dnsguard.shield.core.Permissions
import com.dnsguard.shield.core.ShieldRuntime
import com.dnsguard.shield.service.overlay.NsfwOverlay

/**
 * Reddit-only accessibility shield.
 *
 * Lifecycle guarantee (belt AND braces — enforced at two independent layers):
 *
 * 1. The [ShieldWatchdogService] only writes `ACCESSIBILITY_ENABLED = 1` while
 *    `com.reddit.frontpage` is the foreground package, and forces it back to `0`
 *    on the very next observation of any other package.
 * 2. This service *self-destructs* the instant it observes a foreground package
 *    other than Reddit — including the moment the screen turns off — by
 *    forcing `ACCESSIBILITY_ENABLED = 0` and calling [disableSelf].
 *
 * While alive it scans Reddit's view hierarchy for NSFW tags and raises an
 * accessibility overlay that shields the content until the user explicitly
 * reveals it for 10 seconds.
 */
class ShieldAccessibilityService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var overlay: NsfwOverlay? = null
    private var pendingDetection: Runnable? = null
    private var screenReceiver: BroadcastReceiver? = null
    private var destroyed = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        destroyed = false
        overlay = NsfwOverlay(this)
        ShieldRuntime.markActiveInReddit()
        registerScreenReceiver()
        scheduleDetection()
        Log.i(TAG, "Shield connected — Reddit is in the foreground")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || destroyed) return

        val eventPackage = event.packageName?.toString()
        if (eventPackage == OWN_PACKAGE) return

        // Layer 2 of the safety guarantee: any package other than Reddit must
        // tear the service down immediately. (Our own overlay windows are
        // filtered out above; null packages are resolved through the window
        // list below.)
        if (eventPackage != null && eventPackage != AccessibilitySwitch.REDDIT_PACKAGE) {
            handleLeaveReddit("foreground package is $eventPackage")
            return
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (eventPackage == AccessibilitySwitch.REDDIT_PACKAGE) {
                    ShieldRuntime.markActiveInReddit()
                    scheduleDetection()
                } else {
                    resolveAndCheckWindows()
                }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (eventPackage == AccessibilitySwitch.REDDIT_PACKAGE) {
                    scheduleDetection()
                } else {
                    resolveAndCheckWindows()
                }
            }
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                resolveAndCheckWindows()
            }
            else -> Unit
        }
    }

    override fun onInterrupt() {
        // Required by AccessibilityService. The shield never interrupts user
        // interactions outside of its own overlay, so there is nothing to do.
    }

    // ── Foreground resolution helpers ──────────────────────────────────────

    /**
     * When an event carries no package (common for TYPE_WINDOWS_CHANGED) we
     * fall back to the active window's package. Anything that is not Reddit —
     * including "unknown" — brings the shield down.
     */
    private fun resolveAndCheckWindows() {
        val activePackage = rootInActiveWindow?.packageName
        when (activePackage) {
            OWN_PACKAGE -> Unit // our own overlay window; ignore
            AccessibilitySwitch.REDDIT_PACKAGE -> {
                ShieldRuntime.markActiveInReddit()
                scheduleDetection()
            }
            else -> handleLeaveReddit("active window is ${activePackage ?: "unknown"}")
        }
    }

    private fun handleLeaveReddit(reason: String) {
        if (destroyed) return
        Log.i(TAG, "Leaving Reddit ($reason) — forcing ACCESSIBILITY_ENABLED=0")
        destroyed = true

        cancelScheduledDetection()
        overlay?.cleanup()
        overlay = null

        unregisterScreenReceiver()

        // Immediate, event-driven enforcement — do not wait for the watchdog.
        AccessibilitySwitch.forceDisable(this)

        ShieldRuntime.markStandby()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            disableSelf()
        }
    }

    // ── NSFW detection (debounced) ─────────────────────────────────────────

    private fun scheduleDetection() {
        if (destroyed) return
        cancelScheduledDetection()
        val runnable = Runnable { runDetection() }
        pendingDetection = runnable
        mainHandler.postDelayed(runnable, DETECTION_DEBOUNCE_MS)
    }

    private fun cancelScheduledDetection() {
        pendingDetection?.let { mainHandler.removeCallbacks(it) }
        pendingDetection = null
    }

    private fun runDetection() {
        pendingDetection = null
        if (destroyed) return

        val root = rootInActiveWindow
        when {
            // Our own shield overlay became the active window: hold current
            // state, never treat it as "some other app".
            root?.packageName == OWN_PACKAGE -> Unit

            root == null -> handleLeaveReddit("no active window")

            root.packageName != AccessibilitySwitch.REDDIT_PACKAGE ->
                handleLeaveReddit("active window is ${root.packageName}")

            else -> {
                val nsfwVisible = containsNsfwTag(root)
                overlay?.onContentState(nsfwVisible)
            }
        }
    }

    /**
     * Depth-first scan of the accessibility node tree for Reddit's NSFW marker.
     *
     * Reddit labels adult posts with a chip whose text is exactly "NSFW".
     * Matching is therefore exact (trimmed, case-insensitive) to avoid flagging
     * ordinary comments that merely mention the letters. A view-id fallback
     * catches builds that expose ids containing "nsfw".
     *
     * The walk is hard-capped so a pathological tree can never jank the main
     * thread.
     */
    private fun containsNsfwTag(root: AccessibilityNodeInfo): Boolean {
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.addLast(root)
        var visited = 0

        while (stack.isNotEmpty() && visited < MAX_NODES_VISITED) {
            val node = stack.removeLast()
            visited++

            if (node.isVisibleToUser) {
                val text = node.text?.toString()?.trim()
                if (text != null && text.equals(NSFW_TAG, ignoreCase = true)) return true

                val desc = node.contentDescription?.toString()?.trim()
                if (desc != null && desc.equals(NSFW_TAG, ignoreCase = true)) return true

                val id = node.viewIdResourceName
                if (id != null && id.contains(NSFW_TAG, ignoreCase = true)) return true
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) stack.addLast(child)
            }
        }
        return false
    }

    // ── Screen-off handling ────────────────────────────────────────────────

    private fun registerScreenReceiver() {
        if (screenReceiver != null) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                    handleLeaveReddit("screen turned off")
                }
            }
        }
        runCatching {
            registerReceiver(receiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
            screenReceiver = receiver
        }
    }

    private fun unregisterScreenReceiver() {
        screenReceiver?.let {
            runCatching { unregisterReceiver(it) }
        }
        screenReceiver = null
    }

    // ── Teardown ───────────────────────────────────────────────────────────

    override fun onDestroy() {
        destroyed = true
        cancelScheduledDetection()
        unregisterScreenReceiver()
        overlay?.cleanup()
        overlay = null
        // Never leave the switch on when the service dies for any reason.
        AccessibilitySwitch.forceDisable(this)
        ShieldRuntime.markStandby()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "ShieldA11yService"
        private const val OWN_PACKAGE = "com.dnsguard.shield"
        private const val NSFW_TAG = "NSFW"
        private const val DETECTION_DEBOUNCE_MS = 250L
        private const val MAX_NODES_VISITED = 400

        fun componentName(context: Context): ComponentName =
            ComponentName(context, ShieldAccessibilityService::class.java)

        /** Cheap helper used by the dashboard to reflect on current state. */
        fun isPrerequisiteHealthy(context: Context): Boolean =
            Permissions.hasWriteSecureSettings(context)
    }
}
