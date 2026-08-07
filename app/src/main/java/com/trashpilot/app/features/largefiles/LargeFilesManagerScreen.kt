package com.trashpilot.app.features.largefiles

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.trashpilot.app.R
import com.trashpilot.app.core.largefiles.*
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
fun LargeFilesManagerScreen(
    onBack: () -> Unit,
    onFilesDeleted: (StorageScanResult, com.trashpilot.app.core.storage.DuplicateCleaningReport) -> Unit
) {
    val context = LocalContext.current
    val rootName = stringResource(R.string.results_storage)
    val unknownName = stringResource(R.string.reports_result_unknown)
    val automaticScanner = remember(context, rootName, unknownName) {
        MediaStoreStorageScanner(context.contentResolver, rootName, unknownName)
    }
    val folderScanner = remember(context, rootName, unknownName) {
        DocumentTreeStorageScanner(context.contentResolver, rootName, unknownName)
    }
    val cleaner = remember(context) { DuplicateCleaner(context.contentResolver) }
    val scope = rememberCoroutineScope()
    var scanJob by remember { mutableStateOf<Job?>(null) }
    var status by remember { mutableStateOf<LargeScanStatus>(LargeScanStatus.Preparing) }
    val scannedItems = remember { mutableStateListOf<LargeFileItem>() }
    var scanResult by remember { mutableStateOf<StorageScanResult?>(null) }
    var threshold by rememberSaveable { mutableStateOf(LargeFileThreshold.MB_100) }
    var filter by rememberSaveable { mutableStateOf<LargeFileType?>(null) }
    var sort by rememberSaveable { mutableStateOf(LargeFileSort.LARGEST) }
    var search by rememberSaveable { mutableStateOf("") }
    var selectedUris by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<List<ScannedFile>>(emptyList()) }
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
        scannedItems.removeAll { it.file.uri in deletedUris }
        selectedUris = emptySet()
        if (report.deletedFiles.isNotEmpty()) onFilesDeleted(updated, report)
        deletionError = report.failedFiles.isNotEmpty()
    }

    val deleteConsentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            finishDeletion(DuplicateCleaningReport(pendingDelete, emptyList()))
        } else {
            deletionError = true
        }
        pendingDelete = emptyList()
    }

    fun delete(files: List<ScannedFile>) {
        if (files.isEmpty()) return
        pendingDelete = files
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && files.all { it.uri.startsWith("content://media/") }) {
            runCatching {
                val request = MediaStore.createDeleteRequest(
                    context.contentResolver,
                    files.map { it.uri.toUri() }
                )
                deleteConsentLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
            }.onFailure {
                pendingDelete = emptyList()
                deletionError = true
            }
        } else {
            scope.launch {
                val report = cleaner.clean(files)
                pendingDelete = emptyList()
                finishDeletion(report)
            }
        }
    }

    fun startAutomaticScan() {
        scanJob?.cancel()
        scannedItems.clear()
        scanResult = null
        status = LargeScanStatus.Scanning(StorageScanProgress(0, null))
        scanJob = scope.launch {
            try {
                val result = automaticScanner.scan(
                    onFileScanned = { file, progress ->
                        withContext(Dispatchers.Main.immediate) {
                            scannedItems.add(file.toLargeFileItem())
                            status = LargeScanStatus.Scanning(progress)
                        }
                    }
                )
                scanResult = result
                status = LargeScanStatus.Complete
            } catch (cancelled: CancellationException) {
                status = LargeScanStatus.Cancelled
            } catch (_: SecurityException) {
                status = LargeScanStatus.PermissionRequired
            } catch (_: StorageAccessRequiredException) {
                status = LargeScanStatus.PermissionRequired
            } catch (_: Exception) {
                status = LargeScanStatus.StorageUnavailable
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) startAutomaticScan() else status = LargeScanStatus.PermissionRequired
    }
    val folderLauncher = rememberLauncherForActivityResult(OpenDocumentTreeWithFlags()) { selection ->
        if (selection == null) return@rememberLauncherForActivityResult
        scanJob?.cancel()
        scannedItems.clear()
        status = LargeScanStatus.Scanning(StorageScanProgress(0, null))
        scanJob = scope.launch {
            try {
                context.contentResolver.takePersistableUriPermission(selection.uri, selection.persistableFlags)
                val result = folderScanner.scan(selection.uri, onStage = {}, onFileScanned = { file, progress ->
                    withContext(Dispatchers.Main.immediate) {
                        scannedItems.add(file.toLargeFileItem())
                        status = LargeScanStatus.Scanning(progress)
                    }
                })
                scanResult = result
                status = LargeScanStatus.Complete
            } catch (cancelled: CancellationException) {
                status = LargeScanStatus.Cancelled
            } catch (_: SecurityException) {
                status = LargeScanStatus.PermissionRequired
            } catch (_: Exception) {
                status = LargeScanStatus.StorageUnavailable
            }
        }
    }

    LaunchedEffect(Unit) {
        val permissions = requiredScanPermissions()
        if (permissions.any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
            startAutomaticScan()
        } else permissionLauncher.launch(permissions.toTypedArray())
    }

    Scaffold(topBar = { TrashPilotTopAppBar(stringResource(R.string.results_label_large_files), onBack = onBack) }) { padding ->
        when (val current = status) {
            LargeScanStatus.Preparing -> Unit
            LargeScanStatus.PermissionRequired -> TrashPilotErrorState(
                stringResource(R.string.large_files_permission_title),
                stringResource(R.string.large_files_permission_body),
                Modifier.fillMaxSize().padding(padding).padding(TrashPilotSpacing.Screen),
                stringResource(R.string.scanner_choose_action), { folderLauncher.launch(null) }
            )
            LargeScanStatus.StorageUnavailable -> TrashPilotErrorState(
                stringResource(R.string.large_files_storage_title),
                stringResource(R.string.large_files_storage_body),
                Modifier.fillMaxSize().padding(padding).padding(TrashPilotSpacing.Screen),
                stringResource(R.string.large_files_scan_again), { startAutomaticScan() }
            )
            LargeScanStatus.Cancelled -> TrashPilotEmptyState(
                stringResource(R.string.large_files_cancelled_title),
                stringResource(R.string.large_files_cancelled_body),
                Modifier.fillMaxSize().padding(padding).padding(TrashPilotSpacing.Screen),
                stringResource(R.string.large_files_scan_again), { startAutomaticScan() }
            )
            else -> LargeFilesContent(
                items = scannedItems,
                scanning = current is LargeScanStatus.Scanning,
                progress = (current as? LargeScanStatus.Scanning)?.progress,
                threshold = threshold, onThreshold = { threshold = it; selectedUris = emptySet() },
                filter = filter, onFilter = { filter = it; selectedUris = emptySet() },
                sort = sort, onSort = { sort = it }, search = search,
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
        val files = scannedItems.map(LargeFileItem::file).filter { it.uri in selectedUris }
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.large_files_delete_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)) {
                    Text(stringResource(R.string.large_files_delete_warning))
                    Text(stringResource(R.string.large_files_delete_summary, files.size, formatBytes(files.sumOf { it.sizeBytes })), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = { TrashPilotTextButton(stringResource(android.R.string.cancel), { confirmDelete = false }) },
            confirmButton = { TrashPilotTextButton(stringResource(R.string.large_files_delete), { confirmDelete = false; delete(files) }) }
        )
    }
    if (deletionError) AlertDialog(
        onDismissRequest = { deletionError = false },
        title = { Text(stringResource(R.string.large_files_delete_error_title)) },
        text = { Text(stringResource(R.string.large_files_delete_error_body)) },
        confirmButton = { TrashPilotTextButton(stringResource(android.R.string.ok), { deletionError = false }) }
    )
}

@Composable private fun LargeFilesContent(
    items: List<LargeFileItem>, scanning: Boolean, progress: StorageScanProgress?,
    threshold: LargeFileThreshold, onThreshold: (LargeFileThreshold) -> Unit,
    filter: LargeFileType?, onFilter: (LargeFileType?) -> Unit,
    sort: LargeFileSort, onSort: (LargeFileSort) -> Unit,
    search: String, onSearch: (String) -> Unit,
    selectedUris: Set<String>, onToggle: (String) -> Unit,
    onSelectAll: (List<LargeFileItem>) -> Unit, onClearSelection: () -> Unit,
    onDelete: () -> Unit, onCancelScan: () -> Unit, modifier: Modifier
) {
    val visible = remember(items, threshold, filter, search, sort) { items.largeFilesView(threshold, filter, search, sort) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(TrashPilotSpacing.Screen), verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)) {
        if (scanning) item {
            TrashPilotCard(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(TrashPilotSpacing.Card), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (progress?.totalFiles != null && progress.totalFiles > 0) {
                        LinearProgressIndicator({ progress.scannedFiles.toFloat() / progress.totalFiles }, Modifier.fillMaxWidth())
                    } else CircularProgressIndicator()
                    Spacer(Modifier.height(TrashPilotSpacing.Standard))
                    Text(stringResource(R.string.large_files_scanning), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.large_files_scanned_count, progress?.scannedFiles ?: 0), style = MaterialTheme.typography.bodySmall)
                    TrashPilotTextButton(stringResource(R.string.large_files_cancel_scan), onCancelScan)
                }
            }
        }
        item { ChoiceChips(LargeFileThreshold.entries, threshold, onThreshold) { stringResource(it.label()) } }
        item {
            OutlinedTextField(search, onSearch, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.large_files_search)) }, singleLine = true, shape = TrashPilotRadii.ControlShape)
        }
        item { ChoiceChips(listOf<LargeFileType?>(null) + LargeFileType.entries, filter, onFilter) { stringResource(it?.label() ?: R.string.large_files_all) } }
        item { ChoiceChips(LargeFileSort.entries, sort, onSort) { stringResource(it.label()) } }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.results_large_files), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TrashPilotTextButton(stringResource(R.string.large_files_select_all), { onSelectAll(visible) })
                if (selectedUris.isNotEmpty()) TrashPilotTextButton(stringResource(R.string.large_files_clear_selection), onClearSelection)
            }
        }
        if (!scanning && visible.isEmpty()) item {
            TrashPilotEmptyState(stringResource(R.string.large_files_empty_title), stringResource(R.string.large_files_empty_body), Modifier.fillMaxWidth())
        }
        items(visible, key = { it.file.uri }) { item -> LargeFileRow(item, item.file.uri in selectedUris) { onToggle(item.file.uri) } }
        item { TrashPilotPrimaryButton(stringResource(R.string.large_files_delete_selected), onDelete, Modifier.fillMaxWidth(), enabled = selectedUris.isNotEmpty()) }
    }
}

