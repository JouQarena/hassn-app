package com.dnsguard.shield.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * ---------------------------------------------------------------------------
 * The product palette. Every color used anywhere in the app is defined here
 * with the exact hex values from the specification. There is deliberately no
 * Purple80 / Purple40 (or any purple at all) anywhere in this project.
 * ---------------------------------------------------------------------------
 */

/** TopAppBar background. */
val TopBarBlue = Color(0xFF0D47A1)

/** Primary interactive color: buttons, selected radio buttons, links. */
val PrimaryBlue = Color(0xFF1E88E5)

/** Secondary accent: hostnames, highlights, subtle glows. */
val AccentLightBlue = Color(0xFF64B5F6)

/** App window background. */
val AppBackground = Color(0xFF121212)

/** Card / sheet surface. */
val CardSurface = Color(0xFF1E1E1E)

/** Card borders and input field backgrounds. */
val BorderAndInputBg = Color(0xFF2C2C2C)

/** Input field outline. */
val InputBorder = Color(0xFF424242)

/** Primary text. */
val TextPrimary = Color(0xFFE0E0E0)

/** Secondary / supporting text. */
val TextSecondary = Color(0xFF9E9E9E)

/** Positive status ("Active & Protected", granted permissions). */
val StatusGreen = Color(0xFF66BB6A)

/** Attention status (missing permission, degraded state). */
val WarningOrange = Color(0xFFFFA726)

/** Failure status (DNS inactive, errors). */
val DangerRed = Color(0xFFEF5350)

/** Code block background (ADB guide). */
val CodeBlockBg = Color(0xFF0A0A0A)

/** Code block text (ADB guide). */
val CodeBlockText = Color(0xFF80CBC4)

/** Pure white for text on saturated blue buttons. */
val PureWhite = Color(0xFFFFFFFF)

/** Scrim behind dialogs / snackbar base. */
val ScrimBlack = Color(0x99000000)

/** Subtle green halo used behind "Active" status dots. */
val StatusGreenContainer = Color(0x3366BB6A)

/** Subtle red halo used behind "Inactive" status dots. */
val DangerRedContainer = Color(0x33EF5350)
