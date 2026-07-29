@file:Suppress("LocalContextGetResourceValueCall")

package com.trashpilot.app.features.privacy

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trashpilot.app.R
import com.trashpilot.app.core.privacy.InstalledAppPermissionReader
import com.trashpilot.app.core.privacy.PrivacyApp
import com.trashpilot.app.core.privacy.PrivacyPermissionCategory
import com.trashpilot.app.core.privacy.PrivacySnapshot
import com.trashpilot.app.ui.components.TrashPilotTopAppBar
import com.trashpilot.app.ui.components.TrashPilotCard
import com.trashpilot.app.ui.components.TrashPilotPrimaryButton
import com.trashpilot.app.ui.components.TrashPilotTextButton
import com.trashpilot.app.ui.theme.TrashPilotRadii
import com.trashpilot.app.ui.theme.TrashPilotSpacing
import com.trashpilot.app.ui.theme.TrashPilotTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class PrivacyPage { OVERVIEW, CATEGORIES, APPS, DETAILS, RECOMMENDATIONS }
private enum class AppSort { PERMISSION_COUNT, NAME }
private enum class CategorySort { APP_COUNT, NAME }

private sealed interface PrivacyUiState {
    data object Loading : PrivacyUiState
    data class Success(val snapshot: PrivacySnapshot) : PrivacyUiState
    data class Error(val message: String) : PrivacyUiState
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PrivacyMonitorScreen(
    onBack: () -> Unit,
    onSnapshotLoaded: (PrivacySnapshot) -> Unit = {},
    loadSnapshot: (suspend () -> PrivacySnapshot)? = null
) {
    val context = LocalContext.current
    val snapshotLoader = remember(context, loadSnapshot) {
        loadSnapshot ?: suspend {
            InstalledAppPermissionReader(context.applicationContext).read()
        }
    }
    var state: PrivacyUiState by remember { mutableStateOf(PrivacyUiState.Loading) }
    var page by remember { mutableStateOf(PrivacyPage.OVERVIEW) }
    var selectedCategory by remember { mutableStateOf(PrivacyPermissionCategory.CAMERA) }

    LaunchedEffect(Unit) {
        state = runCatching { withContext(Dispatchers.IO) { snapshotLoader() } }
            .fold(
                onSuccess = {
                    onSnapshotLoaded(it)
                    PrivacyUiState.Success(it)
                },
                onFailure = { PrivacyUiState.Error(it.message ?: context.getString(R.string.privacy_error_android)) }
            )
    }

    BackHandler {
        if (page == PrivacyPage.OVERVIEW) onBack() else page = PrivacyPage.OVERVIEW
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TrashPilotTopAppBar(
                title = page.title(),
                onBack = {
                    if (page == PrivacyPage.OVERVIEW) onBack() else page = PrivacyPage.OVERVIEW
                }
            )
        }
    ) { padding ->
        when (val current = state) {
            PrivacyUiState.Loading -> StateCard(
                modifier = Modifier.padding(padding),
                title = stringResource(R.string.privacy_loading_title),
                body = stringResource(R.string.privacy_loading_body)
            )
            is PrivacyUiState.Error -> StateCard(
                modifier = Modifier.padding(padding),
                title = stringResource(R.string.privacy_error_title),
                body = current.message
            )
            is PrivacyUiState.Success -> when (page) {
                PrivacyPage.OVERVIEW -> Overview(
                    current.snapshot,
                    Modifier.padding(padding),
                    onCategories = { page = PrivacyPage.CATEGORIES },
                    onApps = { page = PrivacyPage.APPS },
                    onRecommendations = { page = PrivacyPage.RECOMMENDATIONS }
                )
                PrivacyPage.CATEGORIES -> Categories(
                    current.snapshot,
                    Modifier.padding(padding)
                ) {
                    selectedCategory = it
                    page = PrivacyPage.DETAILS
                }
                PrivacyPage.APPS -> Apps(current.snapshot, Modifier.padding(padding))
                PrivacyPage.DETAILS -> Details(
                    current.snapshot,
                    selectedCategory,
                    Modifier.padding(padding)
                )
                PrivacyPage.RECOMMENDATIONS ->
                    Recommendations(current.snapshot, Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun Overview(
    snapshot: PrivacySnapshot,
    modifier: Modifier,
    onCategories: () -> Unit,
    onApps: () -> Unit,
    onRecommendations: () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(TrashPilotSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Large)
    ) {
        item {
            Text(
                stringResource(R.string.privacy_intro),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            InfoCard(
                stringResource(R.string.privacy_glance_title),
                stringResource(R.string.privacy_glance_body),
                highlighted = true
            )
        }
        item {
            MetricCard(
                listOf(
                    stringResource(R.string.privacy_apps_checked) to snapshot.appsChecked.toString(),
                    stringResource(R.string.privacy_sensitive_apps) to snapshot.sensitiveAppCount.toString(),
                    stringResource(R.string.privacy_categories) to PrivacyPermissionCategory.entries.size.toString()
                )
            )
        }
        item {
            InfoCard(
                stringResource(R.string.privacy_on_device_title),
                stringResource(R.string.privacy_on_device_body)
            )
        }
        item {
            TrashPilotPrimaryButton(
                text = stringResource(R.string.privacy_view_categories),
                onClick = onCategories,
                modifier = Modifier.fillMaxWidth(),
                height = null
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TrashPilotTextButton(
                    text = stringResource(R.string.privacy_sensitive_apps_short),
                    onClick = onApps
                )
                TrashPilotTextButton(
                    text = stringResource(R.string.privacy_recommendations),
                    onClick = onRecommendations
                )
            }
        }
    }
}

@Composable
private fun Categories(
    snapshot: PrivacySnapshot,
    modifier: Modifier,
    onSelect: (PrivacyPermissionCategory) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(CategorySort.APP_COUNT) }
    val labels = PrivacyPermissionCategory.entries.associateWith { it.label() }
    val categories = PrivacyPermissionCategory.entries
        .filter { labels.getValue(it).contains(query, ignoreCase = true) }
        .let { list ->
            when (sort) {
                CategorySort.APP_COUNT -> list.sortedByDescending { snapshot.categoryCounts[it] ?: 0 }
                CategorySort.NAME -> list.sortedBy { labels.getValue(it) }
            }
        }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(TrashPilotSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.MediumLarge)
    ) {
        item { SearchField(query, stringResource(R.string.privacy_search_permissions)) { query = it } }
        item {
            SortRow {
                FilterChip(
                    selected = sort == CategorySort.APP_COUNT,
                    onClick = { sort = CategorySort.APP_COUNT },
                    label = { Text(stringResource(R.string.privacy_sort_app_count)) }
                )
                FilterChip(
                    selected = sort == CategorySort.NAME,
                    onClick = { sort = CategorySort.NAME },
                    label = { Text(stringResource(R.string.results_sort_name)) }
                )
            }
        }
        if (categories.isEmpty()) {
            item { InfoCard(stringResource(R.string.privacy_no_categories), stringResource(R.string.privacy_try_search)) }
        } else {
            items(categories) { category ->
                PermissionRow(
                    title = category.label(),
                    count = snapshot.categoryCounts[category] ?: 0,
                    onClick = { onSelect(category) }
                )
            }
        }
    }
}

@Composable
private fun Apps(snapshot: PrivacySnapshot, modifier: Modifier) {
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(AppSort.PERMISSION_COUNT) }
    val apps = snapshot.apps.filter { it.declaredCategories.isNotEmpty() }
        .filter { it.label.contains(query, true) || it.packageName.contains(query, true) }
        .let { list ->
            when (sort) {
                AppSort.PERMISSION_COUNT -> list.sortedByDescending { it.declaredCategories.size }
                AppSort.NAME -> list.sortedBy { it.label.lowercase() }
            }
        }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(TrashPilotSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.MediumLarge)
    ) {
        item { SearchField(query, stringResource(R.string.privacy_search_apps)) { query = it } }
        item {
            SortRow {
                FilterChip(
                    selected = sort == AppSort.PERMISSION_COUNT,
                    onClick = { sort = AppSort.PERMISSION_COUNT },
                    label = { Text(stringResource(R.string.privacy_sort_permission_count)) }
                )
                FilterChip(
                    selected = sort == AppSort.NAME,
                    onClick = { sort = AppSort.NAME },
                    label = { Text(stringResource(R.string.privacy_sort_app_name)) }
                )
            }
        }
        if (apps.isEmpty()) {
            item {
                InfoCard(
                    stringResource(R.string.privacy_no_apps),
                    stringResource(R.string.privacy_no_apps_body)
                )
            }
        } else {
            items(apps, key = PrivacyApp::packageName) { app -> AppRow(app) }
        }
    }
}

@Composable
private fun Details(
    snapshot: PrivacySnapshot,
    category: PrivacyPermissionCategory,
    modifier: Modifier
) {
    val context = LocalContext.current
    val apps = snapshot.apps.filter { category in it.declaredCategories }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(TrashPilotSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.HomeCard)
    ) {
        item {
            InfoCard(
                category.label(),
                stringResource(R.string.privacy_apps_declare, apps.size),
                highlighted = true
            )
        }
        item { InfoCard(stringResource(R.string.privacy_what_means), category.explanation()) }
        if (apps.isEmpty()) {
            item { InfoCard(stringResource(R.string.privacy_no_category_apps), stringResource(R.string.privacy_no_declarations)) }
        } else {
            items(apps, key = PrivacyApp::packageName) { AppRow(it, category) }
        }
        item {
            TrashPilotPrimaryButton(
                text = stringResource(R.string.privacy_open_settings),
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_PRIVACY_SETTINGS))
                },
                modifier = Modifier.fillMaxWidth(),
                height = null
            )
        }
    }
}

