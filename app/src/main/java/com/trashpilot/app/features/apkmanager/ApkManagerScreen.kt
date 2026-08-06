package com.trashpilot.app.features.apkmanager

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.trashpilot.app.R
import com.trashpilot.app.core.apkmanager.*
import com.trashpilot.app.core.hiddenfiles.isProtectedStoragePath
import com.trashpilot.app.core.hiddenfiles.isProtectedTreeDocumentId
import com.trashpilot.app.core.storage.*
import com.trashpilot.app.features.scanner.OpenDocumentTreeWithFlags
import com.trashpilot.app.ui.components.*
import com.trashpilot.app.ui.theme.*
import kotlinx.coroutines.*
import java.text.DateFormat
import java.util.Date

@Composable
fun ApkManagerScreen(
    onBack: () -> Unit,
    onFilesDeleted: (StorageScanResult, DuplicateCleaningReport) -> Unit
) {
    val context = LocalContext.current
    val rootName = stringResource(R.string.results_storage)
    val unknownName = stringResource(R.string.reports_result_unknown)
    val mediaScanner = remember(context, rootName, unknownName) { MediaStoreStorageScanner(context.contentResolver, rootName, unknownName) }
    val folderScanner = remember(context, rootName, unknownName) { DocumentTreeStorageScanner(context.contentResolver, rootName, unknownName) }
    val repository = remember(context) { ApkRepository(ApkMetadataParser(context.applicationContext)) }
    val cleaner = remember(context) { DuplicateCleaner(context.contentResolver) }
    val scope = rememberCoroutineScope()
    val apkItems = remember { mutableStateListOf<ApkFileItem>() }
    var scanResult by remember { mutableStateOf<StorageScanResult?>(null) }
    var state by remember { mutableStateOf<ApkManagerState>(ApkManagerState.Preparing) }
    var scanJob by remember { mutableStateOf<Job?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(ApkSort.LARGEST) }
    var filter by rememberSaveable { mutableStateOf(ApkFilter.ALL) }
    var selectedUris by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var details by remember { mutableStateOf<ApkFileItem?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<List<ScannedFile>>(emptyList()) }
    var deleteIssue by remember { mutableStateOf<DeleteIssue?>(null) }

    fun finishDeletion(report: DuplicateCleaningReport) {
        val current = scanResult ?: return
        val deletedUris = report.deletedFiles.mapTo(hashSetOf()) { it.uri }
        val remaining = current.files.filterNot { it.uri in deletedUris }
        val updated = current.copy(
            files = remaining,
            scannedFileCount = remaining.size,
            categoryBytes = FileCategory.entries.associateWith { category -> remaining.filter { it.category == category }.sumOf(ScannedFile::sizeBytes) },
            disposableCandidates = current.disposableCandidates.filterNot { it.uri in deletedUris }
        )
        scanResult = updated
        apkItems.removeAll { it.file.uri in deletedUris }
        selectedUris = emptySet()
        if (report.deletedFiles.isNotEmpty()) onFilesDeleted(updated, report)
        if (report.failedFiles.isNotEmpty()) deleteIssue = if (report.permissionDenied) DeleteIssue.DENIED else DeleteIssue.MISSING
    }

    val deleteConsentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) finishDeletion(DuplicateCleaningReport(pendingDelete, emptyList())) else deleteIssue = DeleteIssue.DENIED
        pendingDelete = emptyList()
    }

    fun delete(files: List<ScannedFile>) {
        if (files.isEmpty()) return
        pendingDelete = files
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && files.all { it.uri.startsWith("content://media/") }) {
            runCatching {
                val request = MediaStore.createDeleteRequest(context.contentResolver, files.map { it.uri.toUri() })
                deleteConsentLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
            }.onFailure { pendingDelete = emptyList(); deleteIssue = DeleteIssue.DENIED }
        } else scope.launch { pendingDelete = emptyList(); finishDeletion(cleaner.clean(files)) }
    }

    suspend fun inspect(file: ScannedFile, progress: StorageScanProgress) {
        val item = repository.inspect(file)
        withContext(Dispatchers.Main.immediate) {
            item?.let(apkItems::add)
            state = ApkManagerState.Scanning(progress)
        }
    }

    fun startMediaScan() {
        scanJob?.cancel(); apkItems.clear(); scanResult = null; selectedUris = emptySet()
        state = ApkManagerState.Scanning(StorageScanProgress(0, null))
        scanJob = scope.launch {
            try {
                val result = mediaScanner.scan(onFileScanned = ::inspect)
                scanResult = result
                state = ApkManagerState.Ready
            } catch (_: CancellationException) { state = ApkManagerState.Cancelled }
            catch (_: SecurityException) { state = ApkManagerState.PermissionDenied }
            catch (_: StorageAccessRequiredException) { state = ApkManagerState.PermissionDenied }
            catch (_: Exception) { state = ApkManagerState.StorageUnavailable }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) startMediaScan() else state = ApkManagerState.PermissionDenied
    }
    val folderLauncher = rememberLauncherForActivityResult(OpenDocumentTreeWithFlags()) { selection ->
        if (selection == null) return@rememberLauncherForActivityResult
        if (isProtectedTreeDocumentId(DocumentsContract.getTreeDocumentId(selection.uri))) {
            state = ApkManagerState.PermissionDenied
            return@rememberLauncherForActivityResult
        }
        scanJob?.cancel(); apkItems.clear(); scanResult = null; selectedUris = emptySet()
        state = ApkManagerState.Scanning(StorageScanProgress(0, null))
        scanJob = scope.launch {
            try {
                context.contentResolver.takePersistableUriPermission(selection.uri, selection.persistableFlags)
                val result = folderScanner.scan(selection.uri, onStage = {}, onFileScanned = ::inspect, shouldTraverseDirectory = { !isProtectedStoragePath(it) })
                scanResult = result
                state = ApkManagerState.Ready
            } catch (_: CancellationException) { state = ApkManagerState.Cancelled }
            catch (_: SecurityException) { state = ApkManagerState.PermissionDenied }
            catch (_: Exception) { state = ApkManagerState.StorageUnavailable }
        }
    }

    LaunchedEffect(Unit) {
        val permissions = requiredScanPermissions()
        if (permissions.any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) startMediaScan()
        else permissionLauncher.launch(permissions.toTypedArray())
    }

    Scaffold(topBar = { TrashPilotTopAppBar(stringResource(R.string.apk_manager_title), onBack = onBack) }) { padding ->
        when (val current = state) {
            ApkManagerState.Preparing -> TrashPilotLoadingState(stringResource(R.string.apk_manager_preparing), stringResource(R.string.apk_manager_preparing_body), Modifier.fillMaxSize().padding(padding))
            ApkManagerState.PermissionDenied -> TrashPilotErrorState(stringResource(R.string.apk_manager_permission_title), stringResource(R.string.apk_manager_permission_body), Modifier.fillMaxSize().padding(padding).padding(TrashPilotSpacing.Screen), stringResource(R.string.apk_manager_choose_folder), { folderLauncher.launch(null) })
            ApkManagerState.StorageUnavailable -> TrashPilotErrorState(stringResource(R.string.apk_manager_storage_title), stringResource(R.string.apk_manager_storage_body), Modifier.fillMaxSize().padding(padding).padding(TrashPilotSpacing.Screen), stringResource(R.string.apk_manager_scan_again), { folderLauncher.launch(null) })
            ApkManagerState.Cancelled -> TrashPilotEmptyState(stringResource(R.string.apk_manager_cancelled_title), stringResource(R.string.apk_manager_cancelled_body), Modifier.fillMaxSize().padding(padding).padding(TrashPilotSpacing.Screen), stringResource(R.string.apk_manager_scan_again), { folderLauncher.launch(null) })
            else -> ApkManagerContent(
                items = apkItems, scanning = current is ApkManagerState.Scanning,
                progress = (current as? ApkManagerState.Scanning)?.progress,
                query = query, onQuery = { query = it; selectedUris = emptySet() }, sort = sort, onSort = { sort = it },
                filter = filter, onFilter = { filter = it; selectedUris = emptySet() }, selectedUris = selectedUris,
                onToggle = { uri -> selectedUris = if (uri in selectedUris) selectedUris - uri else selectedUris + uri },
                onSelectAll = { visible -> val uris = visible.mapTo(mutableSetOf()) { it.file.uri }; selectedUris = if (uris.isNotEmpty() && selectedUris.containsAll(uris)) selectedUris - uris else selectedUris + uris },
                onClear = { selectedUris = emptySet() }, onDetails = { details = it }, onDelete = { confirmDelete = true },
                onCancel = { scanJob?.cancel() }, onScanAgain = { folderLauncher.launch(null) }, modifier = Modifier.padding(padding)
            )
        }
    }

    details?.let { ApkDetailsDialog(it) { details = null } }
    if (confirmDelete) {
        val files = apkItems.map(ApkFileItem::file).filter { it.uri in selectedUris }
        AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text(stringResource(R.string.apk_manager_delete_title)) }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)) {
                Text(stringResource(R.string.apk_manager_delete_summary, files.size, formatBytes(files.sumOf(ScannedFile::sizeBytes))))
                Text(stringResource(R.string.apk_manager_delete_warning))
                Text(stringResource(R.string.apk_manager_delete_install_note))
            }
        }, dismissButton = { TrashPilotTextButton(stringResource(android.R.string.cancel), { confirmDelete = false }) }, confirmButton = { TrashPilotTextButton(stringResource(R.string.apk_manager_delete), { confirmDelete = false; delete(files) }) })
    }
    deleteIssue?.let { issue -> AlertDialog(onDismissRequest = { deleteIssue = null }, title = { Text(stringResource(if (issue == DeleteIssue.DENIED) R.string.apk_manager_delete_denied_title else R.string.apk_manager_file_missing_title)) }, text = { Text(stringResource(if (issue == DeleteIssue.DENIED) R.string.apk_manager_delete_denied_body else R.string.apk_manager_file_missing_body)) }, confirmButton = { TrashPilotTextButton(stringResource(android.R.string.ok), { deleteIssue = null }) }) }
}

