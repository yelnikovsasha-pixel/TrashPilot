package com.trashpilot.app.features.photoquality

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
import com.trashpilot.app.core.photoquality.*
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
fun PhotoQualityAnalyzerScreen(onBack: () -> Unit, onDeleted: (Set<String>, DuplicateCleaningReport) -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { PhotoQualityRepository(context.contentResolver) }
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<AnalyzerState>(AnalyzerState.Preparing) }
    var result by remember { mutableStateOf(PhotoAnalysisResult(0, emptyList(), 0)) }
    var partialAccess by rememberSaveable { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf(PhotoQualityFilter.ALL) }
    var sort by rememberSaveable { mutableStateOf(PhotoQualitySort.LARGEST) }
    var selected by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var detail by remember { mutableStateOf<PhotoQualityItem?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf(emptyList<PhotoQualityItem>()) }
    var deletionResult by remember { mutableStateOf<PhotoDeletionAccounting?>(null) }
    var analysisJob by remember { mutableStateOf<Job?>(null) }

    fun analyze(isPartial: Boolean) {
        analysisJob?.cancel(); selected = emptySet(); partialAccess = isPartial; result = PhotoAnalysisResult(0, emptyList(), 0)
        state = AnalyzerState.Analyzing(PhotoAnalysisProgress(0, 0, 0))
        analysisJob = scope.launch {
            try {
                val progressive = mutableListOf<PhotoQualityItem>()
                result = repository.analyze { item, progress ->
                    item?.let(progressive::add)
                    result = PhotoAnalysisResult(progress.analyzed, progressive.toList(), 0)
                    state = AnalyzerState.Analyzing(progress)
                }
                state = AnalyzerState.Complete
            } catch (_: CancellationException) { state = AnalyzerState.Cancelled }
            catch (_: SecurityException) { state = AnalyzerState.PermissionDenied }
            catch (_: Exception) { state = AnalyzerState.StorageUnavailable }
        }
    }

    fun verifyDeletion(requested: List<PhotoQualityItem>) {
        scope.launch {
            val deletedUris = withContext(Dispatchers.IO) { requested.filter { item -> runCatching {
                context.contentResolver.query(item.uri.toUri(), arrayOf(MediaStore.Images.Media._ID), null, null, null)?.use { !it.moveToFirst() } ?: false
            }.getOrDefault(false) }.mapTo(hashSetOf(), PhotoQualityItem::uri) }
            val accounting = accountPhotoDeletion(requested, deletedUris)
            deletionResult = accounting; selected = emptySet()
            if (accounting.deleted.isNotEmpty()) onDeleted(deletedUris, DuplicateCleaningReport(accounting.deleted.map { it.toScannedFile() }, accounting.failed.map { it.toScannedFile() }))
            analyze(partialAccess)
        }
    }

    val deleteConsent = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { response ->
        val requested = pendingDelete; pendingDelete = emptyList()
        if (response.resultCode == Activity.RESULT_OK) verifyDeletion(requested) else deletionResult = accountPhotoDeletion(requested, emptySet())
    }
    fun delete(items: List<PhotoQualityItem>) {
        if (items.isEmpty()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pendingDelete = items
            runCatching { MediaStore.createDeleteRequest(context.contentResolver, items.map { it.uri.toUri() }) }
                .onSuccess { deleteConsent.launch(IntentSenderRequest.Builder(it.intentSender).build()) }
                .onFailure { pendingDelete = emptyList(); deletionResult = accountPhotoDeletion(items, emptySet()) }
        } else scope.launch {
            withContext(Dispatchers.IO) { items.forEach { runCatching { context.contentResolver.delete(it.uri.toUri(), null, null) } } }
            verifyDeletion(items)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val full = grants[photoPermission()] == true || ContextCompat.checkSelfPermission(context, photoPermission()) == PackageManager.PERMISSION_GRANTED
        val partial = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && (grants[Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] == true || ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED)
        if (full || partial) analyze(!full && partial) else state = AnalyzerState.PermissionDenied
    }
    LaunchedEffect(Unit) {
        val full = ContextCompat.checkSelfPermission(context, photoPermission()) == PackageManager.PERMISSION_GRANTED
        val partial = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
        if (full || partial) analyze(!full && partial) else permissionLauncher.launch(photoPermissions())
    }

    Scaffold(topBar = { TrashPilotTopAppBar(stringResource(R.string.photo_quality_title), onBack = onBack) }) { padding ->
        when (val current = state) {
            AnalyzerState.Preparing -> TrashPilotLoadingState(stringResource(R.string.photo_quality_preparing), stringResource(R.string.photo_quality_transparency), Modifier.fillMaxSize().padding(padding))
            AnalyzerState.PermissionDenied -> TrashPilotErrorState(stringResource(R.string.photo_quality_permission_title), stringResource(R.string.photo_quality_permission_body), Modifier.fillMaxSize().padding(padding).padding(TrashPilotSpacing.Screen), stringResource(R.string.photo_quality_try_again), { permissionLauncher.launch(photoPermissions()) })
            AnalyzerState.StorageUnavailable -> TrashPilotErrorState(stringResource(R.string.photo_quality_storage_title), stringResource(R.string.photo_quality_storage_body), Modifier.fillMaxSize().padding(padding).padding(TrashPilotSpacing.Screen), stringResource(R.string.photo_quality_analyze_again), { analyze(partialAccess) })
            AnalyzerState.Cancelled -> TrashPilotEmptyState(stringResource(R.string.photo_quality_cancelled_title), stringResource(R.string.photo_quality_cancelled_body), Modifier.fillMaxSize().padding(padding).padding(TrashPilotSpacing.Screen), stringResource(R.string.photo_quality_analyze_again), { analyze(partialAccess) })
            else -> AnalyzerContent(result, current as? AnalyzerState.Analyzing, partialAccess, filter, { filter = it; selected = emptySet() }, sort, { sort = it }, selected, { selected = togglePhotoSelection(selected, it.uri) }, { visible -> selected = selected + visible.map(PhotoQualityItem::uri) }, { selected = emptySet() }, { detail = it }, { confirmDelete = true }, { analysisJob?.cancel() }, { analyze(partialAccess) }, Modifier.padding(padding))
        }
    }
    if (confirmDelete) {
        val items = result.flagged.filter { it.uri in selected }
        AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text(stringResource(R.string.photo_quality_delete_title)) }, text = { Column(verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)) { Text(stringResource(R.string.photo_quality_delete_body)); Text(stringResource(R.string.photo_quality_delete_summary, items.size, formatBytes(items.sumOf(PhotoQualityItem::sizeBytes))), fontWeight = FontWeight.SemiBold) } }, dismissButton = { TrashPilotTextButton(stringResource(android.R.string.cancel), { confirmDelete = false }) }, confirmButton = { TrashPilotTextButton(stringResource(R.string.photo_quality_delete), { confirmDelete = false; delete(items) }) })
    }
    detail?.let { PhotoDetailDialog(it) { detail = null } }
    deletionResult?.let { report -> AlertDialog(onDismissRequest = { deletionResult = null }, title = { Text(stringResource(R.string.photo_quality_result_title)) }, text = { Text(stringResource(R.string.photo_quality_result_body, report.deleted.size, report.failed.size, formatBytes(report.reclaimedBytes))) }, confirmButton = { TrashPilotTextButton(stringResource(android.R.string.ok), { deletionResult = null }) }) }
}

