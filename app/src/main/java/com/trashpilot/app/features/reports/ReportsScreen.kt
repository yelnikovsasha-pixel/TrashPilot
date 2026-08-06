package com.trashpilot.app.features.reports

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.trashpilot.app.R
import com.trashpilot.app.core.reports.ReportsAnalyzer
import com.trashpilot.app.core.reports.ReportsSummary
import com.trashpilot.app.core.reports.ScanReport
import com.trashpilot.app.core.storage.FileCategory
import com.trashpilot.app.core.storage.formatBytes
import com.trashpilot.app.core.trashdna.TrashDnaRepository
import com.trashpilot.app.ui.components.TrashPilotCard
import com.trashpilot.app.ui.components.TrashPilotPrimaryButton
import com.trashpilot.app.ui.components.TrashPilotTextButton
import com.trashpilot.app.ui.components.TrashPilotTopAppBar
import com.trashpilot.app.ui.theme.TrashPilotColors
import com.trashpilot.app.ui.theme.TrashPilotComponentSizes
import com.trashpilot.app.ui.theme.TrashPilotRadii
import com.trashpilot.app.ui.theme.TrashPilotSpacing
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

private sealed interface ReportsUiState {
    data object Loading : ReportsUiState
    data class Success(val summary: ReportsSummary?) : ReportsUiState
    data object Error : ReportsUiState
}

@Composable
fun ReportsScreen(
    repository: TrashDnaRepository,
    onBack: () -> Unit,
    onScanNow: () -> Unit
) {
    var reloadKey by remember { mutableIntStateOf(0) }
    var state: ReportsUiState by remember { mutableStateOf(ReportsUiState.Loading) }
    var selectedScanId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showClearDialog by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(repository, reloadKey) {
        state = ReportsUiState.Loading
        state = runCatching { ReportsAnalyzer.summarize(repository.loadReportHistory()) }
            .fold({ ReportsUiState.Success(it) }, { ReportsUiState.Error })
    }
    val selectedReport = (state as? ReportsUiState.Success)?.summary?.scans
        ?.firstOrNull { it.id == selectedScanId }
    BackHandler {
        if (selectedScanId != null) selectedScanId = null else onBack()
    }

    Column(Modifier.fillMaxSize()) {
        TrashPilotTopAppBar(
            title = stringResource(if (selectedScanId == null) R.string.reports_title else R.string.reports_detail_title),
            onBack = { if (selectedScanId != null) selectedScanId = null else onBack() }
        )
        when (val current = state) {
            ReportsUiState.Loading -> CenterState(stringResource(R.string.reports_loading), loading = true)
            ReportsUiState.Error -> CenterState(
                stringResource(R.string.reports_error_title),
                action = stringResource(R.string.trash_dna_retry),
                onAction = { reloadKey++ }
            )
            is ReportsUiState.Success -> when {
                current.summary == null -> EmptyReports(onScanNow)
                selectedReport != null -> ReportDetail(selectedReport)
                else -> ReportsOverview(current.summary, onSelect = { selectedScanId = it.id },
                    onClear = { showClearDialog = true })
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.reports_clear_confirm_title)) },
            text = { Text(stringResource(R.string.reports_clear_confirm_body)) },
            confirmButton = {
                TrashPilotTextButton(text = stringResource(R.string.reports_delete), onClick = {
                    showClearDialog = false
                    state = ReportsUiState.Loading
                    scope.launch {
                        runCatching { repository.clearReportHistory() }
                            .onSuccess { selectedScanId = null; reloadKey++ }
                            .onFailure { state = ReportsUiState.Error }
                    }
                })
            },
            dismissButton = {
                TrashPilotTextButton(text = stringResource(R.string.quick_clean_cancel),
                    onClick = { showClearDialog = false })
            }
        )
    }
}

