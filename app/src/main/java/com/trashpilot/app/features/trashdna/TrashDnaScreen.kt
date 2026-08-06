package com.trashpilot.app.features.trashdna

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.trashpilot.app.R
import com.trashpilot.app.core.storage.formatBytes
import com.trashpilot.app.core.trashdna.HistoryRepository
import com.trashpilot.app.core.trashdna.TrashDnaAnalysis
import com.trashpilot.app.core.trashdna.TrashDnaAnalyzer
import com.trashpilot.app.core.trashdna.TrashDnaCategory
import com.trashpilot.app.core.trashdna.TrashDnaHistoryItem
import com.trashpilot.app.core.trashdna.TrashDnaInsight
import com.trashpilot.app.core.trashdna.TrashDnaProfile
import com.trashpilot.app.core.trashdna.TrashDnaRecommendation
import com.trashpilot.app.core.trashdna.TrendDirection
import com.trashpilot.app.ui.components.TrashPilotCard
import com.trashpilot.app.ui.components.TrashPilotPrimaryButton
import com.trashpilot.app.ui.components.TrashPilotTextButton
import com.trashpilot.app.ui.components.TrashPilotTopAppBar
import com.trashpilot.app.ui.theme.TrashPilotRadii
import com.trashpilot.app.ui.theme.TrashPilotSpacing
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

private sealed interface TrashDnaUiState {
    data object Loading : TrashDnaUiState
    data class Success(val analysis: TrashDnaAnalysis?) : TrashDnaUiState
    data object Error : TrashDnaUiState
}

@Composable
fun TrashDnaScreen(
    repository: HistoryRepository,
    onBack: () -> Unit,
    onScanAgain: () -> Unit
) {
    var reloadKey by remember { mutableIntStateOf(0) }
    var state: TrashDnaUiState by remember { mutableStateOf(TrashDnaUiState.Loading) }
    var showResetDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(repository, reloadKey) {
        state = TrashDnaUiState.Loading
        state = runCatching { TrashDnaAnalyzer.analyze(repository.loadTrashDnaHistory()) }
            .fold({ TrashDnaUiState.Success(it) }, { TrashDnaUiState.Error })
    }
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize()) {
        TrashPilotTopAppBar(title = stringResource(R.string.trash_dna_title), onBack = onBack)
        when (val current = state) {
            TrashDnaUiState.Loading -> CenterState(loading = true, text = stringResource(R.string.trash_dna_loading))
            TrashDnaUiState.Error -> CenterState(
                text = stringResource(R.string.trash_dna_error),
                action = stringResource(R.string.trash_dna_retry),
                onAction = { reloadKey++ }
            )
            is TrashDnaUiState.Success -> current.analysis?.let {
                AnalysisContent(it, onReset = { showResetDialog = true })
            } ?: EmptyContent(onScanAgain)
        }
    }
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.trash_dna_reset_confirm_title)) },
            text = { Text(stringResource(R.string.trash_dna_reset_confirm_body)) },
            confirmButton = {
                TrashPilotTextButton(
                    text = stringResource(R.string.trash_dna_reset_confirm),
                    onClick = {
                        showResetDialog = false
                        state = TrashDnaUiState.Loading
                        scope.launch {
                            runCatching { repository.resetTrashDnaHistory() }
                                .onSuccess { reloadKey++ }
                                .onFailure { state = TrashDnaUiState.Error }
                        }
                    }
                )
            },
            dismissButton = {
                TrashPilotTextButton(
                    text = stringResource(R.string.quick_clean_cancel),
                    onClick = { showResetDialog = false }
                )
            }
        )
    }
}

