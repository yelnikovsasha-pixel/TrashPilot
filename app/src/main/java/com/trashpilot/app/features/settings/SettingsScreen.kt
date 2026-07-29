@file:Suppress("LocalContextGetResourceValueCall")

package com.trashpilot.app.features.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
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
import androidx.compose.material.icons.outlined.AutoAwesome
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trashpilot.app.R
import com.trashpilot.app.core.settings.LanguagePreference
import com.trashpilot.app.core.settings.LanguagePreferences
import com.trashpilot.app.core.settings.SettingsBackup
import com.trashpilot.app.core.settings.SettingsBackupCodec
import com.trashpilot.app.core.settings.SettingsPreferences
import com.trashpilot.app.core.settings.ThemePreference
import com.trashpilot.app.core.settings.applyLanguagePreference
import com.trashpilot.app.core.trashdna.TrashDnaRepository
import com.trashpilot.app.ui.components.TrashPilotTopAppBar
import com.trashpilot.app.ui.components.TrashPilotCard
import com.trashpilot.app.ui.components.TrashPilotPrimaryButton
import com.trashpilot.app.ui.components.TrashPilotTextButton
import com.trashpilot.app.ui.theme.TrashPilotRadii
import com.trashpilot.app.ui.theme.TrashPilotSpacing
import com.trashpilot.app.ui.theme.TrashPilotComponentSizes
import com.trashpilot.app.ui.theme.TrashPilotIconSizes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Date

