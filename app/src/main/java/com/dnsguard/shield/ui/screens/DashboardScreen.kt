package com.dnsguard.shield.ui.screens

import android.content.Intent
import android.app.admin.DevicePolicyManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.dnsguard.shield.LocalLanguageSwitcher
import com.dnsguard.shield.LocalStrings
import com.dnsguard.shield.LocalPinGate
import com.dnsguard.shield.PinGate
import com.dnsguard.shield.MasterPinDialog
import com.dnsguard.shield.core.DeviceAdminManager
import com.dnsguard.shield.core.ProtectionRuntime
import com.dnsguard.shield.core.security.PinVault
import com.dnsguard.shield.core.DnsManager
import com.dnsguard.shield.core.Permissions
import com.dnsguard.shield.core.ShieldRuntime
import com.dnsguard.shield.service.ShieldWatchdogService
import com.dnsguard.shield.ui.components.PermissionRow
import com.dnsguard.shield.ui.components.StatusCard
import com.dnsguard.shield.ui.components.StatusRow
import com.dnsguard.shield.ui.navigation.rememberResumeTick
import com.dnsguard.shield.ui.theme.AccentLightBlue
import com.dnsguard.shield.ui.theme.AppBackground
import com.dnsguard.shield.ui.theme.BorderAndInputBg
import com.dnsguard.shield.ui.theme.DangerRed
import com.dnsguard.shield.ui.theme.PrimaryBlue
import com.dnsguard.shield.ui.theme.PureWhite
import com.dnsguard.shield.ui.theme.StatusGreen
import com.dnsguard.shield.ui.theme.TextPrimary
import com.dnsguard.shield.ui.theme.TextSecondary
import com.dnsguard.shield.ui.theme.TopBarBlue
import com.dnsguard.shield.ui.theme.WarningOrange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Screen 1 — Main Dashboard (standard mode, zero commands).
 *
 * Reproduces the wireframe exactly:
 *  - TopAppBar #0D47A1 with "🛡️ DNS Guard & Shield" and the "🌐 EN" toggle
 *  - Background #121212
 *  - DNS Protection card (green/red dot + hostname in #64B5F6)
 *  - Reddit NSFW Shield card (armed / active / disabled states)
 *  - Permissions card with clickable ✅ / ⚠️ rows — every one of them
 *    granted through a normal Android screen (no ADB anywhere)
 *  - Full-width primary "⚙️ DNS Settings" button (#1E88E5)
 *  - Full-width outlined "💻 Setup Guide" button (#2C2C2C)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenDnsSettings: () -> Unit,
    onOpenSetupGuide: () -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gate = LocalPinGate.current
    var showPinManagement by remember { androidx.compose.runtime.mutableStateOf(false) }
    val protection by ProtectionRuntime.state.collectAsState()
    // Refresh from DevicePolicyManager on every return from Android's
    // activation/deactivation consent UI (including a cancelled request).
    val resumeTick = rememberResumeTick()
    androidx.compose.runtime.LaunchedEffect(resumeTick) { ProtectionRuntime.refresh(context) }
    val requestAdmin = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { ProtectionRuntime.refresh(context) }

    if (showPinManagement) {
        MasterPinDialog(onDismiss = { showPinManagement = false; ProtectionRuntime.refresh(context) })
    }

    val snackbarHostState = remember { SnackbarHostState() }

    var refreshTick by remember { mutableIntStateOf(0) }
    val refreshKey = resumeTick + refreshTick

    // Live shield phase from the accessibility service / watchdog.
    val shieldSnapshot by ShieldRuntime.state.collectAsState()

    // Snapshots that only change on resume / explicit actions.
    val dnsStatus = remember(refreshKey) { DnsManager.currentStatus(context) }
    val shieldEnabled = remember(refreshKey) { Permissions.isShieldServiceEnabled(context) }
    val hasUsageStats = remember(refreshKey) { Permissions.hasUsageStats(context) }
    val watchdogRunning = remember(refreshKey) { ShieldWatchdogService.isRunning }

    val notificationPermissionGranted = remember(refreshKey) {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            Permissions.hasNotificationPermission(context)
    }

    val requestNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Notification permission is optional — the shield works either way.
        ShieldWatchdogService.start(context)
        scheduleRefresh(scope) { refreshTick++ }
        scope.launch { snackbarHostState.showSnackbar(strings.watchdogStarted) }
    }

    fun startWatchdogWithPermissionFlow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !notificationPermissionGranted
        ) {
            requestNotifications.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            ShieldWatchdogService.start(context)
            scheduleRefresh(scope) { refreshTick++ }
            scope.launch { snackbarHostState.showSnackbar(strings.watchdogStarted) }
        }
    }

    // ── Shield card state machine (display only) ──────────────────────────
    val shieldActiveInReddit =
        shieldSnapshot.phase == ShieldRuntime.Phase.ACTIVE_IN_REDDIT
    val (shieldDotColor, shieldPrimary, shieldSecondary) = when {
        !shieldEnabled ->
            Triple(
                WarningOrange,
                strings.shieldDisabled,
                if (shieldSnapshot.redditVisible) strings.shieldRedditOpenNow
                else strings.shieldActivatesSubtitle
            )
        !watchdogRunning ->
            Triple(WarningOrange, strings.shieldIncomplete, strings.shieldActivatesSubtitle)
        shieldActiveInReddit ->
            Triple(StatusGreen, strings.shieldActiveNow, strings.shieldActivatesSubtitle)
        else ->
            Triple(StatusGreen, strings.shieldArmed, strings.shieldActivatesSubtitle)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "🛡️ ${strings.appTitle}",
                        color = TextPrimary
                    )
                },
                actions = {
                    val switchLanguage = LocalLanguageSwitcher.current
                    TextButton(onClick = switchLanguage) {
                        Text(
                            text = "🌐 ${strings.language.code}",
                            color = TextPrimary,
                            fontSize = 15.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TopBarBlue,
                    titleContentColor = TextPrimary,
                    actionIconContentColor = TextPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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

            // ── Card 1: DNS Protection ────────────────────────────────────
            StatusCard(title = "🌐  ${strings.dnsProtection}") {
                if (dnsStatus.isActive) {
                    StatusRow(
                        dotColor = StatusGreen,
                        primaryText = strings.activeAndProtected,
                        secondaryText = dnsStatus.displayHostname,
                        secondaryColor = AccentLightBlue
                    )
                } else {
                    StatusRow(
                        dotColor = DangerRed,
                        primaryText = strings.dnsInactiveTitle,
                        secondaryText = strings.dnsAutomaticHostname,
                        secondaryColor = TextSecondary
                    )
                }
            }

            // ── Card 2: Reddit NSFW Shield ────────────────────────────────
            StatusCard(title = "📱  ${strings.redditShield}") {
                StatusRow(
                    dotColor = shieldDotColor,
                    primaryText = shieldPrimary,
                    secondaryText = shieldSecondary
                )
            }

            // ── Card 3: Permissions Status ────────────────────────────────
            StatusCard(title = "🔐  ${strings.permissionsStatus}") {
                Column {
                    PermissionRow(
                        label = strings.permShieldService,
                        granted = shieldEnabled,
                        statusText = if (shieldEnabled) strings.permGranted else strings.permTapToFix,
                        onClick = {
                            if (shieldEnabled) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(strings.permGranted)
                                }
                            } else {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    )
                                }
                                scope.launch {
                                    snackbarHostState.showSnackbar(strings.armHint)
                                }
                            }
                        }
                    )
                    PermissionRow(
                        label = strings.permUsageStats,
                        granted = hasUsageStats,
                        statusText = if (hasUsageStats) strings.permGranted else strings.permTapToFix,
                        onClick = {
                            if (hasUsageStats) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(strings.permGranted)
                                }
                            } else {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                    )
                                }
                                scope.launch {
                                    snackbarHostState.showSnackbar(strings.openUsageAccess)
                                }
                            }
                        }
                    )
                    PermissionRow(
                        label = strings.permForegroundService,
                        granted = watchdogRunning,
                        statusText = if (watchdogRunning) strings.permGranted else strings.permTapToFix,
                        onClick = {
                            if (watchdogRunning) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(strings.permGranted)
                                }
                            } else {
                                startWatchdogWithPermissionFlow()
                            }
                        }
                    )
                }
            }

            // ── Uninstall Protection card (live Device Admin + PIN status) ─
            StatusCard(title = "🛡️  ${strings.protectionTitle}") {
                StatusRow(
                    dotColor = if (protection.fullyProtected) StatusGreen else WarningOrange,
                    primaryText = if (protection.fullyProtected) strings.protectionActive
                        else strings.protectionInactive,
                    secondaryText = strings.protectionDetail,
                    primaryColor = if (protection.fullyProtected) StatusGreen else TextPrimary
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { showPinManagement = true }) {
                    Text(if (protection.pinSet) strings.protectionChangePin else strings.protectionSetupPin)
                }
                if (!protection.adminActive) {
                    Button(onClick = {
                        if (!PinVault.isPinSet()) {
                            showPinManagement = true
                        } else {
                            requestAdmin.launch(
                                DeviceAdminManager.activationIntent(context, strings.adminEnableExplanation)
                            )
                        }
                    }) { Text(strings.protectionActivateAdmin) }
                } else {
                    OutlinedButton(onClick = {
                        gate.request(PinGate.Purpose.DEACTIVATE_ADMIN) {
                            DeviceAdminManager.deactivate(context)
                            ProtectionRuntime.refresh(context)
                        }
                    }) { Text(strings.protectionDeactivateAdmin) }
                }
                Spacer(Modifier.height(8.dp))
                StatusRow(
                    dotColor = if (protection.guardServiceEnabled) StatusGreen else WarningOrange,
                    primaryText = if (protection.guardServiceEnabled) strings.protectionGuardEnabled
                        else strings.protectionGuardDisabled
                )
                if (!protection.guardServiceEnabled) {
                    OutlinedButton(onClick = {
                        runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                    }) { Text(strings.protectionOpenGuardSettings) }
                }
                if (PinVault.storageDegraded) {
                    Text(strings.protectionStorageDegraded, color = WarningOrange)
                }
            }

            // ── Action buttons ────────────────────────────────────────────
            Button(
                onClick = onOpenDnsSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    contentColor = PureWhite
                )
            ) {
                Text(
                    text = strings.dnsSettingsButton,
                    fontSize = 16.sp
                )
            }

            OutlinedButton(
                onClick = onOpenSetupGuide,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, BorderAndInputBg),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = BorderAndInputBg,
                    contentColor = TextPrimary
                )
            ) {
                Text(
                    text = strings.setupGuideButton,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/** One-shot delayed re-read used after starting the watchdog. */
private fun scheduleRefresh(
    scope: kotlinx.coroutines.CoroutineScope,
    block: () -> Unit
) {
    scope.launch {
        delay(700)
        block()
    }
}