@Composable private fun ApkManagerContent(
    items: List<ApkFileItem>, scanning: Boolean, progress: StorageScanProgress?, query: String, onQuery: (String) -> Unit,
    sort: ApkSort, onSort: (ApkSort) -> Unit, filter: ApkFilter, onFilter: (ApkFilter) -> Unit,
    selectedUris: Set<String>, onToggle: (String) -> Unit, onSelectAll: (List<ApkFileItem>) -> Unit,
    onClear: () -> Unit, onDetails: (ApkFileItem) -> Unit, onDelete: () -> Unit, onCancel: () -> Unit,
    onScanAgain: () -> Unit, modifier: Modifier
) {
    val visible = remember(items, query, sort, filter) { items.apkView(query, sort, filter) }
    val selectedSize = items.filter { it.file.uri in selectedUris }.sumOf { it.file.sizeBytes }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(TrashPilotSpacing.Screen), verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)) {
        if (scanning) item { TrashPilotCard(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) { Column(Modifier.fillMaxWidth().padding(TrashPilotSpacing.Card), horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(); Spacer(Modifier.height(TrashPilotSpacing.Standard)); Text(stringResource(R.string.apk_manager_scanning), style = MaterialTheme.typography.titleMedium); Text(stringResource(R.string.apk_manager_scanned_count, progress?.scannedFiles ?: 0), style = MaterialTheme.typography.bodySmall); TrashPilotTextButton(stringResource(R.string.apk_manager_cancel_scan), onCancel) } } }
        item { TrashPilotCard(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) { Column(Modifier.fillMaxWidth().padding(TrashPilotSpacing.Card), verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Compact)) { ApkMetric(R.string.apk_manager_file_count, items.size.toString()); ApkMetric(R.string.apk_manager_total_storage, formatBytes(items.sumOf { it.file.sizeBytes })); ApkMetric(R.string.apk_manager_selected_size, formatBytes(selectedSize)) } } }
        if (!scanning && items.isEmpty()) item { TrashPilotEmptyState(stringResource(R.string.apk_manager_empty_title), stringResource(R.string.apk_manager_empty_body), Modifier.fillMaxWidth(), stringResource(R.string.apk_manager_scan_again), onScanAgain) }
        if (items.isNotEmpty()) {
            item { OutlinedTextField(query, onQuery, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.apk_manager_search)) }, singleLine = true, shape = TrashPilotRadii.ControlShape) }
            item { ApkChoiceChips(ApkSort.entries, sort, onSort) { stringResource(it.label()) } }
            item { ApkChoiceChips(ApkFilter.entries, filter, onFilter) { stringResource(it.label()) } }
            item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.apk_manager_files), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)); TrashPilotTextButton(stringResource(R.string.apk_manager_select_all), { onSelectAll(visible) }); if (selectedUris.isNotEmpty()) TrashPilotTextButton(stringResource(R.string.apk_manager_clear_selection), onClear) } }
            if (visible.isEmpty()) item { TrashPilotEmptyState(stringResource(R.string.apk_manager_no_matches_title), stringResource(R.string.apk_manager_no_matches_body), Modifier.fillMaxWidth()) }
            items(visible, key = { it.file.uri }) { item -> ApkFileRow(item, item.file.uri in selectedUris, { onDetails(item) }) { onToggle(item.file.uri) } }
            item { TrashPilotPrimaryButton(stringResource(R.string.apk_manager_delete_selected), onDelete, Modifier.fillMaxWidth(), enabled = selectedUris.isNotEmpty()) }
        }
    }
}

