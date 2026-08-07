package com.trashpilot.app.features.duplicates

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.trashpilot.app.R
import com.trashpilot.app.core.storage.*
import com.trashpilot.app.ui.components.*
import com.trashpilot.app.ui.theme.TrashPilotSpacing
import com.trashpilot.app.ui.theme.TrashPilotComponentSizes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

@Composable
fun DuplicateScannerScreen(
    scanResult: StorageScanResult,
    onBack: () -> Unit,
    onScanAgain: () -> Unit,
    onCleaningComplete: (StorageScanResult, DuplicateCleaningReport) -> Unit
) {
    val context = LocalContext.current
    val analyzer = remember(context) { DuplicateAnalyzer(context.contentResolver) }
    val cleaner = remember(context) { DuplicateCleaner(context.contentResolver) }
    val scope = rememberCoroutineScope()
    var runId by rememberSaveable { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<DuplicateState>(DuplicateState.Scanning(DuplicateScanProgress(0, 0))) }
    var selectedUris by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var expandedUris by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var confirm by remember { mutableStateOf(false) }

    LaunchedEffect(scanResult, runId) {
        state = DuplicateState.Scanning(DuplicateScanProgress(0, 0))
        state = try {
            val analysis = analyzer.analyze(scanResult.files) { state = DuplicateState.Scanning(it) }
            selectedUris = analysis.initialSelection
            DuplicateState.Ready(analysis)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            DuplicateState.Error(failure is SecurityException)
        }
    }

    Scaffold(topBar = { TrashPilotTopAppBar(stringResource(R.string.results_label_duplicates), onBack = onBack) }) { padding ->
        when (val current = state) {
            is DuplicateState.Scanning -> DuplicateLoading(current.progress, Modifier.padding(padding))
            is DuplicateState.Error -> TrashPilotErrorState(
                title = stringResource(if (current.permissionDenied) R.string.duplicate_permission_title else R.string.duplicate_storage_error_title),
                body = stringResource(if (current.permissionDenied) R.string.duplicate_permission_body else R.string.duplicate_storage_error_body),
                actionText = stringResource(R.string.duplicate_scan_again), onAction = onScanAgain,
                modifier = Modifier.fillMaxSize().padding(padding).padding(TrashPilotSpacing.Screen)
            )
            is DuplicateState.Ready -> if (current.analysis.groups.isEmpty()) {
                TrashPilotEmptyState(
                    title = stringResource(R.string.duplicate_empty_title),
                    body = stringResource(R.string.duplicate_empty_body),
                    actionText = stringResource(R.string.duplicate_scan_again), onAction = { runId++ },
                    modifier = Modifier.fillMaxSize().padding(padding).padding(TrashPilotSpacing.Screen)
                )
            } else DuplicateResults(
                analysis = current.analysis, selectedUris = selectedUris, expandedUris = expandedUris,
                onToggle = { group, file ->
                    val selectedInGroup = group.files.count { it.uri in selectedUris }
                    selectedUris = if (file.uri in selectedUris) selectedUris - file.uri
                    else if (selectedInGroup < group.files.size - 1) selectedUris + file.uri else selectedUris
                },
                onDetails = { uri -> expandedUris = if (uri in expandedUris) expandedUris - uri else expandedUris + uri },
                onClean = { confirm = true }, modifier = Modifier.padding(padding)
            )
            DuplicateState.Cleaning -> TrashPilotLoadingState(
                stringResource(R.string.duplicate_cleaning_title), stringResource(R.string.duplicate_cleaning_body),
                Modifier.fillMaxSize().padding(padding).padding(TrashPilotSpacing.Screen)
            )
        }
    }

    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text(stringResource(R.string.duplicate_confirm_title)) },
        text = {
            val files = scanResult.files.filter { it.uri in selectedUris }
            Text(stringResource(R.string.duplicate_confirm_body, files.size, formatBytes(files.sumOf { it.sizeBytes })))
        },
        confirmButton = { TextButton(onClick = {
            confirm = false
            val files = scanResult.files.filter { it.uri in selectedUris }
            state = DuplicateState.Cleaning
            scope.launch {
                val report = cleaner.clean(files)
                val removed = report.deletedFiles.mapTo(hashSetOf()) { it.uri }
                val updated = scanResult.withoutFiles(removed)
                if (report.deletedFiles.isNotEmpty()) onCleaningComplete(updated, report)
                if (report.deletedFiles.isEmpty() && report.failedFiles.isNotEmpty()) {
                    state = DuplicateState.Error(report.permissionDenied)
                } else runId++
            }
        }) { Text(stringResource(R.string.duplicate_delete)) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(android.R.string.cancel)) } }
    )
}

