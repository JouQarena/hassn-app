package com.dnsguard.shield.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.dnsguard.shield.core.ShieldPolicy
import com.dnsguard.shield.core.ShieldRuntime
import com.dnsguard.shield.service.overlay.NsfwOverlay

/**
 * Persistent Reddit-only accessibility shield.
 *
 * Android keeps the permission enabled until the user turns it off. Events
 * outside Reddit are ignored; they never call disableSelf(). The separate
 * TamperGuardAccessibilityService handles protected Android Settings pages.
 */
class ShieldAccessibilityService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var overlay: NsfwOverlay? = null
    private var pendingDetection: Runnable? = null
    private var redditActive = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlay = NsfwOverlay(this)
        Log.i(TAG, "Persistent Reddit shield connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> Unit
            else -> return
        }

        val pkg = event.packageName?.toString()
            ?: rootInActiveWindow?.packageName?.toString()
        if (pkg != ShieldPolicy.REDDIT_PACKAGE) {
            if (redditActive) {
                redditActive = false
                cancelScheduledDetection()
                overlay?.onContentState(false)
                ShieldRuntime.markStandby()
            }
            return
        }

        if (!redditActive) {
            redditActive = true
            ShieldRuntime.markActiveInReddit()
        }
        scheduleDetection()
    }

    override fun onInterrupt() = Unit

    private fun scheduleDetection() {
        cancelScheduledDetection()
        val runnable = Runnable { runDetection() }
        pendingDetection = runnable
        mainHandler.postDelayed(runnable, DETECTION_DEBOUNCE_MS)
    }

    private fun cancelScheduledDetection() {
        pendingDetection?.let(mainHandler::removeCallbacks)
        pendingDetection = null
    }

    private fun runDetection() {
        pendingDetection = null
        if (!redditActive) return
        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != ShieldPolicy.REDDIT_PACKAGE) {
            redditActive = false
            overlay?.onContentState(false)
            ShieldRuntime.markStandby()
            return
        }
        overlay?.onContentState(containsNsfwTag(root))
    }

    private fun containsNsfwTag(root: AccessibilityNodeInfo): Boolean {
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.addLast(root)
        var visited = 0
        while (stack.isNotEmpty() && visited++ < MAX_NODES_VISITED) {
            val node = stack.removeLast()
            if (node.isVisibleToUser) {
                if (node.text?.toString()?.trim().equals(NSFW_TAG, ignoreCase = true) ||
                    node.contentDescription?.toString()?.trim().equals(NSFW_TAG, ignoreCase = true) ||
                    node.viewIdResourceName?.contains(NSFW_TAG, ignoreCase = true) == true
                ) return true
            }
            for (i in 0 until node.childCount) node.getChild(i)?.let(stack::addLast)
        }
        return false
    }

    override fun onDestroy() {
        redditActive = false
        cancelScheduledDetection()
        overlay?.cleanup()
        overlay = null
        ShieldRuntime.markStandby()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "ShieldA11yService"
        private const val NSFW_TAG = "NSFW"
        private const val DETECTION_DEBOUNCE_MS = 250L
        private const val MAX_NODES_VISITED = 400

        fun componentName(context: Context): ComponentName =
            ComponentName(context, ShieldAccessibilityService::class.java)
    }
}
