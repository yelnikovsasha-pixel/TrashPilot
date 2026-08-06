package com.trashpilot.app.features.privacy

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.graphics.drawable.toBitmap
import com.trashpilot.app.R
import com.trashpilot.app.core.privacy.InstalledAppPermissionReader
import com.trashpilot.app.core.privacy.PrivacyApp
import com.trashpilot.app.core.privacy.PrivacyPermissionCategory
import com.trashpilot.app.core.privacy.PrivacyPermissionStatus
import com.trashpilot.app.core.privacy.PrivacySnapshot
import com.trashpilot.app.ui.components.TrashPilotCard
import com.trashpilot.app.ui.components.TrashPilotTopAppBar
import com.trashpilot.app.ui.theme.TrashPilotColors
import com.trashpilot.app.ui.theme.TrashPilotComponentSizes
import com.trashpilot.app.ui.theme.TrashPilotRadii
import com.trashpilot.app.ui.theme.TrashPilotSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface PrivacyUiState {
    data object Loading : PrivacyUiState
    data class Success(val snapshot: PrivacySnapshot) : PrivacyUiState
    data class Error(val message: String) : PrivacyUiState
}

@Composable
fun PrivacyMonitorScreen(
    onBack: () -> Unit,
    onSnapshotLoaded: (PrivacySnapshot) -> Unit = {},
    loadSnapshot: (suspend () -> PrivacySnapshot)? = null
) {
    val context = LocalContext.current
    val loader = remember(context, loadSnapshot) {
        loadSnapshot ?: suspend { InstalledAppPermissionReader(context.applicationContext).read() }
    }
    var state: PrivacyUiState by remember { mutableStateOf(PrivacyUiState.Loading) }
    var selectedApp by remember { mutableStateOf<PrivacyApp?>(null) }

    LaunchedEffect(Unit) {
        state = runCatching { withContext(Dispatchers.IO) { loader() } }.fold(
            onSuccess = { onSnapshotLoaded(it); PrivacyUiState.Success(it) },
            onFailure = { PrivacyUiState.Error(it.message ?: context.getString(R.string.privacy_error_android)) }
        )
    }
    BackHandler { if (selectedApp != null) selectedApp = null else onBack() }

    Column(Modifier.fillMaxSize()) {
        TrashPilotTopAppBar(
            title = stringResource(if (selectedApp == null) R.string.privacy_monitor_title else R.string.privacy_app_details),
            onBack = { if (selectedApp != null) selectedApp = null else onBack() }
        )
        when (val current = state) {
            PrivacyUiState.Loading -> StateMessage(R.string.privacy_loading_title, R.string.privacy_loading_body)
            is PrivacyUiState.Error -> StateMessage(R.string.privacy_error_title, body = current.message)
            is PrivacyUiState.Success -> selectedApp?.let { AppDetails(it) }
                ?: AppList(current.snapshot, onAppClick = { selectedApp = it })
        }
    }
}

@Composable
private fun AppList(snapshot: PrivacySnapshot, onAppClick: (PrivacyApp) -> Unit) {
    var sortCategory by remember { mutableStateOf<PrivacyPermissionCategory?>(null) }
    val apps = remember(snapshot, sortCategory) {
        snapshot.apps.sortedWith(
            sortCategory?.let { category ->
                compareByDescending<PrivacyApp> { it.status(category).sortWeight }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.label }
            } ?: compareBy(String.CASE_INSENSITIVE_ORDER) { it.label }
        )
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(TrashPilotSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)
    ) {
        item {
            Text(stringResource(R.string.privacy_installed_apps_body, snapshot.appsChecked),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Text(stringResource(R.string.privacy_sort_by), style = MaterialTheme.typography.titleSmall)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Medium)
            ) {
                FilterChip(selected = sortCategory == null, onClick = { sortCategory = null },
                    label = { Text(stringResource(R.string.privacy_sort_app_name)) })
                PrivacyPermissionCategory.entries.forEach { category ->
                    FilterChip(selected = sortCategory == category, onClick = { sortCategory = category },
                        label = { Text(category.label()) })
                }
            }
        }
        items(apps, key = PrivacyApp::packageName) { app -> AppCard(app) { onAppClick(app) } }
    }
}

@Composable
private fun AppCard(app: PrivacyApp, onClick: () -> Unit) {
    TrashPilotCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = TrashPilotRadii.CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(TrashPilotSpacing.HomeCard)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(app.packageName, app.label)
                Spacer(Modifier.width(TrashPilotSpacing.Standard))
                Column(Modifier.weight(1f)) {
                    Text(app.label, style = MaterialTheme.typography.titleMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(app.packageName, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(TrashPilotSpacing.Standard))
            PrivacyPermissionCategory.entries.forEach { category ->
                PermissionStatusRow(category, app.status(category))
            }
        }
    }
}

