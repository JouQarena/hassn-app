package com.hassn.app.ui.challenge.activities

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hassn.app.ui.challenge.ActivityDefinition
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Color sequence (Simon-says) — the app shows a sequence of colored
 * tiles that light up one at a time.  The user must then tap them
 * back in the same order.  Each successful round adds one tile to
 * the sequence, so the challenge escalates on its own.
 */
class ColorSequenceActivity(
    private val startLength: Int = 4,
    private val targetLength: Int = 7
) : ActivityDefinition() {

    override val key = "color_seq"
    override val title = "تسلسل الألوان"
    override val subtitle = "شاهد المربعات تضيء ثم أعد الضغط بنفس الترتيب. خطأ = فشل."
    override val icon: ImageVector = Icons.Default.Palette
    override val accent = Color(0xFFAD1457)
    override val estimatedSeconds = 60

    private val palette = listOf(
        Color(0xFFE53935), // red
        Color(0xFF43A047), // green
        Color(0xFF1E88E5), // blue
        Color(0xFFFDD835)  // yellow
    )

    @Composable
    override fun Render(onDone: () -> Unit, onFail: () -> Unit, modifier: Modifier) {
        var sequence by remember {
            mutableStateOf(List(startLength) { Random.nextInt(0, palette.size) })
        }
        var playbackIndex by remember { mutableStateOf(-1) }
        var userIndex by remember { mutableIntStateOf(0) }
        var phase by remember { mutableStateOf(Phase.SHOW) }
        var round by remember { mutableIntStateOf(1) }

        // Playback loop — runs once per sequence change.
        LaunchedEffect(sequence) {
            phase = Phase.SHOW
            userIndex = 0
            delay(700)
            for (tile in sequence) {
                playbackIndex = tile
                delay(420)
                playbackIndex = -1
                delay(140)
            }
            phase = Phase.WAIT
        }

        val onTileTap: (Int) -> Unit = onTileTap@ { idx ->
            if (phase != Phase.WAIT) return@onTileTap
            if (idx == sequence[userIndex]) {
                val next = userIndex + 1
                if (next >= sequence.size) {
                    if (sequence.size >= targetLength) {
                        phase = Phase.WIN
                        onDone()
                    } else {
                        // grow sequence and start next round
                        sequence = sequence + Random.nextInt(0, palette.size)
                        round += 1
                    }
                } else {
                    userIndex = next
                }
            } else {
                phase = Phase.FAIL
                onFail()
            }
        }

        Column(
            modifier = modifier.padding(20.dp),
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

            Text(
                when (phase) {
                    Phase.SHOW -> "Watch carefully… (${sequence.size} tiles)"
                    Phase.WAIT -> "Your turn — tile ${userIndex + 1} of ${sequence.size}"
                    Phase.WIN -> "Round complete!"
                    Phase.FAIL -> "Wrong!"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = when (phase) {
                    Phase.SHOW -> accent
                    Phase.WAIT -> MaterialTheme.colorScheme.primary
                    Phase.WIN -> Color(0xFF2E7D32)
                    Phase.FAIL -> Color(0xFFD32F2F)
                }
            )
            Spacer(Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Tile(0, palette[0], playbackIndex == 0, phase == Phase.WAIT, onTileTap)
                    Tile(1, palette[1], playbackIndex == 1, phase == Phase.WAIT, onTileTap)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Tile(2, palette[2], playbackIndex == 2, phase == Phase.WAIT, onTileTap)
                    Tile(3, palette[3], playbackIndex == 3, phase == Phase.WAIT, onTileTap)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Round $round • ${sequence.size} of $targetLength tiles",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    @Composable
    private fun Tile(
        index: Int,
        base: Color,
        lit: Boolean,
        interactive: Boolean,
        onTap: (Int) -> Unit
    ) {
        val litAlpha by animateFloatAsState(
            targetValue = if (lit) 1f else 0.35f,
            animationSpec = tween(180),
            label = "tile-glow-$index"
        )
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(base.copy(alpha = litAlpha))
                .border(2.dp, base.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                .clickable(enabled = interactive) { onTap(index) },
            contentAlignment = Alignment.Center
        ) {
            if (lit) {
                Text("●", color = Color.White, style = MaterialTheme.typography.headlineLarge)
            }
        }
    }

    private enum class Phase { SHOW, WAIT, WIN, FAIL }
}
