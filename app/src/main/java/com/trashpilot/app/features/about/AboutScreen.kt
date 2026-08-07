package com.trashpilot.app.features.about

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderShared
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.trashpilot.app.R
import com.trashpilot.app.core.about.AboutDestination
import com.trashpilot.app.core.about.AboutDestinationAvailability
import com.trashpilot.app.core.about.AboutInfoProvider
import com.trashpilot.app.core.about.AboutVersionInfo
import com.trashpilot.app.core.about.aboutDestinationAvailability
import com.trashpilot.app.ui.components.TrashPilotCard
import com.trashpilot.app.ui.components.TrashPilotInfoCard
import com.trashpilot.app.ui.components.TrashPilotSectionHeader
import com.trashpilot.app.ui.components.TrashPilotTopAppBar
import com.trashpilot.app.ui.theme.TrashPilotComponentSizes
import com.trashpilot.app.ui.theme.TrashPilotIconSizes
import com.trashpilot.app.ui.theme.TrashPilotRadii
import com.trashpilot.app.ui.theme.TrashPilotSpacing
import kotlinx.coroutines.launch

private enum class AboutPage { OVERVIEW, PRIVACY_POLICY, OPEN_SOURCE_LICENSES }

private data class LicenseNotice(val component: String, val license: String)

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onOpenPermissions: () -> Unit,
    onViewIntroduction: () -> Unit,
    privacyPolicyAvailable: Boolean = true
) {
    val context = LocalContext.current
    val versionInfo = remember(context) { AboutInfoProvider(context).load() }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val privacyUnavailableMessage = stringResource(R.string.about_privacy_unavailable)
    var page by rememberSaveable { mutableStateOf(AboutPage.OVERVIEW) }
    val navigateBack = {
        if (page == AboutPage.OVERVIEW) onBack() else page = AboutPage.OVERVIEW
    }

    BackHandler(onBack = navigateBack)
    Scaffold(
        topBar = {
            TrashPilotTopAppBar(
                title = stringResource(
                    when (page) {
                        AboutPage.OVERVIEW -> R.string.about_title
                        AboutPage.PRIVACY_POLICY -> R.string.settings_privacy_policy
                        AboutPage.OPEN_SOURCE_LICENSES -> R.string.about_open_source_licenses
                    }
                ),
                onBack = navigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        when (page) {
            AboutPage.OVERVIEW -> AboutOverview(
                modifier = Modifier.padding(padding),
                versionInfo = versionInfo,
                privacyPolicyAvailable = privacyPolicyAvailable,
                onPrivacyPolicy = {
                    if (aboutDestinationAvailability(AboutDestination.PRIVACY_POLICY, privacyPolicyAvailable) == AboutDestinationAvailability.AVAILABLE) {
                        page = AboutPage.PRIVACY_POLICY
                    } else {
                        scope.launch { snackbar.showSnackbar(privacyUnavailableMessage) }
                    }
                },
                onLicenses = { page = AboutPage.OPEN_SOURCE_LICENSES },
                onOpenPermissions = onOpenPermissions,
                onViewIntroduction = onViewIntroduction
            )
            AboutPage.PRIVACY_POLICY -> PolicyPage(Modifier.padding(padding))
            AboutPage.OPEN_SOURCE_LICENSES -> LicensesPage(Modifier.padding(padding))
        }
    }
}

@Composable
private fun AboutOverview(
    modifier: Modifier,
    versionInfo: AboutVersionInfo,
    privacyPolicyAvailable: Boolean,
    onPrivacyPolicy: () -> Unit,
    onLicenses: () -> Unit,
    onOpenPermissions: () -> Unit,
    onViewIntroduction: () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(TrashPilotSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)
    ) {
        item { BrandCard(versionInfo) }
        item { AboutHeading(R.string.about_philosophy_title) }
        item {
            TrashPilotInfoCard(
                title = stringResource(R.string.about_control_message),
                body = stringResource(R.string.about_philosophy_body),
                highlighted = true
            )
        }
        item { AboutHeading(R.string.about_privacy_title) }
        item {
            TrashPilotInfoCard(
                title = stringResource(R.string.about_local_title),
                body = stringResource(R.string.about_privacy_body)
            )
        }
        item { AboutHeading(R.string.about_app_information) }
        item {
            TrashPilotCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                AboutValueRow(
                    stringResource(R.string.about_version_name),
                    versionInfo.versionName ?: stringResource(R.string.about_version_unavailable)
                )
                AboutValueRow(stringResource(R.string.about_version_code), versionInfo.versionCode.toString())
            }
        }
        item { AboutHeading(R.string.about_information_title) }
        item {
            AboutActionRow(
                icon = Icons.Outlined.Lock,
                title = stringResource(R.string.settings_privacy_policy),
                body = stringResource(
                    if (privacyPolicyAvailable) R.string.about_privacy_policy_body
                    else R.string.about_privacy_unavailable
                ),
                enabled = true,
                onClick = onPrivacyPolicy
            )
        }
        item {
            AboutActionRow(
                Icons.Outlined.Description,
                stringResource(R.string.about_open_source_licenses),
                stringResource(R.string.about_open_source_licenses_body),
                onClick = onLicenses
            )
        }
        item {
            AboutActionRow(
                Icons.Outlined.FolderShared,
                stringResource(R.string.about_app_permissions),
                stringResource(R.string.about_app_permissions_body),
                onClick = onOpenPermissions
            )
        }
        item {
            AboutActionRow(
                Icons.Outlined.Info,
                stringResource(R.string.settings_view_introduction),
                stringResource(R.string.about_view_introduction_body),
                onClick = onViewIntroduction
            )
        }
    }
}

