package com.trashpilot.app.features.cache

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.core.graphics.drawable.toBitmap
import com.trashpilot.app.R
import com.trashpilot.app.core.cache.*
import com.trashpilot.app.core.storage.formatBytes
import com.trashpilot.app.ui.components.*
import com.trashpilot.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

@Composable
fun RealCacheAnalyzerScreen(
    onBack: () -> Unit,
    onCacheScan: (CacheSnapshot) -> Unit,
    onCacheCleaned: (CacheCleaningReport) -> Unit
) {
    val context = LocalContext.current
    val analyzer = remember(context) { RealCacheAnalyzer(context.applicationContext) }
    val cleaner = remember(context) { OwnCacheCleaner(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var runId by rememberSaveable { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<CacheUiState>(CacheUiState.Checking) }
    var selected by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(CacheSort.LARGEST) }
    var manualQueue by remember { mutableStateOf<List<CacheApp>>(emptyList()) }
    var manualDialog by remember { mutableStateOf(false) }
    var verification by remember { mutableStateOf<Pair<String, Long>?>(null) }

    val usageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        runId += 1
    }
    val appInfoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        runId += 1
    }

    LaunchedEffect(runId) {
        state = when (analyzer.capability()) {
            CacheCapability.USAGE_ACCESS_REQUIRED -> CacheUiState.UsageAccessRequired
            CacheCapability.UNSUPPORTED_ANDROID_VERSION -> CacheUiState.Unsupported
            CacheCapability.AVAILABLE -> try {
                val snapshot = analyzer.scan { state = CacheUiState.Scanning(it) }
                onCacheScan(snapshot)
                verification?.let { (packageName, before) ->
                    val after = snapshot.apps.firstOrNull { it.packageName == packageName }?.cacheBytes
                    if (after != null && before > after) {
                        onCacheCleaned(CacheCleaningReport(before - after, 1))
                    }
                    verification = null
                    manualQueue = manualQueue.drop(1)
                }
                CacheUiState.Ready(snapshot)
            } catch (_: SecurityException) {
                CacheUiState.UsageAccessRequired
            } catch (_: Exception) {
                CacheUiState.Error
            }
        }
    }

    Scaffold(
        topBar = { TrashPilotTopAppBar(stringResource(R.string.results_label_app_cache), onBack = onBack) }
    ) { padding ->
        when (val current = state) {
            CacheUiState.Checking -> Unit
            CacheUiState.UsageAccessRequired -> CacheState(
                R.string.cache_usage_title, R.string.cache_usage_body,
                R.string.cache_open_usage_access,
                { usageLauncher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }, padding
            )
            CacheUiState.Unsupported -> CacheState(
                R.string.cache_unsupported_title, R.string.cache_unsupported_body, null, null, padding
            )
            CacheUiState.Error -> CacheState(
                R.string.cache_error_title, R.string.cache_error_body,
                R.string.cache_scan_again, { runId += 1 }, padding
            )
            is CacheUiState.Scanning -> CacheScanning(current.progress, Modifier.padding(padding))
            is CacheUiState.Ready -> {
                val knownPositive = current.snapshot.apps.filter { (it.cacheBytes ?: 0) > 0 }
                if (knownPositive.isEmpty() && current.snapshot.apps.all { it.cacheBytes != null }) {
                    TrashPilotEmptyState(
                        title = stringResource(R.string.cache_empty_title),
                        body = stringResource(R.string.cache_empty_body),
                        actionText = stringResource(R.string.cache_scan_again),
                        onAction = { runId += 1 },
                        modifier = Modifier.fillMaxSize().padding(padding).padding(TrashPilotSpacing.Screen)
                    )
                } else CacheResults(
                    snapshot = current.snapshot,
                    query = query,
                    onQuery = { query = it },
                    sort = sort,
                    onSort = { sort = it },
                    selected = selected,
                    onToggle = { packageName ->
                        selected = if (packageName in selected) selected - packageName else selected + packageName
                    },
                    onSelectAll = {
                        val selectable = current.snapshot.apps.filter { (it.cacheBytes ?: 0) > 0 }
                            .mapTo(mutableSetOf()) { it.packageName }
                        selected = if (selected.containsAll(selectable)) emptySet() else selectable
                    },
                    onClean = {
                        scope.launch {
                            val chosen = current.snapshot.apps.filter { it.packageName in selected }
                            if (chosen.any { it.packageName == context.packageName }) {
                                val report = cleaner.clean()
                                if (report.cleanedBytes > 0) onCacheCleaned(report)
                            }
                            manualQueue = chosen.filterNot { it.packageName == context.packageName }
                            selected = emptySet()
                            if (manualQueue.isNotEmpty()) manualDialog = true else runId += 1
                        }
                    },
                    manualRemaining = manualQueue.size,
                    onContinueManual = { manualDialog = true },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }

    if (manualDialog && manualQueue.isNotEmpty()) {
        val app = manualQueue.first()
        AlertDialog(
            onDismissRequest = { manualDialog = false },
            title = { Text(stringResource(R.string.cache_manual_title, app.label)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)) {
                    Text(stringResource(R.string.cache_manual_body))
                    Text(stringResource(R.string.cache_manual_step), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TrashPilotTextButton(stringResource(android.R.string.cancel), { manualDialog = false })
            },
            confirmButton = {
                TrashPilotTextButton(stringResource(R.string.cache_open_app_info), {
                    manualDialog = false
                    verification = app.packageName to (app.cacheBytes ?: 0)
                    appInfoLauncher.launch(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", app.packageName, null)
                        }
                    )
                })
            }
        )
    }
}

@Composable private fun CacheState(
    title: Int, body: Int, action: Int?, onAction: (() -> Unit)?, padding: PaddingValues
) = TrashPilotErrorState(
    title = stringResource(title), body = stringResource(body),
    actionText = action?.let { stringResource(it) }, onAction = onAction,
    modifier = Modifier.fillMaxSize().padding(padding).padding(TrashPilotSpacing.Screen)
)

@Composable private fun CacheScanning(progress: CacheScanProgress, modifier: Modifier) {
    Column(
        modifier.fillMaxSize().padding(TrashPilotSpacing.Screen),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            progress = { if (progress.totalApps == 0) 0f else progress.processedApps.toFloat() / progress.totalApps }
        )
        Spacer(Modifier.height(TrashPilotSpacing.Large))
        Text(stringResource(R.string.cache_scanning_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.cache_scan_progress, progress.processedApps, progress.totalApps))
    }
}

@Composable private fun CacheResults(
    snapshot: CacheSnapshot,
    query: String,
    onQuery: (String) -> Unit,
    sort: CacheSort,
    onSort: (CacheSort) -> Unit,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClean: () -> Unit,
    manualRemaining: Int,
    onContinueManual: () -> Unit,
    modifier: Modifier
) {
    val apps = remember(snapshot, query, sort) { snapshot.apps.filteredAndSorted(query, sort) }
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(TrashPilotSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)
    ) {
        item {
            TrashPilotCard(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(TrashPilotSpacing.Card)) {
                    Text(stringResource(R.string.cache_total), style = MaterialTheme.typography.bodyMedium)
                    Text(formatBytes(snapshot.totalCacheBytes), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.cache_last_scan, DateFormat.getDateTimeInstance().format(Date(snapshot.timestampMillis))),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (manualRemaining > 0) item {
            TrashPilotInfoCard(
                title = stringResource(R.string.cache_manual_remaining, manualRemaining),
                body = stringResource(R.string.cache_manual_step)
            )
            TrashPilotTextButton(stringResource(R.string.cache_continue_app_info), onContinueManual)
        }
        item {
            OutlinedTextField(
                value = query, onValueChange = onQuery,
                label = { Text(stringResource(R.string.cache_search)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = TrashPilotRadii.ControlShape
            )
        }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Medium)) {
                CacheSort.entries.forEach { option ->
                    FilterChip(
                        selected = sort == option, onClick = { onSort(option) },
                        label = { Text(stringResource(option.label())) }
                    )
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.cache_apps), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TrashPilotTextButton(stringResource(R.string.cache_select_all), onSelectAll)
            }
        }
        items(apps, key = CacheApp::packageName) { app ->
            CacheAppRow(app, app.packageName in selected) { onToggle(app.packageName) }
        }
        item {
            TrashPilotPrimaryButton(
                stringResource(R.string.cache_clear_selected), onClean,
                Modifier.fillMaxWidth(), enabled = selected.isNotEmpty()
            )
        }
    }
}

@Composable private fun CacheAppRow(app: CacheApp, selected: Boolean, onToggle: () -> Unit) {
    val selectable = (app.cacheBytes ?: 0) > 0
    TrashPilotCard(
        modifier = Modifier.fillMaxWidth().clickable(enabled = selectable, onClick = onToggle),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(Modifier.fillMaxWidth().padding(TrashPilotSpacing.HomeCard), verticalAlignment = Alignment.CenterVertically) {
            CacheAppIcon(app.packageName, app.label)
            Column(Modifier.weight(1f).padding(horizontal = TrashPilotSpacing.Standard)) {
                Text(app.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    app.cacheBytes?.let(::formatBytes) ?: stringResource(R.string.cache_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Checkbox(checked = selected, onCheckedChange = if (selectable) {{ onToggle() }} else null, enabled = selectable)
        }
    }
}

@Composable private fun CacheAppIcon(packageName: String, label: String) {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(null, packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching { context.packageManager.getApplicationIcon(packageName).toBitmap(96, 96) }.getOrNull()
        }
    }
    Surface(
        modifier = Modifier.size(TrashPilotComponentSizes.CardIconContainer),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        bitmap?.let { Image(it.asImageBitmap(), label, Modifier.fillMaxSize()) }
    }
}

private fun CacheSort.label() = when (this) {
    CacheSort.LARGEST -> R.string.cache_sort_largest
    CacheSort.APP_NAME -> R.string.cache_sort_name
    CacheSort.RECENTLY_UPDATED -> R.string.cache_sort_recent
}

private sealed interface CacheUiState {
    data object Checking : CacheUiState
    data object UsageAccessRequired : CacheUiState
    data object Unsupported : CacheUiState
    data object Error : CacheUiState
    data class Scanning(val progress: CacheScanProgress) : CacheUiState
    data class Ready(val snapshot: CacheSnapshot) : CacheUiState
}
