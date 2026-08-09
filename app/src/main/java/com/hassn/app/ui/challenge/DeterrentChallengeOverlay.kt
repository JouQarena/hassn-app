package com.hassn.app.ui.challenge

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hassn.app.data.SettingsDataStore
import com.hassn.app.ui.challenge.activities.BreathHoldActivity
import com.hassn.app.ui.challenge.activities.ColorSequenceActivity
import com.hassn.app.ui.challenge.activities.HoldStillActivity
import com.hassn.app.ui.challenge.activities.MathChainActivity
import com.hassn.app.ui.challenge.activities.PullupActivity
import com.hassn.app.ui.challenge.activities.PushupActivity
import com.hassn.app.ui.challenge.activities.TypingTestActivity
import com.hassn.app.ui.challenge.activities.WaterChugActivity
import com.hassn.app.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Top-level orchestrator for the "disable monitoring" gauntlet.
 *
 * Renders a single activity at a time and a progress strip that
 * shows how many activities are left.  The queue length scales with
 * the user's history:
 *   - First attempt   → [MIN_ACTIVITIES_PER_ATTEMPT] activities
 *   - Each fail       → +1 activity, up to [MAX_ACTIVITIES_PER_ATTEMPT]
 *
 * On bail: the overlay dismisses but [SettingsDataStore] records a
 * fail, bumps a soft lockout, and queues a *longer* set of activities
 * for next time.  There is no escape hatch.
 *
 * @param onSolved  Invoked when the user has completed every activity
 *                  in the queue.  The caller should disable monitoring.
 * @param onDismiss Invoked when the user explicitly bails.  The
 *                  caller should leave monitoring on and remember the
 *                  escalation in DataStore.
 */