@Composable
private fun BrandCard(versionInfo: AboutVersionInfo) {
    TrashPilotCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(TrashPilotSpacing.Card),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Large)
        ) {
            Box(
                modifier = Modifier
                    .size(TrashPilotComponentSizes.BrandMark)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.brand_monogram),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Compact)) {
                Text(
                    stringResource(R.string.app_name),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    stringResource(
                        R.string.about_version_summary,
                        versionInfo.versionName ?: stringResource(R.string.about_version_unavailable)
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AboutHeading(textResource: Int) {
    TrashPilotSectionHeader(
        stringResource(textResource),
        Modifier.semantics { heading() }
    )
}

@Composable
private fun AboutValueRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = TrashPilotComponentSizes.MinimumTouchTarget)
            .padding(horizontal = TrashPilotSpacing.Card),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AboutActionRow(
    icon: ImageVector,
    title: String,
    body: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    TrashPilotCard(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = TrashPilotComponentSizes.MinimumTouchTarget)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = title, onClick = onClick),
        shape = TrashPilotRadii.CompactCardShape
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(TrashPilotSpacing.HomeCard),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(TrashPilotIconSizes.Standard),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun PolicyPage(modifier: Modifier) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(TrashPilotSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)
    ) {
        item { AboutHeading(R.string.settings_privacy_policy) }
        item { Text(stringResource(R.string.settings_privacy_policy_text), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { Text(stringResource(R.string.about_policy_local_note), style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun LicensesPage(modifier: Modifier) {
    val notices = listOf(
        LicenseNotice(stringResource(R.string.about_license_androidx_compose), stringResource(R.string.about_license_apache_2)),
        LicenseNotice(stringResource(R.string.about_license_androidx_core), stringResource(R.string.about_license_apache_2)),
        LicenseNotice(stringResource(R.string.about_license_androidx_navigation), stringResource(R.string.about_license_apache_2)),
        LicenseNotice(stringResource(R.string.about_license_androidx_room_datastore), stringResource(R.string.about_license_apache_2)),
        LicenseNotice(stringResource(R.string.about_license_kotlin), stringResource(R.string.about_license_apache_2))
    )
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(TrashPilotSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)
    ) {
        item { Text(stringResource(R.string.about_licenses_intro), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(notices, key = LicenseNotice::component) { notice ->
            TrashPilotInfoCard(title = notice.component, body = notice.license)
        }
        item { Text(stringResource(R.string.about_licenses_notice), style = MaterialTheme.typography.bodySmall) }
    }
}
