package com.trashpilot.app.features.results

import androidx.compose.foundation.background
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.trashpilot.app.core.navigation.ReviewGroup
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
import com.trashpilot.app.ui.components.TrashPilotSectionHeader
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
    onOpenCache: () -> Unit,
    onOpenSocialMedia: () -> Unit,
    onOpenDuplicates: () -> Unit,
    onOpenLargeFiles: () -> Unit,
    onOpenHiddenFiles: () -> Unit,
    onOpenApkManager: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenEmptyFolders: () -> Unit,
    onOpenScreenshots: () -> Unit,
    onOpenPhotoQuality: () -> Unit
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
            is ResultsUiState.NothingFound -> ResultsContent(
                result = state.result,
                onBack = onBack,
                onQuickClean = onQuickClean,
                onOpenCache = onOpenCache,
                onOpenSocialMedia = onOpenSocialMedia,
                onOpenDuplicates = onOpenDuplicates,
                onOpenLargeFiles = onOpenLargeFiles,
                onOpenHiddenFiles = onOpenHiddenFiles,
                onOpenApkManager = onOpenApkManager,
                onOpenDownloads = onOpenDownloads,
                onOpenEmptyFolders = onOpenEmptyFolders,
                onOpenScreenshots = onOpenScreenshots,
                onOpenPhotoQuality = onOpenPhotoQuality
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
                onOpenCache = onOpenCache,
                onOpenSocialMedia = onOpenSocialMedia,
                onOpenDuplicates = onOpenDuplicates,
                onOpenLargeFiles = onOpenLargeFiles,
                onOpenHiddenFiles = onOpenHiddenFiles,
                onOpenApkManager = onOpenApkManager,
                onOpenDownloads = onOpenDownloads,
                onOpenEmptyFolders = onOpenEmptyFolders,
                onOpenScreenshots = onOpenScreenshots,
                onOpenPhotoQuality = onOpenPhotoQuality
            )
        }
    }
}

@Composable
private fun ResultsContent(
    result: StorageScanResult,
    onBack: () -> Unit,
    onQuickClean: (Set<String>) -> Unit,
    onOpenCache: () -> Unit,
    onOpenSocialMedia: () -> Unit,
    onOpenDuplicates: () -> Unit,
    onOpenLargeFiles: () -> Unit,
    onOpenHiddenFiles: () -> Unit,
    onOpenApkManager: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenEmptyFolders: () -> Unit,
    onOpenScreenshots: () -> Unit,
    onOpenPhotoQuality: () -> Unit
) {
    val overview = remember(result) { result.toResultsOverview() }
    var group by rememberSaveable(result) { mutableStateOf<ReviewGroup?>(null) }
    BackHandler(enabled = group != null) { group = null }
    if (group != null) {
        ResultsGroupPage(
            group = group!!,
            result = result,
            overview = overview,
            onBack = { group = null },
            onOpenCache = onOpenCache,
            onOpenSocialMedia = onOpenSocialMedia,
            onOpenDuplicates = onOpenDuplicates,
            onOpenLargeFiles = onOpenLargeFiles,
            onOpenHiddenFiles = onOpenHiddenFiles,
            onOpenApkManager = onOpenApkManager,
            onOpenDownloads = onOpenDownloads,
            onOpenEmptyFolders = onOpenEmptyFolders,
            onOpenScreenshots = onOpenScreenshots,
            onOpenPhotoQuality = onOpenPhotoQuality
        )
        return
    }
    ResultsHub(
        result = result,
        overview = overview,
        onBack = onBack,
        onQuickClean = { onQuickClean(emptySet()) },
        onOpenGroup = { group = it }
    )
}