@Composable
private fun EmptyContent(onScanAgain: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(TrashPilotSpacing.Screen), contentAlignment = Alignment.Center) {
        TrashPilotCard(
            shape = TrashPilotRadii.CardShape,
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(TrashPilotSpacing.Card),
                verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)
            ) {
                Text(stringResource(R.string.trash_dna_not_enough_scan_history),
                    style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.trash_dna_multiple_scans_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                TrashPilotPrimaryButton(
                    text = stringResource(R.string.results_scan_again),
                    onClick = onScanAgain,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun AnalysisContent(analysis: TrashDnaAnalysis, onReset: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(TrashPilotSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)
    ) {
        item { FeatureCard(R.string.trash_dna_current_profile, highlighted = true) {
            Text(stringResource(analysis.profile.label()), style = MaterialTheme.typography.headlineSmall)
            SupportingText(stringResource(analysis.profile.explanation()))
        } }
        item { FeatureCard(R.string.trash_dna_since_last_scan) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(formatSignedBytes(analysis.storageTrend.changeBytes), style = MaterialTheme.typography.titleLarge)
                val icon = when (analysis.storageTrend.direction) {
                    TrendDirection.UP -> Icons.AutoMirrored.Outlined.TrendingUp
                    TrendDirection.DOWN -> Icons.AutoMirrored.Outlined.TrendingDown
                    TrendDirection.STABLE -> Icons.AutoMirrored.Outlined.TrendingFlat
                }
                Icon(icon, contentDescription = stringResource(analysis.storageTrend.direction.description()))
            }
        } }
        item { FeatureCard(R.string.trash_dna_fastest_growing) {
            val trend = analysis.fastestGrowingCategory
            Text(trend?.category?.let { stringResource(it.label()) }
                ?: stringResource(R.string.trash_dna_no_growth), style = MaterialTheme.typography.titleMedium)
            SupportingText(trend?.let { formatSignedBytes(it.changeBytes) }
                ?: stringResource(R.string.trash_dna_storage_stable))
        } }
        item { FeatureCard(R.string.trash_dna_main_source) {
            Text(analysis.mainSourceName ?: analysis.mainSourceCategory?.let { stringResource(it.label()) }
                ?: stringResource(R.string.trash_dna_no_growth), style = MaterialTheme.typography.titleMedium)
        } }
        item { FeatureCard(R.string.trash_dna_insight_card) {
            SupportingText(analysis.insight.text(analysis.fastestGrowingCategory?.category))
        } }
        item { FeatureCard(R.string.trash_dna_recommendation_card) {
            SupportingText(analysis.recommendation.text(analysis.fastestGrowingCategory?.category))
        } }
        item { Text(stringResource(R.string.trash_dna_history), style = MaterialTheme.typography.titleMedium) }
        items(analysis.history, key = { it.timestampMillis }) { HistoryCard(it) }
        item { FeatureCard(R.string.trash_dna_reset_title) {
            SupportingText(stringResource(R.string.trash_dna_reset_body))
            TrashPilotTextButton(text = stringResource(R.string.trash_dna_reset_action), onClick = onReset)
        } }
    }
}

@Composable
private fun HistoryCard(item: TrashDnaHistoryItem) = FeatureCard(title = null) {
    Text(formatDate(item.timestampMillis), style = MaterialTheme.typography.titleSmall)
    MetricRow(stringResource(R.string.trash_dna_total_storage), formatBytes(item.totalStorageBytes))
    MetricRow(stringResource(R.string.trash_dna_deleted_size), formatBytes(item.deletedBytes))
    MetricRow(stringResource(R.string.trash_dna_largest_category),
        item.largestCategory?.let { stringResource(it.label()) } ?: stringResource(R.string.reports_not_recorded))
}

@Composable
private fun FeatureCard(title: Int?, highlighted: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    TrashPilotCard(
        modifier = Modifier.fillMaxWidth(), shape = TrashPilotRadii.CardShape,
        colors = CardDefaults.cardColors(if (highlighted) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.fillMaxWidth().padding(TrashPilotSpacing.Card),
            verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Medium)) {
            if (title != null) Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            content()
        }
    }
}

@Composable private fun SupportingText(text: String) = Text(text,
    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable private fun MetricRow(label: String, value: String) = Row(
    Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
) { Text(label, modifier = Modifier.weight(1f)); Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant) }

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

private fun formatSignedBytes(bytes: Long): String = when {
    bytes > 0 -> "+${formatBytes(bytes)}"
    bytes < 0 -> "−${formatBytes(-bytes)}"
    else -> formatBytes(0)
}
private fun formatDate(millis: Long) = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))

