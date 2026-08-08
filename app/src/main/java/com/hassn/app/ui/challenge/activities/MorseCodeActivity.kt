package com.hassn.app.ui.challenge.activities

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hassn.app.ui.challenge.ActivityDefinition
import kotlinx.coroutines.delay

/**
 * Morse code entry — the user must tap out a short word in Morse
 * code.  A short press = dot, a long press (≥ 250ms) = dash.
 *
 * There is a 5-second "no activity" timeout that resets the buffer.
 * On completion the buffer is compared to the target Morse.
 */
class MorseCodeActivity(
    private val target: MorseWord = MorseWord.WORDS.random()
) : ActivityDefinition() {

    override val key = "morse"
    override val title = "شفرة مورس"
    override val subtitle = "اضغط مطولاً: ضغطة قصيرة = نقطة (•)، طويلة = شرطة (—). أدخل شفرة مورس لكلمة \"${target.display}\" (${target.morse.length} رمز). الثبات 5 ثوانٍ يمحو."
    override val icon: ImageVector = Icons.Default.TouchApp
    override val accent = Color(0xFF455A64)
    override val estimatedSeconds = 45

    @Composable
    override fun Render(onDone: () -> Unit, onFail: () -> Unit, modifier: Modifier) {
        var buffer by remember { mutableStateOf("") }
        var lastChangeMs by remember { mutableStateOf(System.currentTimeMillis()) }
        var isPressed by remember { mutableStateOf(false) }
        var pressStartMs by remember { mutableStateOf(0L) }

        // Idle-timeout: 5 s of no input resets the buffer.
        LaunchedEffect(buffer) {
            lastChangeMs = System.currentTimeMillis()
            while (true) {
                val idle = System.currentTimeMillis() - lastChangeMs
                if (idle > 5000 && !isPressed && buffer.isNotEmpty()) {
                    buffer = ""
                }
                delay(500)
            }
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

            // Buffer display
            Text(
                text = if (buffer.isEmpty()) "—" else buffer.toCharArray().joinToString(" "),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                ),
                color = accent,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Target: ${target.display}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))

            // Big tap target
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(if (isPressed) accent else accent.copy(alpha = 0.25f))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { _ ->
                                isPressed = true
                                pressStartMs = System.currentTimeMillis()
                                try {
                                    tryAwaitRelease()
                                    val heldMs = System.currentTimeMillis() - pressStartMs
                                    isPressed = false
                                    // Don't allow buffer to grow past target length
                                    if (buffer.length < target.morse.length) {
                                        if (heldMs < 250L) {
                                            buffer += "."
                                        } else {
                                            buffer += "-"
                                        }
                                        lastChangeMs = System.currentTimeMillis()
                                    }
                                } catch (_: Throwable) {
                                    isPressed = false
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isPressed) "Hold…" else "Tap",
                    color = if (isPressed) Color.White else accent,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(16.dp))

            // Submit when buffer is long enough
            val exactMatch = buffer == target.morse
            val tooLong = buffer.length > target.morse.length
            if (exactMatch) {
                Text("✓ Match! Tap below to confirm.", color = Color(0xFF2E7D32),
                    style = MaterialTheme.typography.titleMedium)
            } else if (tooLong) {
                Text("Buffer overflow — fail.", color = Color(0xFFD32F2F),
                    style = MaterialTheme.typography.titleMedium)
                LaunchedEffect("overflow-fail") { onFail() }
            } else {
                Text(
                    "Progress: ${buffer.length} / ${target.morse.length}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (exactMatch) onDone()
                    else onFail()
                },
                enabled = buffer.length == target.morse.length,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(if (exactMatch) "Submit" else "Submit (will fail)")
            }
        }
    }

    /** Single Morse-coded word the user must reproduce. */
    data class MorseWord(val display: String, val morse: String) {
        companion object {
            val WORDS = listOf(
                MorseWord("FOCUS",  "..-. --- -.-. ..- ..."),
                MorseWord("READ",   ".-. . .- -.."),
                MorseWord("WORK",   ".-- --- .-. -.-"),
                MorseWord("DONE",   "-.. --- -. ."),
                MorseWord("STAY",   "... - .- -.--"),
                MorseWord("QUIT",   "--.- ..- .. -"),
                MorseWord("HELP",   ".... . .-.. .--."),
                MorseWord("OK",     "--- -.-")
            )
        }
    }
}