@Composable
private fun AppDetails(app: PrivacyApp) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(TrashPilotSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(app.packageName, app.label)
                Spacer(Modifier.width(TrashPilotSpacing.Standard))
                Column {
                    Text(app.label, style = MaterialTheme.typography.titleLarge)
                    Text(app.packageName, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Text(stringResource(R.string.privacy_permission_details_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(PrivacyPermissionCategory.entries) { category ->
            TrashPilotCard(
                modifier = Modifier.fillMaxWidth(),
                shape = TrashPilotRadii.CompactCardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(Modifier.padding(TrashPilotSpacing.Large)) {
                    PermissionStatusRow(category, app.status(category))
                    Spacer(Modifier.height(TrashPilotSpacing.Medium))
                    Text(category.explanation(), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(category: PrivacyPermissionCategory, status: PrivacyPermissionStatus) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(category.label(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        StatusChip(status)
    }
}

@Composable
private fun StatusChip(status: PrivacyPermissionStatus) {
    val (background, foreground) = when (status) {
        PrivacyPermissionStatus.NOT_GRANTED -> TrashPilotColors.StatusNotGranted to TrashPilotColors.StatusNotGrantedText
        PrivacyPermissionStatus.GRANTED -> TrashPilotColors.StatusGranted to TrashPilotColors.StatusGrantedText
        PrivacyPermissionStatus.SENSITIVE -> TrashPilotColors.StatusSensitive to TrashPilotColors.StatusSensitiveText
    }
    Surface(color = background, contentColor = foreground, shape = TrashPilotRadii.SmallShape) {
        Text(status.label(), modifier = Modifier.padding(horizontal = TrashPilotSpacing.Medium, vertical = TrashPilotSpacing.Compact),
            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AppIcon(packageName: String, label: String) {
    val packageManager = LocalContext.current.packageManager
    val bitmap = remember(packageName) {
        runCatching { packageManager.getApplicationIcon(packageName).toBitmap(48, 48).asImageBitmap() }.getOrNull()
    }
    Box(Modifier.size(TrashPilotComponentSizes.CardIconContainer), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap, contentDescription = stringResource(R.string.privacy_app_icon, label),
            modifier = Modifier.fillMaxSize())
        else Surface(Modifier.fillMaxSize(), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Box(contentAlignment = Alignment.Center) { Text(label.take(1).uppercase()) }
        }
    }
}

@Composable
private fun StateMessage(title: Int, body: Int? = null, bodyText: String? = null) = StateMessage(
    title = stringResource(title), body = bodyText ?: body?.let { stringResource(it) }.orEmpty()
)

@Composable
private fun StateMessage(title: Int, body: String) = StateMessage(stringResource(title), body)

@Composable
private fun StateMessage(title: String, body: String) {
    Box(Modifier.fillMaxSize().padding(TrashPilotSpacing.Screen), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Medium)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private val PrivacyPermissionStatus.sortWeight: Int get() = when (this) {
    PrivacyPermissionStatus.SENSITIVE -> 2
    PrivacyPermissionStatus.GRANTED -> 1
    PrivacyPermissionStatus.NOT_GRANTED -> 0
}

@Composable private fun PrivacyPermissionStatus.label() = stringResource(when (this) {
    PrivacyPermissionStatus.NOT_GRANTED -> R.string.privacy_not_granted
    PrivacyPermissionStatus.GRANTED -> R.string.privacy_granted
    PrivacyPermissionStatus.SENSITIVE -> R.string.privacy_sensitive
})

@Composable private fun PrivacyPermissionCategory.label() = stringResource(when (this) {
    PrivacyPermissionCategory.CAMERA -> R.string.permission_camera
    PrivacyPermissionCategory.MICROPHONE -> R.string.permission_microphone
    PrivacyPermissionCategory.LOCATION -> R.string.permission_location
    PrivacyPermissionCategory.CONTACTS -> R.string.permission_contacts
    PrivacyPermissionCategory.PHOTOS_STORAGE -> R.string.permission_photos_storage
    PrivacyPermissionCategory.NOTIFICATIONS -> R.string.permission_notifications
    PrivacyPermissionCategory.ACCESSIBILITY -> R.string.permission_accessibility
    PrivacyPermissionCategory.BACKGROUND_ACTIVITY -> R.string.permission_background_activity
})

@Composable private fun PrivacyPermissionCategory.explanation() = stringResource(when (this) {
    PrivacyPermissionCategory.CAMERA -> R.string.permission_camera_body
    PrivacyPermissionCategory.MICROPHONE -> R.string.permission_microphone_body
    PrivacyPermissionCategory.LOCATION -> R.string.permission_location_body
    PrivacyPermissionCategory.CONTACTS -> R.string.permission_contacts_body
    PrivacyPermissionCategory.PHOTOS_STORAGE -> R.string.permission_photos_storage_body
    PrivacyPermissionCategory.NOTIFICATIONS -> R.string.permission_notifications_body
    PrivacyPermissionCategory.ACCESSIBILITY -> R.string.permission_accessibility_body
    PrivacyPermissionCategory.BACKGROUND_ACTIVITY -> R.string.permission_background_activity_body
})
