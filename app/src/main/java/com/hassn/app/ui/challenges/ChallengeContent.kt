package com.hassn.app.ui.challenges

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.awaitPointerEventScope
import androidx.compose.ui.input.pointer.awaitPointerEvent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hassn.app.R
import com.hassn.app.data.ChallengeType
import com.hassn.app.data.Difficulty
import com.hassn.app.service.ChallengeResult
import com.hassn.app.util.Constants
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun ChallengeContent(
    type: ChallengeType,
    difficulty: Difficulty,
    onResult: (ChallengeResult) -> Unit
) {
    var done by remember { mutableStateOf(false) }
    val finish: (ChallengeResult) -> Unit = { result ->
        if (!done) {
            done = true
            onResult(result)
        }
    }
    LaunchedEffect(Unit) {
        delay(Constants.CHALLENGE_TIMEOUT_MS)
        finish(ChallengeResult.TIMEOUT)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (type) {
            ChallengeType.PRESS_AND_HOLD -> PressAndHoldChallenge(difficulty, finish)
            ChallengeType.HOLD_BREATH ->
                CountdownChallenge(
                    titleRes = R.string.challenge_breath,
                    instructionRes = R.string.challenge_breath_instruction,
                    emoji = "🫁",
                    seconds = when (difficulty) {
                        Difficulty.EASY -> 10
                        Difficulty.MEDIUM -> 15
                        Difficulty.HARD -> 20
                    },
                    finish = finish
                )
            ChallengeType.DRINK_WATER ->
                CountdownChallenge(
                    titleRes = R.string.challenge_water,
                    instructionRes = R.string.challenge_water_instruction,
                    emoji = "💧",
                    seconds = when (difficulty) {
                        Difficulty.EASY -> 10
                        Difficulty.MEDIUM -> 15
                        Difficulty.HARD -> 20
                    },
                    finish = finish
                )
            ChallengeType.TYPE_TEXT -> TypeTextChallenge(difficulty, finish)
            ChallengeType.MATH -> MathChallenge(difficulty, finish)
            ChallengeType.COLOR_MATCH -> ColorMatchChallenge(difficulty, finish)
            ChallengeType.PATTERN -> PatternChallenge(difficulty, finish)
            ChallengeType.SEQUENCE -> SequenceChallenge(difficulty, finish)
            ChallengeType.MORSE_CODE -> MorseCodeChallenge(difficulty, finish)
        }
    }
}

@Composable
private fun ChallengeHeader(titleRes: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "حصّن",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
    }
}

// region Press and Hold

