package com.trashpilot.app.features.hiddenfiles

import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.trashpilot.app.R
import com.trashpilot.app.core.hiddenfiles.*
import com.trashpilot.app.core.largefiles.LargeFileItem
import com.trashpilot.app.core.largefiles.LargeFileSort
import com.trashpilot.app.core.largefiles.LargeFileType
import com.trashpilot.app.core.storage.*
import com.trashpilot.app.features.scanner.OpenDocumentTreeWithFlags
import com.trashpilot.app.ui.components.*
import com.trashpilot.app.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

@Composable
fun HiddenFilesManagerScreen(
    onBack: () -> Unit,
    onFilesDeleted: (StorageScanResult, DuplicateCleaningReport) -> Unit
) {
    val context = LocalContext.current
    val rootName = stringResource(R.string.results_storage)
    val unknownName = stringResource(R.string.reports_result_unknown)
    val scanner = remember(context, rootName, unknownName) {
        DocumentTreeStorageScanner(context.contentResolver, rootName, unknownName)
    }
    val cleaner = remember(context) { DuplicateCleaner(context.contentResolver) }
    val scope = rememberCoroutineScope()
    var scanJob by remember { mutableStateOf<Job?>(null) }
    var status by remember { mutableStateOf<HiddenScanStatus>(HiddenScanStatus.AccessRequired) }
    val hiddenItems = remember { mutableStateListOf<LargeFileItem>() }
    var scanResult by remember { mutableStateOf<StorageScanResult?>(null) }
    var filter by rememberSaveable { mutableStateOf<LargeFileType?>(null) }
    var sort by rememberSaveable { mutableStateOf(LargeFileSort.LARGEST) }
    var search by rememberSaveable { mutableStateOf("") }
    var selectedUris by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var deletionError by remember { mutableStateOf(false) }

    fun finishDeletion(report: DuplicateCleaningReport) {
        val current = scanResult ?: return
        val deletedUris = report.deletedFiles.mapTo(hashSetOf()) { it.uri }
        val remaining = current.files.filterNot { it.uri in deletedUris }
        val updated = current.copy(
            files = remaining,
            scannedFileCount = remaining.size,
            categoryBytes = FileCategory.entries.associateWith { category ->
                remaining.filter { it.category == category }.sumOf(ScannedFile::sizeBytes)
            },
            disposableCandidates = current.disposableCandidates.filterNot { it.uri in deletedUris }
        )
        scanResult = updated
        hiddenItems.removeAll { it.file.uri in deletedUris }
        selectedUris = emptySet()
        if (report.deletedFiles.isNotEmpty()) onFilesDeleted(updated, report)
        deletionError = report.failedFiles.isNotEmpty()
    }

    fun delete(files: List<ScannedFile>) {
        if (files.isEmpty()) return
        scope.launch { finishDeletion(cleaner.clean(files)) }
    }

    val folderLauncher = rememberLauncherForActivityResult(OpenDocumentTreeWithFlags()) { selection ->
        if (selection == null) {
            status = HiddenScanStatus.AccessRequired
            return@rememberLauncherForActivityResult
        }
        if (isProtectedTreeDocumentId(DocumentsContract.getTreeDocumentId(selection.uri))) {
            status = HiddenScanStatus.PermissionDenied
            return@rememberLauncherForActivityResult
        }
        scanJob?.cancel()
        hiddenItems.clear()
        scanResult = null
        selectedUris = emptySet()
        status = HiddenScanStatus.Scanning(StorageScanProgress(0, null))
        scanJob = scope.launch {
            try {
                context.contentResolver.takePersistableUriPermission(selection.uri, selection.persistableFlags)
                val result = scanner.scan(
                    treeUri = selection.uri,
                    onStage = {},
                    shouldTraverseDirectory = { path -> !isProtectedStoragePath(path) },
                    onFileScanned = { file, progress ->
                        withContext(Dispatchers.Main.immediate) {
                            if (file.isHiddenUserFile()) hiddenItems.add(file.toHiddenFileItem())
                            status = HiddenScanStatus.Scanning(progress)
                        }
                    }
                )
                scanResult = result
                hiddenItems.clear()
                hiddenItems.addAll(hiddenUserFiles(result.files).map(ScannedFile::toHiddenFileItem))
                status = HiddenScanStatus.Complete
            } catch (_: CancellationException) {
                status = HiddenScanStatus.Cancelled
            } catch (_: SecurityException) {
                status = HiddenScanStatus.PermissionDenied
            } catch (_: Exception) {
                status = HiddenScanStatus.StorageUnavailable
            }
        }
    }

    Scaffold(topBar = { TrashPilotTopAppBar(stringResource(R.string.results_label_hidden_files), onBack = onBack) }) { padding ->
        when (val current = status) {
            HiddenScanStatus.AccessRequired -> TrashPilotEmptyState(
                stringResource(R.string.hidden_files_access_title),
                stringResource(R.string.hidden_files_access_body),
                Modifier.fillMaxSize().padding(padding).padding(TrashPilotSpacing.Screen),
                stringResource(R.string.hidden_files_choose_folder), { folderLauncher.launch(null) }
            )
            HiddenScanStatus.PermissionDenied -> TrashPilotErrorState(
                stringResource(R.string.hidden_files_permission_title),
                stringResource(R.string.hidden_files_permission_body),
                Modifier.fillMaxSize().padding(padding).padding(TrashPilotSpacing.Screen),
                stringResource(R.string.hidden_files_choose_folder), { folderLauncher.launch(null) }
            )
            HiddenScanStatus.StorageUnavailable -> TrashPilotErrorState(
                stringResource(R.string.hidden_files_storage_title),
                stringResource(R.string.hidden_files_storage_body),
                Modifier.fillMaxSize().padding(padding).padding(TrashPilotSpacing.Screen),
                stringResource(R.string.hidden_files_scan_again), { folderLauncher.launch(null) }
            )
            HiddenScanStatus.Cancelled -> TrashPilotEmptyState(
                stringResource(R.string.hidden_files_cancelled_title),
                stringResource(R.string.hidden_files_cancelled_body),
                Modifier.fillMaxSize().padding(padding).padding(TrashPilotSpacing.Screen),
                stringResource(R.string.hidden_files_scan_again), { folderLauncher.launch(null) }
            )
            else -> HiddenFilesContent(
                items = hiddenItems,
                scanning = current is HiddenScanStatus.Scanning,
                progress = (current as? HiddenScanStatus.Scanning)?.progress,
                filter = filter,
                onFilter = { filter = it; selectedUris = emptySet() },
                sort = sort,
                onSort = { sort = it },
                search = search,
                onSearch = { search = it; selectedUris = emptySet() },
                selectedUris = selectedUris,
                onToggle = { uri -> selectedUris = if (uri in selectedUris) selectedUris - uri else selectedUris + uri },
                onSelectAll = { visible ->
                    val uris = visible.mapTo(mutableSetOf()) { it.file.uri }
                    selectedUris = if (uris.isNotEmpty() && selectedUris.containsAll(uris)) selectedUris - uris else selectedUris + uris
                },
                onClearSelection = { selectedUris = emptySet() },
                onDelete = { confirmDelete = true },
                onCancelScan = { scanJob?.cancel() },
                modifier = Modifier.padding(padding)
            )
        }
    }

    if (confirmDelete) {
        val files = hiddenItems.map(LargeFileItem::file).filter { it.uri in selectedUris }
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.hidden_files_delete_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)) {
                    Text(stringResource(R.string.hidden_files_delete_warning))
                    Text(stringResource(R.string.hidden_files_delete_summary, files.size, formatBytes(files.sumOf(ScannedFile::sizeBytes))), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = { TrashPilotTextButton(stringResource(android.R.string.cancel), { confirmDelete = false }) },
            confirmButton = { TrashPilotTextButton(stringResource(R.string.hidden_files_delete), { confirmDelete = false; delete(files) }) }
        )
    }
    if (deletionError) AlertDialog(
        onDismissRequest = { deletionError = false },
        title = { Text(stringResource(R.string.hidden_files_delete_error_title)) },
        text = { Text(stringResource(R.string.hidden_files_delete_error_body)) },
        confirmButton = { TrashPilotTextButton(stringResource(android.R.string.ok), { deletionError = false }) }
    )
}

