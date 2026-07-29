package com.trashpilot.app.features.scanner

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trashpilot.app.R
import com.trashpilot.app.core.storage.DocumentTreeStorageScanner
import com.trashpilot.app.core.storage.StorageScanResult
import com.trashpilot.app.ui.components.TrashPilotTopAppBar
import com.trashpilot.app.ui.components.TrashPilotCard
import com.trashpilot.app.ui.components.TrashPilotPrimaryButton
import com.trashpilot.app.ui.theme.TrashPilotRadii
import com.trashpilot.app.ui.theme.TrashPilotSpacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onBack: () -> Unit,
    onScanComplete: (StorageScanResult) -> Unit
) {
    val context = LocalContext.current
    val scanner = remember(context) { DocumentTreeStorageScanner(context.contentResolver) }
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<ScannerUiState>(ScannerUiState.Ready) }
    val folderLauncher = rememberLauncherForActivityResult(
        contract = OpenDocumentTreeWithFlags()
    ) { selection ->
        if (selection == null) return@rememberLauncherForActivityResult
        state = ScannerUiState.Scanning
        scope.launch {
            state = runCatching {
                context.contentResolver.takePersistableUriPermission(
                    selection.uri,
                    selection.persistableFlags
                )
                scanner.scan(selection.uri)
            }.fold(
                onSuccess = {
                    onScanComplete(it)
                    ScannerUiState.Ready
                },
                onFailure = { ScannerUiState.Error }
            )
        }
    }

    BackHandler(enabled = state is ScannerUiState.Scanning) { }

    Scaffold(
        topBar = {
            TrashPilotTopAppBar(
                title = stringResource(R.string.scanner_title),
                onBack = onBack,
                navigationEnabled = state !is ScannerUiState.Scanning
            )
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(
                    horizontal = TrashPilotSpacing.Screen,
                    vertical = TrashPilotSpacing.ExtraLarge
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (val currentState = state) {
                ScannerUiState.Ready -> {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(TrashPilotSpacing.Screen))
                    Text(
                        text = stringResource(R.string.scanner_choose_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(TrashPilotSpacing.Standard))
                    Text(
                        text = stringResource(R.string.scanner_choose_body),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(TrashPilotSpacing.ExtraLarge))
                    TrashPilotPrimaryButton(
                        text = stringResource(R.string.scanner_choose_action),
                        onClick = { folderLauncher.launch(null) },
                        modifier = Modifier.fillMaxWidth(),
                        height = null
                    )
                    Spacer(Modifier.height(TrashPilotSpacing.Screen))
                    PrivacyAssurance()
                }

                ScannerUiState.Scanning -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(TrashPilotSpacing.Screen))
                    Text(
                        text = stringResource(R.string.scanner_scanning),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(TrashPilotSpacing.Medium))
                    Text(
                        text = stringResource(R.string.scanner_scanning_body),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                ScannerUiState.Error -> {
                    Text(
                        text = stringResource(R.string.scanner_error_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(TrashPilotSpacing.Medium))
                    Text(
                        text = stringResource(R.string.scanner_error_generic),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(TrashPilotSpacing.Screen))
                    TrashPilotPrimaryButton(
                        text = stringResource(R.string.scanner_retry),
                        onClick = {
                            state = ScannerUiState.Ready
                            folderLauncher.launch(null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        height = null
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacyAssurance() {
    TrashPilotCard(
        modifier = Modifier.fillMaxWidth(),
        shape = TrashPilotRadii.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(TrashPilotSpacing.Card),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(TrashPilotSpacing.MediumLarge))
            Text(
                text = stringResource(R.string.scanner_privacy_note),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

private sealed interface ScannerUiState {
    data object Ready : ScannerUiState
    data object Scanning : ScannerUiState
    data object Error : ScannerUiState
}
