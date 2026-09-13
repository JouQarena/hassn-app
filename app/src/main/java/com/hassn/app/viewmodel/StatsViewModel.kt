package com.hassn.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hassn.app.data.StatsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class StatsViewModel(
    private val statsRepo: StatsRepository
) : ViewModel() {

    val stats = statsRepo.getStats().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatsRepository.UsageStats(
            daysUsed = 0,
            totalRedirections = 0,
            challengesCompleted = 0,
            challengesFailed = 0,
            hoursSaved = 0
        )
    )
}
