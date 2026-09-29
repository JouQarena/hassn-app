package com.dnsguard.shield.core

import android.content.Context
import com.dnsguard.shield.core.security.PinVault
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide snapshot of the anti-uninstall protection layers, written by
 * [refresh] (dashboard resume, device-admin receiver callbacks, PIN dialog
 * completion) and observed by the dashboard — same singleton-flow pattern as
 * [ShieldRuntime].
 *
 * Layers:
 *  - [Snapshot.adminActive]   — Device Administrator (native uninstall block)
 *  - [Snapshot.pinSet]        — Master PIN enrolled (gates Settings/DNS/deactivation)
 *  - [Snapshot.guardServiceEnabled] — Settings-guard accessibility service ON
 *  - [Snapshot.guardSuspended] — owner recently authenticated; guard standing down
 */
object ProtectionRuntime {

    data class Snapshot(
        val adminActive: Boolean,
        val pinSet: Boolean,
        val guardServiceEnabled: Boolean,
        val guardSuspended: Boolean
    ) {
        /** "Uninstall Protection: Active" needs the hard layer (admin) plus
         *  the PIN that makes deactivation gated. */
        val protectionActive: Boolean get() = adminActive || pinSet
        val fullyProtected: Boolean get() = adminActive && pinSet
    }

    private val _state = MutableStateFlow(
        Snapshot(adminActive = false, pinSet = false, guardServiceEnabled = false, guardSuspended = false)
    )
    val state: StateFlow<Snapshot> = _state.asStateFlow()

    /** Re-reads every layer from the system. Cheap; safe on the main thread
     *  (isAdminActive + AccessibilityManager list + pref reads). */
    fun refresh(context: Context) {
        _state.value = Snapshot(
            adminActive = DeviceAdminManager.isAdminActive(context),
            pinSet = runCatching { PinVault.isPinSet() }.getOrDefault(false),
            guardServiceEnabled = Permissions.isTamperGuardServiceEnabled(context),
            guardSuspended = runCatching { PinVault.isGuardSuspended() }.getOrDefault(false)
        )
    }
}