@Composable
private fun Recommendations(snapshot: PrivacySnapshot, modifier: Modifier) {
    val recommendations = buildList {
        if ((snapshot.categoryCounts[PrivacyPermissionCategory.BACKGROUND_LOCATION] ?: 0) > 0) {
            add(stringResource(R.string.privacy_review_background_title) to
                stringResource(R.string.privacy_review_background_body))
        }
        if ((snapshot.categoryCounts[PrivacyPermissionCategory.CAMERA] ?: 0) > 0 ||
            (snapshot.categoryCounts[PrivacyPermissionCategory.MICROPHONE] ?: 0) > 0
        ) {
            add(stringResource(R.string.privacy_review_av_title) to
                stringResource(R.string.privacy_review_av_body))
        }
        if (snapshot.sensitiveAppCount > 0) {
            add(stringResource(R.string.privacy_android_controls_title) to
                stringResource(R.string.privacy_android_controls_body))
        }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(TrashPilotSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.HomeCard)
    ) {
        item {
            InfoCard(
                stringResource(R.string.privacy_no_invented_title),
                stringResource(R.string.privacy_no_invented_body),
                highlighted = true
            )
        }
        if (recommendations.isEmpty()) {
            item {
                InfoCard(
                    stringResource(R.string.privacy_no_recommendations),
                    stringResource(R.string.privacy_no_recommendations_body)
                )
            }
        } else {
            items(recommendations) { (title, body) -> InfoCard(title, body) }
        }
        item {
            InfoCard(
                stringResource(R.string.privacy_local_review_title),
                stringResource(R.string.privacy_local_review_body)
            )
        }
    }
}

