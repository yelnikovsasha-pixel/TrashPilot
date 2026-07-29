package com.trashpilot.app.features.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trashpilot.app.R
import com.trashpilot.app.core.storage.StorageScanResult
import com.trashpilot.app.core.storage.formatBytes

private data class HomeDestination(
    val label: Int,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun HomeScreen(
    onScan: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenSettings: () -> Unit,
    latestScan: StorageScanResult? = null
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            HomeBottomBar(
                onOpenPrivacy = onOpenPrivacy,
                onOpenReports = onOpenReports,
                onOpenSettings = onOpenSettings
            )
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentPadding = PaddingValues(
                start = 24.dp,
                top = 20.dp,
                end = 24.dp,
                bottom = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { BrandHeader() }
            item {
                Spacer(Modifier.height(20.dp))
                ScanAction(onScan = onScan)
                Spacer(Modifier.height(16.dp))
            }
            item {
                FeatureCard(
                    title = stringResource(R.string.storage_title),
                    subtitle = latestScan?.let {
                        stringResource(
                            R.string.storage_scanned_value,
                            formatBytes(it.usedBytes),
                            formatBytes(it.totalBytes)
                        )
                    } ?: stringResource(R.string.storage_not_scanned),
                    supportingText = latestScan?.let {
                        stringResource(
                            R.string.storage_free_value,
                            formatBytes(it.freeBytes)
                        )
                    } ?: stringResource(R.string.storage_future_state),
                    icon = Icons.Outlined.Storage
                )
            }
            item {
                FeatureCard(
                    title = stringResource(R.string.trash_dna_title),
                    subtitle = stringResource(R.string.trash_dna_subtitle),
                    icon = Icons.Outlined.AutoAwesome
                )
            }
            item {
                FeatureCard(
                    title = stringResource(R.string.privacy_monitor_title),
                    subtitle = stringResource(R.string.privacy_monitor_subtitle),
                    icon = Icons.Outlined.Security,
                    onClick = onOpenPrivacy
                )
            }
            item {
                FeatureCard(
                    title = stringResource(R.string.reports_title),
                    subtitle = stringResource(R.string.reports_subtitle),
                    icon = Icons.Outlined.Assessment,
                    onClick = onOpenReports
                )
            }
            item {
                Text(
                    text = stringResource(R.string.home_offline_note),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun BrandHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "TP",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Column {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ScanAction(onScan: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        ElevatedButton(
            onClick = onScan,
            modifier = Modifier.size(168.dp),
            shape = CircleShape,
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            elevation = ButtonDefaults.elevatedButtonElevation(
                defaultElevation = 8.dp,
                pressedElevation = 3.dp
            )
        ) {
            Text(
                text = stringResource(R.string.scan_action),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun FeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    supportingText: String? = null,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (supportingText == null) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    fontWeight = if (supportingText == null) FontWeight.Normal else FontWeight.Medium
                )
                supportingText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeBottomBar(
    onOpenPrivacy: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val destinations = listOf(
        HomeDestination(R.string.nav_home, Icons.Outlined.Home, {}),
        HomeDestination(R.string.nav_privacy, Icons.Outlined.Security, onOpenPrivacy),
        HomeDestination(R.string.nav_reports, Icons.Outlined.Assessment, onOpenReports),
        HomeDestination(R.string.nav_settings, Icons.Outlined.Settings, onOpenSettings)
    )

    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        destinations.forEachIndexed { index, destination ->
            NavigationBarItem(
                selected = index == 0,
                onClick = destination.onClick,
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = null
                    )
                },
                label = { Text(stringResource(destination.label)) }
            )
        }
    }
}
