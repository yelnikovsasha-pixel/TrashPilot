package com.trashpilot.app.features.results

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.trashpilot.app.R
import com.trashpilot.app.core.storage.FileCategory
import com.trashpilot.app.core.storage.ScannedFile
import com.trashpilot.app.core.storage.StorageScanResult
import com.trashpilot.app.core.storage.formatBytes
import com.trashpilot.app.ui.components.TrashPilotTopAppBar
import com.trashpilot.app.ui.components.TrashPilotCard
import com.trashpilot.app.ui.components.TrashPilotOutlinedButton
import com.trashpilot.app.ui.components.TrashPilotPrimaryButton
import com.trashpilot.app.ui.components.TrashPilotSectionHeader
import com.trashpilot.app.ui.theme.TrashPilotRadii
import com.trashpilot.app.ui.theme.TrashPilotSpacing
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    state: ResultsUiState,
    onBack: () -> Unit,
    onScanAgain: () -> Unit,
    onQuickClean: () -> Unit,
    onOpenCategory: (FileCategory) -> Unit
) {
    Scaffold(
        topBar = {
            TrashPilotTopAppBar(
                title = stringResource(R.string.results_screen_title),
                onBack = onBack
            )
        }
    ) { padding ->
        when (state) {
            ResultsUiState.Loading -> ResultsStateMessage(
                Modifier.padding(padding),
                stringResource(R.string.results_loading_title),
                stringResource(R.string.results_loading_body),
                loading = true
            )
            ResultsUiState.Empty -> ResultsStateMessage(
                Modifier.padding(padding),
                stringResource(R.string.results_empty_title),
                stringResource(R.string.results_missing),
                stringResource(R.string.results_scan_again),
                onScanAgain
            )
            is ResultsUiState.Error -> ResultsStateMessage(
                Modifier.padding(padding),
                stringResource(R.string.results_error_title),
                state.message ?: stringResource(R.string.results_error_body),
                stringResource(R.string.results_scan_again),
                onScanAgain
            )
            is ResultsUiState.Success -> SuccessContent(
                Modifier.padding(padding),
                state.result,
                onScanAgain,
                onQuickClean,
                onOpenCategory
            )
        }
    }
}

