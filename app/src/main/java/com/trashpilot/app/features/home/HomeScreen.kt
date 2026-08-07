package com.trashpilot.app.features.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.trashpilot.app.R
import com.trashpilot.app.core.storage.StorageScanResult
import com.trashpilot.app.core.storage.formatBytes
import com.trashpilot.app.ui.components.TrashPilotAmbientMessage
import com.trashpilot.app.ui.components.TrashPilotBrandHeader
import com.trashpilot.app.ui.components.TrashPilotFeatureCard
import com.trashpilot.app.ui.components.TrashPilotHomeCard
import com.trashpilot.app.ui.components.TrashPilotScanButton
import com.trashpilot.app.ui.theme.TrashPilotColors
import com.trashpilot.app.ui.theme.TrashPilotHomeTokens
import com.trashpilot.app.ui.theme.TrashPilotRadii
import com.trashpilot.app.ui.theme.TrashPilotSpacing

@Composable
fun HomeScreen(
    onScan: () -> Unit,
    onOpenQuickClean: () -> Unit,
    onOpenTrashDna: () -> Unit,
    onOpenPrivacy: () -> Unit,
    latestScan: StorageScanResult? = null
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentPadding = PaddingValues(
            top = TrashPilotHomeTokens.ScreenTop,
            bottom = TrashPilotHomeTokens.ScreenBottom
        )
    ) {
        item { TrashPilotBrandHeader() }
        item {
            Spacer(Modifier.height(TrashPilotHomeTokens.HeaderToHeroSpace))
            ScanAction(onScan)
            Spacer(Modifier.height(TrashPilotHomeTokens.HeroToAmbientSpace))
            TrashPilotAmbientMessage(
                messages = listOf(
                    stringResource(R.string.ambient_message_clear_mind),
                    stringResource(R.string.ambient_message_better_habits),
                    stringResource(R.string.ambient_message_your_control)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TrashPilotHomeTokens.CardHorizontalPadding)
            )
            Spacer(Modifier.height(TrashPilotHomeTokens.AmbientToStorageSpace))
        }
        item {
            StorageCard(
                latestScan = latestScan,
                modifier = Modifier.padding(horizontal = TrashPilotHomeTokens.CardHorizontalPadding)
            )
            Spacer(Modifier.height(TrashPilotHomeTokens.FeatureCardGap))
        }
        item {
            HomeFeatureCard(
                title = stringResource(R.string.quick_clean_title),
                body = stringResource(R.string.home_quick_clean_body),
                icon = Icons.Outlined.CleaningServices,
                onClick = onOpenQuickClean
            )
        }
        item {
            HomeFeatureCard(
                title = stringResource(R.string.trash_dna_title),
                body = stringResource(R.string.home_trash_dna_body),
                icon = Icons.Outlined.AutoAwesome,
                onClick = onOpenTrashDna
            )
        }
        item {
            HomeFeatureCard(
                title = stringResource(R.string.privacy_monitor_title),
                body = stringResource(R.string.home_privacy_body),
                icon = Icons.Outlined.Security,
                onClick = onOpenPrivacy
            )
        }
    }
}

@Composable
private fun ScanAction(onScan: () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        TrashPilotScanButton(
            text = stringResource(R.string.scan_action),
            onClick = onScan
        )
    }
}

@Composable
private fun StorageCard(
    latestScan: StorageScanResult?,
    modifier: Modifier = Modifier
) {
    val progress = latestScan
        ?.takeIf { it.totalBytes > 0L }
        ?.let { (it.usedBytes.toFloat() / it.totalBytes.toFloat()).coerceIn(0f, 1f) }
    val percent = progress?.let { (it * 100f).toInt() }

    TrashPilotHomeCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StorageIcon()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_phone_storage),
                    color = TrashPilotColors.HomeInk,
                    style = TrashPilotHomeTokens.FeatureTitleStyle,
                    maxLines = 1
                )
                if (progress != null) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .width(TrashPilotHomeTokens.StorageProgressWidth)
                            .height(TrashPilotHomeTokens.StorageProgressHeight),
                        color = TrashPilotColors.HomeBlue,
                        trackColor = TrashPilotColors.HomeOutline
                    )
                } else {
                    Text(
                        text = stringResource(R.string.storage_not_scanned),
                        color = TrashPilotColors.HomeTextSecondary,
                        style = TrashPilotHomeTokens.FeatureBodyStyle,
                        maxLines = 1
                    )
                }
            }
            if (latestScan != null && percent != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = stringResource(R.string.home_storage_percent, percent),
                            color = Color.Black,
                            style = TrashPilotHomeTokens.FeatureTitleStyle
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.home_storage_used),
                            color = TrashPilotColors.HomeTextSecondary,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = stringResource(
                            R.string.home_storage_free_total,
                            formatBytes((latestScan.totalBytes - latestScan.usedBytes).coerceAtLeast(0L)),
                            formatBytes(latestScan.totalBytes)
                        ),
                        color = TrashPilotColors.HomeTextSecondary,
                        style = TrashPilotHomeTokens.StorageDetailStyle,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageIcon() {
    Box(
        modifier = Modifier.size(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Folder,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = TrashPilotColors.HomeBlue
        )
    }
}

@Composable
private fun HomeFeatureCard(
    title: String,
    body: String,
    icon: ImageVector,
    onClick: (() -> Unit)? = null
) {
    TrashPilotFeatureCard(
        title = title,
        body = body,
        icon = icon,
        modifier = Modifier
            .padding(horizontal = TrashPilotHomeTokens.CardHorizontalPadding),
        onClick = onClick
    )
    Spacer(Modifier.height(TrashPilotHomeTokens.FeatureCardGap))
}
