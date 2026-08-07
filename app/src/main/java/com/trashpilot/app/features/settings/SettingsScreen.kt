@file:Suppress("LocalContextGetResourceValueCall")

package com.trashpilot.app.features.settings

import android.app.Activity
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.trashpilot.app.R
import com.trashpilot.app.core.settings.LanguagePreference
import com.trashpilot.app.core.settings.LanguagePreferences
import com.trashpilot.app.core.settings.SettingsBackup
import com.trashpilot.app.core.settings.SettingsBackupCodec
import com.trashpilot.app.core.settings.SettingsPreferences
import com.trashpilot.app.core.settings.ThemePreference
import com.trashpilot.app.core.settings.applyLanguagePreference
import com.trashpilot.app.core.settings.PermissionAccessState
import com.trashpilot.app.core.settings.mediaAccessState
import com.trashpilot.app.core.settings.safAccessState
import com.trashpilot.app.core.settings.usageAccessState
import com.trashpilot.app.core.cache.CacheCapability
import com.trashpilot.app.core.cache.OwnCacheCleaner
import com.trashpilot.app.core.cache.RealCacheAnalyzer
import com.trashpilot.app.core.trashdna.TrashDnaRepository
import com.trashpilot.app.ui.components.TrashPilotTopAppBar
import com.trashpilot.app.ui.components.TrashPilotCard
import com.trashpilot.app.ui.components.TrashPilotTextButton
import com.trashpilot.app.ui.theme.TrashPilotRadii
import com.trashpilot.app.ui.theme.TrashPilotSpacing
import com.trashpilot.app.ui.theme.TrashPilotComponentSizes
import com.trashpilot.app.ui.theme.TrashPilotIconSizes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

private enum class SettingsPage { OVERVIEW, APPEARANCE, LANGUAGE, DATA, PRIVACY, ABOUT }
private enum class PolicyKind { PRIVACY, TERMS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(repository: TrashDnaRepository, onBack: () -> Unit, onViewIntroduction: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember { SettingsPreferences(context) }
    val languagePreferences = remember(context) { LanguagePreferences(context.applicationContext) }
    val selectedLanguage by languagePreferences.selectedLanguage.collectAsState(
        initial = LanguagePreference.SYSTEM
    )
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbar = remember { SnackbarHostState() }
    var page by rememberSaveable { mutableStateOf(SettingsPage.OVERVIEW) }
    var busy by remember { mutableStateOf(false) }
    var confirmReports by rememberSaveable { mutableStateOf(false) }
    var confirmTrashDna by rememberSaveable { mutableStateOf(false) }
    var confirmCache by rememberSaveable { mutableStateOf(false) }
    var policy by remember { mutableStateOf<PolicyKind?>(null) }
    var pendingDocument by remember { mutableStateOf("") }
    var accessRefresh by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) accessRefresh++ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val permissionSnapshot = remember(accessRefresh) {
        SettingsPermissionSnapshot(
            currentPhotoAccessState(context),
            currentAudioAccessState(context),
            currentUsageAccessState(context),
            safAccessState(context.contentResolver.persistedUriPermissions.size)
        )
    }

