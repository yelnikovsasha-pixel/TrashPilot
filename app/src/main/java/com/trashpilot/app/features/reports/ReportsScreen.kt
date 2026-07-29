@file:Suppress("LocalContextGetResourceValueCall")

package com.trashpilot.app.features.reports

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trashpilot.app.R
import com.trashpilot.app.core.reports.ReportExporter
import com.trashpilot.app.core.reports.ReportsAnalyzer
import com.trashpilot.app.core.reports.ReportsSummary
import com.trashpilot.app.core.storage.formatBytes
import com.trashpilot.app.core.trashdna.TrashDnaRepository
import com.trashpilot.app.ui.components.TrashPilotTopAppBar
import com.trashpilot.app.ui.components.TrashPilotCard
import com.trashpilot.app.ui.components.TrashPilotPrimaryButton
import com.trashpilot.app.ui.theme.TrashPilotRadii
import com.trashpilot.app.ui.theme.TrashPilotSpacing
import com.trashpilot.app.ui.theme.TrashPilotComponentSizes
import com.trashpilot.app.ui.theme.TrashPilotTypography
import com.trashpilot.app.core.trashdna.TrashDnaSessionEntity
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private enum class ReportsPage {
    OVERVIEW, SCANS, CLEANING, TRENDS, EXPORT
}

