package com.dnsguard.shield.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.dnsguard.shield.core.RedditTogglePolicy
import com.dnsguard.shield.core.ShieldPolicy
import com.dnsguard.shield.core.ShieldRuntime
import com.dnsguard.shield.service.overlay.NsfwOverlay

/**
 * Persistent Reddit-only accessibility shield. The manifest points here (the
 * repository has no class named RedditAccessibilityService).
 *
 * In Reddit settings a keyword may be in a text node while the actual Switch
 * is a sibling. We search the label's immediate parent subtree, or one small
 * wrapper above it, for a CHECKED checkable node. We NEVER click an unchecked
 * node (which would turn the setting on). Click events trigger an immediate
 * rescan, not a blind ACTION_CLICK. Elsewhere in Reddit, matching post labels
 * with no nearby toggle retain the existing NSFW content overlay behavior.
 * No nodes from another package are scanned.
 */
class ShieldAccessibilityService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var overlay: NsfwOverlay? = null
    private var pendingDetection: Runnable? = null
    private var redditActive = false
    private var lastToggleAtMs = -TOGGLE_COOLDOWN_MS

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlay = NsfwOverlay(this)
        Log.i(TAG, "Reddit shield connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED,
            AccessibilityEvent.TYPE_VIEW_CLICKED -> Unit
            else -> return
        }

        val pkg = event.packageName?.toString()
            ?: rootInActiveWindow?.packageName?.toString()
        // An accessibility overlay has our own package, not Reddit's. Its
        // window events must not accidentally dismiss the Reddit shield.
        if (pkg == ShieldPolicy.OWN_PACKAGE) return
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
        // A click on a switch can be delivered before isChecked updates; scan
        // on the next main-loop turn and again after a short settling delay.
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            cancelScheduledDetection()
            mainHandler.post { runDetection() }
            scheduleDetection(CLICK_SETTLE_MS)
        } else {
            scheduleDetection(DETECTION_DEBOUNCE_MS)
        }
    }

    override fun onInterrupt() = Unit

    private fun scheduleDetection(delayMs: Long) {
        cancelScheduledDetection()
        val runnable = Runnable { runDetection() }
        pendingDetection = runnable
        mainHandler.postDelayed(runnable, delayMs)
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
        val result = scanReddit(root)
        val candidate = result.checkedToggle
        if (candidate != null) {
            // Multiple accessibility/content events can describe the same
            // click. Never reactivate a switch based on a stale checked state.
            val now = SystemClock.uptimeMillis()
            if (now - lastToggleAtMs >= TOGGLE_COOLDOWN_MS && candidate.isChecked) {
                lastToggleAtMs = now
                overlay?.onContentState(false)
                val clicked = candidate.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (clicked) {
                    performGlobalAction(GLOBAL_ACTION_BACK)
                } else {
                    Log.w(TAG, "Reddit toggle ACTION_CLICK failed; will retry on next event")
                }
            }
        } else {
            // In a settings row with an OFF switch there is no adult post to
            // overlay; normal Reddit posts with NSFW labels remain shielded.
            overlay?.onContentState(result.hasKeyword && !result.hasSettingToggle)
        }
    }

    private data class ScanResult(
        var hasKeyword: Boolean = false,
        var hasSettingToggle: Boolean = false,
        var checkedToggle: AccessibilityNodeInfo? = null
    )

    /** Recursive (bounded) walk of the visible Reddit hierarchy. */
    private fun scanReddit(root: AccessibilityNodeInfo): ScanResult {
        val result = ScanResult()
        var remaining = MAX_NODES_VISITED

        fun visit(node: AccessibilityNodeInfo, depth: Int) {
            if (remaining-- <= 0 || depth > MAX_TREE_DEPTH || result.checkedToggle != null) return
            if (node.isVisibleToUser) {
                val label = RedditTogglePolicy.matchesLabel(node.text?.toString()) ||
                    RedditTogglePolicy.matchesLabel(node.contentDescription?.toString())
                if (label) {
                    result.hasKeyword = true
                    val toggle = findNearbyToggle(node)
                    if (toggle != null) {
                        result.hasSettingToggle = true
                        if (RedditTogglePolicy.shouldTurnOff(
                                isToggle(toggle), toggle.isChecked
                            )) result.checkedToggle = toggle
                    }
                }
            }
            if (result.checkedToggle != null) return
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                visit(child, depth + 1)
                if (result.checkedToggle != null) return
            }
        }
        visit(root, 0)
        return result
    }

    /** Look for siblings/children in the setting's row, NOT anywhere on the
     *  page. Limit ancestor and descendant breadth so a feed post mentioning
     *  NSFW cannot toggle an unrelated switch elsewhere in Reddit. */
    private fun findNearbyToggle(label: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (isToggle(label) && label.isVisibleToUser) return label
        searchRow(label)?.let { return it }
        val parent = label.parent ?: return null
        searchRow(parent)?.let { return it }
        // A Text wrapper may contain only the label; one small container up
        // can contain the sibling Switch. Reject large sections/whole screens.
        if (parent.childCount <= 2) {
            val row = parent.parent
            if (row != null) return searchRow(row)
        }
        return null
    }

    private fun searchRow(row: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (row.childCount > MAX_ROW_CHILDREN) return null
        var remaining = MAX_ROW_NODES
        fun visit(node: AccessibilityNodeInfo, depth: Int): AccessibilityNodeInfo? {
            if (remaining-- <= 0 || depth > MAX_ROW_DEPTH) return null
            if (node.isVisibleToUser && isToggle(node)) return node
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                visit(child, depth + 1)?.let { return it }
            }
            return null
        }
        return visit(row, 0)
    }

    private fun isToggle(node: AccessibilityNodeInfo): Boolean =
        node.isCheckable || node.className?.toString()?.let {
            it.contains("Switch", ignoreCase = true) || it.contains("CheckBox", ignoreCase = true)
        } == true

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
        private const val DETECTION_DEBOUNCE_MS = 200L
        private const val CLICK_SETTLE_MS = 80L
        private const val TOGGLE_COOLDOWN_MS = 900L
        private const val MAX_NODES_VISITED = 500
        private const val MAX_TREE_DEPTH = 70
        private const val MAX_ROW_NODES = 45
        private const val MAX_ROW_DEPTH = 4
        private const val MAX_ROW_CHILDREN = 12

        fun componentName(context: Context): ComponentName =
            ComponentName(context, ShieldAccessibilityService::class.java)
    }
}