@Composable
private fun PressAndHoldChallenge(
    difficulty: Difficulty,
    finish: (ChallengeResult) -> Unit
) {
    val durationMs = when (difficulty) {
        Difficulty.EASY -> 3000L
        Difficulty.MEDIUM -> 5000L
        Difficulty.HARD -> 8000L
    }.toFloat()

    var progress by remember { mutableFloatStateOf(0f) }
    var holding by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (progress < 1f) {
            if (holding) progress = (progress + 16f / durationMs).coerceAtMost(1f)
            delay(16)
        }
        finish(ChallengeResult.SUCCESS)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ChallengeHeader(R.string.challenge_press)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.challenge_press_instruction),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(40.dp))
        Box(
            modifier = Modifier
                .size(200.dp)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event: PointerEvent = awaitPointerEvent()
                            holding = event.changes.any { it.pressed }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.matchParentSize(),
                strokeWidth = 8.dp
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

// endregion

// region Countdown (breath / water)

@Composable
private fun CountdownChallenge(
    titleRes: Int,
    instructionRes: Int,
    emoji: String,
    seconds: Int,
    finish: (ChallengeResult) -> Unit
) {
    var remainingMs by remember { mutableIntStateOf(seconds * 1000) }

    LaunchedEffect(Unit) {
        val start = System.currentTimeMillis()
        while (true) {
            val left = seconds * 1000 - (System.currentTimeMillis() - start).toInt()
            if (left <= 0) break
            remainingMs = left
            delay(100)
        }
        finish(ChallengeResult.SUCCESS)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ChallengeHeader(titleRes)
        Spacer(modifier = Modifier.height(48.dp))
        Text(text = emoji, fontSize = 64.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "${(remainingMs + 999) / 1000}",
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(instructionRes),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// endregion

// region Type Text

@Composable
private fun TypeTextChallenge(
    difficulty: Difficulty,
    finish: (ChallengeResult) -> Unit
) {
    val (target, maxErrors) = when (difficulty) {
        Difficulty.EASY -> Pair("أنا أركز على أهدافي", 2)
        Difficulty.MEDIUM -> Pair("الوقت أثمن ما أملك وسأستثمره بحكمة", 1)
        Difficulty.HARD -> Pair("أنا أتحكم في انتباهي وأختار ما يستحق وقتي", 0)
    }
    var input by remember { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(input) {
        if (failed || input.isEmpty()) return@LaunchedEffect
        if (input == target) {
            finish(ChallengeResult.SUCCESS)
            return@LaunchedEffect
        }
        val wrong = (0 until input.length).count { i ->
            i >= target.length || input[i] != target[i]
        }
        if (wrong > maxErrors) {
            failed = true
            finish(ChallengeResult.FAILURE)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ChallengeHeader(R.string.challenge_type_text)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.challenge_type_text_instruction),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = target,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = input,
            onValueChange = { if (!failed) input = it },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )
    }
}

// endregion

// region Math

@Composable
private fun MathChallenge(
    difficulty: Difficulty,
    finish: (ChallengeResult) -> Unit
) {
    val (display, answer) = remember { generateMathProblem(difficulty) }
    var input by remember { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ChallengeHeader(R.string.challenge_math)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.challenge_math_instruction),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = "$display = ?",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = input,
            onValueChange = { if (!failed) input = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            enabled = input.isNotBlank() && !failed,
            onClick = {
                if (input.trim().toIntOrNull() == answer) {
                    finish(ChallengeResult.SUCCESS)
                } else {
                    failed = true
                    finish(ChallengeResult.FAILURE)
                }
            }
        ) {
            Text(stringResource(R.string.challenge_verify))
        }
    }
}

private fun generateMathProblem(difficulty: Difficulty): Pair<String, Int> {
    val range = when (difficulty) {
        Difficulty.EASY -> 20
        Difficulty.MEDIUM -> 50
        Difficulty.HARD -> 100
    }
    val ops = when (difficulty) {
        Difficulty.EASY -> listOf("+", "-")
        Difficulty.MEDIUM -> listOf("+", "-", "×")
        Difficulty.HARD -> listOf("+", "-", "×", "÷")
    }
    return when (val op = ops.random()) {
        "+" -> {
            val a = Random.nextInt(range)
            val b = Random.nextInt(1, range + 1)
            "${a} + $b" to (a + b)
        }
        "-" -> {
            val a = Random.nextInt(range)
            val b = Random.nextInt(0, a + 1)
            "${a} - $b" to (a - b)
        }
        "×" -> {
            val a = Random.nextInt(2, 13)
            val b = Random.nextInt(2, 13)
            "${a} × $b" to (a * b)
        }
        else -> {
            val b = Random.nextInt(2, 12)
            val q = Random.nextInt(2, range / b + 1)
            val a = b * q
            "${a} ÷ $b" to q
        }
    }
}

// endregion

// region Color Match

private val COLOR_PALETTE: List<Pair<String, Int>> = listOf(
    "أحمر" to 0xFFF44336.toInt(),
    "أزرق" to 0xFF2196F3.toInt(),
    "أخضر" to 0xFF4CAF50.toInt(),
    "أصفر" to 0xFFFFEB3B.toInt(),
    "برتقالي" to 0xFFFF9800.toInt(),
    "بنفسجي" to 0xFF9C27B0.toInt(),
    "أسود" to 0xFF212121.toInt(),
    "أبيض" to 0xFFEEEEEE.toInt()
)

@Composable
private fun ColorMatchChallenge(
    difficulty: Difficulty,
    finish: (ChallengeResult) -> Unit
) {
    val (targetName, targetColor) = remember { COLOR_PALETTE.random() }
    val options = remember {
        val others = COLOR_PALETTE
            .filter { it.second != targetColor }
            .map { it.second }
            .shuffled()
            .take(when (difficulty) {
                Difficulty.EASY -> 2
                Difficulty.MEDIUM -> 3
                Difficulty.HARD -> 5
            })
        (listOf(targetColor) + others).shuffled()
    }
    val hardDeadline = if (difficulty == Difficulty.HARD) System.currentTimeMillis() + 3000 else null
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(hardDeadline) {
        if (hardDeadline != null) {
            delay(3000)
            if (!failed) {
                failed = true
                finish(ChallengeResult.FAILURE)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ChallengeHeader(R.string.challenge_color)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.challenge_color_instruction),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = targetName,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(40.dp))
        options.chunked(3).forEach { rowColors ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(color))
                            .clickable {
                                if (failed) return@clickable
                                if (color == targetColor) {
                                    failed = true
                                    finish(ChallengeResult.SUCCESS)
                                } else {
                                    failed = true
                                    finish(ChallengeResult.FAILURE)
                                }
                            }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// endregion

// region Pattern

@Composable
private fun PatternChallenge(
    difficulty: Difficulty,
    finish: (ChallengeResult) -> Unit
) {
    val gridSize = if (difficulty == Difficulty.HARD) 4 else 3
    val (sequenceLength, displayMs) = when (difficulty) {
        Difficulty.EASY -> 3 to 2000
        Difficulty.MEDIUM -> 5 to 3000
        Difficulty.HARD -> 7 to 4000
    }
    val sequence = remember { (0 until sequenceLength).map { Random.nextInt(gridSize * gridSize) } }
    var highlight by remember { mutableIntStateOf(-1) }
    var inputPhase by remember { mutableStateOf(false) }
    var inputIndex by remember { mutableIntStateOf(0) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val stepMs = displayMs / sequenceLength
        for (cell in sequence) {
            highlight = cell
            delay((stepMs * 0.4f).toLong())
            highlight = -1
            delay((stepMs * 0.6f).toLong())
        }
        inputPhase = true
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ChallengeHeader(R.string.challenge_pattern)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (inputPhase) stringResource(R.string.your_turn)
            else stringResource(R.string.watching),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(32.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (r in 0 until gridSize) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (c in 0 until gridSize) {
                        val cell = r * gridSize + c
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (highlight == cell) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable {
                                    if (!inputPhase || failed) return@clickable
                                    if (cell == sequence[inputIndex]) {
                                        inputIndex++
                                        if (inputIndex == sequenceLength) {
                                            failed = true
                                            finish(ChallengeResult.SUCCESS)
                                        }
                                    } else {
                                        failed = true
                                        finish(ChallengeResult.FAILURE)
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
}

// endregion

// region Sequence

@Composable
private fun SequenceChallenge(
    difficulty: Difficulty,
    finish: (ChallengeResult) -> Unit
) {
    val (shown, answer) = remember { generateSequence(difficulty) }
    var input by remember { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ChallengeHeader(R.string.challenge_sequence)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.challenge_sequence_instruction),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = shown.joinToString(" , ") + " , ?",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = input,
            onValueChange = { if (!failed) input = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            enabled = input.isNotBlank() && !failed,
            onClick = {
                if (input.trim() == answer) {
                    finish(ChallengeResult.SUCCESS)
                } else {
                    failed = true
                    finish(ChallengeResult.FAILURE)
                }
            }
        ) {
            Text(stringResource(R.string.challenge_verify))
        }
    }
}

private fun generateSequence(difficulty: Difficulty): Pair<List<String>, String> = when (difficulty) {
    Difficulty.EASY -> {
        val step = Random.nextInt(2, 10)
        val start = Random.nextInt(1, 11)
        val terms = (0..3).map { start + step * it }
        terms.map { it.toString() } to (terms[3] + step).toString()
    }
    Difficulty.MEDIUM -> if (Random.nextBoolean()) {
        val step = Random.nextInt(2, 10)
        val start = Random.nextInt(1, 11)
        val terms = (0..3).map { start + step * it }
        terms.map { it.toString() } to (terms[3] + step).toString()
    } else {
        val start = Random.nextInt(0, 18)
        val letters = (0..3).map { ('A' + start + 2 * it) }
        letters.map { it.toString() } to ('A' + start + 8).toString()
    }
    Difficulty.HARD -> {
        // Fibonacci-style: each term is the sum of the two previous
        val a = Random.nextInt(1, 6)
        val b = Random.nextInt(1, 6)
        val c = a + b
        val d = b + c
        listOf(a, b, c, d).map { it.toString() } to (c + d).toString()
    }
}

// endregion

// region Morse Code

private val MORSE: Map<String, String> = linkedMapOf(
    "A" to ".-",
    "E" to ".",
    "I" to "..",
    "M" to "--",
    "N" to "-.",
    "O" to "---",
    "S" to "...",
    "T" to "--",
    "U" to "..-"
)

@Composable
private fun MorseCodeChallenge(
    difficulty: Difficulty,
    finish: (ChallengeResult) -> Unit
) {
    val target = remember {
        when (difficulty) {
            Difficulty.EASY -> listOf(listOf("E", "T").random())
            Difficulty.MEDIUM -> listOf("S", "O", "S")
            Difficulty.HARD -> (0 until 5).map { MORSE.keys.random() }
        }
    }
    val expected = remember { target.joinToString("") { MORSE.getValue(it) } }
    var typed by remember { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }

    fun tap(symbol: String) {
        if (failed) return
        val next = typed + symbol
        if (next.length <= expected.length && expected.startsWith(next)) {
            typed = next
        } else {
            failed = true
            finish(ChallengeResult.FAILURE)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ChallengeHeader(R.string.challenge_morse)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.challenge_morse_instruction),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = target.joinToString("  "),
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = expected.map { if (it == '.') "•" else "–" }.joinToString(" "),
            fontSize = 28.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = typed.map { if (it == '.') "•" else "–" }.joinToString(" "),
            fontSize = 28.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(40.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { tap(".") },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "•", fontSize = 40.sp)
            }
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { tap("-") },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "–", fontSize = 40.sp)
            }
        }
    }
}

// endregion