@Composable private fun DuplicateLoading(progress: DuplicateScanProgress, modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(TrashPilotSpacing.Screen), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator(progress = { if (progress.totalFiles == 0) 0f else progress.processedFiles.toFloat() / progress.totalFiles })
        Spacer(Modifier.height(TrashPilotSpacing.Large))
        Text(stringResource(R.string.duplicate_scanning_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.duplicate_progress, progress.processedFiles, progress.totalFiles), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun DuplicateResults(
    analysis: DuplicateAnalysis, selectedUris: Set<String>, expandedUris: Set<String>,
    onToggle: (DuplicateGroup, ScannedFile) -> Unit, onDetails: (String) -> Unit,
    onClean: () -> Unit, modifier: Modifier
) {
    val selected = analysis.groups.flatMap { it.files }.filter { it.uri in selectedUris }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(TrashPilotSpacing.Screen), verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Large)) {
        item { TrashPilotMetricCard(listOf(
            stringResource(R.string.duplicate_total_groups) to analysis.groups.size.toString(),
            stringResource(R.string.duplicate_total_files) to analysis.duplicateFileCount.toString(),
            stringResource(R.string.duplicate_recoverable) to formatBytes(analysis.recoverableBytes)
        )) }
        item { CategorySummary(analysis) }
        items(analysis.groups, key = { it.fingerprint }) { group ->
            TrashPilotCard { Column(Modifier.padding(TrashPilotSpacing.Card), verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Medium)) {
                Text(stringResource(R.string.duplicate_group_files, group.files.size), style = MaterialTheme.typography.titleMedium)
                group.files.forEach { file -> DuplicateFileRow(file, file.uri in selectedUris, file.uri in expandedUris, { onToggle(group, file) }, { onDetails(file.uri) }) }
            } }
        }
        item { TrashPilotPrimaryButton(stringResource(R.string.duplicate_clean_selected), onClean, Modifier.fillMaxWidth(), enabled = selected.isNotEmpty()) }
    }
}

@Composable private fun CategorySummary(analysis: DuplicateAnalysis) {
    fun bytes(category: FileCategory) = analysis.groups.sumOf { group ->
        group.redundantFiles.filter { it.category == category }.sumOf(ScannedFile::sizeBytes)
    }
    TrashPilotMetricCard(listOf(
        stringResource(R.string.duplicate_images) to formatBytes(bytes(FileCategory.IMAGES)),
        stringResource(R.string.duplicate_videos) to formatBytes(bytes(FileCategory.VIDEOS)),
        stringResource(R.string.duplicate_documents) to formatBytes(bytes(FileCategory.DOCUMENTS)),
        stringResource(R.string.duplicate_audio) to formatBytes(bytes(FileCategory.AUDIO))
    ))
}

@Composable private fun DuplicateFileRow(file: ScannedFile, selected: Boolean, expanded: Boolean, onToggle: () -> Unit, onDetails: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onDetails), verticalAlignment = Alignment.CenterVertically) {
        DuplicateThumbnail(file)
        Column(Modifier.weight(1f).padding(horizontal = TrashPilotSpacing.Medium)) {
            Text(file.name, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(formatBytes(file.sizeBytes), color = MaterialTheme.colorScheme.onSurfaceVariant)
            val date = file.createdMillis.takeIf { it > 0 }?.let { DateFormat.getDateTimeInstance().format(Date(it)) }
            Text(date ?: stringResource(R.string.duplicate_date_unavailable), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (expanded) Text(file.relativePath.ifBlank { file.uri }, style = MaterialTheme.typography.bodySmall)
        }
        Checkbox(checked = selected, onCheckedChange = { onToggle() })
    }
}

@Composable private fun DuplicateThumbnail(file: ScannedFile) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(null, file.uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && file.category in setOf(FileCategory.IMAGES, FileCategory.VIDEOS)) {
            value = withContext(Dispatchers.IO) { runCatching { context.contentResolver.loadThumbnail(Uri.parse(file.uri), Size(96, 96), null) }.getOrNull() }
        }
    }
    Surface(Modifier.size(TrashPilotComponentSizes.CardIconContainer), shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
        if (bitmap != null) Image(bitmap!!.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(Icons.AutoMirrored.Outlined.InsertDriveFile, null, Modifier.padding(TrashPilotSpacing.Standard))
    }
}

private fun StorageScanResult.withoutFiles(uris: Set<String>): StorageScanResult {
    val remaining = files.filterNot { it.uri in uris }
    return copy(
        files = remaining, scannedFileCount = remaining.size,
        categoryBytes = FileCategory.entries.associateWith { category -> remaining.filter { it.category == category }.sumOf(ScannedFile::sizeBytes) },
        disposableCandidates = disposableCandidates.filterNot { it.uri in uris }
    )
}

private sealed interface DuplicateState {
    data class Scanning(val progress: DuplicateScanProgress) : DuplicateState
    data class Ready(val analysis: DuplicateAnalysis) : DuplicateState
    data class Error(val permissionDenied: Boolean) : DuplicateState
    data object Cleaning : DuplicateState
}
