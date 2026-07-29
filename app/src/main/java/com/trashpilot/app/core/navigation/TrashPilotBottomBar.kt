package com.trashpilot.app.core.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.trashpilot.app.R
import com.trashpilot.app.ui.theme.TrashPilotDimensions
import com.trashpilot.app.ui.theme.TrashPilotElevation

private data class BottomDestination(
    val route: String,
    @param:StringRes val label: Int,
    val icon: ImageVector
)

private val bottomDestinations = listOf(
    BottomDestination("home", R.string.nav_home, Icons.Filled.Home),
    BottomDestination("privacy", R.string.nav_privacy, Icons.Filled.Security),
    BottomDestination("reports", R.string.nav_reports, Icons.Filled.Assessment),
    BottomDestination("settings", R.string.nav_settings, Icons.Filled.Settings)
)

@Composable
fun TrashPilotBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .height(TrashPilotDimensions.BottomBarHeight)
            .zIndex(10f),
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = TrashPilotElevation.None,
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        bottomDestinations.forEach { destination ->
            val selected = currentRoute == destination.route ||
                (destination.route == "home" && currentRoute in setOf(
                    "splash", "scanner", "results", "quick-clean", "category-files", "trash-dna"
                ))
            NavigationBarItem(
                modifier = Modifier.weight(1f),
                selected = selected,
                onClick = { onNavigate(destination.route) },
                icon = {
                    Box(
                        modifier = Modifier
                            .width(if (selected) TrashPilotDimensions.BottomBarIndicatorWidth else TrashPilotDimensions.BottomBarIndicatorHeight)
                            .height(TrashPilotDimensions.BottomBarIndicatorHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = null,
                            modifier = Modifier.size(TrashPilotDimensions.BottomBarIcon)
                        )
                    }
                },
                label = {
                    Text(
                        text = stringResource(destination.label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onBackground,
                    selectedTextColor = MaterialTheme.colorScheme.onBackground,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onBackground,
                    unselectedTextColor = MaterialTheme.colorScheme.onBackground,
                    disabledIconColor = Color.Unspecified,
                    disabledTextColor = Color.Unspecified
                )
            )
        }
    }
}
