package com.trashpilot.app.features.emptyfolders

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.net.toUri
import com.trashpilot.app.R
import com.trashpilot.app.core.emptyfolders.*
import com.trashpilot.app.features.scanner.OpenDocumentTreeWithFlags
import com.trashpilot.app.ui.components.*
import com.trashpilot.app.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun EmptyFoldersCleanerScreen(
    onBack: () -> Unit,
    onFoldersDeleted: (Set<String>, Int, Int) -> Unit
) {
    val context = LocalContext.current
    val scanner = remember(context) { EmptyFolderSafScanner(context.contentResolver) }
    val cleaner = remember(context) { EmptyFolderCleaner(context.contentResolver) }
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<EmptyFolderState>(EmptyFolderState.AccessRequired) }
    var treeUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val treeUri = treeUriString?.toUri()
    var folders by remember { mutableStateOf(emptyList<EmptyFolderItem>()) }
    var partialAccess by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(EmptyFolderSort.NAME) }
    var selected by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var scanJob by remember { mutableStateOf<Job?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var deletionResult by remember { mutableStateOf<EmptyFolderDeletionResult?>(null) }

    fun scan(uri: Uri) {
        scanJob?.cancel()
        folders = emptyList()
        selected = emptySet()
        partialAccess = false
        status = EmptyFolderState.Scanning(EmptyFolderScanProgress(0, 0))
        scanJob = scope.launch {
            try {
                val result = scanner.scan(uri) { _, progress -> status = EmptyFolderState.Scanning(progress) }
                folders = result.folders
                partialAccess = result.partialAccess
                status = EmptyFolderState.Complete
            } catch (_: CancellationException) { status = EmptyFolderState.Cancelled }
            catch (_: SecurityException) { status = EmptyFolderState.PermissionDenied }
            catch (_: IllegalArgumentException) { status = EmptyFolderState.StorageUnavailable }
            catch (_: Exception) { status = EmptyFolderState.StorageUnavailable }
        }
    }

    val folderPicker = rememberLauncherForActivityResult(OpenDocumentTreeWithFlags()) { selection ->
        if (selection == null) { status = EmptyFolderState.AccessRequired; return@rememberLauncherForActivityResult }
        try {
            context.contentResolver.takePersistableUriPermission(selection.uri, selection.persistableFlags)
            treeUriString = selection.uri.toString()
        } catch (_: SecurityException) { status = EmptyFolderState.PermissionDenied }
    }

    LaunchedEffect(treeUriString) {
        treeUriString?.let { scan(it.toUri()) }
    }

    fun deleteSelected() {
        val requested = folders.filter { it.uri in selected && it.canDelete }
        if (requested.isEmpty()) return
        scope.launch {
            val result = cleaner.delete(requested)
            deletionResult = result
            selected = emptySet()
            if (result.deleted.isNotEmpty()) onFoldersDeleted(result.deleted.mapTo(hashSetOf(), EmptyFolderItem::uri), result.deleted.size, result.failed.size)
            treeUri?.let(::scan)
        }
    }

    Scaffold(topBar = { TrashPilotTopAppBar(stringResource(R.string.empty_folders_title), onBack = onBack) }) { padding ->
        when (val current = status) {
            EmptyFolderState.AccessRequired -> TrashPilotEmptyState(stringResource(R.string.empty_folders_access_title), stringResource(R.string.empty_folders_access_body), Modifier.fillMaxSize().padding(padding).padding(TrashPilotSpacing.Screen), stringResource(R.string.empty_folders_choose_folder), { folderPicker.launch(null) })
            EmptyFolderState.PermissionDenied -> TrashPilotErrorState(stringResource(R.string.empty_folders_denied_title), stringResource(R.string.empty_folders_denied_body), Modifier.fillMaxSize().padding(padding).padding(TrashPilotSpacing.Screen), stringResource(R.string.empty_folders_choose_folder), { folderPicker.launch(null) })
            EmptyFolderState.StorageUnavailable -> TrashPilotErrorState(stringResource(R.string.empty_folders_storage_title), stringResource(R.string.empty_folders_storage_body), Modifier.fillMaxSize().padding(padding).padding(TrashPilotSpacing.Screen), stringResource(R.string.empty_folders_scan_again), { treeUri?.let(::scan) ?: folderPicker.launch(null) })
            EmptyFolderState.Cancelled -> TrashPilotEmptyState(stringResource(R.string.empty_folders_cancelled_title), stringResource(R.string.empty_folders_cancelled_body), Modifier.fillMaxSize().padding(padding).padding(TrashPilotSpacing.Screen), stringResource(R.string.empty_folders_scan_again), { treeUri?.let(::scan) ?: folderPicker.launch(null) })
            else -> EmptyFolderContent(folders, current is EmptyFolderState.Scanning, (current as? EmptyFolderState.Scanning)?.progress, partialAccess, query, { query = it; selected = emptySet() }, sort, { sort = it }, selected,
                { item -> selected = toggleEmptyFolderSelection(selected, item) },
                { visible -> selected = selectAllDeletableFolders(selected, visible) },
                { selected = emptySet() }, { confirmDelete = true }, { scanJob?.cancel() },
                { treeUri?.let(::scan) ?: folderPicker.launch(null) }, Modifier.padding(padding))
        }
    }

    if (confirmDelete) {
        val count = folders.count { it.uri in selected && it.canDelete }
        AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text(stringResource(R.string.empty_folders_delete_title)) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)) { Text(stringResource(R.string.empty_folders_delete_body)); Text(stringResource(R.string.empty_folders_delete_count, count), fontWeight = FontWeight.SemiBold) } },
            dismissButton = { TrashPilotTextButton(stringResource(android.R.string.cancel), { confirmDelete = false }) },
            confirmButton = { TrashPilotTextButton(stringResource(R.string.empty_folders_delete), { confirmDelete = false; deleteSelected() }) })
    }
    deletionResult?.let { result -> AlertDialog(onDismissRequest = { deletionResult = null }, title = { Text(stringResource(R.string.empty_folders_result_title)) }, text = { Text(stringResource(R.string.empty_folders_result_body, result.deleted.size, result.failed.size)) }, confirmButton = { TrashPilotTextButton(stringResource(android.R.string.ok), { deletionResult = null }) }) }
}

