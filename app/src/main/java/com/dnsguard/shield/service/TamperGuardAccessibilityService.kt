package com.dnsguard.shield.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.dnsguard.shield.core.ProtectionRuntime
import com.dnsguard.shield.core.TamperGuardPolicy
import com.dnsguard.shield.core.security.PinPolicy.AttemptOutcome as PinPolicyOutcome
import com.dnsguard.shield.core.security.PinVault
import com.dnsguard.shield.service.overlay.PinChallengeOverlay
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Settings anti-tamper guard — a **dedicated** accessibility service, fully
 * separate from [ShieldAccessibilityService].
 *
 * Why not inside the Reddit shield? [ShieldAccessibilityService] is contract-
 * bound to call `disableSelf()` the instant anything except Reddit owns the
 * screen (the banking-safety guarantee, 17 unit tests). It is therefore *dead*
 * exactly when this guard must work (Settings in the foreground). Two
 * services, two contracts, zero interference:
 *
 *  - Reddit shield: alive only over Reddit, NSFW overlay.
 *  - This guard: alive while the user keeps it enabled, observes **only**
 *    Settings-family windows, never renders over any other app.
 *
 * Every decision comes from the pure [TamperGuardPolicy] (unit-tested in
 * TamperGuardPolicyTest); this class only executes:
 *  - `EXIT_BACK`        → [performGlobalAction] GLOBAL_ACTION_BACK
 *  - `SHOW_CHALLENGE`   → [PinChallengeOverlay] (PIN dialog above Settings)
 *  - `DISMISS_CHALLENGE`→ overlay teardown
 *
 * Privacy posture: events from every app arrive (the service cannot know the
 * OEM Settings package statically), but anything outside the Settings family
 * exits through the policy's IGNORE/DISMISS branch without a single node
 * being read — node scans happen only for guarded packages.
 */
