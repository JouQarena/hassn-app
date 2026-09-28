package com.dnsguard.shield.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnsguard.shield.ui.theme.AppBackground
import com.dnsguard.shield.ui.theme.BorderAndInputBg
import com.dnsguard.shield.ui.theme.CardSurface
import com.dnsguard.shield.ui.theme.TextPrimary
import com.dnsguard.shield.ui.theme.TextSecondary

/**
 * The shared status card used across the dashboard and settings screens:
 * #1E1E1E surface, #2C2C2C border, 12 dp radius — exactly as specified.
 */
@Composable
fun StatusCard(
    title: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, BorderAndInputBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

/**
 * A glowing status dot: #66BB6A green / #EF5350 red / #FFA726 orange.
 */
@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(10.dp)
            .background(color = color.copy(alpha = 0.18f), shape = CircleShape)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color = color, shape = CircleShape)
        )
    }
}

/** Row with a status dot + primary line + optional secondary line. */
@Composable
fun StatusRow(
    dotColor: Color,
    primaryText: String,
    modifier: Modifier = Modifier,
    primaryColor: Color = TextPrimary,
    secondaryText: String? = null,
    secondaryColor: Color = TextSecondary
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        StatusDot(color = dotColor)
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = primaryText,
                style = MaterialTheme.typography.bodyLarge,
                color = primaryColor
            )
            if (secondaryText != null) {
                Text(
                    text = secondaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryColor
                )
            }
        }
    }
}

/**
 * Clickable permission row used on the dashboard.
 * Leading glyph is a green ✅ when granted, orange ⚠️ when missing.
 */
@Composable
fun PermissionRow(
    label: String,
    granted: Boolean,
    statusText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (granted) "✅" else "⚠️",
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = TextPrimary
        )
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (granted) Color(0x3366BB6A) else Color(0x33FFA726)
        ) {
            Text(
                text = statusText,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall,
                color = if (granted) Color(0xFF66BB6A) else Color(0xFFFFA726),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/** Small pill badge (Active / Inactive) used on the DNS settings screen. */
@Composable
fun Badge(text: String, containerColor: Color, contentColor: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor,
        contentColor = contentColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Section heading used inside the settings / guide screens. */
@Composable
fun SectionHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

/** App background modifier shared by all screens. */
fun Modifier.appBackground(): Modifier =
    this.background(AppBackground)
