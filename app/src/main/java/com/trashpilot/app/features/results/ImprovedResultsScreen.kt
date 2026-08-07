package com.trashpilot.app.features.results

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Screenshot
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.trashpilot.app.R
import com.trashpilot.app.core.quickclean.DisposableCategory
import com.trashpilot.app.core.storage.FileCategory
import com.trashpilot.app.core.storage.ScannedFile
import com.trashpilot.app.core.storage.SocialMediaAnalyzer
import com.trashpilot.app.core.storage.StorageScanResult
import com.trashpilot.app.core.storage.formatBytes
import com.trashpilot.app.core.screenshots.isConfidentScreenshotPath
import com.trashpilot.app.ui.components.TrashPilotBrandHeader
import com.trashpilot.app.ui.components.TrashPilotFeatureCard
import com.trashpilot.app.ui.components.TrashPilotHomeCard
import com.trashpilot.app.ui.components.TrashPilotPrimaryButton
import com.trashpilot.app.ui.components.TrashPilotOutlinedButton
import com.trashpilot.app.ui.theme.TrashPilotColors
import com.trashpilot.app.ui.theme.TrashPilotHomeTokens
import com.trashpilot.app.ui.theme.TrashPilotRadii
import com.trashpilot.app.ui.theme.TrashPilotSpacing
import java.text.DateFormat
import java.util.Date

@Composable
fun ImprovedResultsScreen(
    state: ResultsUiState,
    onBack: () -> Unit,
    onScanAgain: () -> Unit,
    onQuickClean: (Set<String>) -> Unit,
    @Suppress("UNUSED_PARAMETER") onOpenCategory: (FileCategory) -> Unit,
    onOpenSocialMedia: () -> Unit,
    onOpenDuplicates: () -> Unit,
    onOpenLargeFiles: () -> Unit,
    onOpenHiddenFiles: () -> Unit,
    onOpenApkManager: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenEmptyFolders: () -> Unit,
    onOpenScreenshots: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        when (state) {
            ResultsUiState.Scanning -> ResultsStateMessage(
                title = stringResource(R.string.results_loading_title),
                body = stringResource(R.string.results_loading_body),
                onBack = onBack,
                loading = true
            )
            is ResultsUiState.NothingFound -> ResultsStateMessage(
                title = stringResource(R.string.results_nothing_found_title),
                body = stringResource(R.string.results_nothing_found_body),
                onBack = onBack,
                actionLabel = stringResource(R.string.screenshots_title),
                onAction = onOpenScreenshots,
                secondaryAction = true
            )
            is ResultsUiState.Error -> ResultsStateMessage(
                title = stringResource(R.string.results_error_title),
                body = state.message ?: stringResource(R.string.results_error_body),
                onBack = onBack,
                actionLabel = stringResource(R.string.results_scan_again),
                onAction = onScanAgain
            )
            is ResultsUiState.Results -> ResultsContent(
                result = state.result,
                onBack = onBack,
                onQuickClean = onQuickClean,
                onOpenSocialMedia = onOpenSocialMedia,
                onOpenDuplicates = onOpenDuplicates,
                onOpenLargeFiles = onOpenLargeFiles,
                onOpenHiddenFiles = onOpenHiddenFiles,
                onOpenApkManager = onOpenApkManager,
                onOpenDownloads = onOpenDownloads,
                onOpenEmptyFolders = onOpenEmptyFolders,
                onOpenScreenshots = onOpenScreenshots
            )
        }
    }
}

