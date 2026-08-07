package com.trashpilot.app.features.screenshots

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.util.Size
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
import androidx.compose.material.icons.outlined.BrokenImage
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
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.trashpilot.app.R
import com.trashpilot.app.core.screenshots.*
import com.trashpilot.app.core.storage.DuplicateCleaningReport
import com.trashpilot.app.core.storage.FileCategory
import com.trashpilot.app.core.storage.ScannedFile
import com.trashpilot.app.core.storage.formatBytes
import com.trashpilot.app.ui.components.*
import com.trashpilot.app.ui.theme.*
import kotlinx.coroutines.*
import java.text.DateFormat
import java.util.Date

@Composable
fun ScreenshotsCleanerScreen(onBack: () -> Unit, onDeleted: (Set<String>, DuplicateCleaningReport) -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { MediaStoreScreenshotRepository(context.contentResolver) }
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<ScreenshotState>(ScreenshotState.Preparing) }
    var screenshots by remember { mutableStateOf(emptyList<ScreenshotItem>()) }
    var partialAccess by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(ScreenshotSort.NEWEST) }
    var selected by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var detail by remember { mutableStateOf<ScreenshotItem?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf(emptyList<ScreenshotItem>()) }
    var resultMessage by remember { mutableStateOf<ScreenshotDeletionAccounting?>(null) }
    var scanJob by remember { mutableStateOf<Job?>(null) }

    fun scan(isPartial: Boolean) {
        scanJob?.cancel(); screenshots = emptyList(); selected = emptySet(); partialAccess = isPartial
        state = ScreenshotState.Scanning(ScreenshotScanProgress(0, null))
        scanJob = scope.launch {
            try {
                val found = mutableListOf<ScreenshotItem>()
                screenshots = repository.scan { item, progress ->
                    item?.let(found::add)
                    screenshots = found.toList()
                    state = ScreenshotState.Scanning(progress)
                }
                screenshots = found
                state = ScreenshotState.Complete
            } catch (_: CancellationException) { state = ScreenshotState.Cancelled }
            catch (_: SecurityException) { state = ScreenshotState.PermissionDenied }
            catch (_: Exception) { state = ScreenshotState.StorageUnavailable }
        }
    }

    fun verifyDeletion(requested: List<ScreenshotItem>) {
        scope.launch {
            val deletedUris = withContext(Dispatchers.IO) {
                requested.filter { item -> runCatching {
                    context.contentResolver.query(item.uri.toUri(), arrayOf(MediaStore.Images.Media._ID), null, null, null)
                        ?.use { !it.moveToFirst() } ?: false
                }.getOrDefault(false) }.mapTo(hashSetOf(), ScreenshotItem::uri)
            }
            val accounting = accountScreenshotDeletion(requested, deletedUris)
            resultMessage = accounting
            selected = emptySet()
            if (accounting.deleted.isNotEmpty()) {
                val deletedFiles = accounting.deleted.map { it.toScannedFile() }
                val failedFiles = accounting.failed.map { it.toScannedFile() }
                onDeleted(deletedUris, DuplicateCleaningReport(deletedFiles, failedFiles))
            }
            scan(partialAccess)
        }
    }

    val deleteConsent = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { activityResult ->
        val requested = pendingDelete; pendingDelete = emptyList()
        if (activityResult.resultCode == Activity.RESULT_OK) verifyDeletion(requested)
        else resultMessage = accountScreenshotDeletion(requested, emptySet())
    }

    fun delete(items: List<ScreenshotItem>) {
        if (items.isEmpty()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pendingDelete = items
            runCatching { MediaStore.createDeleteRequest(context.contentResolver, items.map { it.uri.toUri() }) }
                .onSuccess { deleteConsent.launch(IntentSenderRequest.Builder(it.intentSender).build()) }
                .onFailure { pendingDelete = emptyList(); resultMessage = accountScreenshotDeletion(items, emptySet()) }
        } else scope.launch {
            withContext(Dispatchers.IO) { items.forEach { runCatching { context.contentResolver.delete(it.uri.toUri(), null, null) } } }
            verifyDeletion(items)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val full = grants[imagePermission()] == true || ContextCompat.checkSelfPermission(context, imagePermission()) == PackageManager.PERMISSION_GRANTED
        val selectedOnly = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            (grants[Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] == true || ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED)
        if (full || selectedOnly) scan(isPartial = !full && selectedOnly) else state = ScreenshotState.PermissionDenied
    }

    LaunchedEffect(Unit) {
        val full = ContextCompat.checkSelfPermission(context, imagePermission()) == PackageManager.PERMISSION_GRANTED
        val selectedOnly = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
        if (full || selectedOnly) scan(!full && selectedOnly) else permissionLauncher.launch(screenshotPermissions())
    }

    Scaffold(topBar = { TrashPilotTopAppBar(stringResource(R.string.results_label_screenshots), onBack = onBack) }) { padding ->
        when (val current = state) {
            ScreenshotState.Preparing -> TrashPilotLoadingState(stringResource(R.string.screenshots_preparing), stringResource(R.string.screenshots_local_only), Modifier.fillMaxSize().padding(padding))
            ScreenshotState.PermissionDenied -> TrashPilotErrorState(stringResource(R.string.screenshots_permission_title), stringResource(R.string.screenshots_permission_body), Modifier.fillMaxSize().padding(padding).padding(TrashPilotSpacing.Screen), stringResource(R.string.screenshots_try_again), { permissionLauncher.launch(screenshotPermissions()) })
            ScreenshotState.StorageUnavailable -> TrashPilotErrorState(stringResource(R.string.screenshots_storage_title), stringResource(R.string.screenshots_storage_body), Modifier.fillMaxSize().padding(padding).padding(TrashPilotSpacing.Screen), stringResource(R.string.screenshots_scan_again), { scan(partialAccess) })
            ScreenshotState.Cancelled -> TrashPilotEmptyState(stringResource(R.string.screenshots_cancelled_title), stringResource(R.string.screenshots_cancelled_body), Modifier.fillMaxSize().padding(padding).padding(TrashPilotSpacing.Screen), stringResource(R.string.screenshots_scan_again), { scan(partialAccess) })
            else -> ScreenshotContent(screenshots, current is ScreenshotState.Scanning, (current as? ScreenshotState.Scanning)?.progress, partialAccess, query, { query = it; selected = emptySet() }, sort, { sort = it }, selected,
                { selected = toggleScreenshotSelection(selected, it.uri) }, { visible -> selected = selected + visible.map(ScreenshotItem::uri) }, { selected = emptySet() }, { detail = it }, { confirmDelete = true }, { scanJob?.cancel() }, { scan(partialAccess) }, Modifier.padding(padding))
        }
    }

    if (confirmDelete) {
        val items = screenshots.filter { it.uri in selected }
        AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text(stringResource(R.string.screenshots_delete_title)) }, text = { Column(verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)) { Text(stringResource(R.string.screenshots_delete_body)); Text(stringResource(R.string.screenshots_delete_summary, items.size, formatBytes(items.sumOf(ScreenshotItem::sizeBytes))), fontWeight = FontWeight.SemiBold) } }, dismissButton = { TrashPilotTextButton(stringResource(android.R.string.cancel), { confirmDelete = false }) }, confirmButton = { TrashPilotTextButton(stringResource(R.string.screenshots_delete), { confirmDelete = false; delete(items) }) })
    }
    detail?.let { item -> ScreenshotDetailDialog(item) { detail = null } }
    resultMessage?.let { report -> AlertDialog(onDismissRequest = { resultMessage = null }, title = { Text(stringResource(R.string.screenshots_result_title)) }, text = { Text(stringResource(R.string.screenshots_result_body, report.deleted.size, report.failed.size, formatBytes(report.reclaimedBytes))) }, confirmButton = { TrashPilotTextButton(stringResource(android.R.string.ok), { resultMessage = null }) }) }
}

