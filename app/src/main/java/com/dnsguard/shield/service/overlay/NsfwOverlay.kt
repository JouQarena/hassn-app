package com.dnsguard.shield.service.overlay

import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.dnsguard.shield.R
import com.dnsguard.shield.service.ShieldAccessibilityService
import com.dnsguard.shield.ui.i18n.AppLanguage
import com.dnsguard.shield.ui.i18n.stringsFor

/**
 * Full-screen `TYPE_ACCESSIBILITY_OVERLAY` window shown *only* while Reddit is
 * the foreground application and NSFW content is detected.
 *
 * The overlay is built programmatically (no XML, no Compose — an accessibility
 * overlay window has no LifecycleOwner for Compose) using the exact palette
 * from the design spec:
 *
 *  - background      #121212 at 90% (scrim over Reddit)
 *  - title           #E0E0E0
 *  - body / footer   #9E9E9E
 *  - reveal button   #1E88E5 with white label
 *
 * Taps on the overlay are consumed (they never fall through to the content
 * underneath) so the shield genuinely blocks accidental exposure. "Reveal for
 * 10 seconds" dismisses it temporarily and it returns if NSFW content is still
 * on screen when the cooldown expires.
 */
class NsfwOverlay(private val service: ShieldAccessibilityService) {

    private val windowManager =
        service.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val density = service.resources.displayMetrics.density

    private var overlayView: View? = null
    private var nsfwCurrentlyVisible = false
    private var revealedUntilMs = 0L

    private val revealExpired = Runnable {
        revealedUntilMs = 0L
        if (nsfwCurrentlyVisible) show()
    }

    /** Called by the service after each debounced NSFW scan. */
    fun onContentState(nsfwVisible: Boolean) {
        nsfwCurrentlyVisible = nsfwVisible
        if (!nsfwVisible) {
            handler.removeCallbacks(revealExpired)
            revealedUntilMs = 0L
            remove()
            return
        }

        val now = System.currentTimeMillis()
        if (now < revealedUntilMs) {
            // User already revealed — re-arm the expiry check exactly once.
            handler.removeCallbacks(revealExpired)
            handler.postDelayed(revealExpired, revealedUntilMs - now)
            remove()
            return
        }
        show()
    }

    private fun show() {
        if (overlayView != null) return
        val strings = stringsFor(currentLanguage())
        val view = buildView(strings)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
        try {
            windowManager.addView(view, params)
            overlayView = view
        } catch (_: WindowManager.BadTokenException) {
            // Window went away with the service — nothing to shield against.
            overlayView = null
        } catch (_: IllegalStateException) {
            overlayView = null
        }
    }

    private fun remove() {
        val view = overlayView ?: return
        overlayView = null
        try {
            windowManager.removeViewImmediate(view)
        } catch (_: IllegalArgumentException) {
            // Already detached.
        }
    }

    /** Releases everything; must be called from the service's onDestroy path. */
    fun cleanup() {
        handler.removeCallbacks(revealExpired)
        nsfwCurrentlyVisible = false
        revealedUntilMs = 0L
        remove()
    }

    private fun currentLanguage(): AppLanguage =
        runCatching { com.dnsguard.shield.DnsGuardApplication.prefs().language }
            .getOrDefault(AppLanguage.EN)

    // ── View construction ──────────────────────────────────────────────────

    private fun buildView(strings: com.dnsguard.shield.ui.i18n.Strings): View {
        val root = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor(SCrimArgb))
            setPadding(dp(32), dp(24), dp(32), dp(24))
            isClickable = true
            isFocusable = true
        }

        root.addView(textView("🛡️", 56f, 0xFFE0E0E0.toInt(), bold = false).apply {
            gravity = Gravity.CENTER
        })

        root.addView(spacer(dp(16)))
        root.addView(
            textView(strings.overlayTitle, 22f, 0xFFE0E0E0.toInt(), bold = true).apply {
                gravity = Gravity.CENTER
            }
        )
        root.addView(spacer(dp(12)))
        root.addView(
            textView(strings.overlayBody, 15f, 0xFF9E9E9E.toInt(), bold = false).apply {
                gravity = Gravity.CENTER
                maxWidth = dp(320)
            }
        )
        root.addView(spacer(dp(24)))

        root.addView(revealButton(strings.overlayRevealButton))
        root.addView(spacer(dp(16)))
        root.addView(textView(strings.overlayFooter, 12f, 0xFF9E9E9E.toInt(), bold = false).apply {
            gravity = Gravity.CENTER
        })

        return root
    }

    private fun revealButton(label: String): Button {
        val button = Button(service).apply {
            text = label
            textSize = 15f
            isAllCaps = false
            setTextColor(Color.WHITE)
            stateListAnimator = null
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(10).toFloat()
                setColor(Color.parseColor(PRIMARY_BLUE))
            }
            setPadding(dp(24), dp(12), dp(24), dp(12))
            setOnClickListener {
                revealedUntilMs = System.currentTimeMillis() + REVEAL_DURATION_MS
                handler.removeCallbacks(revealExpired)
                handler.postDelayed(revealExpired, REVEAL_DURATION_MS)
                remove()
            }
        }
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        button.layoutParams = lp
        return button
    }

    private fun textView(text: String, sizeSp: Float, color: Int, bold: Boolean): TextView =
        TextView(service).apply {
            this.text = text
            textSize = sizeSp
            setTextColor(color)
            if (bold) {
                paint.isFakeBoldText = true
            }
            includeFontPadding = false
        }

    private fun spacer(heightPx: Int): View = View(service).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, heightPx
        )
    }

    private fun dp(value: Int): Int = (value * density).toInt()

    private companion object {
        /** #121212 at 90 % opacity. */
        const val SCrimArgb = "#E6121212"
        const val PRIMARY_BLUE = "#1E88E5"
        const val REVEAL_DURATION_MS = 10_000L
    }
}
