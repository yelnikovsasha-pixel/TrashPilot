package com.trashpilot.app.features.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trashpilot.app.R
import com.trashpilot.app.core.storage.FileCategory
import com.trashpilot.app.core.storage.StorageScanResult
import com.trashpilot.app.ui.components.TrashPilotTopAppBar
import com.trashpilot.app.ui.theme.TrashPilotSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFilesScreen(result: StorageScanResult, category: FileCategory, onBack: () -> Unit) {
    val files = result.files.filter { it.category == category }.sortedFor(FileSortOption.SIZE)
    Scaffold(
        topBar = {
            TrashPilotTopAppBar(
                title = stringResource(category.improvedLabelResource()),
                onBack = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(TrashPilotSpacing.Screen),
            verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)
        ) {
            if (files.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.results_category_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(files, key = { it.uri.toString() }) { ImprovedFileRow(it) }
            }
        }
    }
}