@Composable
private fun SuccessContent(
    modifier: Modifier,
    result: StorageScanResult,
    onScanAgain: () -> Unit,
    onQuickClean: () -> Unit,
    onOpenCategory: (FileCategory) -> Unit
) {
    var sort by remember { mutableStateOf(FileSortOption.SIZE) }
    val displayedFiles = remember(result.files, sort) { result.largestFiles.sortedFor(sort) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            TrashPilotSpacing.Screen,
            TrashPilotSpacing.Large,
            TrashPilotSpacing.Screen,
            TrashPilotSpacing.Screen
        ),
        verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Large)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Compact)) {
                Text(
                    result.selectedRootName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(R.string.results_files_scanned, result.scannedFileCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item { ImprovedStorageCard(result) }
        item {
            ImprovedSectionTitle(stringResource(R.string.results_categories))
            Spacer(Modifier.height(TrashPilotSpacing.MediumLarge))
            TrashPilotCard(
                modifier = Modifier.fillMaxWidth(),
                shape = TrashPilotRadii.CardShape,
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                FileCategory.entries.forEach { category ->
                    ImprovedCategoryRow(
                        category,
                        result.categoryBytes[category] ?: 0L,
                        { onOpenCategory(category) }
                    )
                }
            }
        }
        item {
            ImprovedSectionTitle(stringResource(R.string.results_largest_files))
            Spacer(Modifier.height(TrashPilotSpacing.Medium))
            Row(horizontalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Medium)) {
                FileSortOption.entries.forEach { option ->
                    FilterChip(
                        selected = sort == option,
                        onClick = { sort = option },
                        label = { Text(stringResource(option.labelResource())) }
                    )
                }
            }
        }
        if (displayedFiles.isEmpty()) {
            item {
                ImprovedListCard {
                    Text(
                        stringResource(R.string.results_no_files),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(displayedFiles, key = { it.uri.toString() }) { ImprovedFileRow(it) }
        }
        item {
            Text(
                stringResource(R.string.results_read_only_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            TrashPilotOutlinedButton(
                text = stringResource(R.string.results_quick_clean),
                onClick = onQuickClean,
                modifier = Modifier.fillMaxWidth(),
                shape = TrashPilotRadii.PillShape,
                fontWeight = FontWeight.SemiBold
            )
        }
        item {
            TrashPilotPrimaryButton(
                text = stringResource(R.string.results_scan_again),
                onClick = onScanAgain,
                modifier = Modifier.fillMaxWidth(),
                shape = TrashPilotRadii.PillShape,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ImprovedStorageCard(result: StorageScanResult) {
    TrashPilotCard(
        modifier = Modifier.fillMaxWidth(),
        shape = TrashPilotRadii.CardShape,
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = TrashPilotSpacing.Card,
                vertical = TrashPilotSpacing.CardDense
            ),
            verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.MediumLarge)
        ) {
            Text(
                stringResource(R.string.results_storage),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            ImprovedMetric(stringResource(R.string.results_total_storage), formatBytes(result.totalBytes))
            ImprovedMetric(stringResource(R.string.results_used_storage), formatBytes(result.usedBytes))
            ImprovedMetric(stringResource(R.string.results_free_storage), formatBytes(result.freeBytes))
        }
    }
}

@Composable
private fun ImprovedMetric(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ImprovedCategoryRow(
    category: FileCategory,
    size: Long,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = TrashPilotSpacing.CardDense,
                vertical = TrashPilotSpacing.MediumLarge
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(category.improvedLabelResource()),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            formatBytes(size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            stringResource(R.string.results_open_category),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun ImprovedFileRow(file: ScannedFile) {
    ImprovedListCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TrashPilotSpacing.HomeCard)
        ) {
            Icon(Icons.Outlined.Description, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(
                    file.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    file.modifiedDate(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                formatBytes(file.sizeBytes),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ImprovedListCard(content: @Composable () -> Unit) {
    TrashPilotCard(
        modifier = Modifier.fillMaxWidth(),
        shape = TrashPilotRadii.CompactCardShape,
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Box(
            Modifier.padding(
                horizontal = TrashPilotSpacing.CardDense,
                vertical = TrashPilotSpacing.Large
            )
        ) { content() }
    }
}

@Composable
private fun ResultsStateMessage(
    modifier: Modifier,
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    loading: Boolean = false
) {
    Column(
        modifier.fillMaxSize().padding(TrashPilotSpacing.Screen),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (loading) {
            CircularProgressIndicator()
            Spacer(Modifier.height(TrashPilotSpacing.Screen))
        }
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(TrashPilotSpacing.Medium))
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(TrashPilotSpacing.Screen))
            TrashPilotPrimaryButton(
                text = actionLabel,
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(),
                height = null
            )
        }
    }
}

@Composable
private fun ImprovedSectionTitle(text: String) {
    TrashPilotSectionHeader(text)
}

@Composable
private fun ScannedFile.modifiedDate(): String =
    if (lastModifiedMillis <= 0) stringResource(R.string.results_date_unknown)
    else DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(lastModifiedMillis))

internal fun FileCategory.improvedLabelResource(): Int = when (this) {
    FileCategory.IMAGES -> R.string.category_images
    FileCategory.VIDEOS -> R.string.category_videos
    FileCategory.AUDIO -> R.string.category_audio
    FileCategory.DOCUMENTS -> R.string.category_documents
    FileCategory.APK_FILES -> R.string.category_apk
    FileCategory.DOWNLOADS -> R.string.category_downloads
    FileCategory.OTHER -> R.string.category_other
}

private fun FileSortOption.labelResource(): Int = when (this) {
    FileSortOption.SIZE -> R.string.results_sort_size
    FileSortOption.NAME -> R.string.results_sort_name
    FileSortOption.DATE -> R.string.results_sort_date
}
