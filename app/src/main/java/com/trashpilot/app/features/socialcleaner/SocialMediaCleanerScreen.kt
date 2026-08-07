package com.trashpilot.app.features.socialcleaner

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import android.provider.DocumentsContract
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.*
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
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.trashpilot.app.R
import com.trashpilot.app.core.socialcleaner.*
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
fun SocialMediaCleanerScreen(
    onBack: () -> Unit,
    onFilesDeleted: (StorageScanResult, DuplicateCleaningReport) -> Unit
) {
    val context = LocalContext.current
    val rootName = stringResource(R.string.results_storage)
    val unknownName = stringResource(R.string.reports_result_unknown)
    val appsRepository = remember(context) { InstalledSocialAppsRepository(context.applicationContext) }
    val mediaScanner = remember(context, rootName, unknownName) { MediaStoreStorageScanner(context.contentResolver, rootName, unknownName) }
    val folderScanner = remember(context, rootName, unknownName) { DocumentTreeStorageScanner(context.contentResolver, rootName, unknownName) }
    val cleaner = remember(context) { DuplicateCleaner(context.contentResolver) }
    val scope = rememberCoroutineScope()
    var installedApps by remember { mutableStateOf<List<InstalledSocialApp>>(emptyList()) }
    val mediaItems = remember { mutableStateListOf<SocialMediaItem>() }
    var scanResult by remember { mutableStateOf<StorageScanResult?>(null) }
    var state by remember { mutableStateOf<SocialCleanerState>(SocialCleanerState.LoadingApps) }
    var scanJob by remember { mutableStateOf<Job?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var typeFilter by rememberSaveable { mutableStateOf<SocialMediaType?>(null) }
    var appFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var sort by rememberSaveable { mutableStateOf(SocialMediaSort.LARGEST) }
    var selectedUris by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<List<ScannedFile>>(emptyList()) }
    var deletionError by remember { mutableStateOf(false) }
    var recoveredBytes by rememberSaveable { mutableLongStateOf(0L) }

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
        mediaItems.removeAll { it.file.uri in deletedUris }
        selectedUris = emptySet()
        recoveredBytes += report.reclaimedBytes
        if (report.deletedFiles.isNotEmpty()) onFilesDeleted(updated, report)
        deletionError = report.failedFiles.isNotEmpty()
    }

    val deleteConsentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) finishDeletion(DuplicateCleaningReport(pendingDelete, emptyList())) else deletionError = true
        pendingDelete = emptyList()
    }

    fun delete(files: List<ScannedFile>) {
        if (files.isEmpty()) return
        pendingDelete = files
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && files.all { it.uri.startsWith("content://media/") }) {
            runCatching {
                val request = MediaStore.createDeleteRequest(context.contentResolver, files.map { it.uri.toUri() })
                deleteConsentLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
            }.onFailure { pendingDelete = emptyList(); deletionError = true }
        } else scope.launch { pendingDelete = emptyList(); finishDeletion(cleaner.clean(files)) }
    }

    fun acceptFile(file: ScannedFile, progress: StorageScanProgress) {
        socialMediaItem(file, installedApps)?.let(mediaItems::add)
        state = SocialCleanerState.Scanning(progress)
    }

    fun startMediaScan() {
        scanJob?.cancel()
        mediaItems.clear()
        scanResult = null
        state = SocialCleanerState.Scanning(StorageScanProgress(0, null))
        scanJob = scope.launch {
            try {
                val result = mediaScanner.scan(onFileScanned = { file, progress -> withContext(Dispatchers.Main.immediate) { acceptFile(file, progress) } })
                scanResult = result
                state = SocialCleanerState.Ready
            } catch (_: CancellationException) { state = SocialCleanerState.Cancelled }
            catch (_: SecurityException) { state = SocialCleanerState.PermissionDenied }
            catch (_: StorageAccessRequiredException) { state = SocialCleanerState.PermissionDenied }
            catch (_: Exception) { state = SocialCleanerState.StorageUnavailable }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) startMediaScan() else state = SocialCleanerState.PermissionDenied
    }
    val folderLauncher = rememberLauncherForActivityResult(OpenDocumentTreeWithFlags()) { selection ->
        if (selection == null) return@rememberLauncherForActivityResult
        if (isProtectedTreeDocumentId(DocumentsContract.getTreeDocumentId(selection.uri))) {
            state = SocialCleanerState.PermissionDenied
            return@rememberLauncherForActivityResult
        }
        scanJob?.cancel(); mediaItems.clear(); scanResult = null
        state = SocialCleanerState.Scanning(StorageScanProgress(0, null))
        scanJob = scope.launch {
            try {
                context.contentResolver.takePersistableUriPermission(selection.uri, selection.persistableFlags)
                val result = folderScanner.scan(selection.uri, onStage = {}, onFileScanned = { file, progress -> withContext(Dispatchers.Main.immediate) { acceptFile(file, progress) } }, shouldTraverseDirectory = { path -> !isProtectedStoragePath(path) })
                scanResult = result
                state = SocialCleanerState.Ready
            } catch (_: CancellationException) { state = SocialCleanerState.Cancelled }
            catch (_: SecurityException) { state = SocialCleanerState.PermissionDenied }
            catch (_: Exception) { state = SocialCleanerState.StorageUnavailable }
        }
    }

    LaunchedEffect(Unit) {
        installedApps = appsRepository.installed()
        if (installedApps.isEmpty()) {
            state = SocialCleanerState.NoApps
        } else {
            val permissions = requiredScanPermissions()
            if (permissions.any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) startMediaScan()
            else permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    Scaffold(topBar = { TrashPilotTopAppBar(stringResource(R.string.results_label_social_media), onBack = onBack) }) { padding ->
        when (val current = state) {
            SocialCleanerState.LoadingApps -> TrashPilotLoadingState(stringResource(R.string.social_cleaner_loading_apps), stringResource(R.string.social_cleaner_loading_apps_body), Modifier.fillMaxSize().padding(padding))
            SocialCleanerState.NoApps -> TrashPilotEmptyState(stringResource(R.string.social_cleaner_no_apps_title), stringResource(R.string.social_cleaner_no_apps_body), Modifier.fillMaxSize().padding(padding).padding(TrashPilotSpacing.Screen))
            SocialCleanerState.PermissionDenied -> TrashPilotErrorState(stringResource(R.string.social_cleaner_permission_title), stringResource(R.string.social_cleaner_permission_body), Modifier.fillMaxSize().padding(padding).padding(TrashPilotSpacing.Screen), stringResource(R.string.social_cleaner_choose_folder), { folderLauncher.launch(null) })
            SocialCleanerState.StorageUnavailable -> TrashPilotErrorState(stringResource(R.string.social_cleaner_storage_title), stringResource(R.string.social_cleaner_storage_body), Modifier.fillMaxSize().padding(padding).padding(TrashPilotSpacing.Screen), stringResource(R.string.social_cleaner_scan_again), { startMediaScan() })
            SocialCleanerState.Cancelled -> TrashPilotEmptyState(stringResource(R.string.social_cleaner_cancelled_title), stringResource(R.string.social_cleaner_cancelled_body), Modifier.fillMaxSize().padding(padding).padding(TrashPilotSpacing.Screen), stringResource(R.string.social_cleaner_scan_again), { startMediaScan() })
            else -> SocialCleanerContent(
                apps = installedApps, items = mediaItems, scanning = current is SocialCleanerState.Scanning,
                progress = (current as? SocialCleanerState.Scanning)?.progress, recoveredBytes = recoveredBytes,
                query = query, onQuery = { query = it; selectedUris = emptySet() }, typeFilter = typeFilter,
                onTypeFilter = { typeFilter = it; selectedUris = emptySet() }, appFilter = appFilter,
                onAppFilter = { appFilter = it; selectedUris = emptySet() }, sort = sort, onSort = { sort = it },
                selectedUris = selectedUris, onToggle = { uri -> selectedUris = if (uri in selectedUris) selectedUris - uri else selectedUris + uri },
                onSelectAll = { visible -> val uris = visible.mapTo(mutableSetOf()) { it.file.uri }; selectedUris = if (uris.isNotEmpty() && selectedUris.containsAll(uris)) selectedUris - uris else selectedUris + uris },
                onClear = { selectedUris = emptySet() }, onDelete = { confirmDelete = true }, onCancel = { scanJob?.cancel() },
                modifier = Modifier.padding(padding)
            )
        }
    }

    if (confirmDelete) {
        val files = mediaItems.map(SocialMediaItem::file).filter { it.uri in selectedUris }
        AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text(stringResource(R.string.social_cleaner_delete_title)) }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)) {
                Text(stringResource(R.string.social_cleaner_delete_warning))
                Text(stringResource(R.string.social_cleaner_delete_summary, files.size, formatBytes(files.sumOf(ScannedFile::sizeBytes))), fontWeight = FontWeight.SemiBold)
            }
        }, dismissButton = { TrashPilotTextButton(stringResource(android.R.string.cancel), { confirmDelete = false }) }, confirmButton = { TrashPilotTextButton(stringResource(R.string.social_cleaner_delete), { confirmDelete = false; delete(files) }) })
    }
    if (deletionError) AlertDialog(onDismissRequest = { deletionError = false }, title = { Text(stringResource(R.string.social_cleaner_delete_error_title)) }, text = { Text(stringResource(R.string.social_cleaner_delete_error_body)) }, confirmButton = { TrashPilotTextButton(stringResource(android.R.string.ok), { deletionError = false }) })
}

