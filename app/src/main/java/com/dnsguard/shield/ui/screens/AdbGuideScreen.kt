package com.dnsguard.shield.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dnsguard.shield.LocalStrings
import com.dnsguard.shield.ui.components.CodeBlock
import com.dnsguard.shield.ui.components.StatusCard
import com.dnsguard.shield.ui.theme.AppBackground
import com.dnsguard.shield.ui.theme.BorderAndInputBg
import com.dnsguard.shield.ui.theme.PrimaryBlue
import com.dnsguard.shield.ui.theme.TextPrimary
import com.dnsguard.shield.ui.theme.TextSecondary
import com.dnsguard.shield.ui.theme.TopBarBlue
import com.dnsguard.shield.ui.theme.WarningOrange

private const val COMMAND_GRANT_SECURE_SETTINGS =
    "adb shell pm grant com.dnsguard.shield android.permission.WRITE_SECURE_SETTINGS"
private const val COMMAND_GRANT_USAGE_STATS =
    "adb shell appops set com.dnsguard.shield GET_USAGE_STATS allow"
private const val COMMAND_VERIFY_ACCESSIBILITY_OFF =
    "adb shell settings get secure accessibility_enabled"

/**
 * Screen 3 — ADB Setup Guide.
 *
 * Numbered steps plus copyable command blocks (bg #0A0A0A, text #80CBC4):
 *  1. enable developer options
 *  2. enable USB debugging
 *  3. grant WRITE_SECURE_SETTINGS + PACKAGE_USAGE_STATS over adb
 * Verification block proves the accessibility master switch reads 0.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdbGuideScreen(onBack: () -> Unit) {
    val strings = LocalStrings.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = strings.adbGuideTitle,
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
                text = strings.adbIntro,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            // ── Step 1 ────────────────────────────────────────────────────
            StepCard(
                number = 1,
                title = strings.step1Title,
                body = strings.step1Body
            )

            // ── Step 2 ────────────────────────────────────────────────────
            StepCard(
                number = 2,
                title = strings.step2Title,
                body = strings.step2Body
            )

            // ── Step 3 ────────────────────────────────────────────────────
            StepCard(
                number = 3,
                title = strings.step3Title,
                body = strings.step3Body
            )
            CodeBlock(command = COMMAND_GRANT_SECURE_SETTINGS, strings = strings)
            CodeBlock(command = COMMAND_GRANT_USAGE_STATS, strings = strings)

            // ── Verification ──────────────────────────────────────────────
            StatusCard(title = strings.verifyTitle) {
                Column {
                    Text(
                        text = strings.verifyBody,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    CodeBlock(command = COMMAND_VERIFY_ACCESSIBILITY_OFF, strings = strings)
                }
            }

            // ── Safety model ──────────────────────────────────────────────
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
private fun StepCard(number: Int, title: String, body: String) {
    StatusCard(title = title) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = PrimaryBlue,
                modifier = Modifier.size(30.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = number.toString(),
                        color = Color.White,
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
    }
}
