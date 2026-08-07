package com.trashpilot.app.features.onboarding

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.AutoDelete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.trashpilot.app.R
import com.trashpilot.app.ui.components.TrashPilotPrimaryButton
import com.trashpilot.app.ui.theme.*

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = step > 0) { step-- }
    val content = onboardingSteps()[step]

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = TrashPilotSpacing.Screen, vertical = TrashPilotSpacing.Card),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Medium)
            ) {
                repeat(3) { index ->
                    Surface(
                        modifier = Modifier.size(if (index == step) TrashPilotSpacing.Standard else TrashPilotSpacing.Medium),
                        shape = CircleShape,
                        color = if (index == step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    ) {}
                }
            }
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(TrashPilotComponentSizes.SplashMark),
                    shape = TrashPilotRadii.IconContainerShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(content.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(TrashPilotIconSizes.Standard))
                    }
                }
                Spacer(Modifier.height(TrashPilotSpacing.Large))
                Text(
                    text = stringResource(content.title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(Modifier.height(TrashPilotSpacing.Standard))
                Text(
                    text = stringResource(content.message),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(TrashPilotSpacing.Large))
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Standard)) {
                    content.points.forEach { point -> OnboardingPoint(point.icon, point.text) }
                }
            }
            TrashPilotPrimaryButton(
                text = stringResource(if (step == 2) R.string.onboarding_start else R.string.onboarding_continue),
                onClick = { if (step == 2) onComplete() else step++ },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun OnboardingPoint(icon: ImageVector, @StringRes text: Int) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(TrashPilotComponentSizes.CardIconContainer),
            shape = TrashPilotRadii.IconContainerShape,
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        }
        Spacer(Modifier.width(TrashPilotSpacing.Standard))
        Text(stringResource(text), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
    }
}

private data class OnboardingStep(@param:StringRes val title: Int, @param:StringRes val message: Int, val icon: ImageVector, val points: List<OnboardingPointModel>)
private data class OnboardingPointModel(val icon: ImageVector, @param:StringRes val text: Int)

private fun onboardingSteps() = listOf(
    OnboardingStep(R.string.app_name, R.string.onboarding_welcome_message, Icons.Outlined.Storage, listOf(
        OnboardingPointModel(Icons.Outlined.Storage, R.string.onboarding_real_analysis),
        OnboardingPointModel(Icons.Outlined.TouchApp, R.string.onboarding_you_choose),
        OnboardingPointModel(Icons.Outlined.AutoDelete, R.string.onboarding_no_automatic_deletion)
    )),
    OnboardingStep(R.string.onboarding_privacy_title, R.string.onboarding_privacy_message, Icons.Outlined.Lock, listOf(
        OnboardingPointModel(Icons.Outlined.PhoneAndroid, R.string.onboarding_local_analysis),
        OnboardingPointModel(Icons.Outlined.Lock, R.string.onboarding_no_upload),
        OnboardingPointModel(Icons.Outlined.TouchApp, R.string.onboarding_control)
    )),
    OnboardingStep(R.string.onboarding_access_title, R.string.onboarding_access_message, Icons.Outlined.Android, listOf(
        OnboardingPointModel(Icons.Outlined.TouchApp, R.string.onboarding_contextual_permissions),
        OnboardingPointModel(Icons.Outlined.Android, R.string.onboarding_android_controls),
        OnboardingPointModel(Icons.Outlined.Lock, R.string.onboarding_change_later)
    ))
)
