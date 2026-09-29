package com.dnsguard.shield.core

/**
 * Pure decision engine for the Settings anti-tamper guard
 * (executed by `TamperGuardAccessibilityService`, unit-tested in
 * `TamperGuardPolicyTest`).
 *
 * Scope (deliberately narrow):
 *  - The guard only ever reacts to **system Settings-family windows**
 *    (`com.android.settings`, resolved OEM Settings packages, and the
 *    `*packageinstaller*` uninstall dialogs). Events from any other app —
 *    including the Reddit shield's own flow — are ignored.
 *  - A window is *guarded* only when it exposes DNS Guard's full app label
 *    or exact package name. Destructive keywords identify actions *within*
 *    that screen — seeing “Uninstall” for a different app is not sufficient.
 *  - Response layers:
 *      WINDOW_STATE / CONTENT on a guarded screen → SHOW_CHALLENGE (PIN
 *        overlay) when a Master PIN is set, otherwise EXIT_BACK.
 *      VIEW_CLICKED on a destructive control → EXIT_BACK immediately
 *        (`performGlobalAction(GLOBAL_ACTION_BACK)`), no challenge first.
 *  - After a successful PIN entry the guard stands down for
 *    [UNINSTALL_RELEASE_MINUTES] minutes so the legitimate owner can finish
 *    the operation they unlocked (deactivate / uninstall). An in-app,
 *    PIN-gated "Deactivate protection" suspends it for
 *    [DEACTIVATE_RELEASE_MINUTES].
 *
 * All timing/locking constants live here (single source of truth) and every
 * transition is a pure function of [GuardState] + [Observation] so the whole
 * adversarial matrix is testable on the JVM.
 */
object TamperGuardPolicy {

    // ── Identity constants ─────────────────────────────────────────────────

    /** Our real applicationId (see app/build.gradle.kts). */
    const val OWN_PACKAGE = "com.dnsguard.shield"

    /** The launcher label (res/values/strings.xml `app_name`) — kept in sync
     *  by `GuardCopyTest`. */
    const val APP_LABEL = "DNS Guard & Shield"

    /** Fallback Settings package when runtime resolution fails. */
    const val DEFAULT_SETTINGS_PACKAGE = "com.android.settings"

    /** Any package containing this token is treated as an (un)installer UI
     *  (com.google.android.packageinstaller, com.android.packageinstaller,
     *  OEM variants). */
    const val PACKAGE_INSTALLER_TOKEN = "packageinstaller"

    // ── Destructive keywords (EN + AR, exactly as specified) ───────────────

    /** English destructive UI copy (matched case-insensitively, whitespace
     *  normalised, as *contains*). */
    val ENGLISH_KEYWORDS: List<String> = listOf(
        "uninstall",
        "force stop",
        "clear data",
        "clear storage",
        "deactivate device admin",
        "device admin"
    )

    /** Arabic destructive UI copy. */
    val ARABIC_KEYWORDS: List<String> = listOf(
        "إلغاء التثبيت",
        "إيقاف إجباري",
        "مسح البيانات",
        "مسح مساحة التخزين",
        "مسؤولو الجهاز"
    )

    val ALL_KEYWORDS: List<String> = ENGLISH_KEYWORDS + ARABIC_KEYWORDS

    /** Settings activity class-name fingerprints (substring, case-insensitive)
     *  that identify our app-info / device-admin screens even before any text
     *  node is available. */
    val GUARDED_CLASS_FRAGMENTS: List<String> = listOf(
        "InstalledAppDetails",   // Settings → Apps → DNS Guard
        "AppDeviceAdminInfo",    // Settings → our device-admin detail page
        "UninstallApp",          // Settings uninstall confirm variant
        "Uninstaller"            // packageinstaller UninstallerActivity
    )

    // ── Timing constants (single source of truth) ──────────────────────────

    /** Grace after a successful PIN challenge: the owner may finish the
     *  unlocked operation (deactivate device admin → uninstall). */
    const val UNINSTALL_RELEASE_MINUTES = 2

    /** Grace after the in-app, PIN-gated "Deactivate protection". */
    const val DEACTIVATE_RELEASE_MINUTES = 10

    /** Minimum gap between two EXIT_BACK actions (anti back-fight). */
    const val BACK_COOLDOWN_MS = 900L

    /** Minimum gap between two content-scan evaluations. */
    const val CONTENT_DEBOUNCE_MS = 400L