@Composable private fun SocialCleanerContent(
    apps: List<InstalledSocialApp>, items: List<SocialMediaItem>, scanning: Boolean, progress: StorageScanProgress?, recoveredBytes: Long,
    query: String, onQuery: (String) -> Unit, typeFilter: SocialMediaType?, onTypeFilter: (SocialMediaType?) -> Unit,
    appFilter: String?, onAppFilter: (String?) -> Unit, sort: SocialMediaSort, onSort: (SocialMediaSort) -> Unit,
    selectedUris: Set<String>, onToggle: (String) -> Unit, onSelectAll: (List<SocialMediaItem>) -> Unit,
    onClear: () -> Unit, onDelete: () -> Unit, onCancel: () -> Unit, modifier: Modifier
) {
    val visible = remember(items, query, typeFilter, appFilter, sort) { items.socialMediaView(query, typeFilter, appFilter, sort) }
    val summaries = remember(apps, items) { socialAppSummaries(apps, items) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(TrashPilotSpacing.Screen), verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)) {
        if (scanning) item { TrashPilotCard(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) { Column(Modifier.fillMaxWidth().padding(TrashPilotSpacing.Card), horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(); Spacer(Modifier.height(TrashPilotSpacing.Standard)); Text(stringResource(R.string.social_cleaner_scanning), style = MaterialTheme.typography.titleMedium); Text(stringResource(R.string.social_cleaner_scanned_count, progress?.scannedFiles ?: 0), style = MaterialTheme.typography.bodySmall); TrashPilotTextButton(stringResource(R.string.social_cleaner_cancel_scan), onCancel) } } }
        if (recoveredBytes > 0) item { TrashPilotCard(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) { Column(Modifier.fillMaxWidth().padding(TrashPilotSpacing.Card)) { Text(stringResource(R.string.social_cleaner_recovered), style = MaterialTheme.typography.titleMedium); Text(formatBytes(recoveredBytes), style = MaterialTheme.typography.headlineSmall) } } }
        items(summaries, key = { "app-${it.app.definition.packageName}" }) { SocialAppCard(it) }
        item { OutlinedTextField(query, onQuery, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.social_cleaner_search)) }, singleLine = true, shape = TrashPilotRadii.ControlShape) }
        item { SocialChoiceChips(listOf<SocialMediaType?>(null) + SocialMediaType.entries, typeFilter, onTypeFilter) { stringResource(it?.label() ?: R.string.social_cleaner_all_types) } }
        item { SocialChoiceChips(listOf<String?>(null) + apps.map { it.definition.packageName }, appFilter, onAppFilter) { pkg -> apps.firstOrNull { it.definition.packageName == pkg }?.definition?.name ?: stringResource(R.string.social_cleaner_all_apps) } }
        item { SocialChoiceChips(SocialMediaSort.entries, sort, onSort) { stringResource(it.label()) } }
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.social_cleaner_media), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)); TrashPilotTextButton(stringResource(R.string.social_cleaner_select_all), { onSelectAll(visible) }); if (selectedUris.isNotEmpty()) TrashPilotTextButton(stringResource(R.string.social_cleaner_clear_selection), onClear) } }
        if (!scanning && visible.isEmpty()) item { TrashPilotEmptyState(stringResource(R.string.social_cleaner_no_media_title), stringResource(R.string.social_cleaner_no_media_body), Modifier.fillMaxWidth()) }
        items(visible, key = { it.file.uri }) { item -> SocialMediaRow(item, item.file.uri in selectedUris) { onToggle(item.file.uri) } }
        item { TrashPilotPrimaryButton(stringResource(R.string.social_cleaner_delete_selected), onDelete, Modifier.fillMaxWidth(), enabled = selectedUris.isNotEmpty()) }
    }
}

