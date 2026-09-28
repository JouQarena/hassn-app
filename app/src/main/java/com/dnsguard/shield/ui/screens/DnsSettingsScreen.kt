package com.dnsguard.shield.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnsguard.shield.LocalStrings
import com.dnsguard.shield.core.DnsManager
import com.dnsguard.shield.core.Permissions
import com.dnsguard.shield.ui.components.Badge
import com.dnsguard.shield.ui.components.SectionHeading
import com.dnsguard.shield.ui.components.StatusCard
import com.dnsguard.shield.ui.navigation.rememberResumeTick
import com.dnsguard.shield.ui.theme.AccentLightBlue
import com.dnsguard.shield.ui.theme.AppBackground
import com.dnsguard.shield.ui.theme.BorderAndInputBg
import com.dnsguard.shield.ui.theme.CardSurface
import com.dnsguard.shield.ui.theme.DangerRed
import com.dnsguard.shield.ui.theme.InputBorder
import com.dnsguard.shield.ui.theme.PrimaryBlue
import com.dnsguard.shield.ui.theme.PureWhite
import com.dnsguard.shield.ui.theme.StatusGreen
import com.dnsguard.shield.ui.theme.TextPrimary
import com.dnsguard.shield.ui.theme.TextSecondary
import com.dnsguard.shield.ui.theme.TopBarBlue
import com.dnsguard.shield.util.DnsHostname
import kotlinx.coroutines.launch

/**
 * Screen 2 — DNS Settings.
 *
 * Radio-list of vetted presets (AdGuard Family is the default product choice),
 * a validated free-form hostname field, and an Apply button that writes Android's
 * Private DNS keys via `WRITE_SECURE_SETTINGS`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsSettingsScreen(
    onBack: () -> Unit,
    onOpenAdbGuide: () -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val refreshKey = rememberResumeTick()
    val currentStatus = remember(refreshKey) { DnsManager.currentStatus(context) }
    val hasSecureSettings = remember(refreshKey) { Permissions.hasWriteSecureSettings(context) }

    val presets = remember(strings) {
        DnsManager.presets(
            familyName = strings.presetAdguardFamilyName,
            familyDesc = strings.presetAdguardFamilyDesc,
            adguardName = strings.presetAdguardName,
            adguardDesc = strings.presetAdguardDesc,
            unfilteredName = strings.presetAdguardUnfilteredName,
            unfilteredDesc = strings.presetAdguardUnfilteredDesc,
            cloudflareName = strings.presetCloudflareName,
            cloudflareDesc = strings.presetCloudflareDesc,
            cloudflareFamilyName = strings.presetCloudflareFamilyName,
            cloudflareFamilyDesc = strings.presetCloudflareFamilyDesc,
            quad9Name = strings.presetQuad9Name,
            quad9Desc = strings.presetQuad9Desc,
            automaticName = strings.presetAutomaticName,
            automaticDesc = strings.presetAutomaticDesc
        )
    }

    // Pre-select the preset that matches the currently active hostname.
    var selectedId by remember(refreshKey) {
        val active = currentStatus.specifier
        mutableStateOf(
            presets.firstOrNull { it.hostname != null && it.hostname == active }?.id
                ?: presets.last().id
        )
    }
    var customHostname by remember(refreshKey) {
        mutableStateOf(currentStatus.specifier.orEmpty())
    }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun apply(hostname: String?) {
        if (!hasSecureSettings) {
            errorText = strings.errorNoSecureSettings
            return
        }
        if (hostname == null) {
            val ok = DnsManager.applyAutomatic(context)
            if (ok) {
                errorText = null
                scope.launch {
                    snackbarHostState.showSnackbar(strings.dnsAutomaticAppliedMessage)
                }
            }
            return
        }
        if (!DnsHostname.isValid(hostname)) {
            errorText = strings.errorInvalidHostname
            return
        }
        val ok = DnsManager.applyHostname(context, hostname)
        if (ok) {
            errorText = null
            scope.launch {
                snackbarHostState.showSnackbar(
                    strings.dnsAppliedMessage.format(DnsHostname.normalize(hostname))
                )
            }
        } else {
            errorText = strings.errorNoSecureSettings
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = strings.dnsSettingsTitle,
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

            // ── Current configuration ─────────────────────────────────────
            StatusCard(title = strings.currentLabel) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentStatus.displayHostname
                                ?: strings.dnsAutomaticHostname,
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = FontFamily.Monospace,
                            color = if (currentStatus.isActive) AccentLightBlue else TextSecondary
                        )
                        Text(
                            text = strings.dnsNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (currentStatus.isActive) {
                        Badge(
                            text = strings.dnsActiveBadge,
                            containerColor = StatusGreen.copy(alpha = 0.18f),
                            contentColor = StatusGreen
                        )
                    } else {
                        Badge(
                            text = strings.dnsInactiveBadge,
                            containerColor = DangerRed.copy(alpha = 0.18f),
                            contentColor = DangerRed
                        )
                    }
                }
            }

            // ── Preset list ───────────────────────────────────────────────
            SectionHeading(text = strings.choosePreset)
            presets.forEach { preset ->
                val selected = selectedId == preset.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                .clickable {
                    selectedId = preset.id
                    customHostname = preset.hostname.orEmpty()
                    errorText = null
                },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) BorderAndInputBg else CardSurface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (selected) PrimaryBlue else BorderAndInputBg
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = {
                                selectedId = preset.id
                                customHostname = preset.hostname.orEmpty()
                                errorText = null
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = PrimaryBlue,
                                unselectedColor = TextSecondary
                            )
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = preset.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            if (preset.hostname != null) {
                                Text(
                                    text = preset.hostname,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = AccentLightBlue
                                )
                            }
                        }
                    }
                }
            }

            // ── Custom hostname ───────────────────────────────────────────
            SectionHeading(text = strings.customHostnameTitle)
            OutlinedTextField(
                value = customHostname,
                onValueChange = { value ->
                    customHostname = value
                    errorText = null
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = strings.customHostnamePlaceholder,
                        color = TextSecondary
                    )
                },
                singleLine = true,
                isError = errorText != null,
                supportingText = errorText?.let { message ->
                    { Text(text = message, color = DangerRed) }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = BorderAndInputBg,
                    unfocusedContainerColor = BorderAndInputBg,
                    disabledContainerColor = BorderAndInputBg,
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = InputBorder,
                    errorBorderColor = DangerRed,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentLightBlue,
                    focusedLabelColor = AccentLightBlue
                ),
                shape = RoundedCornerShape(8.dp)
            )

            // ── Apply ─────────────────────────────────────────────────────
            Button(
                onClick = {
                    val typed = customHostname.trim()
                    if (typed.isEmpty()) {
                        apply(null)
                    } else {
                        apply(typed)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    contentColor = PureWhite
                )
            ) {
                Text(text = strings.applyButton, fontSize = 16.sp)
            }

            if (!hasSecureSettings) {
                Button(
                    onClick = onOpenAdbGuide,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BorderAndInputBg,
                        contentColor = PrimaryBlue
                    )
                ) {
                    Text(text = strings.openAdbGuide, fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
