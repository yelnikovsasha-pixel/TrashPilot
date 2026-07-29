package com.trashpilot.app.features.quickclean

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.trashpilot.app.R
import com.trashpilot.app.core.quickclean.*
import com.trashpilot.app.core.storage.StorageScanResult
import com.trashpilot.app.core.storage.formatBytes
import com.trashpilot.app.ui.components.TrashPilotTopAppBar
import com.trashpilot.app.ui.components.TrashPilotCard
import com.trashpilot.app.ui.components.TrashPilotPrimaryButton
import com.trashpilot.app.ui.components.TrashPilotOutlinedButton
import com.trashpilot.app.ui.components.TrashPilotTextButton
import com.trashpilot.app.ui.components.TrashPilotSectionHeader
import com.trashpilot.app.ui.theme.TrashPilotRadii
import com.trashpilot.app.ui.theme.TrashPilotSpacing
import kotlinx.coroutines.launch

private enum class Step { OVERVIEW, REVIEW, CONFIRMATION, REPORT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCleanScreen(
    scanResult: StorageScanResult,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onCleaningComplete: (CleaningReport) -> Unit = {}
) {
    val context = LocalContext.current
    val cleaner = remember(context) { DocumentTreeCleaner(context.contentResolver) }
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(Step.OVERVIEW) }
    var selectedUris by remember { mutableStateOf(emptySet<String>()) }
    var report by remember { mutableStateOf<CleaningReport?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var cleaning by remember { mutableStateOf(false) }
    val selected = scanResult.disposableCandidates.filter { it.uri in selectedUris }

    BackHandler(enabled = !cleaning) {
        step = when (step) {
            Step.OVERVIEW -> return@BackHandler onBack()
            Step.REVIEW -> Step.OVERVIEW
            Step.CONFIRMATION -> Step.REVIEW
            Step.REPORT -> return@BackHandler onDone()
        }
    }
    BackHandler(enabled = cleaning) { }

    Scaffold(
        topBar = {
            TrashPilotTopAppBar(
                title = stringResource(step.titleResource()),
                onBack = {
                    when (step) {
                        Step.OVERVIEW -> onBack()
                        Step.REVIEW -> step = Step.OVERVIEW
                        Step.CONFIRMATION -> step = Step.REVIEW
                        Step.REPORT -> onDone()
                    }
                }
            )
        }
    ) { padding ->
        when (step) {
            Step.OVERVIEW -> Overview(
                Modifier.padding(padding),
                scanResult.disposableCandidates
            ) { step = Step.REVIEW }
            Step.REVIEW -> ReviewItems(
                Modifier.padding(padding),
                scanResult.disposableCandidates,
                selectedUris,
                onToggle = { uri ->
                    selectedUris = if (uri in selectedUris) selectedUris - uri else selectedUris + uri
                },
                onContinue = { step = Step.CONFIRMATION }
            )
            Step.CONFIRMATION -> Confirmation(
                Modifier.padding(padding),
                selected,
                cleaning,
                onCancel = { step = Step.REVIEW },
                onClean = { showDialog = true }
            )
            Step.REPORT -> ReportContent(
                Modifier.padding(padding),
                checkNotNull(report),
                onDone
            )
        }
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.quick_clean_dialog_title)) },
            text = { Text(stringResource(R.string.quick_clean_dialog_body, selected.size)) },
            dismissButton = {
                TrashPilotTextButton(
                    text = stringResource(R.string.quick_clean_cancel),
                    onClick = { showDialog = false }
                )
            },
            confirmButton = {
                TrashPilotTextButton(
                    text = stringResource(R.string.quick_clean_clean_now),
                    onClick = {
                        showDialog = false
                        cleaning = true
                        scope.launch {
                            report = cleaner.clean(selected)
                            report?.let(onCleaningComplete)
                            cleaning = false
                            step = Step.REPORT
                        }
                    }
                )
            }
        )
    }
}