@Composable
@OptIn(ExperimentalAnimationApi::class)
fun DeterrentChallengeOverlay(
    onSolved: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val settings = remember { SettingsDataStore(context.applicationContext) }

    // Have we finished reading the persisted fail count?  Until then we
    // hide the queue so we don't flash the "easy" version first.
    var attemptCount by remember { mutableStateOf(0) }
    var ready by remember { mutableStateOf(false) }
    var showLockedOut by remember { mutableStateOf(false) }
    var lockedOutSec by remember { mutableStateOf(0L) }

    // Record this attempt + load the persisted escalation counters.
    LaunchedEffect(Unit) {
        settings.maybeResetStaleFailCounter()
        settings.recordDisableAttempt()
        attemptCount = settings.disableAttemptsAsSnapshot().toInt()
        ready = true
    }

    // Watch the lockout deadline.  While the user is locked out, we
    // just show a countdown.
    LaunchedEffect(Unit) {
        while (true) {
            val until = settings.lockoutUntilAsSnapshot()
            val nowMs = System.currentTimeMillis()
            if (until > nowMs) {
                lockedOutSec = (until - nowMs) / 1000L
                showLockedOut = true
                delay(500)
            } else {
                showLockedOut = false
                lockedOutSec = 0
                delay(1500)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .clip(RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 10.dp
        ) {
            if (!ready) {
                // Brief "loading" state while we read the counters.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("جاري تحضير التحدي…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (showLockedOut) {
                LockedOutPanel(lockedOutSec, onDismiss)
            } else {
                ChallengeBody(
                    attemptCount = attemptCount,
                    settings = settings,
                    onSolved = onSolved,
                    onBailed = onDismiss
                )
            }
        }
    }
}

@Composable
private fun LockedOutPanel(lockedOutSec: Long, onDismiss: () -> Unit) {
    val mins = lockedOutSec / 60
    val secs = lockedOutSec % 60
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Lock, null,
            tint = Color(0xFFD32F2F),
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "فترة انتظار",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "انسحبت سابقاً. خذ استراحة وحاول مرة أخرى لاحقاً.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Text(
            String.format("%02d:%02d", mins, secs),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Black,
            color = Color(0xFFD32F2F)
        )
        Spacer(Modifier.height(20.dp))
        TextButton(onClick = onDismiss) {
            Text("إغلاق")
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ChallengeBody(
    attemptCount: Int,
    settings: SettingsDataStore,
    onSolved: () -> Unit,
    onBailed: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val fails = remember { settings.disableFailsAsSnapshot() }
    val queueSize = (Constants.MIN_ACTIVITIES_PER_ATTEMPT + fails.toInt())
        .coerceAtMost(Constants.MAX_ACTIVITIES_PER_ATTEMPT)
    val queue = remember(attemptCount, fails) { buildQueue(queueSize) }

    var current by remember { mutableStateOf(0) }
    var confirmBail by remember { mutableStateOf(false) }

    val onActivityDone: () -> Unit = {
        if (current + 1 >= queue.size) {
            // Completed the entire queue.  Clear the fail counter and
            // any outstanding lockout as a reward — the user earned it.
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try { settings.clearFailures() } catch (_: Throwable) {}
            }
            onSolved()
        } else {
            current += 1
        }
    }
    val onActivityFail: () -> Unit = {
        handleBail(scope, settings, onBailed)
    }

    val activity = queue.getOrNull(current)
    val progress = (current.toFloat() / queue.size.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Lock, null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "تحدي الإيقاف",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "النشاط ${current + 1} من ${queue.size} • " +
                            "المحاولة #$attemptCount" +
                            if (fails > 0) " • ${fails} محاولات فاشلة" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { confirmBail = true }) {
                Icon(Icons.Default.Close, "انسحاب")
            }
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.error,
            trackColor = MaterialTheme.colorScheme.errorContainer
        )
        Spacer(Modifier.height(12.dp))

        // Activity card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(440.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = activity,
                    transitionSpec = {
                        (fadeIn(tween(220)) togetherWith fadeOut(tween(140)))
                    },
                    label = "activity-swap"
                ) { currentActivity ->
                    if (currentActivity != null) {
                        currentActivity.Render(
                            onDone = onActivityDone,
                            onFail = onActivityFail,
                            modifier = Modifier
                        )
                    } else {
                        Text("أحسنت — يمكنك الآن إيقاف الحماية.")
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "الانسحاب سيُحتسب فشلاً ويجعل المحاولة القادمة أصعب.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (confirmBail) {
        AlertDialog(
            onDismissRequest = { confirmBail = false },
            title = { Text("هل تريد الانسحاب؟") },
            text = {
                Text(
                    "إذا انسحبت الآن:\n" +
                            "• سيزيد عداد الفشل.\n" +
                            "• في المرة القادمة ستحتاج لإكمال المزيد من التحديات.\n" +
                            "• سيتم قفلك لمدة ${(fails + 1) * Constants.LOCKOUT_STEP_SECONDS / 60} دقيقة."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmBail = false
                        handleBail(scope, settings, onBailed)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("انسحاب", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { confirmBail = false }) { Text("متابعة") }
            }
        )
    }
}

private fun handleBail(
    scope: CoroutineScope,
    settings: SettingsDataStore,
    onBailed: () -> Unit
) {
    scope.launch(Dispatchers.IO) {
        try {
            settings.recordDisableFail()
            val fails = settings.disableFailsAsSnapshot()
            val lockoutSec = (fails * Constants.LOCKOUT_STEP_SECONDS)
                .coerceAtMost(Constants.LOCKOUT_MAX_SECONDS)
            if (lockoutSec > 0) {
                settings.setLockoutUntil(System.currentTimeMillis() + lockoutSec * 1000L)
            }
        } catch (_: Throwable) { }
    }
    onBailed()
}

/** Build a random queue of activities.  Avoids immediate repeats. */
private fun buildQueue(size: Int): List<ActivityDefinition> {
    val pool: List<Pair<String, () -> ActivityDefinition>> = listOf(
        "pushup"       to { PushupActivity(targetReps = Random.nextInt(8, 12)) },
        "pullup"       to { PullupActivity(targetReps = Random.nextInt(6, 10)) },
        "hold_still"   to { HoldStillActivity(targetMillis = (20_000L..35_000L).random()) },
        "breath_hold"  to { BreathHoldActivity(targetSeconds = Random.nextInt(20, 30)) },
        "water_chug"   to { WaterChugActivity() },
        "typing"       to { TypingTestActivity() },
        "math_chain"   to { MathChainActivity(chainLength = Random.nextInt(3, 5)) },
        "color_seq"    to { ColorSequenceActivity(startLength = 4, targetLength = Random.nextInt(6, 8)) },
    )
    val out = mutableListOf<ActivityDefinition>()
    var lastKey: String? = null
    repeat(size) {
        // Pick a non-repeating activity
        val candidates = pool.filter { it.first != lastKey }
        val pick = (if (candidates.isNotEmpty()) candidates else pool).random()
        val act = pick.second()
        out += act
        lastKey = act.key
    }
    return out
}