private sealed interface ReportsUiState {
    data object Loading : ReportsUiState
    data class Success(val summary: ReportsSummary) : ReportsUiState
    data class Error(val message: String) : ReportsUiState
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ReportsScreen(repository: TrashDnaRepository, onBack: () -> Unit) {
    val context = LocalContext.current
    var state: ReportsUiState by remember { mutableStateOf(ReportsUiState.Loading) }
    var page by remember { mutableStateOf(ReportsPage.OVERVIEW) }
    LaunchedEffect(repository) {
        state = runCatching {
            ReportsAnalyzer.summarize(repository.loadReportHistory())
        }.fold(
            onSuccess = ReportsUiState::Success,
            onFailure = { ReportsUiState.Error(it.message ?: context.getString(R.string.reports_error_history)) }
        )
    }
    BackHandler {
        if (page == ReportsPage.OVERVIEW) onBack() else page = ReportsPage.OVERVIEW
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TrashPilotTopAppBar(
                title = page.title(),
                onBack = {
                    if (page == ReportsPage.OVERVIEW) onBack() else page = ReportsPage.OVERVIEW
                }
            )
        }
    ) { padding ->
        when (val current = state) {
            ReportsUiState.Loading -> MessageState(
                Modifier.padding(padding), stringResource(R.string.reports_loading), stringResource(R.string.reports_loading_body)
            )
            is ReportsUiState.Error -> MessageState(
                Modifier.padding(padding), stringResource(R.string.reports_error_title), current.message
            )
            is ReportsUiState.Success -> when (page) {
                ReportsPage.OVERVIEW -> Overview(
                    current.summary,
                    Modifier.padding(padding),
                    onPage = { page = it }
                )
                ReportsPage.SCANS -> ScanHistory(current.summary, Modifier.padding(padding))
                ReportsPage.CLEANING -> CleaningHistory(current.summary, Modifier.padding(padding))
                ReportsPage.TRENDS -> StorageTrends(current.summary, Modifier.padding(padding))
                ReportsPage.EXPORT -> ExportReport(current.summary, Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun Overview(
    summary: ReportsSummary,
    modifier: Modifier,
    onPage: (ReportsPage) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(TrashPilotSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.HomeCard)
    ) {
        item { Text(stringResource(R.string.reports_intro),
            color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item {
            MetricCard(
                listOf(
                    stringResource(R.string.trash_dna_scans) to summary.scans.size.toString(),
                    stringResource(R.string.trash_dna_cleanups) to summary.cleanups.size.toString(),
                    stringResource(R.string.reports_storage_reclaimed) to formatBytes(summary.reclaimedBytes),
                    stringResource(R.string.reports_privacy_reviews) to summary.privacyReviews.size.toString(),
                    stringResource(R.string.reports_dna_sessions) to (summary.scans.size + summary.cleanups.size).toString()
                ),
                highlighted = true
            )
        }
        item {
            val latestPrivacyReview = summary.privacyReviews.maxByOrNull { it.timestampMillis }
            if (latestPrivacyReview == null) {
                InfoCard(
                    stringResource(R.string.reports_privacy_history),
                    stringResource(R.string.reports_no_privacy_history)
                )
            } else {
                InfoCard(
                    stringResource(R.string.reports_latest_privacy),
                    stringResource(
                        R.string.reports_privacy_summary,
                        formatDate(latestPrivacyReview.timestampMillis),
                        latestPrivacyReview.privacyAppsChecked,
                        latestPrivacyReview.privacySensitiveAppCount
                    )
                )
            }
        }
        item {
            ReportNavigationCard(
                stringResource(R.string.reports_scan_history),
                stringResource(R.string.reports_scan_history_body)
            ) { onPage(ReportsPage.SCANS) }
        }
        item {
            ReportNavigationCard(
                stringResource(R.string.reports_cleaning_history),
                stringResource(R.string.reports_cleaning_history_body)
            ) { onPage(ReportsPage.CLEANING) }
        }
        item {
            ReportNavigationCard(stringResource(R.string.reports_storage_trends), stringResource(R.string.reports_trends_body)) {
                onPage(ReportsPage.TRENDS)
            }
        }
        item {
            ReportNavigationCard(stringResource(R.string.reports_export), stringResource(R.string.reports_export_body)) {
                onPage(ReportsPage.EXPORT)
            }
        }
    }
}

@Composable
private fun ScanHistory(summary: ReportsSummary, modifier: Modifier) {
    HistoryList(
        modifier = modifier,
        sessions = summary.scans,
        emptyTitle = stringResource(R.string.reports_no_scans),
        emptyBody = stringResource(R.string.reports_no_scans_body)
    ) { session ->
        SessionCard(
            title = session.scannedFolderName.ifBlank { stringResource(R.string.reports_selected_storage) },
            rows = listOf(
                stringResource(R.string.reports_date) to formatDate(session.timestampMillis),
                stringResource(R.string.reports_scan_duration) to formatDuration(session.scanDurationMillis),
                stringResource(R.string.reports_files_scanned) to if (session.scanDurationMillis > 0) {
                    session.scannedFileCount.toString()
                } else {
                    stringResource(R.string.reports_not_recorded)
                },
                stringResource(R.string.trash_dna_reclaimable) to formatBytes(session.reclaimableBytes),
                stringResource(R.string.trash_dna_result) to session.result.displayResult()
            )
        )
    }
}

@Composable
private fun CleaningHistory(summary: ReportsSummary, modifier: Modifier) {
    HistoryList(
        modifier = modifier,
        sessions = summary.cleanups,
        emptyTitle = stringResource(R.string.reports_no_cleanups),
        emptyBody = stringResource(R.string.reports_no_cleanups_body)
    ) { session ->
        SessionCard(
            title = session.scannedFolderName.ifBlank { stringResource(R.string.reports_selected_storage) },
            rows = listOf(
                stringResource(R.string.reports_date) to formatDate(session.timestampMillis),
                stringResource(R.string.reports_before_cleaning) to formatBytes(session.reclaimableBytes),
                stringResource(R.string.reports_actually_reclaimed) to formatBytes(session.reclaimedBytes),
                stringResource(R.string.trash_dna_result) to session.result.displayResult()
            )
        )
    }
}

@Composable
private fun StorageTrends(summary: ReportsSummary, modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(TrashPilotSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Large)
    ) {
        item { Text(stringResource(R.string.reports_charts_real),
            color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (summary.scans.size < 2) {
            item {
                InfoCard(
                    stringResource(R.string.reports_not_enough_scans),
                    stringResource(R.string.reports_not_enough_scans_body),
                    highlighted = true
                )
            }
        } else {
            item {
                ChartCard(
                    stringResource(R.string.reports_reclaimable_chart),
                    summary.scans.sortedBy { it.timestampMillis }.map { it.reclaimableBytes },
                    valueLabel = ::formatBytes
                )
            }
            item {
                val recordedFileCounts = summary.scans
                    .sortedBy { it.timestampMillis }
                    .filter { it.scanDurationMillis > 0 }
                if (recordedFileCounts.isEmpty()) {
                    InfoCard(
                        stringResource(R.string.reports_no_file_trend),
                        stringResource(R.string.reports_no_file_trend_body)
                    )
                } else {
                    ChartCard(
                        stringResource(R.string.reports_file_count_chart),
                        recordedFileCounts.map { it.scannedFileCount }
                    )
                }
            }
        }
        if (summary.cleanups.isNotEmpty()) {
            item {
                ChartCard(
                    stringResource(R.string.reports_reclaimed_chart),
                    summary.cleanups.sortedBy { it.timestampMillis }.map { it.reclaimedBytes },
                    valueLabel = ::formatBytes
                )
            }
        } else {
            item { InfoCard(stringResource(R.string.reports_no_cleaning_trend), stringResource(R.string.reports_no_cleaning_trend_body)) }
        }
    }
}

@Composable
private fun ExportReport(summary: ReportsSummary, modifier: Modifier) {
    val context = LocalContext.current
    val report = remember(summary) { ReportExporter.create(summary) }
    var exportResult by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            exportResult = runCatching {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(report.toByteArray(Charsets.UTF_8))
                } ?: error(context.getString(R.string.reports_document_error))
                context.getString(R.string.reports_exported)
            }.getOrElse { context.getString(R.string.reports_export_failed, it.message ?: context.getString(R.string.reports_unknown_error)) }
        }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(TrashPilotSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Large)
    ) {
        item {
            InfoCard(
                stringResource(R.string.reports_included),
                stringResource(R.string.reports_included_body),
                highlighted = true
            )
        }
        item {
            InfoCard(
                stringResource(R.string.reports_metadata_only),
                stringResource(R.string.reports_metadata_only_body)
            )
        }
        item {
            InfoCard(
                stringResource(R.string.reports_plain_text),
                stringResource(R.string.reports_plain_text_body)
            )
        }
        item {
            TrashPilotPrimaryButton(
                text = stringResource(R.string.reports_export),
                onClick = { launcher.launch("trashpilot-report.txt") },
                modifier = Modifier.fillMaxWidth(),
                height = null
            )
        }
        exportResult?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }
    }
}

@Composable
private fun <T> HistoryList(
    modifier: Modifier,
    sessions: List<T>,
    emptyTitle: String,
    emptyBody: String,
    itemContent: @Composable (T) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(TrashPilotSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)
    ) {
        if (sessions.isEmpty()) {
            item { InfoCard(emptyTitle, emptyBody, highlighted = true) }
        } else {
            items(sessions) { itemContent(it) }
        }
        item {
            Text(
                stringResource(R.string.reports_local_metadata),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChartCard(
    title: String,
    values: List<Long>,
    valueLabel: (Long) -> String = { it.toString() }
) {
    TrashPilotCard(
        shape = TrashPilotRadii.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            Modifier.padding(TrashPilotSpacing.Card),
            verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)
        ) {
            Text(title, fontWeight = FontWeight.SemiBold)
            val lineColor = MaterialTheme.colorScheme.primary
            Canvas(Modifier.fillMaxWidth().height(TrashPilotComponentSizes.ReportChartHeight)) {
                val max = values.maxOrNull()?.coerceAtLeast(1) ?: 1
                val min = values.minOrNull() ?: 0
                val range = (max - min).coerceAtLeast(1)
                val step = if (values.size > 1) size.width / (values.size - 1) else 0f
                val points = values.mapIndexed { index, value ->
                    Offset(
                        x = index * step,
                        y = size.height - ((value - min).toFloat() / range * size.height)
                    )
                }
                points.zipWithNext().forEach { (start, end) ->
                    drawLine(lineColor, start, end, strokeWidth = 6f, cap = StrokeCap.Round)
                }
                points.forEach { drawCircle(lineColor, radius = 7f, center = it) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.reports_first_value, valueLabel(values.first())), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.reports_latest_value, valueLabel(values.last())), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ReportNavigationCard(title: String, body: String, onClick: () -> Unit) {
    TrashPilotCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = TrashPilotRadii.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            Modifier.padding(TrashPilotSpacing.Card),
            verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.MediumCompact)
        ) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SessionCard(title: String, rows: List<Pair<String, String>>) {
    TrashPilotCard(
        shape = TrashPilotRadii.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            Modifier.padding(TrashPilotSpacing.Card),
            verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.MediumLarge)
        ) {
            Text(title, style = TrashPilotTypography.FeatureHeading)
            rows.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun MetricCard(rows: List<Pair<String, String>>, highlighted: Boolean) {
    TrashPilotCard(
        shape = TrashPilotRadii.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            Modifier.padding(TrashPilotSpacing.Card),
            verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)
        ) {
            Text(
                stringResource(R.string.reports_recorded_activity),
                style = TrashPilotTypography.ReportActivityHeading
            )
            rows.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label)
                    Text(value, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String, highlighted: Boolean = false) {
    TrashPilotCard(
        shape = TrashPilotRadii.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            Modifier.padding(TrashPilotSpacing.Card),
            verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Medium)
        ) {
            Text(title, style = TrashPilotTypography.FeatureHeading)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MessageState(modifier: Modifier, title: String, body: String) {
    Column(modifier.fillMaxSize().padding(TrashPilotSpacing.Screen)) {
        InfoCard(title, body, highlighted = true)
    }
}

@Composable
private fun ReportsPage.title() = stringResource(when (this) {
    ReportsPage.OVERVIEW -> R.string.reports_title
    ReportsPage.SCANS -> R.string.reports_scan_history
    ReportsPage.CLEANING -> R.string.reports_cleaning_history
    ReportsPage.TRENDS -> R.string.reports_storage_trends
    ReportsPage.EXPORT -> R.string.reports_export
})

private fun formatDate(timestampMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(Date(timestampMillis))

@Composable
private fun formatDuration(durationMillis: Long): String = when {
    durationMillis <= 0 -> stringResource(R.string.reports_not_recorded)
    durationMillis < 1_000 -> stringResource(R.string.reports_duration_ms, durationMillis)
    else -> stringResource(R.string.reports_duration_seconds, durationMillis / 1_000.0)
}

@Composable
private fun String.displayResult() = when (uppercase()) {
    "SUCCESS" -> stringResource(R.string.reports_result_success)
    "PARTIAL" -> stringResource(R.string.reports_result_partial)
    "FAILED", "ERROR" -> stringResource(R.string.reports_result_failed)
    else -> stringResource(R.string.reports_result_unknown)
}
