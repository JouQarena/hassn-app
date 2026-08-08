package com.hassn.app.ui.challenge.activities

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hassn.app.ui.challenge.ActivityDefinition

/**
 * Typing test — user must transcribe a random sentence exactly.
 *
 * Forces the user to slow down and pay attention to the screen, and
 * makes a rapid "disable and run" attempt very annoying.  We pick
 * from a pool of focus-themed sentences so the act of typing
 * reinforces the message of the app.
 */
class TypingTestActivity(
    private val sentence: String = pickSentence(),
    private val minimumMillis: Long = 12_000L
) : ActivityDefinition() {

    override val key = "typing"
    override val title = "اكتب هذه الجملة"
    override val subtitle = "اكتب الجملة أدناه حرفاً بحرف بدقة. الحد الأدنى ${minimumMillis / 1000} ثانية كتابة."
    override val icon: ImageVector = Icons.Default.Keyboard
    override val accent = Color(0xFF455A64)
    override val estimatedSeconds = 30

    @Composable
    override fun Render(onDone: () -> Unit, onFail: () -> Unit, modifier: Modifier) {
        var typed by remember { mutableStateOf("") }
        val startMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
        var submitted by remember { mutableStateOf(false) }

        LaunchedEffect(typed) {
            if (!submitted && typed == sentence) {
                val elapsed = System.currentTimeMillis() - startMs
                if (elapsed >= minimumMillis) {
                    submitted = true
                    onDone()
                }
            }
        }

        // Calculate per-character match to give live feedback
        val correctChars = typed.indices.count { i -> i < sentence.length && typed[i] == sentence[i] }
        val totalLen = sentence.length
        val matchProgress = correctChars.toFloat() / totalLen

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

            // Target sentence
            Text(
                text = sentence,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )

            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { matchProgress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp)),
                color = accent,
                trackColor = accent.copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "$correctChars / $totalLen characters correct",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = typed,
                onValueChange = { v ->
                    if (v.length <= sentence.length) typed = v
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("اكتب الجملة هنا…") },
                singleLine = true,
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 16.sp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                supportingText = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${typed.length} / ${sentence.length}")
                        val elapsedSec = (System.currentTimeMillis() - startMs) / 1000
                        Text("min ${minimumMillis / 1000}s · $elapsedSec s elapsed")
                    }
                }
            )
        }
    }

    companion object {
        private val pool = listOf(
            "Do the work even when you don't feel like doing the work.",
            "A moment of discipline now saves an hour of regret later.",
            "You are what you do, not what you say you will do.",
            "Discomfort is the price of growth. Pay it gladly.",
            "The expert in anything was once a beginner who refused to quit.",
            "Future you is built by the choices you make right now.",
            "Don't let a temporary feeling ruin a long-term goal.",
            "Boredom is the doorway to a better version of yourself.",
            "Phone down, eyes up, life on.",
            "Read a book. Drink water. Touch grass. Repeat."
        )

        fun pickSentence(): String = pool.random()
    }
}
