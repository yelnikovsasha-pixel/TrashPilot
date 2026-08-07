package com.trashpilot.app.features.downloads

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
import com.trashpilot.app.core.downloads.*
import com.trashpilot.app.core.storage.*
import com.trashpilot.app.features.scanner.OpenDocumentTreeWithFlags
import com.trashpilot.app.ui.components.*
import com.trashpilot.app.ui.theme.*
import kotlinx.coroutines.*
import java.text.DateFormat
import java.util.Date

@Composable
fun DownloadsCleanerScreen(onBack: () -> Unit, onFilesDeleted: (StorageScanResult, DuplicateCleaningReport) -> Unit) {
    val context = LocalContext.current
    val rootName = stringResource(R.string.downloads_cleaner_title)
    val unknownName = stringResource(R.string.reports_result_unknown)
    val mediaScanner = remember { MediaStoreStorageScanner(context.contentResolver, rootName, unknownName) }
    val treeScanner = remember { DocumentTreeStorageScanner(context.contentResolver, rootName, unknownName) }
    val cleaner = remember { DuplicateCleaner(context.contentResolver) }
    val scope = rememberCoroutineScope()
    val discovered = remember { mutableStateListOf<DownloadItem>() }
    var scanResult by remember { mutableStateOf<StorageScanResult?>(null) }
    var state by remember { mutableStateOf<DownloadState>(DownloadState.Preparing) }
    var scanJob by remember { mutableStateOf<Job?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf<DownloadType?>(null) }
    var sort by rememberSaveable { mutableStateOf(DownloadSort.LARGEST) }
    var selected by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf(emptyList<ScannedFile>()) }
    var deletionMessage by remember { mutableStateOf<DeletionMessage?>(null) }
    var reclaimedBytes by rememberSaveable { mutableLongStateOf(0L) }

    fun applyDeletion(report: DuplicateCleaningReport) {
        val current = scanResult ?: return
        val deletedUris = report.deletedFiles.mapTo(hashSetOf(), ScannedFile::uri)
        val remaining = current.files.filterNot { it.uri in deletedUris }
        val updated = current.copy(files = remaining, scannedFileCount = remaining.size,
            categoryBytes = FileCategory.entries.associateWith { category -> remaining.filter { it.category == category }.sumOf(ScannedFile::sizeBytes) },
            disposableCandidates = current.disposableCandidates.filterNot { it.uri in deletedUris })
        scanResult = updated
        discovered.removeAll { it.file.uri in deletedUris }
        selected = emptySet()
        reclaimedBytes += report.reclaimedBytes
        if (report.deletedFiles.isNotEmpty()) onFilesDeleted(updated, report)
        deletionMessage = DeletionMessage(report.deletedFiles.size, report.failedFiles.size, report.reclaimedBytes)
    }

    val deleteConsent = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val files = pending
        pending = emptyList()
        if (result.resultCode == Activity.RESULT_OK) scope.launch {
            val deletedUris = withContext(Dispatchers.IO) {
                files.filter { file ->
                    runCatching {
                        context.contentResolver.query(file.uri.toUri(), arrayOf(MediaStore.MediaColumns._ID), null, null, null)
                            ?.use { !it.moveToFirst() } ?: false
                    }.getOrDefault(false)
                }.mapTo(hashSetOf(), ScannedFile::uri)
            }
            applyDeletion(DuplicateCleaningReport(files.filter { it.uri in deletedUris }, files.filterNot { it.uri in deletedUris }))
        } else deletionMessage = DeletionMessage(0, files.size, 0)
    }

    fun delete(files: List<ScannedFile>) {
        if (files.isEmpty()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && files.all { it.uri.startsWith("content://media/") }) {
            pending = files
            runCatching { MediaStore.createDeleteRequest(context.contentResolver, files.map { it.uri.toUri() }) }
                .onSuccess { deleteConsent.launch(IntentSenderRequest.Builder(it.intentSender).build()) }
                .onFailure { pending = emptyList(); deletionMessage = DeletionMessage(0, files.size, 0) }
        } else scope.launch { applyDeletion(cleaner.clean(files)) }
    }

    fun accept(file: ScannedFile, progress: StorageScanProgress) {
        file.toDownloadItem()?.let { item -> if (discovered.none { it.file.uri == item.file.uri }) discovered.add(item) }
        state = DownloadState.Scanning(progress)
    }

    fun startMediaScan() {
        scanJob?.cancel(); discovered.clear(); scanResult = null; selected = emptySet()
        state = DownloadState.Scanning(StorageScanProgress(0, null))
        scanJob = scope.launch {
            try { scanResult = mediaScanner.scan(onFileScanned = { file, progress -> withContext(Dispatchers.Main.immediate) { accept(file, progress) } }); state = DownloadState.Complete }
            catch (_: CancellationException) { state = DownloadState.Cancelled }
            catch (_: SecurityException) { state = DownloadState.AccessRequired }
            catch (_: Exception) { state = DownloadState.Unavailable }
        }
    }

    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) startMediaScan() else state = DownloadState.AccessRequired
    }
    val folder = rememberLauncherForActivityResult(OpenDocumentTreeWithFlags()) { selection ->
        if (selection == null) return@rememberLauncherForActivityResult
        scanJob?.cancel(); discovered.clear(); selected = emptySet(); state = DownloadState.Scanning(StorageScanProgress(0, null))
        scanJob = scope.launch {
            try {
                context.contentResolver.takePersistableUriPermission(selection.uri, selection.persistableFlags)
                scanResult = treeScanner.scan(selection.uri, onStage = {}, onFileScanned = { file, progress -> withContext(Dispatchers.Main.immediate) { accept(file, progress) } })
                state = DownloadState.Complete
            } catch (_: CancellationException) { state = DownloadState.Cancelled }
            catch (_: SecurityException) { state = DownloadState.AccessRequired }
            catch (_: Exception) { state = DownloadState.Unavailable }
        }
    }
    LaunchedEffect(Unit) {
        val required = requiredScanPermissions()
        if (required.any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) startMediaScan()
        else permissions.launch(required.toTypedArray())
    }

    Scaffold(topBar = { TrashPilotTopAppBar(stringResource(R.string.downloads_cleaner_title), onBack = onBack) }) { padding ->
        when (val current = state) {
            DownloadState.Preparing -> TrashPilotLoadingState(stringResource(R.string.downloads_preparing), stringResource(R.string.downloads_local_only), Modifier.fillMaxSize().padding(padding))
            DownloadState.AccessRequired -> TrashPilotErrorState(stringResource(R.string.downloads_access_title), stringResource(R.string.downloads_access_body), Modifier.fillMaxSize().padding(padding).padding(TrashPilotSpacing.Screen), stringResource(R.string.downloads_choose_folder), { folder.launch(null) })
            DownloadState.Unavailable -> TrashPilotErrorState(stringResource(R.string.downloads_unavailable_title), stringResource(R.string.downloads_unavailable_body), Modifier.fillMaxSize().padding(padding).padding(TrashPilotSpacing.Screen), stringResource(R.string.downloads_scan_again), { startMediaScan() })
            DownloadState.Cancelled -> TrashPilotEmptyState(stringResource(R.string.downloads_cancelled_title), stringResource(R.string.downloads_cancelled_body), Modifier.fillMaxSize().padding(padding).padding(TrashPilotSpacing.Screen), stringResource(R.string.downloads_scan_again), { startMediaScan() })
            else -> DownloadsContent(discovered, current is DownloadState.Scanning, (current as? DownloadState.Scanning)?.progress, reclaimedBytes, query, { query = it; selected = emptySet() }, filter, { filter = it; selected = emptySet() }, sort, { sort = it }, selected,
                { uri -> selected = if (uri in selected) selected - uri else selected + uri },
                { visible -> val uris = visible.mapTo(mutableSetOf()) { it.file.uri }; selected = selected + uris },
                { selected = emptySet() }, { confirmDelete = true }, { scanJob?.cancel() }, Modifier.padding(padding))
        }
    }

    if (confirmDelete) {
        val files = discovered.filter { it.file.uri in selected }.map(DownloadItem::file)
        AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text(stringResource(R.string.downloads_delete_title)) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)) { Text(stringResource(R.string.downloads_delete_warning)); Text(stringResource(R.string.downloads_delete_summary, files.size, formatBytes(files.sumOf(ScannedFile::sizeBytes))), fontWeight = FontWeight.SemiBold) } },
            dismissButton = { TrashPilotTextButton(stringResource(android.R.string.cancel), { confirmDelete = false }) },
            confirmButton = { TrashPilotTextButton(stringResource(R.string.downloads_delete), { confirmDelete = false; delete(files) }) })
    }
    deletionMessage?.let { message -> AlertDialog(onDismissRequest = { deletionMessage = null }, title = { Text(stringResource(R.string.downloads_result_title)) }, text = { Text(stringResource(R.string.downloads_result_body, message.deleted, message.failed, formatBytes(message.bytes))) }, confirmButton = { TrashPilotTextButton(stringResource(android.R.string.ok), { deletionMessage = null }) }) }
}

