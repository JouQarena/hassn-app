package com.hassn.app.viewmodel

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hassn.app.HassnApp
import com.hassn.app.data.ChallengeSettings
import com.hassn.app.data.ChallengeType
import com.hassn.app.data.Difficulty
import com.hassn.app.data.MessageSettings
import com.hassn.app.data.MonitoredApp
import com.hassn.app.data.RedirectSettings
import com.hassn.app.util.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InstalledApp(
    val packageName: String,
    val name: String,
    val icon: Bitmap
)

class MainViewModel(private val app: HassnApp) : ViewModel() {

    private val settingsRepo = app.settingsRepository
    private val appsRepo = app.monitoredAppsRepository

    val protectionEnabled = settingsRepo.protectionEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val selectedBehaviors = settingsRepo.selectedBehaviors.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Constants.DEFAULT_BEHAVIORS
    )

    val behaviorOrder = settingsRepo.behaviorOrder.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Constants.DEFAULT_BEHAVIOR_ORDER
    )

    val messageSettings = settingsRepo.messageSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MessageSettings.DEFAULT
    )

    val challengeSettings = settingsRepo.challengeSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChallengeSettings()
    )

    val redirectSettings = settingsRepo.redirectSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RedirectSettings()
    )

    val monitoredApps = appsRepo.getAllMonitoredApps().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val locale = settingsRepo.locale.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "ar"
    )

    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps

    init {
        viewModelScope.launch {
            _installedApps.value = loadInstalledApps()
        }
    }

    fun toggleProtection() {
        viewModelScope.launch {
            settingsRepo.setProtectionEnabled(!protectionEnabled.value)
        }
    }

    fun updateBehaviors(behaviors: Set<String>) {
        viewModelScope.launch {
            settingsRepo.setSelectedBehaviors(behaviors)
        }
    }

    fun moveBehavior(behavior: String, direction: Int) {
        viewModelScope.launch {
            val order = behaviorOrder.value.toMutableList()
            val from = order.indexOf(behavior)
            val to = from + direction
            if (from !in order.indices || to !in order.indices) return@launch
            order[from] = order[to]
            order[to] = behavior
            settingsRepo.setBehaviorOrder(order)
        }
    }

    fun updateChallengeSettings(settings: ChallengeSettings) {
        viewModelScope.launch {
            settingsRepo.updateChallengeSettings(settings)
        }
    }

    fun updateDifficulty(difficulty: Difficulty) {
        viewModelScope.launch {
            settingsRepo.updateChallengeSettings(challengeSettings.value.copy(difficulty = difficulty))
        }
    }

    fun toggleChallenge(type: ChallengeType) {
        val current = challengeSettings.value
        val next = if (type in current.enabledChallenges) {
            current.enabledChallenges - type
        } else {
            current.enabledChallenges + type
        }
        if (next.isNotEmpty()) {
            updateChallengeSettings(current.copy(enabledChallenges = next))
        }
    }

    fun selectDestinationApp(packageName: String) {
        viewModelScope.launch {
            settingsRepo.updateRedirectSettings(
                redirectSettings.value.copy(destinationPackage = packageName)
            )
        }
    }

    fun addOrUpdateMonitoredApp(app: MonitoredApp) {
        viewModelScope.launch {
            if (monitoredApps.value.any { it.packageName == app.packageName }) {
                appsRepo.updateMonitoredApp(app)
            } else {
                appsRepo.addMonitoredApp(app)
            }
        }
    }

    fun removeMonitoredApp(packageName: String) {
        viewModelScope.launch {
            appsRepo.removeMonitoredApp(packageName)
        }
    }

    fun toggleLocale(onChanged: () -> Unit) {
        viewModelScope.launch {
            val next = if (locale.value == "ar") "en" else "ar"
            settingsRepo.setLocale(next)
            app.appLocale = next
            onChanged()
        }
    }

    fun hasBuiltInDetector(packageName: String): Boolean =
        app.privacyDetectionEngine.hasBuiltInDetector(packageName)

    private suspend fun loadInstalledApps(): List<InstalledApp> = try {
        val pm = app.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, 0)
            .map {
                InstalledApp(
                    packageName = it.activityInfo.packageName,
                    name = it.loadLabel(pm).toString(),
                    icon = loadAppIcon(it.loadIcon(pm))
                )
            }
            .filter { it.packageName != Constants.APP_PACKAGE }
            .distinctBy { it.packageName }
            .sortedBy { it.name.lowercase() }
    } catch (e: Exception) {
        emptyList()
    }

    private fun loadAppIcon(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) return drawable.bitmap
        val width = drawable.intrinsicWidth.coerceAtLeast(1)
        val height = drawable.intrinsicHeight.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
