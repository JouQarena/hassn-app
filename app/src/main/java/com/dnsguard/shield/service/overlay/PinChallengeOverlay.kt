package com.dnsguard.shield.service.overlay

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.dnsguard.shield.core.security.PinPolicy
import com.dnsguard.shield.ui.i18n.AppLanguage
import com.dnsguard.shield.ui.i18n.Strings
import com.dnsguard.shield.ui.i18n.stringsFor
import kotlin.math.ceil

/**
 * High-priority Master-PIN challenge shown as a `TYPE_ACCESSIBILITY_OVERLAY`
 * **above the Settings screen** the guard stopped (spec: "show a high-priority
 * PIN authentication overlay dialog").
 *
 * Differences from [NsfwOverlay] (which is deliberately NOT focusable):
 *  - this window must accept text input → focusable, IME-friendly flags;
 *  - touches are still consumed by the root (clickable scrim) so nothing
 *    falls through to the destructive screen underneath;
 *  - verification runs off the main thread (injected [verify] callback,
 *    BCrypt ≈ 50–150 ms) and results return on the main looper.
 *
 * Outcomes:
 *  - Success → [onUnlocked] (service suspends the guard for the release
 *    window) and the overlay is removed.
 *  - Failure → inline error + remaining attempts; after the 5th failure the
 *    lockout countdown renders every second and input is rejected client-side
 *    until it expires (the server-side truth lives in [PinPolicy]).
 *  - "Leave" → [onLeave] (service removes the overlay and presses BACK,
 *    exiting the guarded screen without the PIN).
 */
