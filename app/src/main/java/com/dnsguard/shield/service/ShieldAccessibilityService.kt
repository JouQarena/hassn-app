package com.dnsguard.shield.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.dnsguard.shield.core.ShieldPolicy
import com.dnsguard.shield.core.ShieldRuntime
import com.dnsguard.shield.service.overlay.NsfwOverlay

/**
 * Reddit-only accessibility shield — **standard mode, zero ADB**.
 *
 * The user arms it the normal way (Android Settings → Accessibility →
 * "DNS Guard Reddit Shield" → ON). From that second on the service enforces
 * the banking-safety rule itself:
 *
 *  - GRACE (30 s): Settings / launcher / our own windows are neutral so the
 *    arm-and-switch ritual works; Reddit activates the shield; any *other*
 *    app kills it instantly.
 *  - ACTIVE: only `com.reddit.frontpage` may keep it alive. ANY other
 *    package, or the screen turning off, triggers cleanup + [disableSelf]
 *    within milliseconds.
 *  - DEAD: terminal until the user arms it again.
 *
 * Every decision comes from the pure [ShieldPolicy] state machine (unit
 * tested in `ShieldPolicyTest`); this class only executes actions. It never
 * writes `Settings.Secure` — that would need the ADB-only
 * `WRITE_SECURE_SETTINGS` permission this mode deliberately does not use.
 *
 * While ACTIVE it scans Reddit's view hierarchy for NSFW tags and raises an
 * accessibility overlay that shields content until "Reveal for 10 seconds".
 */
class ShieldAccessibilityService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var overlay: NsfwOverlay? = null
    private var pendingDetection: Runnable? = null
    private var graceTimeout: Runnable? = null
    private var screenReceiver: BroadcastReceiver? = null

    private var phase = ShieldPolicy.Phase.DEAD
    private var neutralPackages: Set<String> = emptySet()
    private var tornDown = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        tornDown = false
        phase = ShieldPolicy.Phase.GRACE
        neutralPackages = resolveNeutralPackages()
        overlay = NsfwOverlay(this)
        registerScreenReceiver()
        scheduleGraceTimeout()
        Log.i(TAG, "Shield armed — GRACE for ${ShieldPolicy.GRACE_PERIOD_MS} ms; neutral=$neutralPackages")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || tornDown) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> Unit
            else -> return
        }

        // Resolve the foreground package: event first, active window second.
        val pkg = event.packageName?.toString()
            ?: rootInActiveWindow?.packageName?.toString()

        apply(ShieldPolicy.onEvent(phase, pkg, neutralPackages), "event pkg=$pkg")
    }

    override fun onInterrupt() {
        // Required by AccessibilityService. The shield never interrupts user
        // interactions outside of its own overlay, so there is nothing to do.
    }

    // ── Decision execution ────────────────────────────────────────────────

    private fun apply(decision: ShieldPolicy.Decision, why: String) {
        val previous = phase
        phase = decision.nextPhase
        when (decision.action) {
            ShieldPolicy.Action.IGNORE -> Unit

            ShieldPolicy.Action.ACTIVATE -> {
                ShieldRuntime.markActiveInReddit()
                cancelGraceTimeout()
                Log.i(TAG, "ACTIVATE ($why)")
                scheduleDetection()
            }

            ShieldPolicy.Action.SCAN -> {
                if (previous != ShieldPolicy.Phase.ACTIVE) {
                    ShieldRuntime.markActiveInReddit()
                }
                scheduleDetection()
            }

            ShieldPolicy.Action.DISABLE -> performDisable("$why → ${decision.action}")
        }
    }

    private fun performDisable(reason: String) {
        if (tornDown) return
        tornDown = true
        phase = ShieldPolicy.Phase.DEAD
        Log.i(TAG, "DISABLE: $reason — removing this service now")

        cancelScheduledDetection()
        cancelGraceTimeout()
        overlay?.cleanup()
        overlay = null
        unregisterScreenReceiver()

        // The guarantee: our component leaves enabled_accessibility_services
        // immediately. No Settings write is needed or attempted.
        ShieldRuntime.markStandby()
        disableSelf()
    }

    // ── Grace timer ───────────────────────────────────────────────────────

    private fun scheduleGraceTimeout() {
        val runnable = Runnable {
            apply(
                ShieldPolicy.onGraceTimeout(phase),
                "grace timeout after ${ShieldPolicy.GRACE_PERIOD_MS} ms"
            )
        }
        graceTimeout = runnable
        mainHandler.postDelayed(runnable, ShieldPolicy.GRACE_PERIOD_MS)
    }

    private fun cancelGraceTimeout() {
        graceTimeout?.let { mainHandler.removeCallbacks(it) }
        graceTimeout = null
    }

    // ── NSFW detection (debounced, ACTIVE only) ───────────────────────────

    private fun scheduleDetection() {
        if (tornDown) return
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
        if (tornDown || phase != ShieldPolicy.Phase.ACTIVE) return

        val root = rootInActiveWindow
        if (root == null) {
            apply(ShieldPolicy.onRootLost(phase), "no active window during scan")
            return
        }

        // Route the resolved window through the same policy so an unexpected
        // active window during ACTIVE can only mean one thing: disable.
        val rootPkg = root.packageName?.toString()
        if (rootPkg != ShieldPolicy.REDDIT_PACKAGE) {
            if (rootPkg == ShieldPolicy.OWN_PACKAGE) return // our overlay is up
            apply(ShieldPolicy.onEvent(phase, rootPkg, neutralPackages), "active window=$rootPkg")
            return
        }

        val nsfwVisible = containsNsfwTag(root)
        overlay?.onContentState(nsfwVisible)
    }

    /**
     * Depth-first scan of the accessibility node tree for Reddit's NSFW marker.
     * Reddit labels adult posts with a chip whose text is exactly "NSFW";
     * matching is exact (trimmed, case-insensitive) to avoid flagging ordinary
     * comments that merely mention the letters. Hard-capped so a pathological
     * tree can never jank the main thread.
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

    // ── Neutral package discovery (GRACE) ─────────────────────────────────

    /**
     * Resolves the *actual* Settings and launcher packages at runtime so the
     * grace window works on every OEM skin (no hardcoded assumptions beyond
     * our own package and SystemUI).
     */
    private fun resolveNeutralPackages(): Set<String> {
        val pm = packageManager
        val settingsPkg = runCatching {
            pm.resolveActivity(Intent(Settings.ACTION_SETTINGS), PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo?.packageName
        }.getOrNull()
        val launcherPkg = runCatching {
            val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            pm.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
        }.getOrNull()
        return setOfNotNull(ShieldPolicy.OWN_PACKAGE, settingsPkg, launcherPkg, "com.android.systemui")
    }

    // ── Screen-off handling ───────────────────────────────────────────────

    private fun registerScreenReceiver() {
        if (screenReceiver != null) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                    apply(ShieldPolicy.onScreenOff(phase), "screen off")
                }
            }
        }
        runCatching {
            registerReceiver(receiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
            screenReceiver = receiver
        }
    }

    private fun unregisterScreenReceiver() {
        screenReceiver?.let { runCatching { unregisterReceiver(it) } }
        screenReceiver = null
    }

    // ── Teardown ──────────────────────────────────────────────────────────

    override fun onDestroy() {
        tornDown = true
        phase = ShieldPolicy.Phase.DEAD
        cancelScheduledDetection()
        cancelGraceTimeout()
        unregisterScreenReceiver()
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