    val createText = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(pendingDocument) }
                        ?: error(context.getString(R.string.settings_destination_error))
                }
            }.onSuccess { snackbar.showSnackbar(context.getString(R.string.settings_file_saved)) }
                .onFailure { snackbar.showSnackbar(it.message ?: context.getString(R.string.settings_file_save_error)) }
        }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            runCatching {
                val backup = withContext(Dispatchers.IO) {
                    val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: error(context.getString(R.string.settings_backup_open_error))
                    SettingsBackupCodec.decode(text)
                }
                repository.replaceLocalHistory(backup.sessions)
                val restoredLanguage = preferences.restoreValues(backup.preferences)
                languagePreferences.setSelectedLanguage(restoredLanguage)
            }.onSuccess {
                snackbar.showSnackbar(context.getString(R.string.settings_restored))
                (context as? Activity)?.recreate()
            }.onFailure { snackbar.showSnackbar(it.message ?: context.getString(R.string.settings_restore_failed)) }
            busy = false
        }
    }

    fun export(name: String, content: String) {
        pendingDocument = content
        createText.launch(name)
    }

    fun openAndroidSettings(intent: Intent) {
        val available = intent.resolveActivity(context.packageManager) != null
        if (available) runCatching { context.startActivity(intent) }.onFailure {
            scope.launch { snackbar.showSnackbar(context.getString(R.string.settings_android_settings_unavailable)) }
        } else scope.launch { snackbar.showSnackbar(context.getString(R.string.settings_android_settings_unavailable)) }
    }

    BackHandler {
        if (page == SettingsPage.OVERVIEW) onBack() else page = SettingsPage.OVERVIEW
    }

    Scaffold(
        topBar = {
            TrashPilotTopAppBar(
                title = page.title(),
                onBack = {
                    if (page == SettingsPage.OVERVIEW) onBack() else page = SettingsPage.OVERVIEW
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (busy) {
            Column(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(TrashPilotSpacing.Large))
                Text(stringResource(R.string.settings_working))
            }
        } else {
            when (page) {
                SettingsPage.OVERVIEW -> OverviewPage(Modifier.padding(padding), onOpen = { page = it }, onViewIntroduction = onViewIntroduction)
                SettingsPage.APPEARANCE -> AppearancePage(
                    modifier = Modifier.padding(padding),
                    selected = preferences.theme,
                    onSelected = {
                        preferences.theme = it
                        (context as? Activity)?.recreate()
                    }
                )
                SettingsPage.LANGUAGE -> LanguagePage(
                    modifier = Modifier.padding(padding),
                    selected = selectedLanguage,
                    onSelected = {
                        scope.launch {
                            languagePreferences.setSelectedLanguage(it)
                            applyLanguagePreference(context, it)
                            (context as? Activity)?.recreate()
                        }
                    }
                )
                SettingsPage.DATA -> DataPage(
                    modifier = Modifier.padding(padding),
                    onDiagnostics = {
                        scope.launch {
                            val history = repository.loadReportHistory()
                            export("trashpilot-diagnostics.txt", diagnosticsText(context, history.size))
                        }
                    },
                    onBackup = {
                        scope.launch {
                            val backup = SettingsBackup(
                                preferences.exportValues(selectedLanguage),
                                repository.loadReportHistory()
                            )
                            export("trashpilot-backup.tpbackup", SettingsBackupCodec.encode(backup))
                        }
                    },
                    onRestore = { restore.launch(arrayOf("text/plain", "application/octet-stream")) },
                    onClearCache = { confirmCache = true },
                    onClearReports = { confirmReports = true },
                    onResetTrashDna = { confirmTrashDna = true }
                )
                SettingsPage.PRIVACY -> PrivacyPage(
                    Modifier.padding(padding),
                    photoState = permissionSnapshot.photos,
                    audioState = permissionSnapshot.audio,
                    usageState = permissionSnapshot.usage,
                    safState = permissionSnapshot.saf,
                    onReviewPermissions = { openAndroidSettings(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())) },
                    onReviewUsageAccess = { openAndroidSettings(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                    onPolicy = { policy = it }
                )
                SettingsPage.ABOUT -> AboutPage(
                    modifier = Modifier.padding(padding),
                    version = appVersion(context),
                    onPolicy = { policy = it },
                    onFeedback = {
                        val intent = Intent(Intent.ACTION_SENDTO, "mailto:feedback@trashpilot.app".toUri())
                            .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.settings_feedback_subject))
                        runCatching { context.startActivity(intent) }.onFailure {
                            scope.launch { snackbar.showSnackbar(context.getString(R.string.settings_no_email)) }
                        }
                    }
                )
            }
        }
    }

    if (confirmReports) ConfirmDialog(
        title = stringResource(R.string.settings_clear_reports_title),
        body = stringResource(R.string.settings_clear_reports_body),
        action = stringResource(R.string.settings_clear_reports_action),
        onDismiss = { confirmReports = false },
        onConfirm = {
            confirmReports = false
            scope.launch {
                repository.clearReportHistory()
                snackbar.showSnackbar(context.getString(R.string.settings_clear_reports_done))
            }
        }
    )
    if (confirmTrashDna) ConfirmDialog(
        title = stringResource(R.string.settings_reset_trash_dna_title),
        body = stringResource(R.string.settings_reset_trash_dna_body),
        action = stringResource(R.string.settings_reset_trash_dna_action),
        onDismiss = { confirmTrashDna = false },
        onConfirm = {
            confirmTrashDna = false
            scope.launch {
                repository.resetTrashDnaHistory()
                snackbar.showSnackbar(context.getString(R.string.settings_reset_trash_dna_done))
            }
        }
    )
    if (confirmCache) ConfirmDialog(
        title = stringResource(R.string.settings_cache_title),
        body = stringResource(R.string.settings_cache_body),
        action = stringResource(R.string.settings_cache_action),
        onDismiss = { confirmCache = false },
        onConfirm = {
            confirmCache = false
            scope.launch {
                OwnCacheCleaner(context).clean()
                snackbar.showSnackbar(context.getString(R.string.settings_cache_done))
            }
        }
    )
    policy?.let { kind ->
        AlertDialog(
            onDismissRequest = { policy = null },
            title = { Text(stringResource(if (kind == PolicyKind.PRIVACY) R.string.settings_privacy_policy else R.string.settings_terms)) },
            text = { Text(policyText(kind)) },
            confirmButton = {
                TrashPilotTextButton(
                    text = stringResource(R.string.quick_clean_done),
                    onClick = { policy = null }
                )
            }
        )
    }
}