@Composable
private fun ResultsHub(
    result: StorageScanResult,
    overview: ResultsOverview,
    onBack: () -> Unit,
    onQuickClean: () -> Unit,
    onOpenGroup: (ReviewGroup) -> Unit
) {
    val listState = remember(result) { LazyListState() }
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
                scannedFileCount = result.scannedFileCount,
                modifier = Modifier.padding(
                    horizontal = TrashPilotHomeTokens.CardHorizontalPadding
                )
            )
            Spacer(Modifier.height(TrashPilotSpacing.Standard))
            if (result.disposableCandidates.isNotEmpty()) {
                TrashPilotPrimaryButton(
                    text = stringResource(R.string.results_review_quick_clean),
                    onClick = onQuickClean,
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
        }
        item {
            TrashPilotSectionHeader(
                text = stringResource(R.string.results_review_categories),
                modifier = Modifier.padding(
                    start = TrashPilotHomeTokens.CardHorizontalPadding,
                    end = TrashPilotHomeTokens.CardHorizontalPadding,
                    bottom = TrashPilotSpacing.Medium
                )
            )
        }
        item {
            ResultCategoryCard(
                icon = Icons.Outlined.Android,
                title = stringResource(R.string.results_group_apps),
                subtitle = stringResource(R.string.results_group_apps_body),
                value = "",
                onClick = { onOpenGroup(ReviewGroup.APPS) }
            )
        }
        item {
            ResultCategoryCard(
                icon = Icons.Outlined.PhotoLibrary,
                title = stringResource(R.string.results_group_photos),
                subtitle = stringResource(R.string.results_group_photos_body),
                value = "",
                onClick = { onOpenGroup(ReviewGroup.PHOTOS) }
            )
        }
        item {
            ResultCategoryCard(
                icon = Icons.Outlined.Description,
                title = stringResource(R.string.results_group_files),
                subtitle = stringResource(R.string.results_group_files_body),
                value = "",
                onClick = { onOpenGroup(ReviewGroup.FILES) }
            )
        }
        item { ResultsLocalReminder() }
    }
}