@Composable
private fun HiddenFilesContent(
    items: List<LargeFileItem>, scanning: Boolean, progress: StorageScanProgress?,
    filter: LargeFileType?, onFilter: (LargeFileType?) -> Unit,
    sort: LargeFileSort, onSort: (LargeFileSort) -> Unit,
    search: String, onSearch: (String) -> Unit,
    selectedUris: Set<String>, onToggle: (String) -> Unit,
    onSelectAll: (List<LargeFileItem>) -> Unit, onClearSelection: () -> Unit,
    onDelete: () -> Unit, onCancelScan: () -> Unit, modifier: Modifier
) {
    val visible = remember(items, filter, search, sort) { items.hiddenFilesView(filter, search, sort) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(TrashPilotSpacing.Screen), verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)) {
        if (scanning) item {
            TrashPilotCard(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(TrashPilotSpacing.Card), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(TrashPilotSpacing.Standard))
                    Text(stringResource(R.string.hidden_files_scanning), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.hidden_files_scanned_count, progress?.scannedFiles ?: 0), style = MaterialTheme.typography.bodySmall)
                    TrashPilotTextButton(stringResource(R.string.hidden_files_cancel_scan), onCancelScan)
                }
            }
        }
        item {
            TrashPilotCard(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(TrashPilotSpacing.Card)) {
                    Text(stringResource(R.string.hidden_files_total), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.hidden_files_total_summary, items.size, formatBytes(items.sumOf { it.file.sizeBytes })), style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
        item { OutlinedTextField(search, onSearch, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.hidden_files_search)) }, singleLine = true, shape = TrashPilotRadii.ControlShape) }
        item { HiddenChoiceChips(listOf<LargeFileType?>(null) + LargeFileType.entries, filter, onFilter) { stringResource(it?.hiddenLabel() ?: R.string.hidden_files_all) } }
        item { HiddenChoiceChips(LargeFileSort.entries, sort, onSort) { stringResource(it.hiddenLabel()) } }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.results_hidden_files), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TrashPilotTextButton(stringResource(R.string.hidden_files_select_all), { onSelectAll(visible) })
                if (selectedUris.isNotEmpty()) TrashPilotTextButton(stringResource(R.string.hidden_files_clear_selection), onClearSelection)
            }
        }
        if (!scanning && visible.isEmpty()) item { TrashPilotEmptyState(stringResource(R.string.hidden_files_empty_title), stringResource(R.string.hidden_files_empty_body), Modifier.fillMaxWidth()) }
        items(visible, key = { it.file.uri }) { item -> HiddenFileRow(item, item.file.uri in selectedUris) { onToggle(item.file.uri) } }
        item { TrashPilotPrimaryButton(stringResource(R.string.hidden_files_delete_selected), onDelete, Modifier.fillMaxWidth(), enabled = selectedUris.isNotEmpty()) }
    }
}

