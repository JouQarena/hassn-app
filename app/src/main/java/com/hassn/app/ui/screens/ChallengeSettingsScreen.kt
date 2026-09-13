package com.hassn.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hassn.app.R
import com.hassn.app.data.ChallengeType
import com.hassn.app.data.Difficulty
import com.hassn.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeSettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.challengeSettings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.challenge_settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.cancel))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.difficulty),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Difficulty.values().forEach { difficulty ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = settings.difficulty == difficulty,
                                onClick = { viewModel.updateDifficulty(difficulty) }
                            )
                            Text(stringResource(difficultyResId(difficulty)))
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.padding(8.dp))
                Text(
                    text = stringResource(R.string.available_challenges),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(ChallengeType.values()) { type ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = type in settings.enabledChallenges,
                        onCheckedChange = { viewModel.toggleChallenge(type) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(challengeNameResId(type)))
                }
            }
        }
    }
}

private fun difficultyResId(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.difficulty_easy
    Difficulty.MEDIUM -> R.string.difficulty_medium
    Difficulty.HARD -> R.string.difficulty_hard
}

private fun challengeNameResId(type: ChallengeType): Int = when (type) {
    ChallengeType.PRESS_AND_HOLD -> R.string.challenge_press
    ChallengeType.HOLD_BREATH -> R.string.challenge_breath
    ChallengeType.DRINK_WATER -> R.string.challenge_water
    ChallengeType.TYPE_TEXT -> R.string.challenge_type_text
    ChallengeType.MATH -> R.string.challenge_math
    ChallengeType.COLOR_MATCH -> R.string.challenge_color
    ChallengeType.PATTERN -> R.string.challenge_pattern
    ChallengeType.SEQUENCE -> R.string.challenge_sequence
    ChallengeType.MORSE_CODE -> R.string.challenge_morse
}