@Composable private fun ScreenshotContent(items: List<ScreenshotItem>, scanning: Boolean, progress: ScreenshotScanProgress?, partial: Boolean, query: String, onQuery: (String) -> Unit, sort: ScreenshotSort, onSort: (ScreenshotSort) -> Unit, selected: Set<String>, onToggle: (ScreenshotItem) -> Unit, onSelectAll: (List<ScreenshotItem>) -> Unit, onClear: () -> Unit, onDetail: (ScreenshotItem) -> Unit, onDelete: () -> Unit, onCancel: () -> Unit, onScanAgain: () -> Unit, modifier: Modifier) {
    val visible = remember(items, query, sort) { items.screenshotsView(query, sort) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(TrashPilotSpacing.Screen), verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)) {
        if (scanning) item { TrashPilotCard(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) { Column(Modifier.fillMaxWidth().padding(TrashPilotSpacing.Card), horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(); Text(stringResource(R.string.screenshots_scanning), style = MaterialTheme.typography.titleMedium); Text(stringResource(R.string.screenshots_progress, progress?.inspectedImages ?: 0), style = MaterialTheme.typography.bodySmall); TrashPilotTextButton(stringResource(R.string.screenshots_cancel_scan), onCancel) } } }
        item { TrashPilotCard(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)) { Column(Modifier.fillMaxWidth().padding(TrashPilotSpacing.Card), verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Compact)) { Text(stringResource(R.string.screenshots_count_size, items.size, formatBytes(items.sumOf(ScreenshotItem::sizeBytes))), style = MaterialTheme.typography.titleMedium); Text(stringResource(R.string.screenshots_selected_summary, selected.size, formatBytes(selectedScreenshotBytes(items, selected)))); if (partial) Text(stringResource(R.string.screenshots_partial_access), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
        item { OutlinedTextField(query, onQuery, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.screenshots_search)) }, singleLine = true, shape = TrashPilotRadii.ControlShape) }
        item { Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Medium)) { ScreenshotSort.entries.forEach { option -> FilterChip(option == sort, { onSort(option) }, label = { Text(stringResource(option.label())) }) } } }
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.screenshots_items), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)); TrashPilotTextButton(stringResource(R.string.screenshots_select_all), { onSelectAll(visible) }); if (selected.isNotEmpty()) TrashPilotTextButton(stringResource(R.string.screenshots_clear), onClear) } }
        if (!scanning && visible.isEmpty()) item { TrashPilotEmptyState(stringResource(R.string.screenshots_empty_title), stringResource(R.string.screenshots_empty_body), Modifier.fillMaxWidth(), stringResource(R.string.screenshots_scan_again), onScanAgain) }
        items(visible, key = ScreenshotItem::uri) { item -> ScreenshotRow(item, item.uri in selected, { onToggle(item) }, { onDetail(item) }) }
        item { TrashPilotPrimaryButton(stringResource(R.string.screenshots_delete_selected), onDelete, Modifier.fillMaxWidth(), enabled = selected.isNotEmpty()) }
    }
}