    /** How many UI nodes the guard walks per scan (main-thread cap). */
    const val MAX_NODES_VISITED = 200

    // ── Model ──────────────────────────────────────────────────────────────

    /** Normalised accessibility observation handed to the policy. */
    enum class Kind { WINDOW_STATE, CONTENT, CLICK, LEFT }

    data class Observation(
        val kind: Kind,
        val packageName: String?,
        /** `AccessibilityEvent.className` (window state events). */
        val className: String?,
        /** Normalisation happens inside the policy; raw text is fine. */
        val texts: List<String>,
        val nowMs: Long
    )

    /** Everything the policy remembers between events. */
    data class GuardState(
        /** True when Device Admin is active or a Master PIN is set. The guard
         *  is dormant (never acts) until protection has actually been set up. */
        val protectionActive: Boolean,
        val pinSet: Boolean,
        val suspendedUntilMs: Long,
        val challengeVisible: Boolean,
        val lastBackAtMs: Long,
        /** Signature of the screen the challenge was raised for. */
        val challengedSignature: String?
    ) {
        companion object {
            val DORMANT = GuardState(
                protectionActive = false,
                pinSet = false,
                suspendedUntilMs = 0L,
                challengeVisible = false,
                lastBackAtMs = 0L,
                challengedSignature = null
            )
        }
    }

    enum class Action { IGNORE, EXIT_BACK, SHOW_CHALLENGE, DISMISS_CHALLENGE }

    data class Decision(val action: Action, val state: GuardState)

    /** How a package relates to the guard. */
    enum class Guardedness { GUARDED, OWN, OTHER }

    // ── Pure helpers ───────────────────────────────────────────────────────

    fun classifyPackage(pkg: String?, settingsPackages: Set<String>): Guardedness = when {
        pkg == null -> Guardedness.OTHER
        pkg == OWN_PACKAGE -> Guardedness.OWN
        pkg in settingsPackages -> Guardedness.GUARDED
        pkg.contains(PACKAGE_INSTALLER_TOKEN, ignoreCase = true) -> Guardedness.GUARDED
        else -> Guardedness.OTHER
    }

    /** Lower-cases, trims and collapses all whitespace runs to one space so
     *  "  Force   Stop " still matches "force stop". Arabic text needs no
     *  case folding but gets the same whitespace normalisation. */
    fun normalize(text: String): String =
        text.trim().lowercase().replace(WHITESPACE_RUN, " ")

    private val WHITESPACE_RUN = Regex("\\s+")

    /** Returns every destructive keyword contained in [texts] (normalised). */
    fun matchKeywords(texts: Iterable<String>): List<String> {
        val normalized = texts.map { normalize(it) }.filter { it.isNotEmpty() }
        if (normalized.isEmpty()) return emptyList()
        return ALL_KEYWORDS.filter { keyword ->
            val k = normalize(keyword)
            normalized.any { it.contains(k) }
        }
    }

    /** True when any text exposes our identity (package name or full app
     *  label). Note: the *partial* label "DNS Guard" (e.g. the Reddit shield's
     *  accessibility entry) deliberately does NOT match — enabling/disabling
     *  accessibility services must stay possible (see TamperGuardPolicyTest). */
    fun matchesIdentity(texts: Iterable<String>): Boolean {
        val normalized = texts.map { normalize(it) }
        return normalized.any {
            it.contains(OWN_PACKAGE) || it.contains(normalize(APP_LABEL))
        }
    }

    /** True when the window's class name fingerprints one of the guarded
     *  Settings screens. */
    fun matchesClassFingerprint(className: String?): Boolean {
        if (className.isNullOrBlank()) return false
        val lower = className.lowercase()
        return GUARDED_CLASS_FRAGMENTS.any { lower.contains(it.lowercase()) }
    }

    /** Stable identity of the guarded screen, so a challenge is raised once
     *  per screen instead of once per content event. */
    fun screenSignature(
        packageName: String?,
        className: String?,
        keywordMatches: List<String>
    ): String = buildString {
        append(packageName ?: "?")
        append('|')
        append(className?.substringAfterLast('.') ?: "?")
        append('|')
        append(keywordMatches.sorted().joinToString(","))
    }

    // ── Core transition ────────────────────────────────────────────────────