@Composable
private fun ResultsContent(
    result: StorageScanResult,
    onBack: () -> Unit,
    onQuickClean: (Set<String>) -> Unit,
    onOpenSocialMedia: () -> Unit,
    onOpenDuplicates: () -> Unit,
    onOpenLargeFiles: () -> Unit,
    onOpenHiddenFiles: () -> Unit,
    onOpenApkManager: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenEmptyFolders: () -> Unit,
    onOpenScreenshots: () -> Unit
) {
    val overview = remember(result) { result.toResultsOverview() }
    val listState = remember(result) { LazyListState() }
    var selectedUris by remember(result) { mutableStateOf(emptySet<String>()) }
    fun toggleCandidates(candidates: List<com.trashpilot.app.core.quickclean.DisposableCandidate>) {
        val uris = candidates.mapTo(mutableSetOf()) { it.uri }
        selectedUris = if (uris.isNotEmpty() && uris.all(selectedUris::contains)) {
            selectedUris - uris
        } else {
            selectedUris + uris
        }
    }
    val duplicateValue = stringResource(R.string.results_run_duplicate_analysis)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(
            top = TrashPilotHomeTokens.ScreenTop,
            bottom = TrashPilotSpacing.Screen
        )
    ) {
        item { TrashPilotBrandHeader(onBack = onBack) }
        item {
            Spacer(Modifier.height(TrashPilotSpacing.Standard))
            ScanSummaryCard(
                rootName = result.selectedRootName,
                overview = overview,
                duplicateValue = duplicateValue,
                modifier = Modifier.padding(
                    horizontal = TrashPilotHomeTokens.CardHorizontalPadding
                )
            )
            Spacer(Modifier.height(TrashPilotSpacing.Standard))
            TrashPilotPrimaryButton(
                text = stringResource(R.string.results_clean_selected),
                onClick = { onQuickClean(selectedUris) },
                enabled = selectedUris.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TrashPilotHomeTokens.CardHorizontalPadding),
                shape = TrashPilotRadii.ControlShape,
                fontWeight = FontWeight.SemiBold,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrashPilotColors.HomeBlue,
                    contentColor = Color.White
                )
            )
            Spacer(Modifier.height(TrashPilotSpacing.Standard))
        }
        item {
            ResultCategoryCard(
                icon = Icons.Outlined.Screenshot,
                title = stringResource(R.string.screenshots_title),
                subtitle = stringResource(R.string.results_local_category_subtitle),
                value = stringResource(R.string.results_count_and_size, fileCountText(overview.screenshotCount), formatBytes(overview.screenshotBytes)),
                onClick = onOpenScreenshots
            )
        }
        item {
            ResultCategoryCard(
                icon = Icons.Outlined.Download,
                title = stringResource(R.string.downloads_cleaner_title),
                subtitle = stringResource(R.string.results_local_category_subtitle),
                value = fileCountText(overview.downloadFileCount),
                onClick = onOpenDownloads
            )
        }
        item {
            ResultCategoryCard(
                icon = Icons.Outlined.CleaningServices,
                title = stringResource(R.string.quick_clean_cache),
                subtitle = stringResource(R.string.results_local_category_subtitle),
                value = formatBytes(overview.cacheBytes),
                selected = overview.cacheCandidates.isNotEmpty() &&
                    overview.cacheCandidates.all { it.uri in selectedUris },
                selectionEnabled = overview.cacheCandidates.isNotEmpty(),
                showChevron = false,
                onClick = if (overview.cacheCandidates.isEmpty()) {
                    null
                } else {
                    { toggleCandidates(overview.cacheCandidates) }
                }
            )
        }
        item {
            ResultCategoryCard(
                icon = Icons.Outlined.Description,
                title = stringResource(R.string.results_large_files),
                subtitle = stringResource(R.string.results_local_category_subtitle),
                value = fileCountText(overview.largeFileCount),
                onClick = onOpenLargeFiles
            )
        }
        item {
            ResultCategoryCard(
                icon = Icons.Outlined.VisibilityOff,
                title = stringResource(R.string.results_hidden_files),
                subtitle = stringResource(R.string.results_local_category_subtitle),
                value = formatBytes(overview.hiddenBytes),
                onClick = onOpenHiddenFiles
            )
        }
        item {
            ResultCategoryCard(
                icon = Icons.Outlined.Android,
                title = stringResource(R.string.apk_manager_title),
                subtitle = stringResource(R.string.results_local_category_subtitle),
                value = fileCountText(overview.apkFileCount),
                onClick = onOpenApkManager
            )
        }
        item {
            ResultCategoryCard(
                icon = Icons.Outlined.ContentCopy,
                title = stringResource(R.string.results_duplicates),
                subtitle = stringResource(R.string.results_tap_duplicate_analysis),
                value = duplicateValue,
                onClick = onOpenDuplicates
            )
        }
        item {
            ResultCategoryCard(
                icon = Icons.Outlined.Forum,
                title = stringResource(R.string.results_social_media),
                subtitle = if (overview.socialFiles.isEmpty()) {
                    stringResource(R.string.results_no_social_media)
                } else {
                    stringResource(R.string.results_social_media_description)
                },
                value = if (overview.socialFiles.isEmpty()) {
                    ""
                } else {
                    stringResource(
                        R.string.results_count_and_size,
                        fileCountText(overview.socialFiles.size),
                        formatBytes(overview.socialBytes)
                    )
                },
                titleMaxLines = 2,
                bodyMaxLines = 4,
                showChevron = overview.socialFiles.isNotEmpty(),
                onClick = onOpenSocialMedia.takeIf { overview.socialFiles.isNotEmpty() }
            )
        }
        item {
            ResultCategoryCard(
                icon = Icons.Outlined.FolderOff,
                title = stringResource(R.string.quick_clean_empty_folders),
                subtitle = stringResource(R.string.results_local_category_subtitle),
                value = folderCountText(overview.emptyFolderCount),
                selected = overview.emptyFolderCandidates.isNotEmpty() &&
                    overview.emptyFolderCandidates.all { it.uri in selectedUris },
                selectionEnabled = false,
                showChevron = true,
                onClick = onOpenEmptyFolders
            )
        }
        item {
            Text(
                text = stringResource(R.string.results_local_analysis_reminder),
                modifier = Modifier.padding(
                    start = TrashPilotHomeTokens.CardHorizontalPadding,
                    top = TrashPilotSpacing.Standard,
                    end = TrashPilotHomeTokens.CardHorizontalPadding
                ),
                color = TrashPilotColors.HomeTextSecondary,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ScanSummaryCard(
    rootName: String,
    overview: ResultsOverview,
    duplicateValue: String,
    modifier: Modifier = Modifier
) {
    TrashPilotHomeCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TrashPilotSpacing.Large),
            verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Medium)
        ) {
            Text(
                text = rootName,
                color = TrashPilotColors.HomeInk,
                style = TrashPilotHomeTokens.FeatureTitleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            SummaryMetric(
                stringResource(R.string.results_total_scanned_storage),
                formatBytes(overview.scannedBytes)
            )
            SummaryMetric(
                stringResource(R.string.quick_clean_cache),
                formatBytes(overview.cacheBytes)
            )
            SummaryMetric(
                stringResource(R.string.results_large_files),
                fileCountText(overview.largeFileCount)
            )
            SummaryMetric(
                stringResource(R.string.results_hidden_files),
                formatBytes(overview.hiddenBytes)
            )
            SummaryMetric(
                stringResource(R.string.results_duplicates),
                duplicateValue
            )
            SummaryMetric(
                stringResource(R.string.quick_clean_empty_folders),
                folderCountText(overview.emptyFolderCount)
            )
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String) {
    val largeText = LocalDensity.current.fontScale >= 1.5f
    if (largeText) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Compact)
        ) {
            Text(
                text = label,
                color = TrashPilotColors.HomeTextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = value,
                modifier = Modifier.fillMaxWidth(),
                color = TrashPilotColors.HomeInk,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = TrashPilotColors.HomeTextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = value,
                color = TrashPilotColors.HomeInk,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ResultCategoryCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: String,
    selected: Boolean = false,
    selectionEnabled: Boolean = false,
    titleMaxLines: Int = 2,
    bodyMaxLines: Int = 3,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    TrashPilotFeatureCard(
        title = title,
        body = subtitle,
        icon = icon,
        modifier = Modifier.padding(
            horizontal = TrashPilotHomeTokens.CardHorizontalPadding
        ),
        onClick = onClick,
        titleMaxLines = titleMaxLines,
        bodyMaxLines = bodyMaxLines,
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Compact)
            ) {
                if (value.isNotEmpty()) {
                    Text(
                        text = value,
                        color = TrashPilotColors.HomeTextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (selectionEnabled) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { onClick?.invoke() }
                    )
                } else if (showChevron) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(TrashPilotSpacing.Screen),
                        tint = TrashPilotColors.HomeTextSecondary
                    )
                }
            }
        }
    )
    Spacer(Modifier.height(TrashPilotHomeTokens.FeatureCardGap))
}

