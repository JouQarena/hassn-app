package com.dnsguard.shield.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide state of the Reddit NSFW shield, written by the two services
 * and observed by the dashboard. Keeping it in a singleton flow avoids binder
 * IPC for a value that changes a handful of times per minute.
 *
 * Ownership:
 *  - [ShieldAccessibilityService] owns [Phase]: STANDBY ↔ ACTIVE_IN_REDDIT.
 *  - [ShieldWatchdogService] owns [Snapshot.redditVisible] and demotes a
 *    stale ACTIVE when UsageStats says Reddit is no longer in front.
 */
object ShieldRuntime {

    enum class Phase {
        /** Service armed (enabled) but Reddit not seen yet. */
        STANDBY,

        /** Reddit is in the foreground and the accessibility service is scanning. */
        ACTIVE_IN_REDDIT
    }

    data class Snapshot(
        val phase: Phase,
        val redditVisible: Boolean
    )

    private val _state = MutableStateFlow(Snapshot(Phase.STANDBY, false))
    val state: StateFlow<Snapshot> = _state.asStateFlow()

    /** Called by the accessibility service when Reddit is detected. */
    fun markActiveInReddit() {
        _state.value = Snapshot(Phase.ACTIVE_IN_REDDIT, true)
    }

    /** Called when the shield stands down (disabled, destroyed, left Reddit). */
    fun markStandby() {
        _state.value = _state.value.copy(phase = Phase.STANDBY)
    }

    /** Called by the watchdog with its UsageStats observation. */
    fun setRedditVisible(visible: Boolean) {
        val current = _state.value
        _state.value = if (!visible && current.phase == Phase.ACTIVE_IN_REDDIT) {
            // Reddit is no longer foreground but the service still believes
            // it is active: demote until the service confirms again.
            current.copy(phase = Phase.STANDBY, redditVisible = false)
        } else {
            current.copy(redditVisible = visible)
        }
    }
}