@Composable private fun SocialAppCard(summary: SocialAppSummary) {
    TrashPilotCard(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(TrashPilotSpacing.HomeCard), verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Compact)) {
            Row(verticalAlignment = Alignment.CenterVertically) { SocialAppIcon(summary.app.definition.packageName, summary.app.definition.name); Column(Modifier.weight(1f).padding(start = TrashPilotSpacing.Standard)) { Text(summary.app.definition.name, style = MaterialTheme.typography.titleMedium); Text(stringResource(R.string.social_cleaner_installed), style = MaterialTheme.typography.bodySmall) }; Text(formatBytes(summary.totalBytes), style = MaterialTheme.typography.titleMedium) }
            SocialMediaType.entries.forEach { type -> Row(Modifier.fillMaxWidth()) { Text(stringResource(type.label()), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f)); Text(formatBytes(summary.bytes(type)), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium) } }
            Text(stringResource(R.string.social_cleaner_cache_unavailable), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun SocialAppIcon(packageName: String, label: String) {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(null, packageName) { value = withContext(Dispatchers.IO) { runCatching { context.packageManager.getApplicationIcon(packageName).toBitmap(96, 96) }.getOrNull() } }
    Surface(Modifier.size(TrashPilotComponentSizes.CardIconContainer), shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) { bitmap?.let { Image(it.asImageBitmap(), label, Modifier.fillMaxSize()) } }
}

@Composable private fun SocialMediaRow(item: SocialMediaItem, selected: Boolean, onToggle: () -> Unit) {
    TrashPilotCard(
        Modifier.fillMaxWidth().clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(Modifier.fillMaxWidth().padding(TrashPilotSpacing.HomeCard), verticalAlignment = Alignment.CenterVertically) {
            TrashPilotIconContainer(Icons.AutoMirrored.Outlined.InsertDriveFile)
            Column(Modifier.weight(1f).padding(horizontal = TrashPilotSpacing.Standard)) {
                Text(item.file.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.social_cleaner_file_details, item.app.definition.name, stringResource(item.type.label()), formatBytes(item.file.sizeBytes)), style = MaterialTheme.typography.bodyMedium)
                val date = item.file.lastModifiedMillis.takeIf { it > 0 }?.let { DateFormat.getDateInstance().format(Date(it)) } ?: stringResource(R.string.duplicate_date_unavailable)
                Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Checkbox(selected, onCheckedChange = { onToggle() })
        }
    }
}

@Composable private fun <T> SocialChoiceChips(options: List<T>, selected: T, onSelect: (T) -> Unit, label: @Composable (T) -> String) { Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Medium)) { options.forEach { option -> FilterChip(selected == option, { onSelect(option) }, label = { Text(label(option)) }) } } }

