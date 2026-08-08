package com.hassn.app.ui.challenge

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A single sub-challenge that the user must complete as part of the
 * "disable monitoring" gauntlet.
 *
 * Each activity is an opaque composable.  When the user has satisfied
 * the activity, it must invoke [onDone] exactly once.  Until then it
 * should keep its own state and render its own UI.
 *
 * To make an activity opt-out impossible:
 *   • The composable owns its state (no back-stack to undo).
 *   • The composable is full-screen — there is no "Cancel" button.
 *   • The composable blocks for a real-world duration or sensor input
 *     that the user cannot fake in a couple of seconds.
 */
abstract class ActivityDefinition {

    abstract val key: String
    abstract val title: String
    abstract val subtitle: String
    abstract val icon: ImageVector
    abstract val accent: Color
    abstract val estimatedSeconds: Int

    @Composable
    abstract fun Render(
        onDone: () -> Unit,
        onFail: () -> Unit,
        modifier: Modifier
    )
}

/**
 * Marker for activities that use the device's sensors.  Used by the
 * orchestrator to skip them on devices that lack an accelerometer
 * (e.g. some Android TV builds).
 */
interface RequiresAccelerometer
