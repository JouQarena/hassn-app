package com.dnsguard.shield.core

/**
 * Pure decision engine for the Reddit-only accessibility shield.
 *
 * Standard mode (zero ADB): the user arms the shield by toggling the service
 * in Android's Accessibility settings (the normal, dialog-free flow every
 * app uses). The service then enforces the banking-safety rule by *removing
 * itself* ([android.accessibilityservice.AccessibilityService.disableSelf])
 * the instant anything other than Reddit owns the screen.
 *
 * Lifecycle (all transitions decided here so they are unit-testable):
 *
 * ```
 *  ON in Settings ──► GRACE ──(neutral windows: Settings / launcher / us)…
 *                      │
 *                      ├─ Reddit window seen ──► ACTIVE (scan NSFW, overlay)
 *                      ├─ any other app ──────► DEAD   (disableSelf)
 *                      └─ 30 s with no Reddit ► DEAD   (disableSelf)
 *
 *  ACTIVE ── non-Reddit window ──► DEAD (disableSelf, immediate)
 *  ACTIVE ── screen off ─────────► DEAD (disableSelf, immediate)
 * ```
 *
 * There is deliberately no code path that writes `Settings.Secure` — that
 * would require `WRITE_SECURE_SETTINGS` (an ADB-only permission).
 * The guarantee is therefore: *our component is removed from
 * `enabled_accessibility_services` the moment Reddit is not in front*,
 * rather than flipping the global `accessibility_enabled` bit.
 */
object ShieldPolicy {

    /** The only package the shield is ever allowed to run over. */
    const val REDDIT_PACKAGE = "com.reddit.frontpage"

    /** Our own package (overlay windows must never count as "another app"). */
    const val OWN_PACKAGE = "com.dnsguard.shield"

    /**
     * How long the service may stay alive after being enabled without Reddit
     * appearing. Long enough for Settings → switch-to-Reddit, short enough
     * that arming and then opening a banking app still ends disabled.
     */
    const val GRACE_PERIOD_MS = 30_000L

    /** Where the service is in its lifecycle. */
    enum class Phase {
        /** Just enabled; allowed to watch neutral windows while the user navigates to Reddit. */
        GRACE,

        /** Reddit is in the foreground; NSFW scanning is running. */
        ACTIVE,

        /** disableSelf() issued (or pending); no further actions. */
        DEAD
    }

    /** What the service must do for an observation. */
    enum class Action {
        /** Do nothing, stay in the current phase. */
        IGNORE,

        /** Reddit found during grace: start scanning, mark runtime ACTIVE. */
        ACTIVATE,

        /** Reddit already active: run the (debounced) NSFW scan. */
        SCAN,

        /** Run away: cleanup + disableSelf(). The safety outcome. */
        DISABLE
    }

    /** Decision + the phase to transition to. */
    data class Decision(val action: Action, val nextPhase: Phase)

    /**
     * Core transition: given the current [phase], an observed foreground
     * package [pkg] (already resolved: event package, else active-window
     * package), and the set of [neutral] packages that are acceptable while
     * in GRACE (Settings, launcher, ourselves).
     */
    fun onEvent(phase: Phase, pkg: String?, neutral: Set<String>): Decision =
        when (phase) {
            // A dead service takes no further actions — belt and braces.
            Phase.DEAD -> Decision(Action.IGNORE, Phase.DEAD)

            Phase.GRACE -> when {
                // Cannot resolve the window yet: keep waiting inside grace.
                pkg == null -> Decision(Action.IGNORE, Phase.GRACE)

                // The moment Reddit appears the shield is live.
                pkg == REDDIT_PACKAGE -> Decision(Action.ACTIVATE, Phase.ACTIVE)

                // Settings / launcher / our own UI: expected during the
                // arm-and-switch ritual — stay alive.
                pkg == OWN_PACKAGE || pkg in neutral -> Decision(Action.IGNORE, Phase.GRACE)

                // Any real app during grace = the user went somewhere else:
                // fail safe, die immediately.
                else -> Decision(Action.DISABLE, Phase.DEAD)
            }

            Phase.ACTIVE -> when {
                // Still Reddit: keep scanning.
                pkg == REDDIT_PACKAGE -> Decision(Action.SCAN, Phase.ACTIVE)

                // Our own overlay window resurfacing in the event stream.
                pkg == OWN_PACKAGE -> Decision(Action.IGNORE, Phase.ACTIVE)

                // Unresolvable while active: fail safe.
                pkg == null -> Decision(Action.DISABLE, Phase.DEAD)

                // ANY other package: the banking-safety rule. Die now.
                else -> Decision(Action.DISABLE, Phase.DEAD)
            }
        }

    /** Screen turned off: Reddit is no longer verifiably in front. */
    fun onScreenOff(phase: Phase): Decision = when (phase) {
        Phase.DEAD -> Decision(Action.IGNORE, Phase.DEAD)
        else -> Decision(Action.DISABLE, Phase.DEAD)
    }

    /** GRACE timer expired without Reddit ever appearing. */
    fun onGraceTimeout(phase: Phase): Decision = when (phase) {
        Phase.GRACE -> Decision(Action.DISABLE, Phase.DEAD)
        else -> Decision(Action.IGNORE, phase)
    }

    /** Active scan could not resolve any window at all. */
    fun onRootLost(phase: Phase): Decision = when (phase) {
        Phase.ACTIVE -> Decision(Action.DISABLE, Phase.DEAD)
        else -> Decision(Action.IGNORE, phase)
    }
}