private fun SocialMediaType.label() = when (this) { SocialMediaType.IMAGES -> R.string.social_cleaner_images; SocialMediaType.VIDEOS -> R.string.social_cleaner_videos; SocialMediaType.AUDIO -> R.string.social_cleaner_audio; SocialMediaType.DOCUMENTS -> R.string.social_cleaner_documents; SocialMediaType.DOWNLOADS -> R.string.social_cleaner_downloads; SocialMediaType.VOICE_NOTES -> R.string.social_cleaner_voice_notes; SocialMediaType.GIFS -> R.string.social_cleaner_gifs; SocialMediaType.STICKERS -> R.string.social_cleaner_stickers }
private fun SocialMediaSort.label() = when (this) { SocialMediaSort.LARGEST -> R.string.social_cleaner_sort_largest; SocialMediaSort.NEWEST -> R.string.social_cleaner_sort_newest; SocialMediaSort.OLDEST -> R.string.social_cleaner_sort_oldest; SocialMediaSort.FILE_TYPE -> R.string.social_cleaner_sort_type; SocialMediaSort.APPLICATION -> R.string.social_cleaner_sort_app }
private sealed interface SocialCleanerState { data object LoadingApps : SocialCleanerState; data object NoApps : SocialCleanerState; data class Scanning(val progress: StorageScanProgress) : SocialCleanerState; data object Ready : SocialCleanerState; data object PermissionDenied : SocialCleanerState; data object StorageUnavailable : SocialCleanerState; data object Cancelled : SocialCleanerState }