class TamperGuardAccessibilityService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var verifyExecutor: ExecutorService? = null
    private var overlay: PinChallengeOverlay? = null
    private var pendingScan: Runnable? = null

    private var state = TamperGuardPolicy.GuardState.DORMANT
    private var settingsPackages: Set<String> = setOf(TamperGuardPolicy.DEFAULT_SETTINGS_PACKAGE)

    // ── Lifecycle ──────────────────────────────────────────────────────────

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        verifyExecutor = Executors.newSingleThreadExecutor()
        settingsPackages = resolveSettingsPackages()
        rebuildState()
        ProtectionRuntime.refresh(this)
        Log.i(TAG, "Tamper guard connected; settings packages = $settingsPackages")
    }

    override fun onInterrupt() {
        // Nothing to interrupt: the guard never runs a continuous feedback
        // stream, it only reacts to window events.
    }

    override fun onDestroy() {
        isRunning = false
        cancelPendingScan()
        removeOverlay()
        verifyExecutor?.shutdownNow()
        verifyExecutor = null
        super.onDestroy()
    }

    // ── Event pipeline ─────────────────────────────────────────────────────

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkg = event.packageName?.toString()
        // Our PIN overlay itself emits accessibility events under the app's
        // package. Do not interpret those as "left Settings" or we would
        // dismiss the challenge as soon as its EditText appears.
        if (pkg == packageName && overlay != null) return
        val guarded = TamperGuardPolicy.classifyPackage(pkg, settingsPackages)

        // The soft keyboard and SystemUI emit their own window-state events
        // while our challenge is visible. Ignoring all non-Settings packages
        // here keeps those events from dismissing the PIN prompt. "Leave"
        // and successful verification are explicit dismissal paths.
        if (guarded == TamperGuardPolicy.Guardedness.OTHER) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                cancelPendingScan()
                apply(
                    TamperGuardPolicy.Observation(
                        kind = TamperGuardPolicy.Kind.WINDOW_STATE,
                        packageName = pkg,
                        className = event.className?.toString(),
                        texts = if (guarded == TamperGuardPolicy.Guardedness.GUARDED) collectVisibleTexts() else emptyList(),
                        nowMs = now()
                    )
                )
            }

            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Debounced rescan (content events fire in bursts).
                if (guarded == TamperGuardPolicy.Guardedness.GUARDED || state.challengeVisible) {
                    scheduleScan(pkg, event.className?.toString())
                }
            }

            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                if (guarded == TamperGuardPolicy.Guardedness.GUARDED) {
                    // Only a click on a destructive control triggers BACK;
                    // seeing an unrelated "Uninstall" elsewhere on the
                    // page must not cancel a benign tap.
                    val clickedTexts = event.text.map { it.toString() } +
                        listOfNotNull(event.contentDescription?.toString())
                    if (TamperGuardPolicy.matchKeywords(clickedTexts).isEmpty()) return
                    val texts = clickedTexts + collectVisibleTexts()
                    apply(
                        TamperGuardPolicy.Observation(
                            kind = TamperGuardPolicy.Kind.CLICK,
                            packageName = pkg,
                            className = event.className?.toString(),
                            texts = texts,
                            nowMs = now()
                        )
                    )
                }
            }

            else -> Unit
        }
    }

    private fun scheduleScan(pkg: String?, className: String?) {
        cancelPendingScan()
        val runnable = Runnable {
            pendingScan = null
            val texts = collectVisibleTexts()
            apply(
                TamperGuardPolicy.Observation(
                    kind = TamperGuardPolicy.Kind.CONTENT,
                    packageName = pkg ?: rootInActiveWindow?.packageName?.toString(),
                    className = className,
                    texts = texts,
                    nowMs = now()
                )
            )
        }
        pendingScan = runnable
        mainHandler.postDelayed(runnable, TamperGuardPolicy.CONTENT_DEBOUNCE_MS)
    }

    private fun cancelPendingScan() {
        pendingScan?.let { mainHandler.removeCallbacks(it) }
        pendingScan = null
    }

    // ── Decision execution ─────────────────────────────────────────────────

    private fun apply(obs: TamperGuardPolicy.Observation) {
        rebuildState()
        val decision = TamperGuardPolicy.onObservation(state, obs, settingsPackages)
        state = decision.state

        when (decision.action) {
            TamperGuardPolicy.Action.IGNORE -> Unit

            TamperGuardPolicy.Action.EXIT_BACK -> {
                Log.i(TAG, "EXIT_BACK pkg=${obs.packageName} class=${obs.className}")
                removeOverlay()
                performGlobalAction(GLOBAL_ACTION_BACK)
            }

            TamperGuardPolicy.Action.SHOW_CHALLENGE -> {
                Log.i(TAG, "SHOW_CHALLENGE pkg=${obs.packageName} class=${obs.className}")
                showOverlay()
            }

            TamperGuardPolicy.Action.DISMISS_CHALLENGE -> removeOverlay()
        }
    }

    /** Re-reads the protection inputs so the policy always decides on fresh
     *  facts (PIN set/removed, admin toggled, suspension expired). */
    private fun rebuildState() {
        val adminActive = ProtectionRuntime.state.value.adminActive ||
            com.dnsguard.shield.core.DeviceAdminManager.isAdminActive(this)
        val pinSet = runCatching { PinVault.isPinSet() }.getOrDefault(false)
        val suspendedUntil = runCatching { PinVault.suspendedUntilMs() }.getOrDefault(0L)
        state = state.copy(
            protectionActive = adminActive || pinSet,
            pinSet = pinSet,
            suspendedUntilMs = suspendedUntil
        )
        ProtectionRuntime.refresh(this)
    }

    // ── Node scanning (guarded packages only) ──────────────────────────────

    private fun collectVisibleTexts(): List<String> {
        val root = rootInActiveWindow ?: return emptyList()
        val out = ArrayList<String>()
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.addLast(root)
        var visited = 0
        while (stack.isNotEmpty() && visited < TamperGuardPolicy.MAX_NODES_VISITED) {
            val node = stack.removeLast()
            visited++
            if (node.isVisibleToUser) {
                node.text?.toString()?.takeIf { it.isNotBlank() }?.let(out::add)
                node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let(out::add)
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let(stack::addLast)
            }
        }
        return out
    }

    // ── PIN challenge overlay ──────────────────────────────────────────────

    private fun showOverlay() {
        if (overlay != null) return
        val o = PinChallengeOverlay(
            service = this,
            verify = { pin, callback ->
                val executor = verifyExecutor
                if (executor == null || executor.isShutdown) {
                    // Service tearing down mid-verify: fail closed with the
                    // full attempt budget unknown → report as plain error.
                    callback(PinChallengeOverlay.VerifyResult.Error(remainingAttempts = 0))
                } else {
                    executor.execute {
                        val outcome = runCatching { PinVault.attemptUnlock(pin) }
                        val result = when (val o = outcome.getOrNull()) {
                            is PinPolicyOutcome.Success -> PinChallengeOverlay.VerifyResult.Success
                            is PinPolicyOutcome.Failure ->
                                PinChallengeOverlay.VerifyResult.Error(o.remainingAttempts)
                            is PinPolicyOutcome.Locked ->
                                PinChallengeOverlay.VerifyResult.Locked(o.retryAfterMs)
                            // Vault unreadable → fail closed as a wrong PIN
                            // with zero remaining (forces the "Leave" path).
                            else -> PinChallengeOverlay.VerifyResult.Error(remainingAttempts = 0)
                        }
                        mainHandler.post { callback(result) }
                    }
                }
            },
            onUnlocked = {
                // Owner authenticated: stand the guard down for the release
                // window so they can finish deactivating / uninstalling.
                runCatching {
                    PinVault.suspendGuard(TamperGuardPolicy.UNINSTALL_RELEASE_MINUTES)
                }
                rebuildState()
                removeOverlay()
            },
            onLeave = {
                removeOverlay()
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
        )
        overlay = o
        o.show()
    }

    private fun removeOverlay() {
        overlay?.cleanup()
        overlay = null
        if (state.challengeVisible) {
            state = state.copy(challengeVisible = false, challengedSignature = null)
        }
    }

    // ── Settings package discovery ─────────────────────────────────────────

    /** Resolves the *real* Settings package on this OEM skin; always keeps
     *  the AOSP default as a fallback. */
    private fun resolveSettingsPackages(): Set<String> {
        val pm = packageManager
        val resolved = runCatching {
            pm.resolveActivity(Intent(Settings.ACTION_SETTINGS), PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo?.packageName
        }.getOrNull()
        return setOfNotNull(TamperGuardPolicy.DEFAULT_SETTINGS_PACKAGE, resolved)
    }

    private fun now(): Long = System.currentTimeMillis()

    companion object {
        private const val TAG = "TamperGuardA11y"

        @Volatile
        var isRunning: Boolean = false
            private set

        fun componentName(context: Context): ComponentName =
            ComponentName(context, TamperGuardAccessibilityService::class.java)
    }
}