@Composable
private fun OverviewPage(modifier: Modifier, onOpen: (SettingsPage) -> Unit, onViewIntroduction: () -> Unit) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(TrashPilotSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.MediumLarge)
    ) {
        item { Text(stringResource(R.string.settings_intro), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { SettingRow(Icons.Outlined.DarkMode, stringResource(R.string.settings_appearance), stringResource(R.string.settings_appearance_body)) { onOpen(SettingsPage.APPEARANCE) } }
        item { SettingRow(Icons.Outlined.Language, stringResource(R.string.settings_language), stringResource(R.string.settings_language_body)) { onOpen(SettingsPage.LANGUAGE) } }
        item { SettingRow(Icons.Outlined.Lock, stringResource(R.string.settings_privacy_permissions), stringResource(R.string.settings_privacy_permissions_body)) { onOpen(SettingsPage.PRIVACY) } }
        item { SettingRow(Icons.Outlined.Storage, stringResource(R.string.settings_data_history), stringResource(R.string.settings_data_history_body)) { onOpen(SettingsPage.DATA) } }
        item { SettingRow(Icons.Outlined.Info, stringResource(R.string.settings_view_introduction), stringResource(R.string.settings_view_introduction_body), onViewIntroduction) }
        item { SettingRow(Icons.Outlined.Info, stringResource(R.string.settings_about), stringResource(R.string.settings_about_body)) { onOpen(SettingsPage.ABOUT) } }
        item { Text(stringResource(R.string.settings_local_preferences), style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun AppearancePage(modifier: Modifier, selected: ThemePreference, onSelected: (ThemePreference) -> Unit) {
    ChoicePage(modifier, stringResource(R.string.settings_choose_appearance), ThemePreference.entries, selected, onSelected) {
        when (it) {
            ThemePreference.SYSTEM -> stringResource(R.string.settings_theme_system)
            ThemePreference.LIGHT -> stringResource(R.string.settings_theme_light)
            ThemePreference.DARK -> stringResource(R.string.settings_theme_dark)
        }
    }
}

@Composable
private fun LanguagePage(modifier: Modifier, selected: LanguagePreference, onSelected: (LanguagePreference) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) { filterLanguages(query) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = TrashPilotSpacing.Screen,
            top = TrashPilotSpacing.Standard,
            end = TrashPilotSpacing.Screen,
            bottom = TrashPilotSpacing.Screen
        ),
        verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Medium)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.settings_select_language),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TrashPilotCard(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = TrashPilotRadii.SmallShape
                ) {
                    Text(
                        stringResource(R.string.settings_language_count),
                        Modifier.padding(
                            horizontal = TrashPilotSpacing.MediumLarge,
                            vertical = TrashPilotSpacing.Compact
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = TrashPilotRadii.ControlShape,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                placeholder = { Text(stringResource(R.string.settings_search_languages)) }
            )
        }
        if (filtered.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.settings_no_languages),
                    modifier = Modifier.fillMaxWidth().padding(vertical = TrashPilotSpacing.Screen),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(filtered, key = LanguagePreference::name) { language ->
                TrashPilotCard(
                    modifier = Modifier.fillMaxWidth().clickable { onSelected(language) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (language == selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        }
                    ),
                    shape = TrashPilotRadii.ControlShape
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(TrashPilotComponentSizes.LanguageRowHeight)
                            .padding(horizontal = TrashPilotSpacing.CardDense),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            language.label,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (language == selected) {
                            Text(
                                stringResource(R.string.settings_selected_mark),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun filterLanguages(query: String): List<LanguagePreference> {
    val normalized = query.trim()
    return if (normalized.isEmpty()) {
        LanguagePreference.entries
    } else {
        LanguagePreference.entries.filter {
            it.label.contains(normalized, ignoreCase = true) ||
                it.tag.contains(normalized, ignoreCase = true)
        }
    }
}

@Composable
private fun <T> ChoicePage(
    modifier: Modifier,
    intro: String,
    choices: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    label: @Composable (T) -> String
) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(TrashPilotSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.MediumLarge)
    ) {
        item { Text(intro, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(choices) { choice ->
            TrashPilotCard(
                modifier = Modifier.fillMaxWidth().clickable { onSelected(choice) },
                colors = CardDefaults.cardColors(
                    containerColor = if (choice == selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(TrashPilotSpacing.Large),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label(choice), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    RadioButton(selected = choice == selected, onClick = { onSelected(choice) })
                }
            }
        }
    }
}

@Composable
private fun DataPage(
    modifier: Modifier,
    onDiagnostics: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onClearCache: () -> Unit,
    onClearReports: () -> Unit,
    onResetTrashDna: () -> Unit
) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(TrashPilotSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.MediumLarge)
    ) {
        item { Text(stringResource(R.string.settings_manage_data), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { SettingRow(Icons.Outlined.Description, stringResource(R.string.settings_export_diagnostics), stringResource(R.string.settings_export_diagnostics_body), onDiagnostics) }
        item { SettingRow(Icons.Outlined.Storage, stringResource(R.string.settings_backup), stringResource(R.string.settings_backup_body), onBackup) }
        item { SettingRow(Icons.Outlined.Storage, stringResource(R.string.settings_restore), stringResource(R.string.settings_restore_body), onRestore) }
        item { SettingRow(Icons.Outlined.DeleteSweep, stringResource(R.string.settings_clear_cache), stringResource(R.string.settings_clear_cache_body), onClearCache) }
        item { SettingRow(Icons.Outlined.DeleteSweep, stringResource(R.string.settings_clear_reports), stringResource(R.string.settings_clear_reports_row_body), onClearReports) }
        item { SettingRow(Icons.Outlined.DeleteSweep, stringResource(R.string.settings_reset_trash_dna), stringResource(R.string.settings_reset_trash_dna_row_body), onResetTrashDna) }
        item {
            TrashPilotCard(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(TrashPilotSpacing.CardDense)) {
                    Text(stringResource(R.string.settings_files_safe), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.settings_files_safe_body))
                }
            }
        }
    }
}

@Composable
private fun PrivacyPage(
    modifier: Modifier,
    photoState: PermissionAccessState,
    audioState: PermissionAccessState,
    usageState: PermissionAccessState,
    safState: PermissionAccessState,
    onReviewPermissions: () -> Unit,
    onReviewUsageAccess: () -> Unit,
    onPolicy: (PolicyKind) -> Unit
) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(TrashPilotSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)
    ) {
        item { Text(stringResource(R.string.settings_permissions_explanation), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { PermissionRow(Icons.Outlined.Storage, stringResource(R.string.settings_photos_videos_access), photoState, onReviewPermissions) }
        item { PermissionRow(Icons.Outlined.Storage, stringResource(R.string.settings_audio_access), audioState, onReviewPermissions) }
        item { PermissionRow(Icons.Outlined.Storage, stringResource(R.string.settings_usage_access), usageState, onReviewUsageAccess) }
        item { PermissionRow(Icons.Outlined.Storage, stringResource(R.string.settings_saf_access), safState, onReviewPermissions) }
        item { SettingRow(Icons.Outlined.Lock, stringResource(R.string.settings_privacy_policy), stringResource(R.string.settings_privacy_policy_body)) { onPolicy(PolicyKind.PRIVACY) } }
        item { SettingRow(Icons.Outlined.Description, stringResource(R.string.settings_terms), stringResource(R.string.settings_terms_body)) { onPolicy(PolicyKind.TERMS) } }
        item { Text(stringResource(R.string.settings_permissions_contextual), style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun AboutPage(modifier: Modifier, version: String, onPolicy: (PolicyKind) -> Unit, onFeedback: () -> Unit) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(TrashPilotSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            TrashPilotCard(shape = CircleShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Column(
                    Modifier.size(TrashPilotComponentSizes.AboutMark),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.brand_monogram), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.headlineMedium)
                }
            }
        }
        item { Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall) }
        item { Text(version, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { SettingRow(Icons.Outlined.Description, stringResource(R.string.settings_send_feedback), stringResource(R.string.settings_send_feedback_body), onFeedback) }
        item { SettingRow(Icons.Outlined.Lock, stringResource(R.string.settings_privacy_policy), stringResource(R.string.settings_stored_app)) { onPolicy(PolicyKind.PRIVACY) } }
        item { SettingRow(Icons.Outlined.Description, stringResource(R.string.settings_terms), stringResource(R.string.settings_stored_app)) { onPolicy(PolicyKind.TERMS) } }
    }
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    TrashPilotCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = TrashPilotRadii.CompactCardShape
    ) {
        Row(
            Modifier.fillMaxWidth().padding(TrashPilotSpacing.HomeCard),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrashPilotCard(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = TrashPilotRadii.SmallShape
            ) {
                Icon(
                    icon,
                    null,
                    Modifier
                        .padding(TrashPilotSpacing.MediumLarge)
                        .size(TrashPilotIconSizes.SettingsRow),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(
                Modifier.weight(1f).padding(horizontal = TrashPilotSpacing.Standard)
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.ChevronRight, null)
        }
    }
}

@Composable
private fun PermissionRow(icon: ImageVector, title: String, state: PermissionAccessState, onClick: () -> Unit) {
    SettingRow(icon, title, permissionStateLabel(state), onClick)
}

@Composable
private fun permissionStateLabel(state: PermissionAccessState): String = stringResource(when (state) {
    PermissionAccessState.GRANTED -> R.string.settings_access_granted
    PermissionAccessState.LIMITED -> R.string.settings_access_limited
    PermissionAccessState.NOT_GRANTED -> R.string.settings_access_not_granted
    PermissionAccessState.MANAGED_BY_ANDROID -> R.string.settings_access_managed_android
    PermissionAccessState.UNSUPPORTED -> R.string.settings_access_unsupported
})

@Composable
private fun ConfirmDialog(title: String, body: String, action: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        dismissButton = {
            TrashPilotTextButton(
                text = stringResource(R.string.quick_clean_cancel),
                onClick = onDismiss
            )
        },
        confirmButton = {
            TrashPilotTextButton(
                text = action,
                onClick = onConfirm
            )
        }
    )
}

@Composable
private fun SettingsPage.title(): String = stringResource(when (this) {
    SettingsPage.OVERVIEW -> R.string.nav_settings
    SettingsPage.APPEARANCE -> R.string.settings_appearance
    SettingsPage.LANGUAGE -> R.string.settings_language
    SettingsPage.DATA -> R.string.settings_data_history
    SettingsPage.PRIVACY -> R.string.settings_privacy_permissions
    SettingsPage.ABOUT -> R.string.settings_about
})

private data class SettingsPermissionSnapshot(
    val photos: PermissionAccessState,
    val audio: PermissionAccessState,
    val usage: PermissionAccessState,
    val saf: PermissionAccessState
)

private fun currentPhotoAccessState(context: android.content.Context): PermissionAccessState {
    val full = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
    } else ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    val partial = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
    return mediaAccessState(full, partial)
}

private fun currentAudioAccessState(context: android.content.Context): PermissionAccessState {
    val granted = ContextCompat.checkSelfPermission(
        context,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED
    return mediaAccessState(granted)
}

private fun currentUsageAccessState(context: android.content.Context): PermissionAccessState {
    val capability = RealCacheAnalyzer(context).capability()
    return usageAccessState(
        supported = capability != CacheCapability.UNSUPPORTED_ANDROID_VERSION,
        granted = capability == CacheCapability.AVAILABLE
    )
}

private fun appVersion(context: android.content.Context): String {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        info.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        info.versionCode.toLong()
    }
    return context.getString(R.string.settings_version, info.versionName, versionCode)
}

private fun diagnosticsText(context: android.content.Context, historyCount: Int): String = buildString {
    appendLine(context.getString(R.string.settings_diagnostics_title))
    appendLine(context.getString(R.string.settings_diagnostics_generated, DateFormat.getDateTimeInstance().format(Date())))
    appendLine(appVersion(context))
    appendLine(context.getString(R.string.settings_diagnostics_android, android.os.Build.VERSION.RELEASE, android.os.Build.VERSION.SDK_INT))
    appendLine(context.getString(R.string.settings_diagnostics_device, android.os.Build.MANUFACTURER, android.os.Build.MODEL))
    appendLine(context.getString(R.string.settings_diagnostics_history, historyCount))
    appendLine(context.getString(R.string.settings_diagnostics_excluded))
}

@Composable
private fun policyText(kind: PolicyKind): String = stringResource(
    if (kind == PolicyKind.PRIVACY) R.string.settings_privacy_policy_text
    else R.string.settings_terms_text
)
