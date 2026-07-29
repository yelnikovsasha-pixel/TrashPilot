package com.trashpilot.app.features.placeholder

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trashpilot.app.R
import com.trashpilot.app.ui.components.TrashPilotTopAppBar
import com.trashpilot.app.ui.components.TrashPilotTextButton
import com.trashpilot.app.ui.theme.TrashPilotSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderDestinationScreen(
    @StringRes title: Int,
    @StringRes message: Int,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TrashPilotTopAppBar(
                title = stringResource(title),
                navigationContent = {
                    TrashPilotTextButton(
                        text = stringResource(R.string.navigate_back),
                        onClick = onBack
                    )
                }
            )
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(TrashPilotSpacing.Screen),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(message),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}
