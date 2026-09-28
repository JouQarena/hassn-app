package com.dnsguard.shield.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A [darkColorScheme] where *every* slot is written out explicitly.
 *
 * Material 3's default color scheme contains several purple values
 * (primary/secondary/tertiary defaults, purple-tinted containers). By
 * enumerating all 36 slots with the specification palette we guarantee the
 * "Zero Purple Rule" holds by construction — no default can leak through.
 */
private val DnsGuardColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = PureWhite,
    primaryContainer = TopBarBlue,
    onPrimaryContainer = TextPrimary,
    inversePrimary = AccentLightBlue,

    secondary = AccentLightBlue,
    onSecondary = AppBackground,
    secondaryContainer = BorderAndInputBg,
    onSecondaryContainer = TextPrimary,

    tertiary = StatusGreen,
    onTertiary = AppBackground,
    tertiaryContainer = BorderAndInputBg,
    onTertiaryContainer = TextPrimary,

    background = AppBackground,
    onBackground = TextPrimary,

    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = BorderAndInputBg,
    onSurfaceVariant = TextSecondary,
    surfaceTint = AccentLightBlue,

    surfaceDim = AppBackground,
    surfaceBright = CardSurface,

    surfaceContainerLowest = Color(0xFF0A0A0A),
    surfaceContainerLow = Color(0xFF171717),
    surfaceContainer = CardSurface,
    surfaceContainerHigh = BorderAndInputBg,
    surfaceContainerHighest = InputBorder,

    inverseSurface = TextPrimary,
    inverseOnSurface = AppBackground,

    outline = InputBorder,
    outlineVariant = BorderAndInputBg,
    scrim = ScrimBlack,

    error = DangerRed,
    onError = PureWhite,
    errorContainer = DangerRedContainer,
    onErrorContainer = DangerRed,
)

/** App typography — system font, sized for a dense dark-mode dashboard. */
private val DnsGuardTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

/** Shared shape system — 12dp card radius as specified in the wireframes. */
object DnsGuardShapes {
    val CardRadius = RoundedCornerShape(12.dp)
    val ButtonRadius = RoundedCornerShape(10.dp)
    val InputRadius = RoundedCornerShape(8.dp)
}

/**
 * Single source of truth for Material theming. Dark mode only: the scheme is
 * passed in unconditionally so light-system devices get the same look.
 */
@Composable
fun DnsGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DnsGuardColorScheme,
        typography = DnsGuardTypography,
        shapes = androidx.compose.material3.Shapes(
            extraSmall = RoundedCornerShape(4.dp),
            small = RoundedCornerShape(6.dp),
            medium = DnsGuardShapes.CardRadius,
            large = RoundedCornerShape(16.dp),
            extraLarge = RoundedCornerShape(24.dp)
        ),
        content = content
    )
}