@Composable private fun ApkMetric(@androidx.annotation.StringRes label: Int, value: String) { Row(Modifier.fillMaxWidth()) { Text(stringResource(label), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f)); Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold) } }

@Composable private fun ApkFileRow(item: ApkFileItem, selected: Boolean, onDetails: () -> Unit, onToggle: () -> Unit) {
    TrashPilotCard(Modifier.fillMaxWidth().clickable(onClick = onDetails), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(TrashPilotSpacing.HomeCard), verticalAlignment = Alignment.CenterVertically) {
            ApkIcon(item)
            Column(Modifier.weight(1f).padding(horizontal = TrashPilotSpacing.Standard)) {
                Text(item.file.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                item.metadata?.appLabel?.let { Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                val date = item.file.lastModifiedMillis.takeIf { it > 0 }?.let { DateFormat.getDateInstance().format(Date(it)) } ?: stringResource(R.string.duplicate_date_unavailable)
                Text(stringResource(R.string.apk_manager_row_details, formatBytes(item.file.sizeBytes), date), style = MaterialTheme.typography.bodySmall)
                Text(item.parentFolder.ifBlank { stringResource(R.string.apk_manager_unknown_folder) }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Checkbox(selected, onCheckedChange = { onToggle() })
        }
    }
}

@Composable private fun ApkIcon(item: ApkFileItem) { Surface(Modifier.size(TrashPilotComponentSizes.CardIconContainer), shape = TrashPilotRadii.IconContainerShape, color = MaterialTheme.colorScheme.secondaryContainer) { item.metadata?.icon?.let { Image(it.asImageBitmap(), item.metadata.appLabel ?: item.file.name, Modifier.fillMaxSize()) } ?: Icon(Icons.Outlined.Android, null, Modifier.padding(TrashPilotSpacing.Standard), tint = MaterialTheme.colorScheme.primary) } }

@Composable private fun ApkDetailsDialog(item: ApkFileItem, onDismiss: () -> Unit) {
    val unavailable = stringResource(R.string.apk_manager_not_available)
    val date = item.file.lastModifiedMillis.takeIf { it > 0 }?.let { DateFormat.getDateInstance().format(Date(it)) } ?: unavailable
    AlertDialog(onDismissRequest = onDismiss, title = { Text(item.file.name) }, text = { Column(verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Medium)) { DetailLine(R.string.apk_manager_package_name, item.metadata?.packageName ?: unavailable); DetailLine(R.string.apk_manager_version, item.metadata?.versionName ?: unavailable); DetailLine(R.string.apk_manager_size, formatBytes(item.file.sizeBytes)); DetailLine(R.string.apk_manager_modified, date); DetailLine(R.string.apk_manager_folder, item.parentFolder.ifBlank { unavailable }); DetailLine(R.string.apk_manager_metadata_status, stringResource(if (item.metadataVerified) R.string.apk_manager_metadata_verified else R.string.apk_manager_metadata_unreadable)) } }, confirmButton = { TrashPilotTextButton(stringResource(android.R.string.ok), onDismiss) })
}

@Composable private fun DetailLine(@androidx.annotation.StringRes label: Int, value: String) { Column { Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.bodyMedium) } }
@Composable private fun <T> ApkChoiceChips(options: List<T>, selected: T, onSelect: (T) -> Unit, label: @Composable (T) -> String) { Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Medium)) { options.forEach { option -> FilterChip(selected == option, { onSelect(option) }, label = { Text(label(option)) }) } } }
private fun ApkSort.label() = when (this) { ApkSort.LARGEST -> R.string.apk_manager_sort_largest; ApkSort.SMALLEST -> R.string.apk_manager_sort_smallest; ApkSort.NEWEST -> R.string.apk_manager_sort_newest; ApkSort.OLDEST -> R.string.apk_manager_sort_oldest; ApkSort.NAME -> R.string.apk_manager_sort_name }
private fun ApkFilter.label() = when (this) { ApkFilter.ALL -> R.string.apk_manager_filter_all; ApkFilter.VALID -> R.string.apk_manager_filter_valid; ApkFilter.UNREADABLE -> R.string.apk_manager_filter_unreadable }
private sealed interface ApkManagerState { data object Preparing : ApkManagerState; data class Scanning(val progress: StorageScanProgress) : ApkManagerState; data object Ready : ApkManagerState; data object PermissionDenied : ApkManagerState; data object StorageUnavailable : ApkManagerState; data object Cancelled : ApkManagerState }
private enum class DeleteIssue { DENIED, MISSING }
