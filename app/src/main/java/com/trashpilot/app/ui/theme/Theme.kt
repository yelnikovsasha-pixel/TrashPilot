package com.trashpilot.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TrashPilotColorScheme = lightColorScheme(
    primary = TrashPilotPrimary,
    onPrimary = TrashPilotOnPrimary,
    primaryContainer = TrashPilotAccentSurface,
    onPrimaryContainer = TrashPilotPrimary,
    secondary = TrashPilotPrimary,
    onSecondary = TrashPilotOnPrimary,
    secondaryContainer = TrashPilotAccentSurface,
    onSecondaryContainer = TrashPilotPrimary,
    background = TrashPilotBackground,
    onBackground = TrashPilotText,
    surface = TrashPilotBackground,
    onSurface = TrashPilotText,
    surfaceVariant = TrashPilotCard,
    onSurfaceVariant = TrashPilotTextSecondary,
    surfaceContainer = TrashPilotCard,
    surfaceContainerLow = TrashPilotCard,
    surfaceContainerLowest = TrashPilotBackground,
    outline = TrashPilotOutline
)

private val TrashPilotDarkColorScheme = darkColorScheme(
    primary = TrashPilotDarkPrimary,
    onPrimary = TrashPilotPrimary,
    primaryContainer = TrashPilotDarkAccentSurface,
    onPrimaryContainer = TrashPilotDarkText,
    secondary = TrashPilotDarkPrimary,
    onSecondary = TrashPilotPrimary,
    secondaryContainer = TrashPilotDarkAccentSurface,
    onSecondaryContainer = TrashPilotDarkText,
    background = TrashPilotDarkBackground,
    onBackground = TrashPilotDarkText,
    surface = TrashPilotDarkSurface,
    onSurface = TrashPilotDarkText,
    surfaceVariant = TrashPilotDarkCard,
    onSurfaceVariant = TrashPilotDarkTextSecondary,
    surfaceContainer = TrashPilotDarkCard,
    surfaceContainerLow = TrashPilotDarkCard,
    surfaceContainerLowest = TrashPilotDarkBackground,
    outline = TrashPilotDarkOutline
)

@Composable
fun TrashPilotTheme(
    darkTheme: Boolean = false,
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) TrashPilotDarkColorScheme else TrashPilotColorScheme,
        typography = Typography,
        content = content
    )
}