@Composable private fun <T> HiddenChoiceChips(options: List<T>, selected: T, onSelect: (T) -> Unit, label: @Composable (T) -> String) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Medium)) {
        options.forEach { option -> FilterChip(selected == option, { onSelect(option) }, label = { Text(label(option)) }) }
    }
}

@Composable private fun HiddenFileRow(item: LargeFileItem, selected: Boolean, onToggle: () -> Unit) {
    TrashPilotCard(Modifier.fillMaxWidth().clickable(onClick = onToggle), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(TrashPilotSpacing.HomeCard), verticalAlignment = Alignment.CenterVertically) {
            TrashPilotIconContainer(item.type.hiddenIcon())
            Column(Modifier.weight(1f).padding(horizontal = TrashPilotSpacing.Standard)) {
                Text(item.file.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.hidden_files_type_size, stringResource(item.type.hiddenLabel()), formatBytes(item.file.sizeBytes)), style = MaterialTheme.typography.bodyMedium)
                val modified = item.file.lastModifiedMillis.takeIf { it > 0 }?.let { DateFormat.getDateInstance().format(Date(it)) } ?: stringResource(R.string.duplicate_date_unavailable)
                Text(stringResource(R.string.hidden_files_folder_date, item.folderName.ifBlank { stringResource(R.string.hidden_files_folder_unknown) }, modified), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Checkbox(selected, onCheckedChange = { onToggle() })
        }
    }
}

private fun LargeFileSort.hiddenLabel() = when (this) {
    LargeFileSort.LARGEST -> R.string.hidden_files_sort_largest
    LargeFileSort.SMALLEST -> R.string.hidden_files_sort_smallest
    LargeFileSort.NEWEST -> R.string.hidden_files_sort_newest
    LargeFileSort.OLDEST -> R.string.hidden_files_sort_oldest
    LargeFileSort.FILE_NAME -> R.string.hidden_files_sort_name
}
private fun LargeFileType.hiddenLabel() = when (this) {
    LargeFileType.VIDEOS -> R.string.hidden_files_videos
    LargeFileType.IMAGES -> R.string.hidden_files_images
    LargeFileType.DOCUMENTS -> R.string.hidden_files_documents
    LargeFileType.AUDIO -> R.string.hidden_files_audio
    LargeFileType.ARCHIVES -> R.string.hidden_files_archives
    LargeFileType.APK -> R.string.hidden_files_apk
    LargeFileType.OTHER -> R.string.hidden_files_other
}
private fun LargeFileType.hiddenIcon(): ImageVector = when (this) {
    LargeFileType.VIDEOS -> Icons.Outlined.Movie
    LargeFileType.IMAGES -> Icons.Outlined.Image
    LargeFileType.DOCUMENTS -> Icons.Outlined.Description
    LargeFileType.AUDIO -> Icons.Outlined.AudioFile
    LargeFileType.ARCHIVES -> Icons.Outlined.FolderZip
    LargeFileType.APK -> Icons.Outlined.Android
    LargeFileType.OTHER -> Icons.AutoMirrored.Outlined.InsertDriveFile
}

private sealed interface HiddenScanStatus {
    data object AccessRequired : HiddenScanStatus
    data class Scanning(val progress: StorageScanProgress) : HiddenScanStatus
    data object Complete : HiddenScanStatus
    data object PermissionDenied : HiddenScanStatus
    data object StorageUnavailable : HiddenScanStatus
    data object Cancelled : HiddenScanStatus
}