private fun TrashDnaProfile.label() = when (this) {
    TrashDnaProfile.BALANCED -> R.string.trash_dna_profile_balanced
    TrashDnaProfile.MEDIA_COLLECTOR -> R.string.trash_dna_profile_media
    TrashDnaProfile.MESSENGER_HEAVY -> R.string.trash_dna_profile_messenger
    TrashDnaProfile.DOWNLOAD_KEEPER -> R.string.trash_dna_profile_downloads
    TrashDnaProfile.SCREENSHOT_COLLECTOR -> R.string.trash_dna_profile_screenshots
    TrashDnaProfile.LARGE_FILE_KEEPER -> R.string.trash_dna_profile_large
}
private fun TrashDnaProfile.explanation() = when (this) {
    TrashDnaProfile.BALANCED -> R.string.trash_dna_profile_balanced_body
    TrashDnaProfile.MEDIA_COLLECTOR -> R.string.trash_dna_profile_media_body
    TrashDnaProfile.MESSENGER_HEAVY -> R.string.trash_dna_profile_messenger_body
    TrashDnaProfile.DOWNLOAD_KEEPER -> R.string.trash_dna_profile_downloads_body
    TrashDnaProfile.SCREENSHOT_COLLECTOR -> R.string.trash_dna_profile_screenshots_body
    TrashDnaProfile.LARGE_FILE_KEEPER -> R.string.trash_dna_profile_large_body
}
private fun TrashDnaCategory.label() = when (this) {
    TrashDnaCategory.MESSENGER_MEDIA -> R.string.results_social_media
    TrashDnaCategory.SCREENSHOTS -> R.string.trash_dna_category_screenshots
    TrashDnaCategory.DOWNLOADS -> R.string.category_downloads
    TrashDnaCategory.LARGE_FILES -> R.string.results_large_files
    TrashDnaCategory.HIDDEN_FILES -> R.string.results_hidden_files
    TrashDnaCategory.CACHE -> R.string.quick_clean_cache
    TrashDnaCategory.IMAGES -> R.string.category_images
    TrashDnaCategory.VIDEOS -> R.string.category_videos
    TrashDnaCategory.AUDIO -> R.string.category_audio
    TrashDnaCategory.DOCUMENTS -> R.string.category_documents
}
private fun TrashDnaInsight.label() = when (this) {
    TrashDnaInsight.MESSENGER_GROWTH -> R.string.trash_dna_insight_messenger
    TrashDnaInsight.DOWNLOADS_GROWTH -> R.string.trash_dna_insight_downloads
    TrashDnaInsight.SCREENSHOTS_GROWTH -> R.string.trash_dna_insight_screenshots
    TrashDnaInsight.LARGE_VIDEOS_DOMINATE -> R.string.trash_dna_insight_large_videos
    TrashDnaInsight.STORAGE_STABLE -> R.string.trash_dna_insight_stable
    TrashDnaInsight.CATEGORY_GROWTH -> R.string.trash_dna_insight_category
    TrashDnaInsight.STORAGE_REDUCED -> R.string.trash_dna_insight_reduced
}
@Composable private fun TrashDnaInsight.text(category: TrashDnaCategory?): String =
    if (this == TrashDnaInsight.CATEGORY_GROWTH) {
        stringResource(label(), category?.let { stringResource(it.label()) }.orEmpty())
    } else stringResource(label())
private fun TrashDnaRecommendation.label() = when (this) {
    TrashDnaRecommendation.REVIEW_MESSENGER_MEDIA -> R.string.trash_dna_recommend_messenger
    TrashDnaRecommendation.REMOVE_OLD_DOWNLOADS -> R.string.trash_dna_recommend_downloads
    TrashDnaRecommendation.CHECK_SCREENSHOTS -> R.string.trash_dna_recommend_screenshots
    TrashDnaRecommendation.REVIEW_LARGE_VIDEOS -> R.string.trash_dna_recommend_large_videos
    TrashDnaRecommendation.REVIEW_FASTEST_CATEGORY -> R.string.trash_dna_recommend_category
    TrashDnaRecommendation.KEEP_CURRENT_HABITS -> R.string.trash_dna_recommend_stable
}
@Composable private fun TrashDnaRecommendation.text(category: TrashDnaCategory?): String =
    if (this == TrashDnaRecommendation.REVIEW_FASTEST_CATEGORY) {
        stringResource(label(), category?.let { stringResource(it.label()) }.orEmpty())
    } else stringResource(label())
private fun TrendDirection.description() = when (this) {
    TrendDirection.UP -> R.string.trash_dna_trend_up
    TrendDirection.DOWN -> R.string.trash_dna_trend_down
    TrendDirection.STABLE -> R.string.trash_dna_trend_stable
}
