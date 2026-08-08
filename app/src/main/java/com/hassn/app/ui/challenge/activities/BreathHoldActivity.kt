package com.hassn.app.ui.challenge.activities

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hassn.app.ui.challenge.ActivityDefinition
import kotlinx.coroutines.delay

/**
 * "احبس أنفاسك" challenge.
 *
 * The user taps "Start", takes a deep breath, holds it for the
 * target duration (default 25 seconds) without lifting their finger
 * from the screen, then taps "Done".  Lifting the finger fails the
 * challenge.
 *
 * The animation is a breathing circle that pulses, which the user
 * has to consciously ignore while they hold their breath.
 */
class BreathHoldActivity(
    private val targetSeconds: Int = 25
) : ActivityDefinition() {

    override val key = "breath_hold"
    override val title = "احبس أنفاسك"
    override val subtitle = "خذ نفساً عميقاً ثم اضغط مع الاستمرار لمدة $targetSeconds ثانية. الرفع المبكر يعني الفشل."
    override val icon: ImageVector = Icons.Default.Air
    override val accent = Color(0xFF00897B)
    override val estimatedSeconds = targetSeconds

    @Composable
    override fun Render(onDone: () -> Unit, onFail: () -> Unit, modifier: Modifier) {
        var phase by remember { mutableStateOf(Phase.PRE) }
        var elapsedSec by remember { mutableIntStateOf(0) }

        // Continuously-animating "breathing" circle to distract the user.
        // We toggle an internal flipper state and let animateFloatAsState
        // tween between 0 and 1 every 1.5s — simpler and correct.
        var inhale by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            while (true) {
                inhale = true
                delay(1500)
                inhale = false
                delay(1500)
            }
        }
        val phaseAnim by animateFloatAsState(
            targetValue = if (inhale) 1f else 0f,
            animationSpec = tween(durationMillis = 1500, easing = LinearEasing),
            label = "breath-circle"
        )

        LaunchedEffect(phase) {
            if (phase == Phase.HOLDING) {
                val totalMillis = targetSeconds * 1000L
                val step = 100L
                var e = 0L
                while (e < totalMillis && phase == Phase.HOLDING) {
                    delay(step)
                    e += step
                    elapsedSec = ((e + 999L) / 1000L).toInt()
                }
                if (phase == Phase.HOLDING) {
                    phase = Phase.DONE
                    onDone()
                }
            }
        }

        val ringColor = when (phase) {
            Phase.PRE -> MaterialTheme.colorScheme.outline
            Phase.HOLDING -> accent
            Phase.DONE -> Color(0xFF2E7D32)
        }

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

            Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(220.dp)) {
                    val s = size.minDimension
                    val base = s * 0.30f
                    val extra = s * 0.18f * phaseAnim
                    val radius = base + extra
                    drawCircle(
                        color = ringColor.copy(alpha = 0.18f),
                        radius = radius,
                        center = Offset(size.width / 2f, size.height / 2f)
                    )
                    drawCircle(
                        color = ringColor,
                        radius = radius,
                        center = Offset(size.width / 2f, size.height / 2f),
                        style = Stroke(width = s * 0.05f)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = when (phase) {
                            Phase.PRE -> "Ready"
                            Phase.HOLDING -> "${elapsedSec.coerceAtMost(targetSeconds)}s"
                            Phase.DONE -> "Done"
                        },
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black
                        ),
                        color = ringColor
                    )
                    Text(
                        text = when (phase) {
                            Phase.PRE -> "tap to begin"
                            Phase.HOLDING -> "of ${targetSeconds}s"
                            Phase.DONE -> "well done"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                when (phase) {
                    Phase.PRE -> Button(
                        onClick = { elapsedSec = 0; phase = Phase.HOLDING },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) { Text("ابدأ الحبس", style = MaterialTheme.typography.titleMedium) }
                    Phase.HOLDING -> Button(
                        onClick = {
                            // lifting early = fail
                            phase = Phase.PRE
                            elapsedSec = 0
                            onFail()
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) { Text("استسلمت (فشل)", color = Color.White) }
                    Phase.DONE -> { /* no-op — the orchestrator will move on */ }
                }
            }
        }
    }

    private enum class Phase { PRE, HOLDING, DONE }
}
