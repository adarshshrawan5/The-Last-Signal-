package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BloodRed,
    secondary = TerminalAmber,
    tertiary = TerminalGreen,
    background = PitchBlack,
    surface = DarkObsidian,
    onPrimary = Color.White,
    onSecondary = PitchBlack,
    onTertiary = PitchBlack,
    onBackground = GhostlyWhite,
    onSurface = GhostlyWhite,
    surfaceVariant = DeepSlateGrey,
    onSurfaceVariant = StaticGrey,
    outline = DarkCrimson
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