private enum class SettingsPage { OVERVIEW, APPEARANCE, LANGUAGE, DATA, PRIVACY, ABOUT, PRO }
private enum class PolicyKind { PRIVACY, TERMS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(repository: TrashDnaRepository, onBack: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember { SettingsPreferences(context) }
    val languagePreferences = remember(context) { LanguagePreferences(context.applicationContext) }
    val selectedLanguage by languagePreferences.selectedLanguage.collectAsState(
        initial = LanguagePreference.SYSTEM
    )
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var page by remember { mutableStateOf(SettingsPage.OVERVIEW) }
    var busy by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var confirmCache by remember { mutableStateOf(false) }
    var policy by remember { mutableStateOf<PolicyKind?>(null) }
    var pendingDocument by remember { mutableStateOf("") }

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
                SettingsPage.OVERVIEW -> OverviewPage(Modifier.padding(padding), onOpen = { page = it })
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
                    onReset = { confirmReset = true }
                )
                SettingsPage.PRIVACY -> PrivacyPage(Modifier.padding(padding), onPolicy = { policy = it })
                SettingsPage.ABOUT -> AboutPage(
                    modifier = Modifier.padding(padding),
                    version = appVersion(context),
                    onPolicy = { policy = it },
                    onFeedback = {
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:feedback@trashpilot.app"))
                            .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.settings_feedback_subject))
                        runCatching { context.startActivity(intent) }.onFailure {
                            scope.launch { snackbar.showSnackbar(context.getString(R.string.settings_no_email)) }
                        }
                    }
                )
                SettingsPage.PRO -> ProPage(Modifier.padding(padding))
            }
        }
    }

    if (confirmReset) ConfirmDialog(
        title = stringResource(R.string.settings_reset_title),
        body = stringResource(R.string.settings_reset_body),
        action = stringResource(R.string.settings_reset_action),
        onDismiss = { confirmReset = false },
        onConfirm = {
            confirmReset = false
            scope.launch {
                repository.resetLocalHistory()
                snackbar.showSnackbar(context.getString(R.string.settings_reset_done))
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
                withContext(Dispatchers.IO) { context.cacheDir.deleteContentsSafely() }
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
private fun OverviewPage(modifier: Modifier, onOpen: (SettingsPage) -> Unit) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(TrashPilotSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.MediumLarge)
    ) {
        item { Text(stringResource(R.string.settings_intro), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { SettingRow(Icons.Outlined.DarkMode, stringResource(R.string.settings_appearance), stringResource(R.string.settings_appearance_body)) { onOpen(SettingsPage.APPEARANCE) } }
        item { SettingRow(Icons.Outlined.Language, stringResource(R.string.settings_language), stringResource(R.string.settings_language_body)) { onOpen(SettingsPage.LANGUAGE) } }
        item { SettingRow(Icons.Outlined.Storage, stringResource(R.string.settings_data), stringResource(R.string.settings_data_body)) { onOpen(SettingsPage.DATA) } }
        item { SettingRow(Icons.Outlined.Lock, stringResource(R.string.nav_privacy), stringResource(R.string.settings_privacy_body)) { onOpen(SettingsPage.PRIVACY) } }
        item { SettingRow(Icons.Outlined.Info, stringResource(R.string.settings_about), stringResource(R.string.settings_about_body)) { onOpen(SettingsPage.ABOUT) } }
        item {
            TrashPilotCard(
                modifier = Modifier.fillMaxWidth().clickable { onOpen(SettingsPage.PRO) },
                shape = TrashPilotRadii.CardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(TrashPilotSpacing.Card)) {
                    Text(stringResource(R.string.settings_pro), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.settings_pro_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(TrashPilotSpacing.Medium))
                    TrashPilotPrimaryButton(
                        text = stringResource(R.string.settings_view_pro),
                        onClick = { onOpen(SettingsPage.PRO) },
                        modifier = Modifier.align(Alignment.End),
                        height = null
                    )
                }
            }
        }
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
    onReset: () -> Unit
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
        item { SettingRow(Icons.Outlined.DeleteSweep, stringResource(R.string.settings_reset_history), stringResource(R.string.settings_reset_history_body), onReset) }
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
private fun PrivacyPage(modifier: Modifier, onPolicy: (PolicyKind) -> Unit) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(TrashPilotSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)
    ) {
        item {
            TrashPilotCard(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(TrashPilotSpacing.Card)) {
                    Text(stringResource(R.string.splash_tagline), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.settings_privacy_local_body))
                }
            }
        }
        item { SettingRow(Icons.Outlined.Lock, stringResource(R.string.settings_privacy_policy), stringResource(R.string.settings_privacy_policy_body)) { onPolicy(PolicyKind.PRIVACY) } }
        item { SettingRow(Icons.Outlined.Description, stringResource(R.string.settings_terms), stringResource(R.string.settings_terms_body)) { onPolicy(PolicyKind.TERMS) } }
        item { Text(stringResource(R.string.settings_export_notice)) }
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
private fun ProPage(modifier: Modifier) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(TrashPilotSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)
    ) {
        item {
            TrashPilotCard(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = TrashPilotRadii.LargeShape
            ) {
                Column(Modifier.padding(TrashPilotSpacing.Screen)) {
                    Icon(Icons.Outlined.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(TrashPilotSpacing.Standard))
                    Text(stringResource(R.string.settings_more_control), style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(R.string.settings_pro_preview))
                }
            }
        }
        items(listOf(R.string.settings_pro_filters, R.string.settings_pro_scans, R.string.settings_pro_appearance)) {
            TrashPilotCard(Modifier.fillMaxWidth()) { Text(stringResource(R.string.settings_pro_feature, stringResource(it)), Modifier.padding(TrashPilotSpacing.Card), style = MaterialTheme.typography.bodyLarge) }
        }
        item {
            TrashPilotPrimaryButton(
                text = stringResource(R.string.settings_upgrade_unavailable),
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                height = null
            )
        }
        item { Text(stringResource(R.string.settings_without_pro), style = MaterialTheme.typography.bodySmall) }
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
    SettingsPage.DATA -> R.string.settings_data
    SettingsPage.PRIVACY -> R.string.nav_privacy
    SettingsPage.ABOUT -> R.string.settings_about
    SettingsPage.PRO -> R.string.settings_pro
})

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

private fun File.deleteContentsSafely() {
    listFiles()?.forEach { child ->
        if (child.isDirectory) child.deleteContentsSafely()
        child.delete()
    }
}
