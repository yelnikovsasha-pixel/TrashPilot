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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.trashpilot.app.R
import com.trashpilot.app.core.storage.StorageScanResult
import com.trashpilot.app.core.storage.formatBytes
import com.trashpilot.app.ui.components.TrashPilotCard
import com.trashpilot.app.ui.components.TrashPilotIconContainer
import com.trashpilot.app.ui.components.TrashPilotScanButton
import com.trashpilot.app.ui.theme.TrashPilotComponentSizes
import com.trashpilot.app.ui.theme.TrashPilotDimensions
import com.trashpilot.app.ui.theme.TrashPilotHomeTokens
import com.trashpilot.app.ui.theme.TrashPilotRadii
import com.trashpilot.app.ui.theme.TrashPilotSpacing

@Composable
fun HomeScreen(
    onScan: () -> Unit,
    onOpenQuickClean: () -> Unit,
    onOpenTrashDna: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenReports: () -> Unit,
    latestScan: StorageScanResult? = null
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = TrashPilotDimensions.ScreenPadding,
            top = TrashPilotDimensions.ContentTopPadding,
            end = TrashPilotDimensions.ScreenPadding,
            bottom = TrashPilotSpacing.Large
        ),
        verticalArrangement = Arrangement.Top
    ) {
        item { BrandHeader() }
        item {
            Spacer(Modifier.height(TrashPilotHomeTokens.HeaderToHeroSpace))
            ScanAction(onScan = onScan)
            Spacer(Modifier.height(TrashPilotHomeTokens.HeroToStorageSpace))
        }
        item { StorageCard(latestScan = latestScan) }
        item {
            HomeSection(title = stringResource(R.string.home_quick_actions)) {
                QuickActions(
                    onOpenQuickClean = onOpenQuickClean,
                    onOpenTrashDna = onOpenTrashDna,
                    onOpenPrivacy = onOpenPrivacy,
                    onOpenReports = onOpenReports
                )
            }
        }
        item {
            HomeSection(title = stringResource(R.string.home_trust_title)) {
                TrustCard()
            }
        }
    }
}

@Composable
private fun BrandHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TrashPilotSpacing.HomeCard)
    ) {
        Surface(
            modifier = Modifier.size(TrashPilotComponentSizes.BrandMark),
            shape = TrashPilotRadii.BrandShape,
            color = MaterialTheme.colorScheme.primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.brand_monogram),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Hairline)
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
            Text(
                text = stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
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
        TrashPilotScanButton(
            text = stringResource(R.string.scan_action),
            onClick = onScan
        )
    }
}

@Composable
private fun StorageCard(latestScan: StorageScanResult?) {
    val storageProgress = latestScan
        ?.takeIf { it.totalBytes > 0L }
        ?.let { (it.usedBytes.toFloat() / it.totalBytes.toFloat()).coerceIn(0f, 1f) }

    TrashPilotCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(TrashPilotHomeTokens.StorageCardHeight),
        shape = TrashPilotDimensions.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier.padding(TrashPilotDimensions.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TrashPilotSpacing.HomeCard)
        ) {
            TrashPilotIconContainer(icon = Icons.Outlined.Folder)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.XSmall)
            ) {
                Text(
                    text = stringResource(R.string.storage_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = latestScan?.let {
                        stringResource(
                            R.string.storage_scanned_value,
                            formatBytes(it.usedBytes),
                            formatBytes(it.totalBytes)
                        )
                    } ?: stringResource(R.string.storage_not_scanned),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                storageProgress?.let { progress ->
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = TrashPilotHomeTokens.StorageProgressTopSpace)
                            .height(TrashPilotHomeTokens.StorageProgressHeight),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeSection(
    title: String,
    content: @Composable () -> Unit
) {
    Spacer(Modifier.height(TrashPilotHomeTokens.SectionSpace))
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(Modifier.height(TrashPilotHomeTokens.SectionTitleToContentSpace))
    content()
}

@Composable
private fun QuickActions(
    onOpenQuickClean: () -> Unit,
    onOpenTrashDna: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenReports: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(TrashPilotHomeTokens.QuickActionGap)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TrashPilotHomeTokens.QuickActionGap)
        ) {
            QuickActionCard(
                title = stringResource(R.string.quick_clean_title),
                icon = Icons.Outlined.CleaningServices,
                onClick = onOpenQuickClean,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = stringResource(R.string.trash_dna_title),
                icon = Icons.Outlined.AutoAwesome,
                onClick = onOpenTrashDna,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TrashPilotHomeTokens.QuickActionGap)
        ) {
            QuickActionCard(
                title = stringResource(R.string.privacy_monitor_title),
                icon = Icons.Outlined.Security,
                onClick = onOpenPrivacy,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = stringResource(R.string.reports_title),
                icon = Icons.Outlined.Assessment,
                onClick = onOpenReports,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TrashPilotCard(
        modifier = modifier
            .height(TrashPilotHomeTokens.QuickActionHeight)
            .clickable(
                onClick = onClick,
                role = Role.Button,
                onClickLabel = title
            ),
        shape = TrashPilotRadii.CompactCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(TrashPilotSpacing.Standard),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TrashPilotSpacing.MediumLarge)
        ) {
            TrashPilotIconContainer(
                icon = icon,
                containerSize = TrashPilotHomeTokens.QuickActionIconContainer,
                iconSize = TrashPilotHomeTokens.QuickActionIcon,
                shape = TrashPilotRadii.SmallShape
            )
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TrustCard() {
    TrashPilotCard(
        modifier = Modifier.fillMaxWidth(),
        shape = TrashPilotRadii.CompactCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TrashPilotHomeTokens.TrustCardPadding),
            horizontalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Medium),
            verticalAlignment = Alignment.Top
        ) {
            TrustItem(
                icon = Icons.Outlined.PhoneAndroid,
                text = stringResource(R.string.home_trust_local),
                modifier = Modifier.weight(1f)
            )
            TrustItem(
                icon = Icons.Outlined.TouchApp,
                text = stringResource(R.string.home_trust_control),
                modifier = Modifier.weight(1f)
            )
            TrustItem(
                icon = Icons.Outlined.DeleteOutline,
                text = stringResource(R.string.home_trust_manual),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TrustItem(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TrashPilotHomeTokens.TrustItemGap)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(TrashPilotHomeTokens.TrustIcon),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3
        )
    }
}
