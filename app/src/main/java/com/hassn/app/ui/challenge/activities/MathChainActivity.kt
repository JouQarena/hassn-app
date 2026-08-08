package com.hassn.app.ui.challenge.activities

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hassn.app.ui.challenge.ActivityDefinition
import kotlin.random.Random

/**
 * Math chain: solve a series of progressively harder arithmetic
 * problems.  The result of problem N is used as the input of problem
 * N+1, so the user can't just "spot" the answer — they have to
 * actually compute it.
 */
class MathChainActivity(
    private val chainLength: Int = 4
) : ActivityDefinition() {

    override val key = "math_chain"
    override val title = "سلسلة حسابية"
    override val subtitle = "حل السلسلة. نتيجة كل مسألة هي بداية التالية. أي خطأ يعيدك للبداية."
    override val icon: ImageVector = Icons.Default.Calculate
    override val accent = Color(0xFF6D4C41)
    override val estimatedSeconds = 45

    @Composable
    override fun Render(onDone: () -> Unit, onFail: () -> Unit, modifier: Modifier) {
        val chain = remember {
            val ops = listOf('+', '-', '*')
            val out = mutableListOf<Triple<Int, Char, Int>>()
            var first = Random.nextInt(11, 30)
            repeat(chainLength) {
                val b = Random.nextInt(2, 12)
                val op = ops.random()
                out += Triple(first, op, b)
                first = compute(first, op, b)
            }
            out
        }
        val expected = remember { chain.map { compute(it.first, it.second, it.third) } }

        val answers = remember {
            androidx.compose.runtime.mutableStateListOf<String>().apply {
                repeat(chainLength) { add("") }
            }
        }
        val errorIndex = remember { mutableStateOf(-1) }

        LaunchedEffect(answers.toList(), errorIndex.value) {
            val filled = answers.size == chainLength && answers.all { it.isNotBlank() }
            if (filled && errorIndex.value < 0) {
                val allOk = answers.indices.all { i -> answers[i].toIntOrNull() == expected[i] }
                if (allOk) onDone()
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
            Spacer(Modifier.height(16.dp))

            chain.forEachIndexed { i, (a, op, b) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        "$a $op $b = ",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    OutlinedTextField(
                        value = answers[i],
                        onValueChange = { v ->
                            if (v.length <= 6 && v.all { it.isDigit() || it == '-' }) {
                                answers[i] = v
                                errorIndex.value = -1
                            }
                        },
                        modifier = Modifier.width(110.dp),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center
                        ),
                        singleLine = true,
                        isError = errorIndex.value == i,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = if (i == chain.lastIndex) ImeAction.Done else ImeAction.Next
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    val wrong = answers.indices.firstOrNull { i -> answers[i].toIntOrNull() != expected[i] }
                    if (wrong != null) {
                        errorIndex.value = wrong
                    } else {
                        onDone()
                    }
                },
                enabled = answers.size == chainLength && answers.all { it.isNotBlank() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("إرسال الإجابات", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    private fun compute(a: Int, op: Char, b: Int): Int = when (op) {
        '+' -> a + b
        '-' -> a - b
        '*' -> a * b
        else -> 0
    }
}

