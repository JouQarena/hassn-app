package com.dnsguard.shield

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.dnsguard.shield.core.Prefs
import com.dnsguard.shield.service.ShieldWatchdogService
import com.dnsguard.shield.ui.i18n.AppLanguage
import com.dnsguard.shield.ui.i18n.Strings
import com.dnsguard.shield.ui.i18n.stringsFor
import com.dnsguard.shield.ui.navigation.DnsGuardNavHost
import com.dnsguard.shield.ui.theme.DnsGuardTheme

/** Composition-local accessor for the active [Strings] bundle. */
val LocalStrings = staticCompositionLocalOf { stringsFor(AppLanguage.EN) }

/**
 * Composition-local callback that cycles the UI language (EN ↔ AR). Provided by
 * [MainActivity]'s content lambda; invoked by the "🌐 EN" top-bar button.
 */
val LocalLanguageSwitcher = staticCompositionLocalOf<() -> Unit>(defaultFactory = { { } })

/**
 * Single activity. Dark-only, edge-to-edge, hosts the Compose navigation graph
 * and starts the watchdog the moment the user opens the app.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // The watchdog owns the accessibility master switch — make sure it is
        // alive as soon as the UI is visible.
        ShieldWatchdogService.start(this)

        val prefs = (application as DnsGuardApplication).prefs

        setContent {
            LanguageProvider(prefs) {
                DnsGuardTheme {
                    DnsGuardNavHost()
                }
            }
        }
    }
}

@Composable
private fun LanguageProvider(prefs: Prefs, content: @Composable () -> Unit) {
    var language by remember { mutableStateOf(prefs.language) }

    val switcher: () -> Unit = remember {
        {
            val next = language.next()
            language = next
            prefs.language = next
        }
    }

    CompositionLocalProvider(
        LocalStrings provides stringsFor(language),
        LocalLanguageSwitcher provides switcher,
        // Arabic gets a true right-to-left layout: rows, padding, back arrows
        // and scroll alignment all mirror automatically.
        LocalLayoutDirection provides if (language.isRtl) {
            LayoutDirection.Rtl
        } else {
            LayoutDirection.Ltr
        }
    ) {
        content()
    }
}
