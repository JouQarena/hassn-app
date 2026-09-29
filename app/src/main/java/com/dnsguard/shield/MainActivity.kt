package com.dnsguard.shield

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.dnsguard.shield.core.DeviceAdminManager
import com.dnsguard.shield.core.Prefs
import com.dnsguard.shield.core.ProtectionRuntime
import com.dnsguard.shield.core.TamperGuardPolicy
import com.dnsguard.shield.core.security.PinPolicy
import com.dnsguard.shield.core.security.PinVault
import com.dnsguard.shield.service.ShieldWatchdogService
import com.dnsguard.shield.ui.i18n.AppLanguage
import com.dnsguard.shield.ui.i18n.Strings
import com.dnsguard.shield.ui.i18n.stringsFor
import com.dnsguard.shield.ui.navigation.DnsGuardNavHost
import com.dnsguard.shield.ui.theme.DnsGuardTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Composition-local accessor for the active [Strings] bundle. */
val LocalStrings = staticCompositionLocalOf { stringsFor(AppLanguage.EN) }

/** Composition-local callback that cycles the UI language (EN ↔ AR). */
val LocalLanguageSwitcher = staticCompositionLocalOf<() -> Unit>(defaultFactory = { { } })

/**
 * The only entry point for privileged UI actions. A pending callback is
 * created *only* by tapping a named button, consumed exactly once on a
 * successful PIN check, and discarded on cancel, rotation, or leaving the
 * activity (no persisted plaintext PIN / no saved lambda). Navigation to
 * DNS Settings and Device Admin removal both pass through this gate.
 */
class PinGate {
    enum class Purpose { DNS_SETTINGS, DEACTIVATE_ADMIN }

    data class Request(val purpose: Purpose, val onApproved: () -> Unit)

    var pending by mutableStateOf<Request?>(null)
        private set

    /** In-memory only; never saved across process death/rotation. */
    var dnsUnlocked by mutableStateOf(false)
        private set

    fun clearDnsUnlock() { dnsUnlocked = false }

    fun request(purpose: Purpose, onApproved: () -> Unit) {
        // Do not replace an already pending challenge (cross-flow safety).
        if (pending == null) pending = Request(purpose, onApproved)
    }

    fun cancel() { pending = null }

    fun approve(request: Request) {
        if (pending !== request) return // stale completion after cancel/re-open
        pending = null                    // burn the callback before executing it
        if (request.purpose == Purpose.DNS_SETTINGS) dnsUnlocked = true
        request.onApproved()
    }
}

val LocalPinGate = staticCompositionLocalOf { PinGate() }

/** Single-activity Compose host. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ShieldWatchdogService.start(this)
        val prefs = (application as DnsGuardApplication).prefs
        ProtectionRuntime.refresh(this)

        setContent {
            LanguageProvider(prefs) {
                DnsGuardTheme {
                    val gate = remember { PinGate() }
                    CompositionLocalProvider(LocalPinGate provides gate) {
                        DnsGuardNavHost()
                        PinGateDialog(gate)
                    }
                }
            }
        }
    }
}

/** In-app PIN challenge for DNS Settings and deactivation. No plaintext PIN
 *  survives dialog dismissal, rotation, or process death. */
@Composable
private fun PinGateDialog(gate: PinGate) {
    val request = gate.pending ?: return
    val strings = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pin by remember(request) { mutableStateOf("") }
    var error by remember(request) { mutableStateOf<String?>(null) }
    var busy by remember(request) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!busy) gate.cancel() },
        title = { Text(strings.pinDialogUnlockTitle) },
        text = {
            Column {
                Text(strings.pinDialogUnlockBody)
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it; error = null },
                    label = { Text(strings.pinDialogUnlockField) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                if (PinPolicy.validate(pin) != PinPolicy.Strength.OK) {
                    error = strings.pinDialogErrorWeak
                    return@TextButton
                }
                busy = true
                val submitted = pin
                pin = ""
                scope.launch {
                    val outcome = withContext(Dispatchers.Default) {
                        PinVault.attemptUnlock(submitted)
                    }
                    busy = false
                    if (gate.pending !== request) return@launch
                    when (outcome) {
                        PinPolicy.AttemptOutcome.Success -> {
                            if (request.purpose == PinGate.Purpose.DEACTIVATE_ADMIN) {
                                // Let Settings finish the user-authorised
                                // deactivation without a back-fighting guard.
                                PinVault.suspendGuard(TamperGuardPolicy.DEACTIVATE_RELEASE_MINUTES)
                            }
                            gate.approve(request)
                        }
                        is PinPolicy.AttemptOutcome.Failure -> {
                            error = if (outcome.lockedUntilMs > 0L) {
                                strings.pinDialogErrorLocked.format(
                                    ((outcome.lockedUntilMs - System.currentTimeMillis()) / 1000L + 1).coerceAtLeast(1)
                                )
                            } else strings.pinDialogErrorWrong.format(outcome.remainingAttempts)
                        }
                        is PinPolicy.AttemptOutcome.Locked ->
                            error = strings.pinDialogErrorLocked.format(
                                (outcome.retryAfterMs / 1000L + 1).coerceAtLeast(1)
                            )
                        else -> error = strings.pinDialogErrorWrong.format(0)
                    }
                    ProtectionRuntime.refresh(context)
                }
            }) { Text(strings.pinDialogUnlockBtn) }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = gate::cancel) { Text(strings.pinDialogCancelBtn) }
        }
    )
}

/**
 * First-time Master PIN setup or PIN change. This dialog is hosted by
 * MainActivity; [PinPolicy] validates strength/confirmation and shares a
 * lockout budget with the unlock dialog. BCrypt runs off the UI thread.
 */
@Composable
fun MasterPinDialog(onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val changing = remember { PinVault.isPinSet() }
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(if (changing) strings.pinDialogChangeTitle else strings.pinDialogSetupTitle) },
        text = {
            Column {
                if (changing) OutlinedTextField(
                    value = current, onValueChange = { current = it; error = null },
                    label = { Text(strings.pinDialogCurrentField) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = next, onValueChange = { next = it; error = null },
                    label = { Text(strings.pinDialogNewField) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirm, onValueChange = { confirm = it; error = null },
                    label = { Text(strings.pinDialogConfirmField) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
                if (PinVault.storageDegraded) {
                    Text(strings.protectionStorageDegraded, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                busy = true
                val c = current; val n = next; val conf = confirm
                current = ""; next = ""; confirm = ""
                scope.launch {
                    val outcome = withContext(Dispatchers.Default) {
                        if (changing) PinVault.changePin(c, n, conf)
                        else PinVault.setPin(n, conf)
                    }
                    busy = false
                    when (outcome) {
                        PinPolicy.SetOutcome.Ok -> {
                            ProtectionRuntime.refresh(context)
                            onDismiss()
                        }
                        is PinPolicy.SetOutcome.Invalid -> error = strings.pinDialogErrorWeak
                        PinPolicy.SetOutcome.Mismatch -> error = strings.pinDialogErrorMismatch
                        PinPolicy.SetOutcome.StorageError -> error = strings.protectionStorageDegraded
                        PinPolicy.SetOutcome.WrongCurrent -> {
                            val remaining = PinVault.lockRemainingMs()
                            error = if (remaining > 0L) {
                                strings.pinDialogErrorLocked.format(remaining / 1000L + 1)
                            } else strings.pinDialogErrorWrong.format(0)
                        }
                    }
                }
            }) { Text(strings.pinDialogSaveBtn) }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) { Text(strings.pinDialogCancelBtn) }
        }
    )
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
        LocalLayoutDirection provides if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) { content() }
}