@Composable private fun EmptyFolderContent(folders: List<EmptyFolderItem>, scanning: Boolean, progress: EmptyFolderScanProgress?, partial: Boolean, query: String, onQuery: (String) -> Unit, sort: EmptyFolderSort, onSort: (EmptyFolderSort) -> Unit, selected: Set<String>, onToggle: (EmptyFolderItem) -> Unit, onSelectAll: (List<EmptyFolderItem>) -> Unit, onClear: () -> Unit, onDelete: () -> Unit, onCancel: () -> Unit, onScanAgain: () -> Unit, modifier: Modifier) {
    val visible = remember(folders, query, sort) { folders.emptyFoldersView(query, sort) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(TrashPilotSpacing.Screen), verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)) {
        if (scanning) item { TrashPilotCard(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) { Column(Modifier.fillMaxWidth().padding(TrashPilotSpacing.Card), horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(); Text(stringResource(R.string.empty_folders_scanning), style = MaterialTheme.typography.titleMedium); Text(stringResource(R.string.empty_folders_progress, progress?.inspectedFolders ?: 0, progress?.verifiedFolders ?: 0), style = MaterialTheme.typography.bodySmall); TrashPilotTextButton(stringResource(R.string.empty_folders_cancel_scan), onCancel) } } }
        item { TrashPilotCard(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)) { Column(Modifier.fillMaxWidth().padding(TrashPilotSpacing.Card), verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Compact)) { Text(stringResource(R.string.empty_folders_verified_count, folders.size), style = MaterialTheme.typography.headlineSmall); Text(stringResource(R.string.empty_folders_selected_count, selected.size)); if (partial) Text(stringResource(R.string.empty_folders_partial_access), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
        item { OutlinedTextField(query, onQuery, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.empty_folders_search)) }, singleLine = true, shape = TrashPilotRadii.ControlShape) }
        item { Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Medium)) { EmptyFolderSort.entries.forEach { option -> FilterChip(option == sort, { onSort(option) }, label = { Text(stringResource(option.label())) }) } } }
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.empty_folders_verified), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)); TrashPilotTextButton(stringResource(R.string.empty_folders_select_all), { onSelectAll(visible) }); if (selected.isNotEmpty()) TrashPilotTextButton(stringResource(R.string.empty_folders_clear), onClear) } }
        if (!scanning && visible.isEmpty()) item { TrashPilotEmptyState(stringResource(R.string.empty_folders_empty_title), stringResource(R.string.empty_folders_empty_body), Modifier.fillMaxWidth(), stringResource(R.string.empty_folders_scan_again), onScanAgain) }
        items(visible, key = EmptyFolderItem::uri) { folder -> EmptyFolderRow(folder, folder.uri in selected) { onToggle(folder) } }
        item { TrashPilotPrimaryButton(stringResource(R.string.empty_folders_delete_selected), onDelete, Modifier.fillMaxWidth(), enabled = selected.isNotEmpty()) }
    }
}

@Composable private fun EmptyFolderRow(folder: EmptyFolderItem, selected: Boolean, onToggle: () -> Unit) { TrashPilotCard(Modifier.fillMaxWidth().clickable(enabled = folder.canDelete, onClick = onToggle), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)) { Row(Modifier.fillMaxWidth().padding(TrashPilotSpacing.HomeCard), verticalAlignment = Alignment.CenterVertically) { TrashPilotIconContainer(Icons.Outlined.Folder); Column(Modifier.weight(1f).padding(horizontal = TrashPilotSpacing.Standard)) { Text(folder.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(stringResource(R.string.empty_folders_parent, folder.parentName.ifBlank { stringResource(R.string.empty_folders_parent_unknown) }), style = MaterialTheme.typography.bodyMedium); val date = folder.lastModifiedMillis.takeIf { it > 0 }?.let { DateFormat.getDateInstance().format(Date(it)) } ?: stringResource(R.string.results_date_unknown); Text(date, style = MaterialTheme.typography.bodySmall); if (!folder.canDelete) Text(stringResource(R.string.empty_folders_read_only), style = MaterialTheme.typography.bodySmall) }; Checkbox(selected, { onToggle() }, enabled = folder.canDelete) } } }

private fun EmptyFolderSort.label() = when (this) { EmptyFolderSort.NAME -> R.string.empty_folders_sort_name; EmptyFolderSort.NEWEST -> R.string.empty_folders_sort_newest; EmptyFolderSort.OLDEST -> R.string.empty_folders_sort_oldest; EmptyFolderSort.PARENT -> R.string.empty_folders_sort_parent }
private sealed interface EmptyFolderState { data object AccessRequired : EmptyFolderState; data class Scanning(val progress: EmptyFolderScanProgress) : EmptyFolderState; data object Complete : EmptyFolderState; data object PermissionDenied : EmptyFolderState; data object StorageUnavailable : EmptyFolderState; data object Cancelled : EmptyFolderState }
