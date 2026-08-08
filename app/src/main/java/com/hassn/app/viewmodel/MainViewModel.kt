package com.hassn.app.viewmodel

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hassn.app.data.AppInfo
import com.hassn.app.data.MonitoredApp
import com.hassn.app.data.SettingsDataStore
import com.hassn.app.util.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = SettingsDataStore(application)

    val monitoringEnabled: StateFlow<Boolean> = settings.monitoringEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val targetApp: StateFlow<AppInfo?> = combine(
        settings.targetAppPackage, settings.targetAppName
    ) { pkg, name ->
        if (pkg.isNullOrBlank() || name.isNullOrBlank()) null
        else AppInfo(packageName = pkg, label = name)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val monitoredApps: StateFlow<List<MonitoredApp>> = settings.monitoredApps
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val appLanguage: StateFlow<String> = settings.appLanguage
        .stateIn(viewModelScope, SharingStarted.Eagerly, "ar")

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _suggestedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val suggestedApps: StateFlow<List<AppInfo>> = _suggestedApps.asStateFlow()

    private var autoPicked = false

    init { loadApps() }

    fun setMonitoringEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try { settings.setMonitoringEnabled(enabled) }
            catch (e: Exception) { Log.e(TAG, "setMonitoringEnabled failed", e) }
        }
    }

    fun selectTargetApp(app: AppInfo) {
        viewModelScope.launch {
            try { settings.setTargetApp(app.packageName, app.label) }
            catch (e: Exception) { Log.e(TAG, "selectTargetApp failed", e) }
        }
    }

    fun addMonitoredApp(app: AppInfo, mode: String = Constants.MODE_ALWAYS) {
        viewModelScope.launch {
            try { settings.addMonitoredApp(MonitoredApp(app.packageName, app.label, mode)) }
            catch (e: Exception) { Log.e(TAG, "addMonitoredApp failed", e) }
        }
    }

    fun addMonitoredApps(apps: List<AppInfo>, mode: String = Constants.MODE_ALWAYS) {
        viewModelScope.launch {
            try {
                val current = monitoredApps.value.toMutableList()
                apps.forEach { app ->
                    if (current.none { it.packageName == app.packageName }) {
                        current.add(MonitoredApp(app.packageName, app.label, mode))
                    }
                }
                settings.setMonitoredApps(current)
            } catch (e: Exception) { Log.e(TAG, "addMonitoredApps failed", e) }
        }
    }

    fun removeMonitoredApp(packageName: String) {
        viewModelScope.launch {
            try { settings.removeMonitoredApp(packageName) }
            catch (e: Exception) { Log.e(TAG, "remove failed", e) }
        }
    }

    fun updateMonitoredAppMode(packageName: String, mode: String) {
        viewModelScope.launch {
            try { settings.updateMonitoredAppMode(packageName, mode) }
            catch (e: Exception) { Log.e(TAG, "update mode failed", e) }
        }
    }

    fun setAppLanguage(lang: String) {
        viewModelScope.launch {
            try { settings.setAppLanguage(lang) }
            catch (e: Exception) { Log.e(TAG, "setLang failed", e) }
        }
    }

    private fun loadApps() {
        viewModelScope.launch {
            try {
                val pm = getApplication<Application>().packageManager
                val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
                val all = pm.queryIntentActivities(intent, 0)
                    .mapNotNull { AppInfo.fromResolveInfoSafe(it, pm) }
                    .map { it.sanitiseLabel() }
                    .sortedBy { it.label.lowercase() }

                val suggested = all.filter { a -> Constants.SUGGESTED_APPS.any { a.packageName == it } }
                val rest = all - suggested.toSet()
                _suggestedApps.value = suggested
                _installedApps.value = suggested + rest

                if (!autoPicked && targetApp.value == null && suggested.isNotEmpty()) {
                    autoPicked = true
                    selectTargetApp(suggested.first())
                }
            } catch (e: Exception) { Log.e(TAG, "loadApps failed", e) }
        }
    }

    companion object { private const val TAG = "MainViewModel" }
}
