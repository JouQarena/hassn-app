package com.hassn.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hassn.app.data.MessageSettings
import com.hassn.app.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MessageSettingsViewModel(
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    val messageSettings = settingsRepo.messageSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MessageSettings.DEFAULT
    )

    private val _previewVisible = MutableStateFlow(false)
    val previewVisible: StateFlow<Boolean> = _previewVisible.asStateFlow()

    fun save(settings: MessageSettings) {
        viewModelScope.launch {
            settingsRepo.updateMessageSettings(settings)
        }
    }

    fun showPreview() {
        _previewVisible.value = true
    }

    fun hidePreview() {
        _previewVisible.value = false
    }
}