@Composable private fun ScreenshotRow(item: ScreenshotItem, selected: Boolean, onToggle: () -> Unit, onDetail: () -> Unit) { TrashPilotCard(Modifier.fillMaxWidth().clickable(onClick = onDetail), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)) { Row(Modifier.fillMaxWidth().padding(TrashPilotSpacing.HomeCard), verticalAlignment = Alignment.CenterVertically) { ScreenshotThumbnail(item, Modifier.size(TrashPilotComponentSizes.CardIconContainer)); Column(Modifier.weight(1f).padding(horizontal = TrashPilotSpacing.Standard)) { Text(item.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(stringResource(R.string.screenshots_folder_size, item.folderName.ifBlank { stringResource(R.string.screenshots_folder_unknown) }, formatBytes(item.sizeBytes)), style = MaterialTheme.typography.bodyMedium); Text(item.dateText(), style = MaterialTheme.typography.bodySmall) }; Checkbox(selected, { onToggle() }) } } }

@Composable private fun ScreenshotDetailDialog(item: ScreenshotItem, onDismiss: () -> Unit) { AlertDialog(onDismissRequest = onDismiss, title = { Text(item.name, maxLines = 2, overflow = TextOverflow.Ellipsis) }, text = { Column(verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)) { ScreenshotThumbnail(item, Modifier.fillMaxWidth().aspectRatio(1f)); Text(stringResource(R.string.screenshots_detail_size, formatBytes(item.sizeBytes))); Text(stringResource(R.string.screenshots_detail_date, item.dateText())); Text(stringResource(R.string.screenshots_detail_dimensions, item.dimensionsText())); Text(stringResource(R.string.screenshots_detail_folder, item.folderName.ifBlank { stringResource(R.string.screenshots_folder_unknown) })) } }, confirmButton = { TrashPilotTextButton(stringResource(R.string.screenshots_close), onDismiss) }) }

@Composable private fun ScreenshotThumbnail(item: ScreenshotItem, modifier: Modifier) { val context = LocalContext.current; val bitmap by produceState<Bitmap?>(null, item.uri) { value = withContext(Dispatchers.IO) { runCatching { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) context.contentResolver.loadThumbnail(item.uri.toUri(), Size(512, 512), null) else { @Suppress("DEPRECATION") MediaStore.Images.Thumbnails.getThumbnail(context.contentResolver, ContentUris.parseId(item.uri.toUri()), MediaStore.Images.Thumbnails.MINI_KIND, null) } }.getOrNull() } }; Surface(modifier, shape = TrashPilotRadii.IconContainerShape, color = MaterialTheme.colorScheme.secondaryContainer) { if (bitmap != null) Image(bitmap!!.asImageBitmap(), stringResource(R.string.screenshots_thumbnail_description, item.name), Modifier.fillMaxSize(), contentScale = ContentScale.Crop) else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.BrokenImage, contentDescription = stringResource(R.string.screenshots_thumbnail_unavailable)) } } }

@Composable private fun ScreenshotItem.dateText() = timestampMillis.takeIf { it > 0 }?.let { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it)) } ?: stringResource(R.string.results_date_unknown)
@Composable private fun ScreenshotItem.dimensionsText() = if (width != null && height != null) stringResource(R.string.screenshots_dimensions_value, width, height) else stringResource(R.string.screenshots_dimensions_unknown)
private fun ScreenshotItem.toScannedFile() = ScannedFile(name, sizeBytes, timestampMillis, uri, FileCategory.IMAGES, relativePath)
private fun ScreenshotSort.label() = when (this) { ScreenshotSort.NEWEST -> R.string.screenshots_sort_newest; ScreenshotSort.OLDEST -> R.string.screenshots_sort_oldest; ScreenshotSort.LARGEST -> R.string.screenshots_sort_largest; ScreenshotSort.SMALLEST -> R.string.screenshots_sort_smallest }
private fun imagePermission() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
private fun screenshotPermissions() = buildList { add(imagePermission()); if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) }.toTypedArray()
private sealed interface ScreenshotState { data object Preparing : ScreenshotState; data class Scanning(val progress: ScreenshotScanProgress) : ScreenshotState; data object Complete : ScreenshotState; data object PermissionDenied : ScreenshotState; data object StorageUnavailable : ScreenshotState; data object Cancelled : ScreenshotState }