@Composable private fun DownloadsContent(items: List<DownloadItem>, scanning: Boolean, progress: StorageScanProgress?, reclaimed: Long, query: String, onQuery: (String) -> Unit, filter: DownloadType?, onFilter: (DownloadType?) -> Unit, sort: DownloadSort, onSort: (DownloadSort) -> Unit, selected: Set<String>, onToggle: (String) -> Unit, onSelectAll: (List<DownloadItem>) -> Unit, onClear: () -> Unit, onDelete: () -> Unit, onCancel: () -> Unit, modifier: Modifier) {
    val visible = remember(items, query, filter, sort) { items.downloadsView(query, filter, sort) }
    val summary = remember(items) { items.downloadSummary() }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(TrashPilotSpacing.Screen), verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)) {
        if (scanning) item { TrashPilotCard(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) { Column(Modifier.fillMaxWidth().padding(TrashPilotSpacing.Card), horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(); Text(stringResource(R.string.downloads_scanning), style = MaterialTheme.typography.titleMedium); Text(stringResource(R.string.downloads_scanned_count, progress?.scannedFiles ?: 0), style = MaterialTheme.typography.bodySmall); TrashPilotTextButton(stringResource(R.string.downloads_cancel_scan), onCancel) } } }
        item { TrashPilotCard(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)) { Column(Modifier.fillMaxWidth().padding(TrashPilotSpacing.Card)) { Text(formatBytes(summary.totalBytes), style = MaterialTheme.typography.headlineSmall); Text(stringResource(R.string.downloads_file_count, summary.fileCount)); if (reclaimed > 0) Text(stringResource(R.string.downloads_reclaimed, formatBytes(reclaimed)), color = MaterialTheme.colorScheme.primary); DownloadType.entries.forEach { type -> val bytes = summary.bytesByType[type] ?: 0; if (bytes > 0) Row(Modifier.fillMaxWidth()) { Text(stringResource(type.label()), Modifier.weight(1f)); Text(formatBytes(bytes)) } } } } }
        item { OutlinedTextField(query, onQuery, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.downloads_search)) }, singleLine = true, shape = TrashPilotRadii.ControlShape) }
        item { DownloadChips(listOf<DownloadType?>(null) + DownloadType.entries, filter, onFilter) { stringResource(it?.label() ?: R.string.downloads_all) } }
        item { DownloadChips(DownloadSort.entries, sort, onSort) { stringResource(it.label()) } }
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.downloads_files), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)); TrashPilotTextButton(stringResource(R.string.downloads_select_all), { onSelectAll(visible) }); if (selected.isNotEmpty()) TrashPilotTextButton(stringResource(R.string.downloads_clear_selection), onClear) } }
        if (!scanning && visible.isEmpty()) item { TrashPilotEmptyState(stringResource(R.string.downloads_empty_title), stringResource(R.string.downloads_empty_body), Modifier.fillMaxWidth()) }
        items(visible, key = { it.file.uri }) { item -> DownloadRow(item, item.file.uri in selected) { onToggle(item.file.uri) } }
        item { TrashPilotPrimaryButton(stringResource(R.string.downloads_delete_selected), onDelete, Modifier.fillMaxWidth(), enabled = selected.isNotEmpty()) }
    }
}

