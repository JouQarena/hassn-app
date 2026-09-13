package com.hassn.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hassn.app.HassnApp
import com.hassn.app.data.ChallengeType
import com.hassn.app.data.Difficulty
import com.hassn.app.util.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Handles the "disable protection" flow: the user must pass an escalating
 * challenge (EASY -> MEDIUM -> HARD) before the protection switch can be
 * turned off. After 7 failed attempts a temporary lock is applied.
 */
class ChallengeViewModel(private val app: HassnApp) : ViewModel() {

    private val settingsRepo = app.settingsRepository
    private val challengeRepo = app.challengeRepository

    data class DisableRequest(val type: ChallengeType, val difficulty: Difficulty)

    val attemptCount = challengeRepo.getAttemptCount().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val isLocked = challengeRepo.isLocked().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private val _disableRequest = MutableStateFlow<DisableRequest?>(null)
    val disableRequest: StateFlow<DisableRequest?> = _disableRequest.asStateFlow()

    fun requestDisable() {
        viewModelScope.launch {
            if (challengeRepo.isLocked().first()) return@launch
            val attempts = challengeRepo.getAttemptCount().first()
            val difficulty = when {
                attempts < 3 -> Difficulty.EASY
                attempts < 6 -> Difficulty.MEDIUM
                else -> Difficulty.HARD
            }
            _disableRequest.value = DisableRequest(
                type = ChallengeType.values()
                    .filter { it != ChallengeType.MORSE_CODE }
                    .random(),
                difficulty = difficulty
            )
        }
    }

    fun onDisableChallengeResult(success: Boolean) {
        viewModelScope.launch {
            _disableRequest.value = null
            app.statsRepository.recordChallengeResult(success)
            if (success) {
                settingsRepo.setProtectionEnabled(false)
                challengeRepo.resetAttemptCount()
            } else {
                challengeRepo.incrementAttemptCount()
                if (challengeRepo.getAttemptCount().first() >= 7) {
                    challengeRepo.setLock(Constants.DISABLE_LOCK_MINUTES)
                }
            }
        }
    }
}
