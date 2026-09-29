package com.dnsguard.shield.ui.screens

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnsguard.shield.LocalStrings
import com.dnsguard.shield.LocalPinGate
import com.dnsguard.shield.PinGate
import com.dnsguard.shield.MasterPinDialog
import com.dnsguard.shield.core.DeviceAdminManager
import com.dnsguard.shield.core.ProtectionRuntime
import com.dnsguard.shield.core.security.PinVault
import com.dnsguard.shield.core.DnsManager
import com.dnsguard.shield.core.Permissions
import com.dnsguard.shield.service.ShieldWatchdogService
import com.dnsguard.shield.ui.components.StatusCard
import com.dnsguard.shield.ui.navigation.rememberResumeTick
import com.dnsguard.shield.ui.theme.AppBackground
import com.dnsguard.shield.ui.theme.BorderAndInputBg
import com.dnsguard.shield.ui.theme.PureWhite
import com.dnsguard.shield.ui.theme.StatusGreen
import com.dnsguard.shield.ui.theme.TextPrimary
import com.dnsguard.shield.ui.theme.TextSecondary
import com.dnsguard.shield.ui.theme.TopBarBlue
import com.dnsguard.shield.ui.theme.WarningOrange
import kotlinx.coroutines.launch

/**
 * Screen 3 — Setup Guide (standard mode, zero commands).
 *
 * Four core steps plus an optional uninstall-protection step, all normal Android screens —
 * the exact replacement for the old ADB command list:
 *
 *  1. DNS protection  → copy hostname + open Private DNS dialog
 *  2. Usage access    → open Android's Usage Access settings
 *  3. Arm the shield  → open Accessibility and toggle the service ON
 *  4. Notifications   → runtime permission dialog (API 33+)
 *
 * Each step shows its live ✅/⚠️ state; the safety note at the bottom spells
 * out what "shield" means without WRITE_SECURE_SETTINGS.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupGuideScreen(onBack: () -> Unit) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val pinGate = LocalPinGate.current

    var showPinManagement by remember { androidx.compose.runtime.mutableStateOf(false) }
    if (showPinManagement) MasterPinDialog(onDismiss = {
        showPinManagement = false
        ProtectionRuntime.refresh(context)
    })
    val requestAdmin = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { ProtectionRuntime.refresh(context) }

    val refreshKey = rememberResumeTick()
    var refreshTick by remember { mutableIntStateOf(0) }
    val refreshAll = refreshKey + refreshTick

    androidx.compose.runtime.LaunchedEffect(refreshAll) { ProtectionRuntime.refresh(context) }
    val adminActive = remember(refreshAll) { DeviceAdminManager.isAdminActive(context) }
    val pinSet = remember(refreshAll) { PinVault.isPinSet() }

    val dnsOk = remember(refreshAll) { DnsManager.currentStatus(context).isActive }
    val usageOk = remember(refreshAll) { Permissions.hasUsageStats(context) }
    val shieldOk = remember(refreshAll) { Permissions.isShieldServiceEnabled(context) }
    val notifOk = remember(refreshAll) {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            Permissions.hasNotificationPermission(context)
    }

    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    val requestNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        ShieldWatchdogService.start(context)
        refreshTick++
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = strings.setupGuideTitle,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.back,
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TopBarBlue,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        },
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
        containerColor = AppBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                text = strings.setupIntro,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            // ── Step 1: DNS ───────────────────────────────────────────────
            StepCard(
                number = 1,
                title = strings.step1Title,
                body = strings.step1Body,
                done = dnsOk && DnsManager.canWriteSecureSettings(context),
                buttonLabel = strings.dnsCopyOpenBtn,
                onButtonClick = {
                    val openDns: () -> Unit = {
                        val host = DnsManager.currentStatus(context).specifier
                            ?: "family.adguard-dns.com"
                        com.dnsguard.shield.DnsGuardApplication.prefs().protectedDnsHostname = host
                        clipboard.setText(AnnotatedString(host))
                        DnsManager.openPrivateDnsSettings(context)
                        scope.launch {
                            snackbarHostState.showSnackbar(strings.dnsAppliedMessage.format(host))
                        }
                        refreshTick++
                    }
                    if (PinVault.isPinSet()) {
                        pinGate.request(PinGate.Purpose.DNS_SETTINGS, openDns)
                    } else openDns()
                },
                hint = "adb shell pm grant com.dnsguard.shield android.permission.WRITE_SECURE_SETTINGS"
            )

            // ── Step 2: Usage access ──────────────────────────────────────
            StepCard(
                number = 2,
                title = strings.step2Title,
                body = strings.step2Body,
                done = usageOk,
                buttonLabel = strings.openUsageBtn,
                onButtonClick = {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                    refreshTick++
                }
            )

            // ── Step 3: Arm the accessibility shield ──────────────────────
            StepCard(
                number = 3,
                title = strings.step3Title,
                body = strings.step3Body,
                done = shieldOk,
                buttonLabel = strings.openA11yBtn,
                onButtonClick = {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                    refreshTick++
                },
                hint = strings.armHint
            )

            // ── Step 4: Notifications ─────────────────────────────────────
            StepCard(
                number = 4,
                title = strings.step4Title,
                body = strings.step4Body,
                done = notifOk,
                buttonLabel = strings.allowNotifBtn,
                onButtonClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestNotifications.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        ShieldWatchdogService.start(context)
                        refreshTick++
                    }
                }
            )

            // ── Step 5: optional native uninstall protection ────────────
            StepCard(
                number = 5,
                title = strings.protectionGuideTitle,
                body = strings.protectionGuideBody,
                done = adminActive && pinSet,
                buttonLabel = if (!pinSet) strings.protectionSetupPin
                    else if (!adminActive) strings.protectionActivateAdmin
                    else strings.protectionOpenGuardSettings,
                onButtonClick = {
                    when {
                        !pinSet -> showPinManagement = true
                        !adminActive -> requestAdmin.launch(
                            DeviceAdminManager.activationIntent(context, strings.adminEnableExplanation)
                        )
                        else -> runCatching {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    }
                }
            )

            // ── Safety note ───────────────────────────────────────────────
            StatusCard(title = "⚠️ ${strings.safetyTitle}") {
                Text(
                    text = strings.safetyBody,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarningOrange
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StepCard(
    number: Int,
    title: String,
    body: String,
    done: Boolean,
    buttonLabel: String,
    onButtonClick: () -> Unit,
    hint: String? = null
) {
    StatusCard(title = title) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Surface(
                    shape = CircleShape,
                    color = if (done) StatusGreen else WarningOrange,
                    modifier = Modifier
                        .width(30.dp)
                        .height(30.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (done) "✓" else number.toString(),
                            color = PureWhite,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
            }

            if (hint != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = com.dnsguard.shield.ui.theme.AccentLightBlue
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onButtonClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (done) BorderAndInputBg else com.dnsguard.shield.ui.theme.PrimaryBlue,
                    contentColor = if (done) StatusGreen else PureWhite
                ),
                border = if (done) BorderStroke(1.dp, StatusGreen) else null
            ) {
                Text(text = buttonLabel, fontSize = 15.sp)
            }
        }
    }
}
