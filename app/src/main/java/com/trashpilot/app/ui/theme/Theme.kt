package com.trashpilot.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
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

@Composable
fun TrashPilotTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = false,
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TrashPilotColorScheme,
        typography = Typography,
        content = content
    )
}
