package com.trashpilot.app.features.trashdna

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trashpilot.app.R
import com.trashpilot.app.core.storage.formatBytes
import com.trashpilot.app.core.trashdna.*
import com.trashpilot.app.ui.components.TrashPilotTopAppBar
import com.trashpilot.app.ui.components.TrashPilotCard
import com.trashpilot.app.ui.components.TrashPilotSectionHeader
import com.trashpilot.app.ui.components.TrashPilotPrimaryButton
import com.trashpilot.app.ui.components.TrashPilotSecondaryButton
import com.trashpilot.app.ui.theme.TrashPilotRadii
import com.trashpilot.app.ui.theme.TrashPilotSpacing
import com.trashpilot.app.ui.theme.TrashPilotComponentSizes
import java.text.DateFormat
import java.util.Date

private enum class TrashDnaPage { OVERVIEW, INSIGHTS, HISTORY }

private sealed interface TrashDnaUiState {
    data object Loading : TrashDnaUiState
    data class Success(val history: List<TrashDnaSessionEntity>) : TrashDnaUiState
    data class Error(val message: String?) : TrashDnaUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashDnaScreen(repository: TrashDnaRepository, onBack: () -> Unit) {
    var page by remember { mutableStateOf(TrashDnaPage.OVERVIEW) }
    var retryKey by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<TrashDnaUiState>(TrashDnaUiState.Loading) }
    LaunchedEffect(repository, retryKey) {
        state = TrashDnaUiState.Loading
        state = try {
            TrashDnaUiState.Success(repository.loadHistory())
        } catch (error: Exception) {
            TrashDnaUiState.Error(error.message)
        }
    }
    BackHandler {
        if (page == TrashDnaPage.OVERVIEW) onBack() else page = TrashDnaPage.OVERVIEW
    }
    Scaffold(
        topBar = {
            TrashPilotTopAppBar(
                title = stringResource(page.titleResource()),
                onBack = {
                    if (page == TrashDnaPage.OVERVIEW) onBack() else page = TrashDnaPage.OVERVIEW
                }
            )
        }
    ) { padding ->
        when (val current = state) {
            TrashDnaUiState.Loading -> StateMessage(
                Modifier.padding(padding),
                stringResource(R.string.trash_dna_loading),
                loading = true
            )
            is TrashDnaUiState.Error -> StateMessage(
                Modifier.padding(padding),
                stringResource(R.string.trash_dna_error),
                action = stringResource(R.string.trash_dna_retry),
                onAction = { retryKey++ }
            )
            is TrashDnaUiState.Success -> when (page) {
                TrashDnaPage.OVERVIEW -> Overview(
                    Modifier.padding(padding),
                    current.history,
                    onInsights = { page = TrashDnaPage.INSIGHTS },
                    onHistory = { page = TrashDnaPage.HISTORY }
                )
                TrashDnaPage.INSIGHTS -> Insights(Modifier.padding(padding), current.history)
                TrashDnaPage.HISTORY -> History(Modifier.padding(padding), current.history)
            }
        }
    }
}

@Composable
private fun Overview(
    modifier: Modifier,
    history: List<TrashDnaSessionEntity>,
    onInsights: () -> Unit,
    onHistory: () -> Unit
) {
    val summary = remember(history) { TrashDnaAnalyzer.summary(history) }
    ScreenColumn(modifier) {
        item { BodyText(stringResource(R.string.trash_dna_local_intro)) }
        if (summary == null) {
            item {
                HighlightCard(
                    stringResource(R.string.trash_dna_not_enough),
                    stringResource(R.string.trash_dna_not_enough_body)
                )
            }
        }
        item { SectionTitle(stringResource(R.string.trash_dna_habit_summary)) }
        item {
            LowCard {
                MetricRow(stringResource(R.string.trash_dna_scans), summary?.scansCompleted?.toString() ?: "—")
                MetricRow(stringResource(R.string.trash_dna_cleanups), summary?.cleanupsCompleted?.toString() ?: "—")
                MetricRow(
                    stringResource(R.string.trash_dna_common_category),
                    summary?.mostCommonCategory?.label() ?: "—"
                )
                MetricRow(
                    stringResource(R.string.trash_dna_average),
                    summary?.let { formatBytes(it.averageReclaimableBytes) } ?: "—"
                )
                MetricRow(
                    stringResource(R.string.trash_dna_last_scan),
                    summary?.let { formatDate(it.lastScanMillis) } ?: "—"
                )
            }
        }
        item { Spacer(Modifier.height(TrashPilotSpacing.Large)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)) {
                TrashPilotSecondaryButton(
                    text = stringResource(R.string.trash_dna_insights),
                    onClick = onInsights,
                    modifier = Modifier.weight(1f),
                    shape = TrashPilotRadii.PillShape,
                    fontWeight = FontWeight.SemiBold
                )
                TrashPilotPrimaryButton(
                    text = stringResource(R.string.trash_dna_history),
                    onClick = onHistory,
                    modifier = Modifier.weight(1f),
                    shape = TrashPilotRadii.PillShape,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun Insights(modifier: Modifier, history: List<TrashDnaSessionEntity>) {
    val insights = remember(history) { TrashDnaAnalyzer.insights(history) }
    ScreenColumn(modifier) {
        item { LocalBadge() }
        item { BodyText(stringResource(R.string.trash_dna_insights_intro)) }
        if (insights.isEmpty()) {
            item {
                HighlightCard(
                    stringResource(R.string.trash_dna_no_insights),
                    stringResource(R.string.trash_dna_no_insights_body)
                )
            }
        } else {
            items(insights.toList(), key = { it.name }) { insight ->
                LowCard {
                    Text(stringResource(insight.titleResource()), fontWeight = FontWeight.SemiBold)
                    BodyText(stringResource(insight.bodyResource()))
                }
            }
        }
        item { BodyText(stringResource(R.string.trash_dna_offline_note)) }
    }
}

@Composable
private fun History(modifier: Modifier, history: List<TrashDnaSessionEntity>) {
    ScreenColumn(modifier) {
        item { BodyText(stringResource(R.string.trash_dna_history_intro)) }
        if (history.isEmpty()) {
            item {
                HighlightCard(
                    stringResource(R.string.trash_dna_no_history),
                    stringResource(R.string.trash_dna_no_history_body)
                )
            }
        } else {
            items(history, key = { it.id }) { session -> HistoryCard(session) }
        }
    }
}

@Composable
private fun HistoryCard(session: TrashDnaSessionEntity) {
    LowCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                stringResource(
                    if (session.sessionType == TrashDnaSessionType.SCAN)
                        R.string.trash_dna_scan_session else R.string.trash_dna_cleanup_session
                ),
                fontWeight = FontWeight.SemiBold
            )
            Text(formatDate(session.timestampMillis), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        MetricRow(stringResource(R.string.trash_dna_folder), session.scannedFolderName)
        MetricRow(stringResource(R.string.trash_dna_reclaimable), formatBytes(session.reclaimableBytes))
        MetricRow(stringResource(R.string.trash_dna_reclaimed), formatBytes(session.reclaimedBytes))
        MetricRow(stringResource(R.string.trash_dna_result), session.result.resultLabel())
    }
}

@Composable
private fun ScreenColumn(
    modifier: Modifier,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) = LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(
        TrashPilotSpacing.Screen,
        TrashPilotSpacing.Large,
        TrashPilotSpacing.Screen,
        TrashPilotSpacing.Screen
    ),
    verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Large),
    content = content
)

@Composable
private fun HighlightCard(title: String, body: String) = TrashPilotCard(
    shape = TrashPilotRadii.CardShape,
    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)
) {
    Column(
        Modifier.padding(TrashPilotSpacing.Card),
        verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Medium)
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        BodyText(body)
    }
}

