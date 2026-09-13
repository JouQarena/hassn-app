package com.hassn.app.data

import kotlinx.serialization.Serializable

@Serializable
enum class ChallengeType {
    PRESS_AND_HOLD,
    HOLD_BREATH,
    DRINK_WATER,
    TYPE_TEXT,
    MATH,
    COLOR_MATCH,
    PATTERN,
    SEQUENCE,
    MORSE_CODE
}

@Serializable
enum class Difficulty {
    EASY,
    MEDIUM,
    HARD
}

@Serializable
data class ChallengeSettings(
    val enabledChallenges: Set<ChallengeType> = defaultEnabledChallenges(),
    val difficulty: Difficulty = Difficulty.MEDIUM
) {
    companion object {
        // All challenges enabled except Morse Code (disabled by default)
        fun defaultEnabledChallenges(): Set<ChallengeType> =
            ChallengeType.values().toSet() - ChallengeType.MORSE_CODE
    }
}