@Composable
private fun SearchField(value: String, placeholder: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        placeholder = { Text(placeholder) },
        shape = TrashPilotRadii.LargeShape
    )
}

@Composable
private fun SortRow(content: @Composable RowScope.() -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Medium), content = content)
}

@Composable
private fun PermissionRow(title: String, count: Int, onClick: () -> Unit) {
    TrashPilotCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = TrashPilotRadii.CompactCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(TrashPilotSpacing.CardDense),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(stringResource(R.string.privacy_app_count, count))
        }
    }
}

@Composable
private fun AppRow(app: PrivacyApp, category: PrivacyPermissionCategory? = null) {
    TrashPilotCard(
        modifier = Modifier.fillMaxWidth(),
        shape = TrashPilotRadii.CompactCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            Modifier.padding(TrashPilotSpacing.CardDense),
            verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Small)
        ) {
            Text(app.label, fontWeight = FontWeight.SemiBold, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(
                app.packageName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                category?.let {
                    if (it in app.grantedCategories) {
                        stringResource(R.string.privacy_granted)
                    } else {
                        stringResource(R.string.privacy_declared_not_granted)
                    }
                } ?: stringResource(R.string.privacy_sensitive_category_count, app.declaredCategories.size),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun MetricCard(rows: List<Pair<String, String>>) {
    TrashPilotCard(
        shape = TrashPilotRadii.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            Modifier.padding(TrashPilotSpacing.Card),
            verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.HomeCard)
        ) {
            rows.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label)
                    Text(value, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    body: String,
    highlighted: Boolean = false
) {
    TrashPilotCard(
        shape = TrashPilotRadii.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
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
private fun StateCard(modifier: Modifier, title: String, body: String) {
    Column(modifier.fillMaxSize().padding(TrashPilotSpacing.Screen)) {
        InfoCard(title, body, highlighted = true)
    }
}

@Composable
private fun PrivacyPage.title() = stringResource(when (this) {
    PrivacyPage.OVERVIEW -> R.string.privacy_monitor_title
    PrivacyPage.CATEGORIES -> R.string.privacy_page_categories
    PrivacyPage.APPS -> R.string.privacy_page_sensitive
    PrivacyPage.DETAILS -> R.string.privacy_page_details
    PrivacyPage.RECOMMENDATIONS -> R.string.privacy_recommendations
})

@Composable
private fun PrivacyPermissionCategory.label() = stringResource(when (this) {
    PrivacyPermissionCategory.CAMERA -> R.string.permission_camera
    PrivacyPermissionCategory.MICROPHONE -> R.string.permission_microphone
    PrivacyPermissionCategory.LOCATION -> R.string.permission_location
    PrivacyPermissionCategory.CONTACTS -> R.string.permission_contacts
    PrivacyPermissionCategory.CALENDAR -> R.string.permission_calendar
    PrivacyPermissionCategory.SMS -> R.string.permission_sms
    PrivacyPermissionCategory.PHONE -> R.string.permission_phone
    PrivacyPermissionCategory.NEARBY_DEVICES -> R.string.permission_nearby
    PrivacyPermissionCategory.NOTIFICATIONS -> R.string.permission_notifications
    PrivacyPermissionCategory.BACKGROUND_LOCATION -> R.string.permission_background_location
})

@Composable
private fun PrivacyPermissionCategory.explanation() = stringResource(when (this) {
    PrivacyPermissionCategory.CAMERA -> R.string.permission_camera_body
    PrivacyPermissionCategory.MICROPHONE -> R.string.permission_microphone_body
    PrivacyPermissionCategory.LOCATION -> R.string.permission_location_body
    PrivacyPermissionCategory.CONTACTS -> R.string.permission_contacts_body
    PrivacyPermissionCategory.CALENDAR -> R.string.permission_calendar_body
    PrivacyPermissionCategory.SMS -> R.string.permission_sms_body
    PrivacyPermissionCategory.PHONE -> R.string.permission_phone_body
    PrivacyPermissionCategory.NEARBY_DEVICES -> R.string.permission_nearby_body
    PrivacyPermissionCategory.NOTIFICATIONS -> R.string.permission_notifications_body
    PrivacyPermissionCategory.BACKGROUND_LOCATION -> R.string.permission_background_location_body
})
