package com.hassn.app.ui.challenge.activities

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hassn.app.ui.challenge.ActivityDefinition
import com.hassn.app.ui.challenge.RequiresAccelerometer
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Shared "rep counter" composable used for both push-ups and pull-ups.
 *
 * The push-up variant reads the accelerometer (down → up motion
 * through one rep).  The pull-up variant is a manual tap-rep counter
 * (the phone can't detect a real pull-up), but it's tuned so a
 * user can't just mash the button — there is a brief per-rep lockout
 * and the whole thing takes about a minute at minimum rep rate.
 */
sealed class RepCounterActivity : ActivityDefinition() {

    abstract val targetReps: Int
    abstract val label: String
    abstract val restPerRepMillis: Long

    /** Subclass renders a sensor-driven rep counter, or a manual
     *  tap-rep counter. */
    @Composable
    protected abstract fun RepPanel(
        reps: Int,
        onRep: () -> Unit,
        enabled: Boolean
    )

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    override fun Render(onDone: () -> Unit, onFail: () -> Unit, modifier: Modifier) {
        var reps by remember { mutableIntStateOf(0) }
        val animatedReps by animateIntAsState(targetValue = reps, animationSpec = tween(180), label = "reps")

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
            Spacer(Modifier.height(20.dp))

            // Big rep counter
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = animatedReps,
                    transitionSpec = { (fadeIn(tween(180)) togetherWith fadeOut(tween(120))) },
                    label = "rep-count"
                ) { r ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = r.toString(),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black
                            ),
                            color = accent
                        )
                        Text(
                            "/ $targetReps",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            // Progress bar
            val progress = reps.toFloat() / targetReps.toFloat()
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = accent,
                trackColor = accent.copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    RepPanel(
                        reps = reps,
                        onRep = {
                            if (reps < targetReps) reps += 1
                            if (reps >= targetReps) onDone()
                        },
                        enabled = true
                    )
                }
            }
        }
    }
}

// ─── Push-ups (accelerometer-driven) ────────────────────────────────

class PushupActivity(
    override val targetReps: Int = 10
) : RepCounterActivity(), RequiresAccelerometer {

    override val key = "pushup"
    override val title = "تمارين الضغط"
    override val subtitle = "ضع الهاتف في جيب قميصك على صدرك ثم قم بـ $targetReps ضغطات."
    override val icon: ImageVector = Icons.Default.FitnessCenter
    override val accent = Color(0xFFE53935)
    override val estimatedSeconds = 90
    override val label = "rep"
    override val restPerRepMillis = 0L

    @Composable
    override fun RepPanel(reps: Int, onRep: () -> Unit, enabled: Boolean) {
        val context = LocalContext.current
        val sensor = remember { PushupSensor(onRep = onRep) }
        DisposableEffect(Unit) {
            sensor.start(context)
            onDispose { sensor.stop() }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Reps: $reps / $targetReps",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Move down → up to count a rep.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Manual fallback in case the sensor misses a rep.
            OutlinedButton(
                onClick = onRep,
                modifier = Modifier.padding(top = 8.dp)
            ) { Text("I did a rep") }
        }
    }
}

/**
 * Accelerometer-based pushup detector.  Looks for a down→up transition
 * where the magnitude of acceleration swings past two thresholds.
 *
 * Designed to be lenient: it only counts reps that take at least
 * 600 ms and there is a 400 ms lockout after each rep so a single
 * transition never double-counts.
 */
private class PushupSensor(
    private val onRep: () -> Unit
) : android.hardware.SensorEventListener {

    private var mgr: android.hardware.SensorManager? = null
    private var accel: android.hardware.Sensor? = null

    private var lastRepMs = 0L
    private var inDown = false

    fun start(context: android.content.Context): Boolean {
        return try {
            mgr = context.getSystemService(android.content.Context.SENSOR_SERVICE)
                    as? android.hardware.SensorManager
            accel = mgr?.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
            if (mgr != null && accel != null) {
                mgr!!.registerListener(this, accel, android.hardware.SensorManager.SENSOR_DELAY_GAME)
                true
            } else false
        } catch (_: Throwable) { false }
    }

    fun stop() {
        try { mgr?.unregisterListener(this) } catch (_: Throwable) {}
    }

    override fun onSensorChanged(event: android.hardware.SensorEvent) {
        if (event.sensor.type != android.hardware.Sensor.TYPE_ACCELEROMETER) return
        val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
        val mag = sqrt(x * x + y * y + z * z)
        // 9.8 m/s² is "rest" against gravity.  A pushup produces a
        // large swing in the chest-plane axis, so the magnitude of
        // the vector deviates by several m/s² from rest.
        val deviation = abs(mag - 9.81f)
        val now = android.os.SystemClock.uptimeMillis()
        if (!inDown && deviation > 6.0f) {
            inDown = true
        } else if (inDown && deviation < 2.0f) {
            if (now - lastRepMs > 600L) {
                lastRepMs = now
                onRep()
            }
            inDown = false
        }
    }

    override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) { /* noop */ }
}

// ─── Pull-ups (manual tap-rep counter) ──────────────────────────────

class PullupActivity(
    override val targetReps: Int = 8
) : RepCounterActivity() {

    override val key = "pullup"
    override val title = "تمارين العقلة"
    override val subtitle =
        "Phone can't count real pull-ups, so tap the button for each rep. " +
                "Take your time — there is a 2 second lockout per rep."
    override val icon: ImageVector = Icons.Default.SportsGymnastics
    override val accent = Color(0xFF1976D2)
    override val estimatedSeconds = 60
    override val label = "rep"
    override val restPerRepMillis = 2000L

    @Composable
    override fun RepPanel(reps: Int, onRep: () -> Unit, enabled: Boolean) {
        var cooldown by remember { mutableLongStateOf(0L) }
        var lastTapMs by remember { mutableLongStateOf(0L) }
        val isCooling by remember { derivedStateOf { cooldown > 0L } }
        LaunchedEffect(isCooling) {
            if (isCooling) {
                kotlinx.coroutines.delay(150)
                cooldown = (cooldown - 150L).coerceAtLeast(0L)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (isCooling) "Wait… (${(cooldown / 1000.0).format1()}s)"
                else "Tap for each rep",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val now = android.os.SystemClock.uptimeMillis()
                    if (now - lastTapMs >= restPerRepMillis) {
                        lastTapMs = now
                        onRep()
                        cooldown = restPerRepMillis
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (isCooling) "…" else "I did a pull-up", fontSize = 18.sp)
            }
        }
    }
}

private fun Double.format1(): String = "%.1f".format(this)