    /**
     * The one function the service calls per (debounced) accessibility event.
     * [settingsPackages] are the runtime-resolved Settings-family packages
     * (always containing [DEFAULT_SETTINGS_PACKAGE] as a fallback).
     */
    fun onObservation(
        state: GuardState,
        obs: Observation,
        settingsPackages: Set<String>
    ): Decision {
        // 1) Dormant until protection actually exists.
        if (!state.protectionActive) return Decision(Action.IGNORE, state)

        // 2) Owner authenticated recently → stand down; drop a stale overlay.
        if (obs.nowMs < state.suspendedUntilMs) {
            return if (state.challengeVisible) {
                Decision(Action.DISMISS_CHALLENGE, state.copy(challengeVisible = false, challengedSignature = null))
            } else {
                Decision(Action.IGNORE, state)
            }
        }

        return when (classifyPackage(obs.packageName, settingsPackages)) {
            Guardedness.OWN ->
                // Back inside our own app: the overlay has no purpose.
                if (state.challengeVisible) {
                    Decision(Action.DISMISS_CHALLENGE, state.copy(challengeVisible = false, challengedSignature = null))
                } else {
                    Decision(Action.IGNORE, state)
                }

            Guardedness.OTHER -> {
                // Left the Settings family: dismiss any floating challenge and
                // reset the per-screen memory. Never act on other apps.
                if (state.challengeVisible) {
                    Decision(Action.DISMISS_CHALLENGE, state.copy(challengeVisible = false, challengedSignature = null))
                } else {
                    Decision(
                        Action.IGNORE,
                        state.copy(challengedSignature = null)
                    )
                }
            }

            Guardedness.GUARDED -> onGuardedEvent(state, obs, settingsPackages)
        }
    }

    private fun onGuardedEvent(
        state: GuardState,
        obs: Observation,
        @Suppress("UNUSED_PARAMETER") settingsPackages: Set<String>
    ): Decision {
        val fingerprinted = matchesClassFingerprint(obs.className)
        val keywordMatches = matchKeywords(obs.texts)
        val identity = matchesIdentity(obs.texts)
        // Never trap unrelated apps or the generic Device Admin list. A
        // destructive keyword alone (e.g. "Uninstall" on another app) is
        // insufficient: require DNS Guard's full label or package name.
        val guardedScreen = identity

        if (!guardedScreen) {
            // A Settings screen with nothing destructive on it (Wi-Fi,
            // display, the accessibility list itself, …): the guard is a
            // bystander.
            return Decision(
                Action.IGNORE,
                state.copy(challengedSignature = null)
            )
        }

        val signature = screenSignature(obs.packageName, obs.className, keywordMatches + identityTag(identity) + fingerprintTag(fingerprinted))

        // One challenge per screen: while the overlay is up (or was raised for
        // this exact screen), further events for it are absorbed.
        if (state.challengeVisible || signature == state.challengedSignature) {
            return Decision(Action.IGNORE, state)
        }

        return when (obs.kind) {
            // A destructive *click* is stopped the hard way, immediately —
            // there is no time to ask for a PIN mid-click.
            Kind.CLICK -> if (keywordMatches.isNotEmpty()) exitBack(state, obs.nowMs)
                else Decision(Action.IGNORE, state)

            Kind.WINDOW_STATE, Kind.CONTENT ->
                if (state.pinSet) {
                    Decision(
                        Action.SHOW_CHALLENGE,
                        state.copy(challengeVisible = true, challengedSignature = signature)
                    )
                } else {
                    exitBack(state, obs.nowMs)
                }

            // LEFT never carries a guarded package; defensive branch.
            Kind.LEFT -> Decision(Action.IGNORE, state)
        }
    }

    private fun identityTag(identity: Boolean): List<String> =
        if (identity) listOf("identity") else emptyList()

    private fun fingerprintTag(fingerprinted: Boolean): List<String> =
        if (fingerprinted) listOf("class") else emptyList()

    private fun exitBack(state: GuardState, nowMs: Long): Decision {
        // Anti back-fight cooldown: rapid duplicate events collapse into one
        // BACK press.
        if (nowMs - state.lastBackAtMs < BACK_COOLDOWN_MS) {
            return Decision(Action.IGNORE, state)
        }
        return Decision(
            Action.EXIT_BACK,
            state.copy(lastBackAtMs = nowMs, challengedSignature = null)
        )
    }

    // ── Suspension bookkeeping (used by the vault / deactivation flow) ─────

    fun suspendUntil(nowMs: Long, minutes: Int): Long = nowMs + minutes * 60_000L
}
