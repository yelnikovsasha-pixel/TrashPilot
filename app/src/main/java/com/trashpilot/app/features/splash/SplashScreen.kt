package com.trashpilot.app.features.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.trashpilot.app.R
import com.trashpilot.app.ui.theme.TrashPilotColors
import com.trashpilot.app.ui.theme.TrashPilotComponentSizes
import com.trashpilot.app.ui.theme.TrashPilotMotion
import com.trashpilot.app.ui.theme.TrashPilotRadii
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(TrashPilotMotion.SplashHoldMillis)
        onFinished()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = TrashPilotColors.White) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(durationMillis = TrashPilotMotion.SplashFadeMillis))
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TrashPilotLogo()
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TrashPilotColors.SplashInk
                )
                Text(
                    text = stringResource(R.string.splash_tagline),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TrashPilotColors.SplashTextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun TrashPilotLogo() {
    Box(
        modifier = Modifier.size(TrashPilotComponentSizes.SplashMark),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(TrashPilotComponentSizes.SplashMark),
            shape = TrashPilotRadii.LargeShape,
            color = TrashPilotColors.SplashInk
        ) {}
        Text(
            text = stringResource(R.string.brand_monogram),
            color = TrashPilotColors.White,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