@Composable
private fun ResultsStateMessage(
    title: String,
    body: String,
    onBack: () -> Unit,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    loading: Boolean = false,
    secondaryAction: Boolean = false
) {
    Column(Modifier.fillMaxSize()) {
        TrashPilotBrandHeader(
            modifier = Modifier.padding(top = TrashPilotHomeTokens.ScreenTop),
            onBack = onBack
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(TrashPilotSpacing.Screen),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (loading) {
                CircularProgressIndicator(color = TrashPilotColors.HomeBlue)
                Spacer(Modifier.height(TrashPilotSpacing.Screen))
            }
            Text(
                text = title,
                color = TrashPilotColors.HomeInk,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(TrashPilotSpacing.Medium))
            Text(
                text = body,
                color = TrashPilotColors.HomeTextSecondary,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(TrashPilotSpacing.Screen))
                if (secondaryAction) {
                    TrashPilotOutlinedButton(
                        text = actionLabel,
                        onClick = onAction,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    TrashPilotPrimaryButton(
                        text = actionLabel,
                        onClick = onAction,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TrashPilotColors.HomeBlue,
                            contentColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

internal data class ResultsOverview(
    val scannedBytes: Long,
    val cacheBytes: Long,
    val largeFileCount: Int,
    val hiddenBytes: Long,
    val emptyFolderCount: Int,
    val cacheCandidates: List<com.trashpilot.app.core.quickclean.DisposableCandidate>,
    val emptyFolderCandidates: List<com.trashpilot.app.core.quickclean.DisposableCandidate>,
    val socialFiles: List<ScannedFile>,
    val socialBytes: Long,
    val apkFileCount: Int,
    val downloadFileCount: Int,
    val screenshotCount: Int,
    val screenshotBytes: Long
)

internal fun StorageScanResult.toResultsOverview(): ResultsOverview {
    val cacheCandidates = disposableCandidates.filter {
        it.category == DisposableCategory.APP_CACHE
    }
    val emptyFolderCandidates = disposableCandidates.filter {
        it.category == DisposableCategory.EMPTY_FOLDERS
    }
    val socialFiles = SocialMediaAnalyzer.filesInSupportedFolders(files)
    return ResultsOverview(
        scannedBytes = files.sumOf(ScannedFile::sizeBytes),
        cacheBytes = cacheCandidates.sumOf { it.sizeBytes },
        largeFileCount = files.count { it.sizeBytes >= LARGE_FILE_MIN_BYTES },
        hiddenBytes = files
            .filter { file ->
                file.relativePath.replace('\\', '/').split('/').any { it.startsWith(".") }
            }
            .sumOf(ScannedFile::sizeBytes),
        emptyFolderCount = emptyFolderCandidates.size,
        cacheCandidates = cacheCandidates,
        emptyFolderCandidates = emptyFolderCandidates,
        socialFiles = socialFiles,
        socialBytes = socialFiles.sumOf(ScannedFile::sizeBytes),
        apkFileCount = files.count { it.name.endsWith(".apk", ignoreCase = true) },
        downloadFileCount = files.count { file -> file.category == FileCategory.DOWNLOADS },
        screenshotCount = files.count(::isScreenshotFile),
        screenshotBytes = files.filter(::isScreenshotFile).sumOf(ScannedFile::sizeBytes)
    )
}

private fun isScreenshotFile(file: ScannedFile): Boolean = isConfidentScreenshotPath(
    file.relativePath,
    file.relativePath.replace('\\', '/').substringBeforeLast('/', "").substringAfterLast('/'),
    file.name
)

private const val LARGE_FILE_MIN_BYTES = 100L * 1024L * 1024L


@Composable
private fun fileCountText(count: Int): String =
    if (count == 1) {
        stringResource(R.string.results_one_file)
    } else {
        stringResource(R.string.results_file_count_value, count)
    }

@Composable
private fun folderCountText(count: Int): String =
    if (count == 1) {
        stringResource(R.string.results_one_folder)
    } else {
        stringResource(R.string.results_folder_count_value, count)
    }

@Composable
internal fun ImprovedFileRow(file: ScannedFile) {
    TrashPilotFeatureCard(
        title = file.name,
        body = file.modifiedDate(),
        icon = Icons.Outlined.Description,
        modifier = Modifier.fillMaxWidth(),
        trailingContent = {
            Text(
                formatBytes(file.sizeBytes),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    )
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
