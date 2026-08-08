package com.hassn.app.ui.challenge.activities

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hassn.app.ui.challenge.ActivityDefinition
import kotlinx.coroutines.delay

/**
 * "اشرب كأس ماء" challenge.
 *
 * A 30-second timer counts down while the user has to actually leave
 * the phone (and ideally the room) to fetch and drink a glass of
 * water.  When they return they tap "Done".
 *
 * The screen also shows a "أنا أشرب" acknowledgement which they
 * must tap once to confirm they're holding a glass.  The catch:
 * acknowledgement only becomes available after 8 seconds — the user
 * can't tap it the instant they open the screen.
 */
class WaterChugActivity(
    private val totalSeconds: Int = 30,
    private val ackUnlockSeconds: Int = 8
) : ActivityDefinition() {

    override val key = "water_chug"
    override val title = "اشرب كأس ماء"
    override val subtitle =
        "قف واحضر كأس ماء كامل واشربه. " +
                "المؤقت $totalSeconds ثانية. لا يمكنك الغش هنا."
    override val icon: ImageVector = Icons.Default.LocalDrink
    override val accent = Color(0xFF0288D1)
    override val estimatedSeconds = totalSeconds

    @Composable
    override fun Render(onDone: () -> Unit, onFail: () -> Unit, modifier: Modifier) {
        var elapsedSec by remember { mutableFloatStateOf(0f) }
        var acknowledged by remember { mutableStateOf(false) }
        var done by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            val start = System.currentTimeMillis()
            val total = totalSeconds * 1000L
            while (!done) {
                delay(200)
                val now = System.currentTimeMillis()
                elapsedSec = ((now - start).toFloat() / 1000f).coerceAtMost(totalSeconds.toFloat())
                if ((now - start) >= total) {
                    // Timer ran out — auto-fail.
                    onFail()
                    return@LaunchedEffect
                }
            }
        }

        val progress = elapsedSec / totalSeconds.toFloat()
        val animatedProgress by animateFloatAsState(
            targetValue = progress,
            animationSpec = tween(220),
            label = "chug-progress"
        )
        val canAck = elapsedSec >= ackUnlockSeconds

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

            // Visual water glass
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                // Water level inside the glass — fills as time elapses.
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((180.dp.value * (1f - animatedProgress)).dp)
                            .background(accent)
                    )
                }
                Text(
                    text = "${(totalSeconds - elapsedSec.toInt()).coerceAtLeast(0)}s",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black
                    ),
                    color = accent
                )
            }

            Spacer(Modifier.height(20.dp))

            if (!acknowledged) {
                Button(
                    onClick = {
                        if (canAck) acknowledged = true
                    },
                    enabled = canAck,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        disabledContainerColor = accent.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        if (canAck) "أنا أشرب"
                        else "Wait ${(ackUnlockSeconds - elapsedSec.toInt()).coerceAtLeast(0)}s",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            } else {
                Button(
                    onClick = {
                        if (!done) {
                            done = true
                            onDone()
                        }
                    },
                    enabled = elapsedSec >= ackUnlockSeconds,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("انتهيت — تم!", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
            }
        }
    }
}