class PinChallengeOverlay(
    private val service: AccessibilityService,
    private val verify: (pin: String, callback: (VerifyResult) -> Unit) -> Unit,
    private val onUnlocked: () -> Unit,
    private val onLeave: () -> Unit
) {

    sealed class VerifyResult {
        object Success : VerifyResult()

        /** Wrong PIN; [remainingAttempts] comes from the vault's real
         *  failure budget (PinPolicy), never from overlay-local counting. */
        data class Error(val remainingAttempts: Int) : VerifyResult()

        data class Locked(val retryAfterMs: Long) : VerifyResult()
    }

    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val density = service.resources.displayMetrics.density

    private var root: View? = null
    private var pinInput: EditText? = null
    private var messageText: TextView? = null
    private var unlockButton: Button? = null

    private var verifying = false
    private var lockedUntilMs = 0L
    private var ticker: Runnable? = null

    /** Shows the overlay; idempotent while attached. */
    fun show() {
        if (root != null) return
        val strings = strings()
        val view = buildView(strings)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            // Focusable so the IME can serve the PIN field; the scrim root
            // still swallows every touch outside the card.
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            dimAmount = 0.75f
        }
        try {
            windowManager.addView(view, params)
            root = view
            pinInput?.requestFocus()
        } catch (_: WindowManager.BadTokenException) {
            root = null
        } catch (_: IllegalStateException) {
            root = null
        }
    }

    /** Detaches everything; safe to call twice. */
    fun cleanup() {
        stopTicker()
        val view = root ?: return
        root = null
        pinInput = null
        messageText = null
        unlockButton = null
        try {
            windowManager.removeViewImmediate(view)
        } catch (_: IllegalArgumentException) {
            // Already detached.
        }
    }

    // ── Verification ───────────────────────────────────────────────────────

    private fun submit() {
        if (verifying) return
        val input = pinInput ?: return
        val strings = strings()

        val now = System.currentTimeMillis()
        if (now < lockedUntilMs) {
            renderLockCountdown(strings)
            return
        }
        val pin = input.text?.toString().orEmpty()
        when (val strength = PinPolicy.validate(pin)) {
            PinPolicy.Strength.OK -> Unit
            else -> {
                showMessage(strings.pinDialogErrorWeak, ERROR_RED)
                return
            }
        }

        verifying = true
        unlockButton?.isEnabled = false
        verify(pin) { result ->
            verifying = false
            unlockButton?.isEnabled = true
            when (result) {
                VerifyResult.Success -> {
                    cleanup()
                    onUnlocked()
                }
                is VerifyResult.Error -> {
                    showMessage(
                        strings.pinDialogErrorWrong.format(result.remainingAttempts),
                        ERROR_RED
                    )
                    input.setText("")
                }
                is VerifyResult.Locked -> {
                    lockedUntilMs = System.currentTimeMillis() + result.retryAfterMs
                    startTicker(strings)
                    input.setText("")
                }
            }
        }
    }

    private fun startTicker(strings: Strings) {
        stopTicker()
        renderLockCountdown(strings)
        val runnable = object : Runnable {
            override fun run() {
                if (System.currentTimeMillis() >= lockedUntilMs) {
                    lockedUntilMs = 0L
                    stopTicker()
                    showMessage(strings.pinDialogUnlockBody, TEXT_SECONDARY)
                    return
                }
                renderLockCountdown(strings)
                handler.postDelayed(this, 1_000L)
            }
        }
        ticker = runnable
        handler.postDelayed(runnable, 1_000L)
    }

    private fun stopTicker() {
        ticker?.let { handler.removeCallbacks(it) }
        ticker = null
    }

    private fun renderLockCountdown(strings: Strings) {
        val remainingMs = (lockedUntilMs - System.currentTimeMillis()).coerceAtLeast(0L)
        showMessage(
            strings.pinDialogErrorLocked.format(ceil(remainingMs / 1000.0).toInt()),
            ERROR_RED
        )
    }

    private fun showMessage(text: String, color: Int) {
        messageText?.apply {
            this.text = text
            setTextColor(color)
        }
    }

    private fun strings(): Strings {
        val language = runCatching {
            com.dnsguard.shield.DnsGuardApplication.prefs().language
        }.getOrDefault(AppLanguage.EN)
        return stringsFor(language)
    }

    // ── View construction (exact palette from ui/theme/Color.kt) ───────────

    private fun buildView(strings: Strings): View {
        val scrim = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor(SCRIM_ARGB))
            setPadding(dp(24), dp(24), dp(24), dp(24))
            // Consume touches: nothing falls through to the Settings screen.
            isClickable = true
            isFocusable = true
        }

        val card = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(12).toFloat()
                setColor(Color.parseColor(CARD_SURFACE))
                setStroke(dp(1), Color.parseColor(BORDER))
            }
            setPadding(dp(20), dp(20), dp(20), dp(20))
            isClickable = true
        }
        card.layoutParams = LinearLayout.LayoutParams(dp(320), LinearLayout.LayoutParams.WRAP_CONTENT)

        card.addView(textView(strings.pinDialogUnlockTitle, 19f, TEXT_PRIMARY, bold = true).apply {
            gravity = Gravity.CENTER
        })
        card.addView(spacer(dp(8)))
        card.addView(textView(strings.guardOverlayBody, 14f, TEXT_SECONDARY, bold = false).apply {
            gravity = Gravity.CENTER
        })
        card.addView(spacer(dp(16)))

        val input = EditText(service).apply {
            hint = strings.pinDialogUnlockField
            setHintTextColor(TEXT_SECONDARY)
            setTextColor(TEXT_PRIMARY)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            isSingleLine = true
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(10).toFloat()
                setColor(Color.parseColor(BORDER))
                setStroke(dp(1), Color.parseColor(INPUT_BORDER))
            }
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        pinInput = input
        card.addView(input)
        card.addView(spacer(dp(8)))

        val message = textView(strings.pinDialogUnlockBody, 13f, TEXT_SECONDARY, bold = false).apply {
            gravity = Gravity.CENTER
        }
        messageText = message
        card.addView(message)
        card.addView(spacer(dp(16)))

        val unlock = button(strings.pinDialogUnlockBtn, PRIMARY_BLUE, Color.WHITE) { submit() }
        unlockButton = unlock
        card.addView(unlock)
        card.addView(spacer(dp(8)))
        card.addView(button(strings.guardOverlayLeaveBtn, BORDER, TEXT_PRIMARY) {
            cleanup()
            onLeave()
        })
        card.addView(spacer(dp(12)))
        card.addView(textView(strings.guardOverlayFooter, 11f, TEXT_SECONDARY, bold = false).apply {
            gravity = Gravity.CENTER
        })

        scrim.addView(card)
        return scrim
    }

    private fun button(label: String, bg: String, textColor: Int, onClick: () -> Unit): Button =
        Button(service).apply {
            text = label
            textSize = 15f
            isAllCaps = false
            setTextColor(textColor)
            stateListAnimator = null
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(10).toFloat()
                setColor(Color.parseColor(bg))
            }
            setPadding(dp(20), dp(12), dp(20), dp(12))
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

    private fun textView(text: String, sizeSp: Float, color: Int, bold: Boolean): TextView =
        TextView(service).apply {
            this.text = text
            textSize = sizeSp
            setTextColor(color)
            if (bold) paint.isFakeBoldText = true
            includeFontPadding = false
        }

    private fun spacer(heightPx: Int): View = View(service).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, heightPx
        )
    }

    private fun dp(value: Int): Int = (value * density).toInt()

    private companion object {
        const val SCRIM_ARGB = "#E6121212"      // #121212 @ 90 %
        const val CARD_SURFACE = "#1E1E1E"
        const val BORDER = "#2C2C2C"
        const val INPUT_BORDER = "#424242"
        const val PRIMARY_BLUE = "#1E88E5"
        const val TEXT_PRIMARY = 0xFFE0E0E0.toInt()
        const val TEXT_SECONDARY = 0xFF9E9E9E.toInt()
        const val ERROR_RED = 0xFFEF5350.toInt()
    }
}
