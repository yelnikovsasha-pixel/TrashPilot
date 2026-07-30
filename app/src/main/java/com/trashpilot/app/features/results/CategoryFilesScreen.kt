package com.trashpilot.app.features.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.trashpilot.app.R
import com.trashpilot.app.core.storage.FileCategory
import com.trashpilot.app.core.storage.StorageScanResult
import com.trashpilot.app.core.storage.SocialMediaAnalyzer
import com.trashpilot.app.core.storage.formatBytes
import com.trashpilot.app.ui.components.TrashPilotFeatureCard
import com.trashpilot.app.ui.components.TrashPilotTopAppBar
import com.trashpilot.app.ui.theme.TrashPilotSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFilesScreen(result: StorageScanResult, category: FileCategory, onBack: () -> Unit) {
    val files = result.files.filter { it.category == category }.sortedFor(FileSortOption.SIZE)
    FilesListScreen(
        title = stringResource(category.improvedLabelResource()),
        files = files,
        emptyMessage = stringResource(R.string.results_category_empty),
        onBack = onBack
    )
}

@Composable
fun SocialMediaFilesScreen(result: StorageScanResult, onBack: () -> Unit) {
    val groups = remember(result) { SocialMediaAnalyzer.groups(result.files) }
    Scaffold(
        topBar = {
            TrashPilotTopAppBar(
                title = stringResource(R.string.results_social_media),
                onBack = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(TrashPilotSpacing.Screen),
            verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)
        ) {
            if (groups.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.results_no_social_media),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                groups.forEach { group ->
                    item(key = "summary-${group.applicationName}") {
                        TrashPilotFeatureCard(
                            title = group.applicationName,
                            body = if (group.files.size == 1) {
                                stringResource(R.string.results_one_file)
                            } else {
                                stringResource(
                                    R.string.results_file_count_value,
                                    group.files.size
                                )
                            },
                            icon = Icons.Outlined.Forum,
                            modifier = Modifier.fillMaxWidth(),
                            trailingContent = {
                                Text(
                                    text = formatBytes(group.totalBytes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        )
                    }
                    items(
                        items = group.files.sortedFor(FileSortOption.SIZE),
                        key = { "${group.applicationName}-${it.uri}" }
                    ) { file ->
                        ImprovedFileRow(file)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilesListScreen(
    title: String,
    files: List<com.trashpilot.app.core.storage.ScannedFile>,
    emptyMessage: String,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TrashPilotTopAppBar(
                title = title,
                onBack = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(TrashPilotSpacing.Screen),
            verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)
        ) {
            if (files.isEmpty()) {
                item {
                    Text(
                        emptyMessage,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(files, key = { it.uri.toString() }) { ImprovedFileRow(it) }
            }
        }
    }
}