@Composable
private fun Overview(
    modifier: Modifier,
    candidates: List<DisposableCandidate>,
    onReview: () -> Unit
) {
    ScreenColumn(modifier) {
        item { BodyText(stringResource(R.string.quick_clean_exclusion_note)) }
        item {
            HighlightCard {
                BodyText(stringResource(R.string.quick_clean_reclaimable))
                Emphasis(formatBytes(candidates.sumOf { it.sizeBytes }))
                BodyText(stringResource(R.string.quick_clean_reclaimable_note))
            }
        }
        item { SectionTitle(stringResource(R.string.quick_clean_categories)) }
        item {
            LowCard {
                DisposableCategory.entries.forEach { category ->
                    MetricRow(
                        stringResource(category.labelResource()),
                        formatBytes(candidates.filter { it.category == category }.sumOf { it.sizeBytes })
                    )
                }
            }
        }
        item { BodyText(stringResource(R.string.quick_clean_manual_note)) }
        item { Spacer(Modifier.height(TrashPilotSpacing.Large)) }
        item { PrimaryAction(stringResource(R.string.quick_clean_review), onReview) }
    }
}

@Composable
private fun ReviewItems(
    modifier: Modifier,
    candidates: List<DisposableCandidate>,
    selectedUris: Set<String>,
    onToggle: (String) -> Unit,
    onContinue: () -> Unit
) {
    val selectedSize = candidates.filter { it.uri in selectedUris }.sumOf { it.sizeBytes }
    ScreenColumn(modifier) {
        item { BodyText(stringResource(R.string.quick_clean_review_note)) }
        if (candidates.isEmpty()) {
            item { LowCard { BodyText(stringResource(R.string.quick_clean_empty)) } }
        } else {
            DisposableCategory.entries.forEach { category ->
                val categoryItems = candidates.filter { it.category == category }
                if (categoryItems.isNotEmpty()) {
                    item { SectionTitle(stringResource(category.labelResource())) }
                    items(categoryItems, key = DisposableCandidate::uri) { candidate ->
                        CandidateRow(
                            candidate,
                            candidate.uri in selectedUris
                        ) { onToggle(candidate.uri) }
                    }
                }
            }
        }
        item {
            HighlightCard {
                MetricRow(
                    stringResource(R.string.quick_clean_selected_total),
                    formatBytes(selectedSize)
                )
            }
        }
        item {
            PrimaryAction(
                stringResource(R.string.quick_clean_continue),
                onContinue,
                selectedUris.isNotEmpty()
            )
        }
    }
}

@Composable
private fun Confirmation(
    modifier: Modifier,
    selected: List<DisposableCandidate>,
    cleaning: Boolean,
    onCancel: () -> Unit,
    onClean: () -> Unit
) {
    ScreenColumn(modifier) {
        item { SectionTitle(stringResource(R.string.quick_clean_ready)) }
        item { BodyText(stringResource(R.string.quick_clean_confirmation_body)) }
        item {
            HighlightCard {
                BodyText(stringResource(R.string.quick_clean_selected_for_deletion))
                Emphasis(
                    stringResource(
                        R.string.quick_clean_items_and_size,
                        selected.size,
                        formatBytes(selected.sumOf { it.sizeBytes })
                    )
                )
            }
        }
        item {
            LowCard {
                SectionTitle(stringResource(R.string.quick_clean_personal_excluded))
                BodyText(stringResource(R.string.quick_clean_personal_excluded_body))
            }
        }
        item { BodyText(stringResource(R.string.quick_clean_irreversible)) }
        item { Spacer(Modifier.height(TrashPilotSpacing.Large)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)) {
                TrashPilotOutlinedButton(
                    text = stringResource(R.string.quick_clean_cancel),
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = TrashPilotRadii.PillShape
                )
                TrashPilotPrimaryButton(
                    text = stringResource(R.string.quick_clean_clean_now),
                    onClick = onClean,
                    enabled = !cleaning,
                    modifier = Modifier.weight(2f),
                    shape = TrashPilotRadii.PillShape
                )
            }
        }
    }
}