@Composable
private fun LowCard(content: @Composable ColumnScope.() -> Unit) = TrashPilotCard(
    shape = TrashPilotRadii.CardShape,
    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)
) {
    Column(
        Modifier.fillMaxWidth().padding(
            horizontal = TrashPilotSpacing.CardDense,
            vertical = TrashPilotSpacing.Standard
        ),
        verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.MediumLarge),
        content = content
    )
}

@Composable
private fun MetricRow(label: String, value: String) = Row(
    Modifier.fillMaxWidth().heightIn(min = TrashPilotComponentSizes.MetricRowMinimumHeight),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Text(label, modifier = Modifier.weight(1f))
    Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable private fun SectionTitle(text: String) =
    TrashPilotSectionHeader(text)

@Composable private fun BodyText(text: String) =
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun LocalBadge() = Surface(
    shape = TrashPilotRadii.IconContainerShape,
    color = MaterialTheme.colorScheme.primaryContainer
) {
    Text(
        stringResource(R.string.trash_dna_local_badge),
        Modifier.padding(
            horizontal = TrashPilotSpacing.HomeCard,
            vertical = TrashPilotSpacing.Medium
        ),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun StateMessage(
    modifier: Modifier,
    message: String,
    loading: Boolean = false,
    action: String? = null,
    onAction: () -> Unit = {}
) = Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Large)
    ) {
        if (loading) CircularProgressIndicator()
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (action != null) {
            TrashPilotPrimaryButton(
                text = action,
                onClick = onAction,
                height = null
            )
        }
    }
}

private fun TrashDnaPage.titleResource() = when (this) {
    TrashDnaPage.OVERVIEW -> R.string.trash_dna_title
    TrashDnaPage.INSIGHTS -> R.string.trash_dna_insights
    TrashDnaPage.HISTORY -> R.string.trash_dna_history
}

@Composable
private fun TrashDnaCategory.label() = stringResource(when (this) {
    TrashDnaCategory.TEMPORARY_FILES -> R.string.quick_clean_temporary
    TrashDnaCategory.APP_CACHE -> R.string.quick_clean_cache
    TrashDnaCategory.EMPTY_FOLDERS -> R.string.quick_clean_empty_folders
    TrashDnaCategory.APK_LEFTOVERS -> R.string.quick_clean_apk_leftovers
    TrashDnaCategory.LOG_FILES -> R.string.quick_clean_logs
})

private fun TrashDnaInsight.titleResource() = when (this) {
    TrashDnaInsight.TEMPORARY_FILES_ACCUMULATE_FASTEST -> R.string.trash_dna_temp_fast_title
    TrashDnaInsight.APK_LEFTOVERS_RECUR -> R.string.trash_dna_apk_recur_title
    TrashDnaInsight.LOG_FILES_REMAIN_LOW -> R.string.trash_dna_logs_low_title
}

private fun TrashDnaInsight.bodyResource() = when (this) {
    TrashDnaInsight.TEMPORARY_FILES_ACCUMULATE_FASTEST -> R.string.trash_dna_temp_fast_body
    TrashDnaInsight.APK_LEFTOVERS_RECUR -> R.string.trash_dna_apk_recur_body
    TrashDnaInsight.LOG_FILES_REMAIN_LOW -> R.string.trash_dna_logs_low_body
}

@Composable
private fun String.resultLabel() = when (uppercase()) {
    "SUCCESS" -> stringResource(R.string.reports_result_success)
    "PARTIAL" -> stringResource(R.string.reports_result_partial)
    "FAILED", "ERROR" -> stringResource(R.string.reports_result_failed)
    else -> stringResource(R.string.reports_result_unknown)
}
private fun formatDate(millis: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))