@Composable
private fun EmptyReports(onScanNow: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(TrashPilotSpacing.Screen), contentAlignment = Alignment.Center) {
        ReportCard(highlighted = true) {
            Text(stringResource(R.string.reports_empty_title), style = MaterialTheme.typography.titleLarge)
            SupportingText(stringResource(R.string.reports_empty_body))
            TrashPilotPrimaryButton(
                text = stringResource(R.string.reports_scan_now),
                onClick = onScanNow,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = TrashPilotColors.HomeBlue,
                    contentColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun ReportsOverview(
    summary: ReportsSummary,
    onSelect: (ScanReport) -> Unit,
    onClear: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(TrashPilotSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)
    ) {
        item { SummaryCard(summary) }
        item { CleanedChart(summary.scans.sortedBy { it.timestampMillis }) }
        item { Text(stringResource(R.string.reports_timeline), style = MaterialTheme.typography.titleMedium) }
        items(summary.scans, key = ScanReport::id) { report -> TimelineCard(report) { onSelect(report) } }
        item {
            ReportCard {
                Text(stringResource(R.string.reports_clear_history), style = MaterialTheme.typography.titleSmall)
                SupportingText(stringResource(R.string.reports_clear_body))
                TrashPilotTextButton(text = stringResource(R.string.reports_clear_history), onClick = onClear)
            }
        }
    }
}

@Composable
private fun SummaryCard(summary: ReportsSummary) = ReportCard(highlighted = true) {
    Text(stringResource(R.string.reports_summary_title), style = MaterialTheme.typography.titleMedium)
    MetricRow(stringResource(R.string.reports_total_scans), summary.totalScans.toString())
    MetricRow(stringResource(R.string.reports_total_analyzed),
        summary.totalAnalyzedBytes?.let(::formatBytes) ?: stringResource(R.string.reports_not_recorded))
    MetricRow(stringResource(R.string.reports_total_cleaned), formatBytes(summary.totalCleanedBytes))
    MetricRow(stringResource(R.string.reports_last_scan_date), formatDate(summary.lastScanMillis))
    MetricRow(stringResource(R.string.reports_average_cleaned), formatBytes(summary.averageCleanedPerScanBytes))
}

@Composable
private fun CleanedChart(scans: List<ScanReport>) = ReportCard {
    Text(stringResource(R.string.reports_cleaned_chart), style = MaterialTheme.typography.titleMedium)
    val maximum = scans.maxOfOrNull { it.cleanedBytes } ?: 0
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(formatBytes(maximum), style = MaterialTheme.typography.labelSmall)
        Text(stringResource(R.string.reports_storage_cleaned_axis), style = MaterialTheme.typography.labelSmall)
    }
    val chartColor = TrashPilotColors.HomeBlue
    val outline = MaterialTheme.colorScheme.outline
    Canvas(Modifier.fillMaxWidth().height(TrashPilotComponentSizes.ReportChartHeight)) {
        drawLine(outline, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2f)
        drawLine(outline, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 2f)
        val slot = size.width / scans.size.coerceAtLeast(1)
        scans.forEachIndexed { index, report ->
            val ratio = if (maximum == 0L) 0f else report.cleanedBytes.toFloat() / maximum
            val barHeight = ratio * (size.height - 4f)
            val centerX = slot * index + slot / 2f
            drawLine(
                color = chartColor,
                start = Offset(centerX, size.height),
                end = Offset(centerX, size.height - barHeight),
                strokeWidth = (slot * 0.45f).coerceAtMost(40f)
            )
            drawCircle(chartColor, radius = 4f, center = Offset(centerX, size.height - barHeight),
                style = Stroke(width = 2f))
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(formatShortDate(scans.first().timestampMillis), style = MaterialTheme.typography.labelSmall)
        if (scans.size > 1) Text(formatShortDate(scans.last().timestampMillis),
            style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun TimelineCard(report: ScanReport, onClick: () -> Unit) {
    TrashPilotCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = TrashPilotRadii.CardShape,
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.fillMaxWidth().padding(TrashPilotSpacing.Card),
            verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Medium)) {
            Text(formatDate(report.timestampMillis), style = MaterialTheme.typography.titleSmall)
            MetricRow(stringResource(R.string.reports_storage_scanned),
                report.scannedBytes?.let(::formatBytes) ?: stringResource(R.string.reports_not_recorded))
            MetricRow(stringResource(R.string.reports_storage_cleaned), formatBytes(report.cleanedBytes))
            MetricRow(stringResource(R.string.reports_largest_category), report.largestCategory.labelOrUnavailable())
            MetricRow(stringResource(R.string.reports_scan_duration), report.durationMillis.formatDuration())
        }
    }
}

@Composable
private fun ReportDetail(report: ScanReport) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(TrashPilotSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)
    ) {
        item { ReportCard(highlighted = true) {
            Text(formatDate(report.timestampMillis), style = MaterialTheme.typography.titleLarge)
            MetricRow(stringResource(R.string.reports_storage_scanned),
                report.scannedBytes?.let(::formatBytes) ?: stringResource(R.string.reports_not_recorded))
            MetricRow(stringResource(R.string.reports_storage_cleaned), formatBytes(report.cleanedBytes))
            MetricRow(stringResource(R.string.reports_largest_category), report.largestCategory.labelOrUnavailable())
            MetricRow(stringResource(R.string.reports_scan_duration), report.durationMillis.formatDuration())
        } }
        item { ReportCard {
            Text(stringResource(R.string.reports_recorded_categories), style = MaterialTheme.typography.titleMedium)
            DetailSizeRow(R.string.quick_clean_cache, report.details.cacheBytes)
            DetailSizeRow(R.string.results_hidden_files, report.details.hiddenBytes)
            DetailCountAndSizeRow(R.string.results_large_files, report.details.largeFileCount, report.details.largeFileBytes)
            DetailCountRow(R.string.quick_clean_empty_folders, report.details.emptyFolderCount)
            DetailCountAndSizeRow(R.string.results_social_media, report.details.socialMediaFileCount,
                report.details.socialMediaBytes)
            report.details.duplicateBytes?.let { DetailSizeRow(R.string.results_duplicates, it) }
        } }
    }
}