@Composable private fun <T> ChoiceChips(options: List<T>, selected: T, onSelect: (T) -> Unit, label: @Composable (T) -> String) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Medium)) {
        options.forEach { option -> FilterChip(selected == option, { onSelect(option) }, label = { Text(label(option)) }) }
    }
}

@Composable private fun LargeFileRow(item: LargeFileItem, selected: Boolean, onToggle: () -> Unit) {
    TrashPilotCard(Modifier.fillMaxWidth().clickable(onClick = onToggle), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(TrashPilotSpacing.HomeCard), verticalAlignment = Alignment.CenterVertically) {
            TrashPilotIconContainer(item.type.icon())
            Column(Modifier.weight(1f).padding(horizontal = TrashPilotSpacing.Standard)) {
                Text(item.file.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.large_files_type_size, stringResource(item.type.label()), formatBytes(item.file.sizeBytes)), style = MaterialTheme.typography.bodyMedium)
                val modified = item.file.lastModifiedMillis.takeIf { it > 0 }?.let { DateFormat.getDateInstance().format(Date(it)) } ?: stringResource(R.string.duplicate_date_unavailable)
                Text(stringResource(R.string.large_files_folder_date, item.folderName.ifBlank { stringResource(R.string.large_files_folder_unknown) }, modified), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Checkbox(selected, onCheckedChange = { onToggle() })
        }
    }
}