@Composable
private fun ReportContent(
    modifier: Modifier,
    report: CleaningReport,
    onDone: () -> Unit
) {
    ScreenColumn(modifier) {
        item { BodyText(stringResource(R.string.quick_clean_complete)) }
        item {
            HighlightCard(centered = true) {
                BodyText(stringResource(R.string.quick_clean_space_reclaimed))
                Emphasis(formatBytes(report.reclaimedBytes))
                BodyText(stringResource(R.string.quick_clean_removed_count, report.deletedItemCount))
            }
        }
        item { SectionTitle(stringResource(R.string.quick_clean_deleted_items)) }
        item {
            LowCard {
                DisposableCategory.entries.forEach { category ->
                    MetricRow(
                        stringResource(category.labelResource()),
                        (report.deletedByCategory[category] ?: 0).toString()
                    )
                }
            }
        }
        item {
            LowCard {
                SectionTitle(stringResource(R.string.quick_clean_failed_items))
                if (report.failedItems.isEmpty()) {
                    BodyText(stringResource(R.string.quick_clean_none))
                } else {
                    report.failedItems.forEach { BodyText(it.candidate.relativePath) }
                }
            }
        }
        item { Spacer(Modifier.height(TrashPilotSpacing.Large)) }
        item { PrimaryAction(stringResource(R.string.quick_clean_done), onDone) }
    }
}

@Composable
private fun CandidateRow(candidate: DisposableCandidate, checked: Boolean, onToggle: () -> Unit) {
    TrashPilotCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
        shape = TrashPilotRadii.CompactCardShape,
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            Modifier.padding(
                horizontal = TrashPilotSpacing.Standard,
                vertical = TrashPilotSpacing.MediumLarge
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f)) {
                Text(candidate.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    candidate.relativePath,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(formatBytes(candidate.sizeBytes), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ScreenColumn(modifier: Modifier, content: LazyListScope.() -> Unit) {
    LazyColumn(
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
}

@Composable
private fun HighlightCard(
    centered: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    TrashPilotCard(
        Modifier.fillMaxWidth(),
        shape = TrashPilotRadii.CardShape,
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            Modifier.padding(TrashPilotSpacing.Card),
            horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.MediumCompact),
            content = content
        )
    }
}

@Composable
private fun LowCard(content: @Composable ColumnScope.() -> Unit) {
    TrashPilotCard(
        Modifier.fillMaxWidth(),
        shape = TrashPilotRadii.CardShape,
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            Modifier.padding(
                horizontal = TrashPilotSpacing.CardDense,
                vertical = TrashPilotSpacing.Standard
            ),
            verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.MediumLarge),
            content = content
        )
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable private fun SectionTitle(text: String) =
    TrashPilotSectionHeader(text)

@Composable private fun BodyText(text: String) =
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable private fun Emphasis(text: String) =
    Text(text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)

@Composable
private fun PrimaryAction(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    TrashPilotPrimaryButton(
        text = text,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        shape = TrashPilotRadii.PillShape,
        fontWeight = FontWeight.SemiBold
    )
}

private fun Step.titleResource() = when (this) {
    Step.OVERVIEW -> R.string.quick_clean_title
    Step.REVIEW -> R.string.quick_clean_review_title
    Step.CONFIRMATION -> R.string.quick_clean_confirm_title
    Step.REPORT -> R.string.quick_clean_report_title
}

private fun DisposableCategory.labelResource() = when (this) {
    DisposableCategory.TEMPORARY_FILES -> R.string.quick_clean_temporary
    DisposableCategory.APP_CACHE -> R.string.quick_clean_cache
    DisposableCategory.EMPTY_FOLDERS -> R.string.quick_clean_empty_folders
    DisposableCategory.APK_LEFTOVERS -> R.string.quick_clean_apk_leftovers
    DisposableCategory.LOG_FILES -> R.string.quick_clean_logs
}
