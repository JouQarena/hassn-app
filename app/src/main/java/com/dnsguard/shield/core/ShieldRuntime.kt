package com.dnsguard.shield.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide state of the Reddit NSFW shield, written by the two services and
 * observed by the dashboard. Keeping it in a singleton flow avoids binder IPC
 * for a value that changes a handful of times per minute.
 */
object ShieldRuntime {

    enum class Phase {
        /** Watchdog is up, prerequisites are met, waiting for Reddit. */
        STANDBY,

        /** Reddit is in the foreground and the accessibility service is connected. */
        ACTIVE_IN_REDDIT,

        /** At least one prerequisite (permission / watchdog) is missing. */
        INCOMPLETE
    }

    data class Snapshot(
        val phase: Phase,
        val accessibilityEnabledAt: Long
    )

    private val _state = MutableStateFlow(Snapshot(Phase.INCOMPLETE, 0L))
    val state: StateFlow<Snapshot> = _state.asStateFlow()

    fun markStandby() {
        _state.value = Snapshot(Phase.STANDBY, _state.value.accessibilityEnabledAt)
    }

    fun markActiveInReddit() {
        _state.value = Snapshot(Phase.ACTIVE_IN_REDDIT, System.currentTimeMillis())
    }

    fun markIncomplete() {
        _state.value = Snapshot(Phase.INCOMPLETE, _state.value.accessibilityEnabledAt)
    }
}
