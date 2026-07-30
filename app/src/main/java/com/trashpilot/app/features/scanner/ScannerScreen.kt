package com.trashpilot.app.features.scanner

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
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
import com.trashpilot.app.R
import com.trashpilot.app.core.storage.DocumentTreeStorageScanner
import com.trashpilot.app.core.storage.MediaStoreStorageScanner
import com.trashpilot.app.core.storage.ScanStage
import com.trashpilot.app.core.storage.StorageAccessRequiredException
import com.trashpilot.app.core.storage.StorageScanResult
import com.trashpilot.app.core.storage.requiredScanPermissions
import com.trashpilot.app.ui.components.TrashPilotBrandHeader
import com.trashpilot.app.ui.components.TrashPilotCard
import com.trashpilot.app.ui.components.TrashPilotPrimaryButton
import com.trashpilot.app.ui.theme.TrashPilotRadii
import com.trashpilot.app.ui.theme.TrashPilotSpacing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onBack: () -> Unit,
    onScanComplete: (StorageScanResult) -> Unit
) {
    val context = LocalContext.current
    val fallbackRootName = stringResource(R.string.reports_selected_storage)
    val unknownFileName = stringResource(R.string.reports_result_unknown)
    val folderScanner = remember(context, fallbackRootName, unknownFileName) {
        DocumentTreeStorageScanner(
            contentResolver = context.contentResolver,
            fallbackRootName = fallbackRootName,
            unknownFileName = unknownFileName
        )
    }
    val sharedStorageName = stringResource(R.string.results_storage)
    val automaticScanner = remember(context, sharedStorageName, unknownFileName) {
        MediaStoreStorageScanner(
            contentResolver = context.contentResolver,
            rootName = sharedStorageName,
            unknownFileName = unknownFileName
        )
    }
    val scope = rememberCoroutineScope()
    var state by remember {
        mutableStateOf<ScannerUiState>(ScannerUiState.Scanning(ScanStage.STORAGE))
    }
    suspend fun reportStage(stage: ScanStage) {
        withContext(Dispatchers.Main.immediate) {
            state = ScannerUiState.Scanning(stage)
        }
    }
    fun startAutomaticScan() {
        Log.d(SCAN_TAG, "Starting automatic MediaStore scan")
        state = ScannerUiState.Scanning(ScanStage.STORAGE)
        scope.launch {
            try {
                val result = automaticScanner.scan(::reportStage)
                Log.d(
                    SCAN_TAG,
                    "ScannerScreen result files=${result.scannedFileCount} " +
                        "bytes=${result.files.sumOf { it.sizeBytes }}"
                )
                onScanComplete(result)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: StorageAccessRequiredException) {
                state = ScannerUiState.AccessRequired
            } catch (_: SecurityException) {
                state = ScannerUiState.AccessRequired
            } catch (_: Exception) {
                state = ScannerUiState.Error
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            startAutomaticScan()
        } else {
            state = ScannerUiState.AccessRequired
        }
    }
    val folderLauncher = rememberLauncherForActivityResult(
        contract = OpenDocumentTreeWithFlags()
    ) { selection ->
        if (selection == null) return@rememberLauncherForActivityResult
        state = ScannerUiState.Scanning(ScanStage.STORAGE)
        scope.launch {
            try {
                context.contentResolver.takePersistableUriPermission(
                    selection.uri,
                    selection.persistableFlags
                )
                onScanComplete(folderScanner.scan(selection.uri, ::reportStage))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                state = ScannerUiState.Error
            }
        }
    }

    LaunchedEffect(Unit) {
        val permissions = requiredScanPermissions()
        val hasGrantedAccess = permissions.any { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
        if (hasGrantedAccess) {
            startAutomaticScan()
        } else {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.White,
        topBar = {
            TrashPilotBrandHeader(onBack = onBack)
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
                ScannerUiState.AccessRequired -> {
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

                is ScannerUiState.Scanning -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(TrashPilotSpacing.Screen))
                    Text(
                        text = stringResource(R.string.scanner_scanning),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(TrashPilotSpacing.Medium))
                    Text(
                        text = stringResource(currentState.stage.labelResource()),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(TrashPilotSpacing.Medium))
                    Text(
                        text = stringResource(R.string.scanner_scanning_body),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
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
                        text = stringResource(R.string.trash_dna_retry),
                        onClick = {
                            startAutomaticScan()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        height = null
                    )
                }
            }
        }
    }
}

private const val SCAN_TAG = "TrashPilotScan"

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
    data class Scanning(val stage: ScanStage) : ScannerUiState
    data object AccessRequired : ScannerUiState
    data object Error : ScannerUiState
}

private fun ScanStage.labelResource(): Int = when (this) {
    ScanStage.STORAGE -> R.string.storage_title
    ScanStage.LARGE_FILES -> R.string.results_large_files
    ScanStage.HIDDEN_FILES -> R.string.results_hidden_files
    ScanStage.SOCIAL_MEDIA -> R.string.results_social_media
    ScanStage.EMPTY_FOLDERS -> R.string.quick_clean_empty_folders
    ScanStage.FINALIZING -> R.string.scanner_stage_finalizing
}