@Composable private fun AnalyzerContent(result: PhotoAnalysisResult, analyzing: AnalyzerState.Analyzing?, partial: Boolean, filter: PhotoQualityFilter, onFilter: (PhotoQualityFilter) -> Unit, sort: PhotoQualitySort, onSort: (PhotoQualitySort) -> Unit, selected: Set<String>, onToggle: (PhotoQualityItem) -> Unit, onSelectAll: (List<PhotoQualityItem>) -> Unit, onClear: () -> Unit, onDetail: (PhotoQualityItem) -> Unit, onDelete: () -> Unit, onCancel: () -> Unit, onAgain: () -> Unit, modifier: Modifier) {
    val visible = remember(result.flagged, filter, sort) { result.flagged.photoQualityView(filter, sort) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(TrashPilotSpacing.Screen), verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)) {
        if (analyzing != null) item { TrashPilotCard(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) { Column(Modifier.fillMaxWidth().padding(TrashPilotSpacing.Card), horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(); Text(stringResource(R.string.photo_quality_analyzing), style = MaterialTheme.typography.titleMedium); Text(stringResource(R.string.photo_quality_progress, analyzing.progress.analyzed, analyzing.progress.total)); TrashPilotTextButton(stringResource(R.string.photo_quality_cancel), onCancel) } } }
        item { TrashPilotCard(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)) { Column(Modifier.fillMaxWidth().padding(TrashPilotSpacing.Card), verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Compact)) { Text(stringResource(R.string.photo_quality_summary, result.analyzedCount, result.flagged.size, formatBytes(result.flagged.sumOf(PhotoQualityItem::sizeBytes))), style = MaterialTheme.typography.titleMedium); Text(stringResource(R.string.photo_quality_selected, selected.size, formatBytes(selectedPhotoBytes(result.flagged, selected)))); Text(stringResource(R.string.photo_quality_transparency), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); if (partial) Text(stringResource(R.string.photo_quality_partial), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
        item { Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Medium)) { PhotoQualityFilter.entries.forEach { option -> FilterChip(option == filter, { onFilter(option) }, label = { Text(stringResource(option.label())) }) } } }
        item { Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Medium)) { PhotoQualitySort.entries.forEach { option -> FilterChip(option == sort, { onSort(option) }, label = { Text(stringResource(option.label())) }) } } }
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.photo_quality_candidates), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)); TrashPilotTextButton(stringResource(R.string.photo_quality_select_all), { onSelectAll(visible) }); if (selected.isNotEmpty()) TrashPilotTextButton(stringResource(R.string.photo_quality_clear), onClear) } }
        if (analyzing == null && visible.isEmpty()) item { TrashPilotEmptyState(if (result.analyzedCount == 0) stringResource(R.string.photo_quality_no_photos) else stringResource(R.string.photo_quality_no_candidates), if (result.analyzedCount == 0) stringResource(R.string.photo_quality_no_photos_body) else stringResource(R.string.photo_quality_no_candidates_body), Modifier.fillMaxWidth(), stringResource(R.string.photo_quality_analyze_again), onAgain) }
        items(visible, key = PhotoQualityItem::uri) { item -> PhotoRow(item, item.uri in selected, { onToggle(item) }, { onDetail(item) }) }
        item { TrashPilotPrimaryButton(stringResource(R.string.photo_quality_delete_selected), onDelete, Modifier.fillMaxWidth(), enabled = selected.isNotEmpty()) }
    }
}