@Composable private fun DetailSizeRow(label: Int, bytes: Long?) = MetricRow(
    stringResource(label), bytes?.let(::formatBytes) ?: stringResource(R.string.reports_not_recorded))

@Composable private fun DetailCountRow(label: Int, count: Long?) = MetricRow(
    stringResource(label), count?.toString() ?: stringResource(R.string.reports_not_recorded))

@Composable private fun DetailCountAndSizeRow(label: Int, count: Long?, bytes: Long?) = MetricRow(
    stringResource(label), if (count == null || bytes == null) stringResource(R.string.reports_not_recorded)
    else stringResource(R.string.reports_count_and_size, count, formatBytes(bytes)))

@Composable
private fun ReportCard(highlighted: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    TrashPilotCard(
        modifier = Modifier.fillMaxWidth(), shape = TrashPilotRadii.CardShape,
        colors = CardDefaults.cardColors(if (highlighted) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.fillMaxWidth().padding(TrashPilotSpacing.Card),
            verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Medium), content = content)
    }
}

@Composable private fun SupportingText(text: String) = Text(text,
    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable private fun MetricRow(label: String, value: String) = Row(
    Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
    Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
        textAlign = TextAlign.End)
}

@Composable
private fun CenterState(text: String, loading: Boolean = false, action: String? = null, onAction: () -> Unit = {}) {
    Box(Modifier.fillMaxSize().padding(TrashPilotSpacing.Screen), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)) {
            if (loading) CircularProgressIndicator()
            Text(text, style = MaterialTheme.typography.bodyMedium)
            if (action != null) TrashPilotPrimaryButton(text = action, onClick = onAction)
        }
    }
}

@Composable private fun FileCategory?.labelOrUnavailable(): String = this?.let {
    stringResource(when (it) {
        FileCategory.IMAGES -> R.string.category_images
        FileCategory.VIDEOS -> R.string.category_videos
        FileCategory.AUDIO -> R.string.category_audio
        FileCategory.DOCUMENTS -> R.string.category_documents
        FileCategory.APK_FILES -> R.string.category_apk
        FileCategory.DOWNLOADS -> R.string.category_downloads
        FileCategory.OTHER -> R.string.category_other
    })
} ?: stringResource(R.string.reports_not_recorded)

@Composable private fun Long?.formatDuration(): String = when {
    this == null -> stringResource(R.string.reports_not_recorded)
    this < 1_000 -> stringResource(R.string.reports_duration_ms, this)
    else -> stringResource(R.string.reports_duration_seconds, this / 1_000.0)
}

private fun formatDate(timestamp: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp))
private fun formatShortDate(timestamp: Long): String =
    DateFormat.getDateInstance(DateFormat.SHORT).format(Date(timestamp))
