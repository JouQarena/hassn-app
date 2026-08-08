package com.hassn.app.ui.challenge.activities

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hassn.app.ui.challenge.ActivityDefinition
import com.hassn.app.ui.challenge.RequiresAccelerometer
import kotlinx.coroutines.delay
import kotlin.math.sqrt

/**
 * "Hold the phone perfectly still" challenge.
 *
 * Reads the accelerometer and counts down a hold timer as long as the
 * device stays within an angular deviation of ~3° from its initial
 * orientation.  If the user moves the phone, the timer resets.
 *
 * Default target: 30 seconds.  Took a real-person test of trying to
 * hold the phone still while also doing something distracting on
 * another surface — most people bail before the timer hits zero.
 */
class HoldStillActivity(
    private val targetMillis: Long = 30_000L,
    private val deviationLimit: Float = 0.6f
) : ActivityDefinition(), RequiresAccelerometer {

    override val key = "hold_still"
    override val title = "اثبت بلا حركة"
    override val subtitle = "ضع الهاتف على سطح مستوٍ ولا تحركه لمدة ${targetMillis / 1000} ثانية. أي اهتزاز يعيد العد."
    override val icon: ImageVector = Icons.Default.SelfImprovement
    override val accent = Color(0xFF6A1B9A)
    override val estimatedSeconds = (targetMillis / 1000).toInt()

    @Composable
    override fun Render(onDone: () -> Unit, onFail: () -> Unit, modifier: Modifier) {
        val context = LocalContext.current
        var remaining by remember { mutableLongStateOf(targetMillis) }
        var baselineX by remember { mutableFloatStateOf(Float.NaN) }
        var baselineY by remember { mutableFloatStateOf(Float.NaN) }
        var baselineZ by remember { mutableFloatStateOf(Float.NaN) }
        var lastDeviation by remember { mutableFloatStateOf(0f) }
        var resetting by remember { mutableStateOf(false) }
        var hasSensor by remember { mutableStateOf(true) }

        val sensor = remember {
            HoldStillSensor(
                onSample = { x, y, z ->
                    if (baselineX.isNaN()) {
                        baselineX = x; baselineY = y; baselineZ = z
                    } else {
                        val dx = x - baselineX
                        val dy = y - baselineY
                        val dz = z - baselineZ
                        val dev = sqrt(dx * dx + dy * dy + dz * dz)
                        lastDeviation = dev
                        // The gravity vector rotates; a small linear
                        // magnitude of change means "basically still".
                        resetting = dev > deviationLimit
                    }
                }
            )
        }

        DisposableEffect(Unit) {
            hasSensor = sensor.start(context)
            onDispose { sensor.stop() }
        }

        // If the device has no accelerometer, don't let the user
        // breeze through by just waiting.  Auto-fail so the
        // orchestrator can move on to the next activity.
        LaunchedEffect(hasSensor) {
            if (!hasSensor) {
                onFail()
            }
        }

        LaunchedEffect(resetting, remaining, hasSensor) {
            while (remaining > 0L && hasSensor) {
                delay(100)
                if (resetting) {
                    remaining = targetMillis
                } else {
                    remaining = (remaining - 100L).coerceAtLeast(0L)
                }
            }
            if (hasSensor && remaining <= 0L) onDone()
        }

        val animatedRemaining by animateFloatAsState(
            targetValue = if (resetting) 0f else remaining.toFloat() / targetMillis.toFloat(),
            animationSpec = tween(220),
            label = "hold-progress"
        )

        Column(
            modifier = modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(220.dp)) {
                    val s = size.minDimension
                    val stroke = Stroke(width = s * 0.06f)
                    // Background ring
                    drawCircle(
                        color = accent.copy(alpha = 0.15f),
                        radius = s * 0.45f,
                        center = Offset(size.width / 2f, size.height / 2f),
                        style = stroke
                    )
                    // Progress arc
                    val sweep = 360f * animatedRemaining
                    drawArc(
                        color = if (resetting) Color(0xFFD32F2F) else accent,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(size.width / 2f - s * 0.45f, size.height / 2f - s * 0.45f),
                        size = androidx.compose.ui.geometry.Size(s * 0.9f, s * 0.9f),
                        style = stroke
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (resetting) "Wiggled! Resetting…"
                        else "${(remaining / 1000L)}s",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black
                        ),
                        color = if (resetting) Color(0xFFD32F2F) else accent
                    )
                    Text(
                        "of ${targetMillis / 1000}s",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "Tilt detected: ${"%.2f".format(lastDeviation)} (limit ${"%.2f".format(deviationLimit)})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private class HoldStillSensor(
    private val onSample: (Float, Float, Float) -> Unit
) : android.hardware.SensorEventListener {

    private var mgr: android.hardware.SensorManager? = null
    private var accel: android.hardware.Sensor? = null

    fun start(context: android.content.Context): Boolean {
        return try {
            mgr = context.getSystemService(android.content.Context.SENSOR_SERVICE)
                    as? android.hardware.SensorManager
            accel = mgr?.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
            if (mgr != null && accel != null) {
                mgr!!.registerListener(this, accel, android.hardware.SensorManager.SENSOR_DELAY_UI)
                true
            } else false
        } catch (_: Throwable) { false }
    }

    fun stop() {
        try { mgr?.unregisterListener(this) } catch (_: Throwable) {}
    }

    override fun onSensorChanged(event: android.hardware.SensorEvent) {
        if (event.sensor.type != android.hardware.Sensor.TYPE_ACCELEROMETER) return
        onSample(event.values[0], event.values[1], event.values[2])
    }

    override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) { /* noop */ }
}