@Composable private fun PhotoRow(item: PhotoQualityItem, selected: Boolean, onToggle: () -> Unit, onDetail: () -> Unit) { TrashPilotCard(Modifier.fillMaxWidth().clickable(onClick = onDetail), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)) { Row(Modifier.fillMaxWidth().padding(TrashPilotSpacing.HomeCard), verticalAlignment = Alignment.CenterVertically) { PhotoThumbnail(item, Modifier.size(TrashPilotComponentSizes.CardIconContainer)); Column(Modifier.weight(1f).padding(horizontal = TrashPilotSpacing.Standard)) { Text(item.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(item.reasonText(), style = MaterialTheme.typography.bodyMedium); Text(stringResource(R.string.photo_quality_size_date, formatBytes(item.sizeBytes), item.dateText()), style = MaterialTheme.typography.bodySmall) }; Checkbox(selected, { onToggle() }) } } }
@Composable private fun PhotoDetailDialog(item: PhotoQualityItem, onDismiss: () -> Unit) { AlertDialog(onDismissRequest = onDismiss, title = { Text(item.name, maxLines = 2, overflow = TextOverflow.Ellipsis) }, text = { Column(verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)) { PhotoThumbnail(item, Modifier.fillMaxWidth().aspectRatio(1f)); Text(stringResource(R.string.photo_quality_detail_size, formatBytes(item.sizeBytes))); Text(stringResource(R.string.photo_quality_detail_dimensions, item.width, item.height)); Text(stringResource(R.string.photo_quality_detail_date, item.dateText())); item.reasons.forEach { Text(stringResource(it.explanation())) }; Text(stringResource(R.string.photo_quality_heuristic_note), style = MaterialTheme.typography.bodySmall) } }, confirmButton = { TrashPilotTextButton(stringResource(R.string.photo_quality_close), onDismiss) }) }
@Composable private fun PhotoThumbnail(item: PhotoQualityItem, modifier: Modifier) { val context = LocalContext.current; val bitmap by produceState<Bitmap?>(null, item.uri) { value = withContext(Dispatchers.IO) { runCatching { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) context.contentResolver.loadThumbnail(item.uri.toUri(), Size(512, 512), null) else { @Suppress("DEPRECATION") MediaStore.Images.Thumbnails.getThumbnail(context.contentResolver, ContentUris.parseId(item.uri.toUri()), MediaStore.Images.Thumbnails.MINI_KIND, null) } }.getOrNull() } }; Surface(modifier, shape = TrashPilotRadii.IconContainerShape, color = MaterialTheme.colorScheme.secondaryContainer) { if (bitmap != null) Image(bitmap!!.asImageBitmap(), stringResource(R.string.photo_quality_thumbnail_description, item.name), Modifier.fillMaxSize(), contentScale = ContentScale.Crop) else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.BrokenImage, stringResource(R.string.photo_quality_thumbnail_unavailable)) } } }
@Composable private fun PhotoQualityItem.dateText() = timestampMillis.takeIf { it > 0 }?.let { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it)) } ?: stringResource(R.string.results_date_unknown)
@Composable private fun PhotoQualityItem.reasonText(): String {
    val labels = mutableListOf<String>()
    for (reason in reasons) labels += stringResource(reason.label())
    return labels.joinToString(" · ")
}
private fun PhotoQualityItem.toScannedFile() = ScannedFile(name, sizeBytes, timestampMillis, uri, FileCategory.IMAGES, relativePath)
private fun PhotoReason.label() = when (this) { PhotoReason.LOW_RESOLUTION -> R.string.photo_quality_low_resolution; PhotoReason.POSSIBLY_BLURRY -> R.string.photo_quality_blurry; PhotoReason.VERY_DARK -> R.string.photo_quality_dark; PhotoReason.VERY_BRIGHT -> R.string.photo_quality_bright }
private fun PhotoReason.explanation() = when (this) { PhotoReason.LOW_RESOLUTION -> R.string.photo_quality_low_resolution_explanation; PhotoReason.POSSIBLY_BLURRY -> R.string.photo_quality_blurry_explanation; PhotoReason.VERY_DARK -> R.string.photo_quality_dark_explanation; PhotoReason.VERY_BRIGHT -> R.string.photo_quality_bright_explanation }
private fun PhotoQualityFilter.label() = when (this) { PhotoQualityFilter.ALL -> R.string.photo_quality_all; PhotoQualityFilter.LOW_RESOLUTION -> R.string.photo_quality_low_resolution; PhotoQualityFilter.POSSIBLY_BLURRY -> R.string.photo_quality_blurry; PhotoQualityFilter.VERY_DARK -> R.string.photo_quality_dark; PhotoQualityFilter.VERY_BRIGHT -> R.string.photo_quality_bright }
private fun PhotoQualitySort.label() = when (this) { PhotoQualitySort.LARGEST -> R.string.photo_quality_sort_largest; PhotoQualitySort.SMALLEST -> R.string.photo_quality_sort_smallest; PhotoQualitySort.NEWEST -> R.string.photo_quality_sort_newest; PhotoQualitySort.OLDEST -> R.string.photo_quality_sort_oldest }
private fun photoPermission() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
private fun photoPermissions() = buildList { add(photoPermission()); if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) }.toTypedArray()
private sealed interface AnalyzerState { data object Preparing : AnalyzerState; data class Analyzing(val progress: PhotoAnalysisProgress) : AnalyzerState; data object Complete : AnalyzerState; data object PermissionDenied : AnalyzerState; data object StorageUnavailable : AnalyzerState; data object Cancelled : AnalyzerState }