@Composable private fun DownloadRow(item: DownloadItem, selected: Boolean, onToggle: () -> Unit) { TrashPilotCard(Modifier.fillMaxWidth().clickable(onClick = onToggle), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)) { Row(Modifier.fillMaxWidth().padding(TrashPilotSpacing.HomeCard), verticalAlignment = Alignment.CenterVertically) { TrashPilotIconContainer(item.type.icon()); Column(Modifier.weight(1f).padding(horizontal = TrashPilotSpacing.Standard)) { Text(item.file.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(stringResource(R.string.downloads_type_size, stringResource(item.type.label()), formatBytes(item.file.sizeBytes))); Text(item.file.lastModifiedMillis.takeIf { it > 0 }?.let { DateFormat.getDateInstance().format(Date(it)) } ?: stringResource(R.string.results_date_unknown), style = MaterialTheme.typography.bodySmall) }; Checkbox(selected, { onToggle() }) } } }
@Composable private fun <T> DownloadChips(options: List<T>, selected: T, onSelect: (T) -> Unit, label: @Composable (T) -> String) { Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Medium)) { options.forEach { FilterChip(it == selected, { onSelect(it) }, label = { Text(label(it)) }) } } }

private fun DownloadType.label() = when (this) { DownloadType.IMAGES -> R.string.downloads_images; DownloadType.VIDEOS -> R.string.downloads_videos; DownloadType.AUDIO -> R.string.downloads_audio; DownloadType.DOCUMENTS -> R.string.downloads_documents; DownloadType.ARCHIVES -> R.string.downloads_archives; DownloadType.APK -> R.string.downloads_apk; DownloadType.OTHER -> R.string.downloads_other }
private fun DownloadSort.label() = when (this) { DownloadSort.LARGEST -> R.string.downloads_sort_largest; DownloadSort.SMALLEST -> R.string.downloads_sort_smallest; DownloadSort.NEWEST -> R.string.downloads_sort_newest; DownloadSort.OLDEST -> R.string.downloads_sort_oldest; DownloadSort.NAME -> R.string.downloads_sort_name }
private fun DownloadType.icon(): ImageVector = when (this) { DownloadType.IMAGES -> Icons.Outlined.Image; DownloadType.VIDEOS -> Icons.Outlined.Movie; DownloadType.AUDIO -> Icons.Outlined.AudioFile; DownloadType.DOCUMENTS -> Icons.Outlined.Description; DownloadType.ARCHIVES -> Icons.Outlined.FolderZip; DownloadType.APK -> Icons.Outlined.Android; DownloadType.OTHER -> Icons.AutoMirrored.Outlined.InsertDriveFile }
private data class DeletionMessage(val deleted: Int, val failed: Int, val bytes: Long)
private sealed interface DownloadState { data object Preparing : DownloadState; data class Scanning(val progress: StorageScanProgress) : DownloadState; data object Complete : DownloadState; data object AccessRequired : DownloadState; data object Unavailable : DownloadState; data object Cancelled : DownloadState }