private fun LargeFileThreshold.label() = when (this) {
    LargeFileThreshold.MB_50 -> R.string.large_files_50_mb
    LargeFileThreshold.MB_100 -> R.string.large_files_100_mb
    LargeFileThreshold.MB_250 -> R.string.large_files_250_mb
    LargeFileThreshold.MB_500 -> R.string.large_files_500_mb
    LargeFileThreshold.GB_1 -> R.string.large_files_1_gb
}
private fun LargeFileSort.label() = when (this) {
    LargeFileSort.LARGEST -> R.string.large_files_sort_largest
    LargeFileSort.SMALLEST -> R.string.large_files_sort_smallest
    LargeFileSort.NEWEST -> R.string.large_files_sort_newest
    LargeFileSort.OLDEST -> R.string.large_files_sort_oldest
    LargeFileSort.FILE_NAME -> R.string.large_files_sort_name
}
private fun LargeFileType.label() = when (this) {
    LargeFileType.VIDEOS -> R.string.large_files_videos
    LargeFileType.IMAGES -> R.string.large_files_images
    LargeFileType.DOCUMENTS -> R.string.large_files_documents
    LargeFileType.AUDIO -> R.string.large_files_audio
    LargeFileType.ARCHIVES -> R.string.large_files_archives
    LargeFileType.APK -> R.string.large_files_apk
    LargeFileType.OTHER -> R.string.large_files_other
}
private fun LargeFileType.icon(): ImageVector = when (this) {
    LargeFileType.VIDEOS -> Icons.Outlined.Movie
    LargeFileType.IMAGES -> Icons.Outlined.Image
    LargeFileType.DOCUMENTS -> Icons.Outlined.Description
    LargeFileType.AUDIO -> Icons.Outlined.AudioFile
    LargeFileType.ARCHIVES -> Icons.Outlined.FolderZip
    LargeFileType.APK -> Icons.Outlined.Android
    LargeFileType.OTHER -> Icons.AutoMirrored.Outlined.InsertDriveFile
}

private sealed interface LargeScanStatus {
    data object Preparing : LargeScanStatus
    data class Scanning(val progress: StorageScanProgress) : LargeScanStatus
    data object Complete : LargeScanStatus
    data object PermissionRequired : LargeScanStatus
    data object StorageUnavailable : LargeScanStatus
    data object Cancelled : LargeScanStatus
}
