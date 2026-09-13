package com.hassn.app.service

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityService
import androidx.compose.ui.platform.ComposeView
import com.hassn.app.data.ChallengeSettings
import com.hassn.app.data.MessageSettings
import com.hassn.app.data.SettingsRepository
import com.hassn.app.data.StatsRepository
import com.hassn.app.ui.challenges.ChallengeContent
import com.hassn.app.ui.components.MessageOverlayContent
import com.hassn.app.ui.theme.HassnTheme
import com.hassn.app.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class ResponseExecutor(
    private val service: AccessibilityService,
    private val settingsRepo: SettingsRepository,
    private val statsRepo: StatsRepository? = null
) {

    suspend fun execute(behaviors: Set<String>, order: List<String>) {
        val enabledBehaviors = order.filter { it in behaviors }

        for (behavior in enabledBehaviors) {
            when (behavior) {
                Constants.BEHAVIOR_MESSAGE -> {
                    val settings = settingsRepo.messageSettings.first()
                    displayMessageOverlay(settings)
                }

                Constants.BEHAVIOR_CHALLENGE -> {
                    val settings = settingsRepo.challengeSettings.first()
                    val result = presentChallenge(settings)
                    // On failure always go home; on success go home unless redirect follows
                    if (result == ChallengeResult.FAILURE ||
                        Constants.BEHAVIOR_REDIRECT !in behaviors
                    ) {
                        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                        return
                    }
                    // SUCCESS with redirect enabled -> fall through to redirect
                }

                Constants.BEHAVIOR_REDIRECT -> {
                    val settings = settingsRepo.redirectSettings.first()
                    service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                    delay(settings.delayMs)
                    if (launchDestinationApp(settings.destinationPackage)) {
                        statsRepo?.recordRedirection()
                    }
                }
            }
        }
    }

    private suspend fun displayMessageOverlay(settings: MessageSettings) {
        if (!Settings.canDrawOverlays(service)) return
        withContext(Dispatchers.Main) {
            val wm = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val view = ComposeView(service).apply {
                setContent {
                    HassnTheme(darkTheme = false) {
                        MessageOverlayContent(settings)
                    }
                }
            }
            try {
                wm.addView(
                    view,
                    overlayParams(
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    )
                )
                delay(settings.duration * 1000L)
                wm.removeView(view)
            } catch (e: Exception) {
                Log.e(TAG, "Message overlay failed", e)
            }
        }
    }

    private suspend fun presentChallenge(settings: ChallengeSettings): ChallengeResult {
        val type = settings.enabledChallenges.randomOrNull()
        if (type == null || !Settings.canDrawOverlays(service)) return ChallengeResult.FAILURE

        val pending = ChallengeResultBus.begin()
        var view: View? = null

        withContext(Dispatchers.Main) {
            val wm = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            view = ComposeView(service).apply {
                setContent {
                    HassnTheme(darkTheme = false) {
                        ChallengeContent(
                            type = type,
                            difficulty = settings.difficulty,
                            onResult = { ChallengeResultBus.complete(it) }
                        )
                    }
                }
            }
            try {
                wm.addView(
                    view,
                    overlayParams(
                        // Focusable window so text challenges can show the IME
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Challenge overlay failed", e)
                return@withContext
            }
        }

        val result = withTimeoutOrNull(Constants.CHALLENGE_TIMEOUT_MS) { pending.await() }
            ?: ChallengeResult.TIMEOUT
        statsRepo?.recordChallengeResult(result == ChallengeResult.SUCCESS)

        withContext(Dispatchers.Main) {
            view?.let {
                try {
                    (service.getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(it)
                } catch (e: Exception) {
                    Log.e(TAG, "Challenge overlay removal failed", e)
                }
            }
        }
        return result
    }

    private fun overlayParams(flags: Int): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            flags,
            PixelFormat.TRANSLUCENT
        )
    }

    private fun launchDestinationApp(packageName: String?): Boolean {
        if (packageName == null) return false
        try {
            val intent = service.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                service.startActivity(intent)
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch destination app", e)
        }
        return false
    }

    companion object {
        private const val TAG = "ResponseExecutor"
    }
}