@Composable
private fun ResultsGroupPage(
    group: ReviewGroup,
    result: StorageScanResult,
    overview: ResultsOverview,
    onBack: () -> Unit,
    onOpenCache: () -> Unit,
    onOpenSocialMedia: () -> Unit,
    onOpenDuplicates: () -> Unit,
    onOpenLargeFiles: () -> Unit,
    onOpenHiddenFiles: () -> Unit,
    onOpenApkManager: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenEmptyFolders: () -> Unit,
    onOpenScreenshots: () -> Unit,
    onOpenPhotoQuality: () -> Unit
) {
    val deeperReview = stringResource(R.string.results_open_review)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = TrashPilotHomeTokens.ScreenTop,
            bottom = TrashPilotSpacing.Screen
        )
    ) {
        item { TrashPilotBrandHeader(onBack = onBack) }
        item {
            TrashPilotSectionHeader(
                text = stringResource(group.titleResource()),
                modifier = Modifier.padding(
                    start = TrashPilotHomeTokens.CardHorizontalPadding,
                    top = TrashPilotSpacing.Standard,
                    end = TrashPilotHomeTokens.CardHorizontalPadding,
                    bottom = TrashPilotSpacing.Medium
                )
            )
            Text(
                text = stringResource(group.bodyResource()),
                modifier = Modifier.padding(
                    start = TrashPilotHomeTokens.CardHorizontalPadding,
                    end = TrashPilotHomeTokens.CardHorizontalPadding,
                    bottom = TrashPilotSpacing.Standard
                ),
                color = TrashPilotColors.HomeTextSecondary
            )
        }
        if (group == ReviewGroup.APPS) {
            item {
                ResultCategoryCard(
                    Icons.Outlined.CleaningServices,
                    stringResource(R.string.results_label_app_cache),
                    stringResource(R.string.results_app_cache_body),
                    deeperReview,
                    onClick = onOpenCache
                )
            }
            item {
                ResultCategoryCard(
                    Icons.Outlined.Forum,
                    stringResource(R.string.results_label_social_media),
                    stringResource(R.string.results_social_media_description),
                    overview.socialFiles.takeIf { it.isNotEmpty() }?.let {
                        stringResource(R.string.results_count_and_size, fileCountText(it.size), formatBytes(overview.socialBytes))
                    } ?: deeperReview,
                    onClick = onOpenSocialMedia
                )
            }
        }
        if (group == ReviewGroup.PHOTOS) {
            item {
                ResultCategoryCard(
                    Icons.Outlined.Screenshot,
                    stringResource(R.string.results_label_screenshots),
                    stringResource(R.string.results_screenshots_body),
                    if (overview.screenshotCount > 0) stringResource(
                        R.string.results_count_and_size,
                        fileCountText(overview.screenshotCount),
                        formatBytes(overview.screenshotBytes)
                    ) else deeperReview,
                    onClick = onOpenScreenshots
                )
            }
            item {
                ResultCategoryCard(
                    Icons.Outlined.ContentCopy,
                    stringResource(R.string.results_label_duplicates),
                    stringResource(R.string.results_tap_duplicate_analysis),
                    stringResource(R.string.results_run_duplicate_analysis),
                    onClick = onOpenDuplicates
                )
            }
            item {
                ResultCategoryCard(
                    Icons.Outlined.PhotoLibrary,
                    stringResource(R.string.results_label_photo_review),
                    stringResource(R.string.photo_quality_results_subtitle),
                    result.files.count { it.category == FileCategory.IMAGES }.takeIf { it > 0 }
                        ?.let { fileCountText(it) } ?: deeperReview,
                    onClick = onOpenPhotoQuality
                )
            }
        }
        if (group == ReviewGroup.FILES) {
            item {
                ResultCategoryCard(Icons.Outlined.Description, stringResource(R.string.results_label_large_files), stringResource(R.string.results_local_category_subtitle), overview.largeFileCount.takeIf { it > 0 }?.let { fileCountText(it) } ?: deeperReview, onClick = onOpenLargeFiles)
            }
            item {
                ResultCategoryCard(Icons.Outlined.Download, stringResource(R.string.results_label_downloads), stringResource(R.string.results_local_category_subtitle), overview.downloadFileCount.takeIf { it > 0 }?.let { fileCountText(it) } ?: deeperReview, onClick = onOpenDownloads)
            }
            item {
                ResultCategoryCard(Icons.Outlined.Android, stringResource(R.string.results_label_apk_installers), stringResource(R.string.results_local_category_subtitle), overview.apkFileCount.takeIf { it > 0 }?.let { fileCountText(it) } ?: deeperReview, onClick = onOpenApkManager)
            }
            item {
                ResultCategoryCard(Icons.Outlined.VisibilityOff, stringResource(R.string.results_label_hidden_files), stringResource(R.string.results_local_category_subtitle), overview.hiddenBytes.takeIf { it > 0 }?.let(::formatBytes) ?: deeperReview, onClick = onOpenHiddenFiles)
            }
            item {
                ResultCategoryCard(Icons.Outlined.FolderOff, stringResource(R.string.results_label_empty_folders), stringResource(R.string.results_empty_folders_body), overview.emptyFolderCount.takeIf { it > 0 }?.let { folderCountText(it) } ?: deeperReview, onClick = onOpenEmptyFolders)
            }
        }
        item { ResultsLocalReminder() }
    }
}

@Composable
private fun ResultsLocalReminder() {
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

private fun ReviewGroup.titleResource(): Int = when (this) {
    ReviewGroup.APPS -> R.string.results_group_apps
    ReviewGroup.PHOTOS -> R.string.results_group_photos
    ReviewGroup.FILES -> R.string.results_group_files
}

private fun ReviewGroup.bodyResource(): Int = when (this) {
    ReviewGroup.APPS -> R.string.results_group_apps_body
    ReviewGroup.PHOTOS -> R.string.results_group_photos_body
    ReviewGroup.FILES -> R.string.results_group_files_body
}
@Composable
private fun ScanSummaryCard(
    rootName: String,
    overview: ResultsOverview,
    scannedFileCount: Int,
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
                stringResource(R.string.results_files_found),
                fileCountText(scannedFileCount)
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
    titleMaxLines: Int = 2,
    bodyMaxLines: Int = 3,
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
                if (onClick != null) {
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
