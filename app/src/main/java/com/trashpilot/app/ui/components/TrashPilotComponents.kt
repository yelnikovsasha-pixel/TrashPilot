package com.trashpilot.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.trashpilot.app.R
import com.trashpilot.app.ui.theme.TrashPilotComponentSizes
import com.trashpilot.app.ui.theme.TrashPilotElevation
import com.trashpilot.app.ui.theme.TrashPilotIconSizes
import com.trashpilot.app.ui.theme.TrashPilotHomeTokens
import com.trashpilot.app.ui.theme.TrashPilotMotion
import com.trashpilot.app.ui.theme.TrashPilotRadii
import com.trashpilot.app.ui.theme.TrashPilotSpacing
import com.trashpilot.app.ui.theme.TrashPilotColors

@Composable
fun TrashPilotBrandHeader(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = TrashPilotHomeTokens.HeaderHeight)
            .padding(horizontal = TrashPilotHomeTokens.HeaderHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Medium)
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.navigate_back),
                    modifier = Modifier.size(TrashPilotIconSizes.Standard),
                    tint = TrashPilotColors.HomeInk
                )
            }
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(TrashPilotColors.HomeBlue, CircleShape)
                .border(3.dp, Color.White.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(Color.White, CircleShape)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.app_name),
                color = Color.Black,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
            Text(
                text = stringResource(R.string.home_subtitle),
                color = Color.Black,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
fun TrashPilotHomeCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    TrashPilotCard(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = TrashPilotHomeTokens.FeatureCardHeight)
            .shadow(
                elevation = TrashPilotHomeTokens.CardShadow,
                shape = TrashPilotRadii.CompactCardShape,
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            ),
        shape = TrashPilotRadii.CompactCardShape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = TrashPilotElevation.None)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = TrashPilotHomeTokens.FeatureCardHeight),
            shape = TrashPilotRadii.CompactCardShape,
            color = Color.White,
            border = BorderStroke(1.dp, TrashPilotColors.HomeOutline)
        ) {
            content()
        }
    }
}

@Composable
fun TrashPilotFeatureCard(
    title: String,
    body: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    titleMaxLines: Int = 1,
    bodyMaxLines: Int = 2,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val actionModifier = if (onClick != null) {
        Modifier.clickable(
            role = Role.Button,
            onClickLabel = title,
            onClick = onClick
        )
    } else {
        Modifier
    }

    TrashPilotHomeCard(modifier = modifier.then(actionModifier)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = TrashPilotHomeTokens.FeatureCardHeight)
                .padding(horizontal = TrashPilotSpacing.Large),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TrashPilotHomeTokens.FeatureContentGap)
        ) {
            Box(
                modifier = Modifier
                    .size(TrashPilotHomeTokens.FeatureIconContainer)
                    .background(TrashPilotColors.HomeBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(TrashPilotHomeTokens.FeatureIcon),
                    tint = Color.White
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TrashPilotColors.HomeInk,
                    style = TrashPilotHomeTokens.FeatureTitleStyle,
                    maxLines = titleMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = body,
                    color = TrashPilotColors.HomeTextSecondary,
                    style = TrashPilotHomeTokens.FeatureBodyStyle,
                    maxLines = bodyMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
            }
            trailingContent?.invoke()
        }
    }
}

@Composable
fun TrashPilotPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp? = TrashPilotComponentSizes.PrimaryButtonHeight,
    shape: Shape = TrashPilotRadii.PillShape,
    fontWeight: FontWeight? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    )
) {
    Button(
        onClick = onClick,
        modifier = if (height == null) modifier else modifier.height(height),
        enabled = enabled,
        shape = shape,
        colors = colors,
        contentPadding = ButtonDefaults.ContentPadding,
        elevation = ButtonDefaults.buttonElevation()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = fontWeight
        )
    }
}

@Composable
fun TrashPilotSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp? = TrashPilotComponentSizes.PrimaryButtonHeight,
    shape: Shape = TrashPilotRadii.PillShape,
    fontWeight: FontWeight? = null
) {
    Button(
        onClick = onClick,
        modifier = if (height == null) modifier else modifier.height(height),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ),
        contentPadding = ButtonDefaults.ContentPadding,
        elevation = ButtonDefaults.buttonElevation()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = fontWeight
        )
    }
}

@Composable
fun TrashPilotOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp? = TrashPilotComponentSizes.PrimaryButtonHeight,
    shape: Shape = TrashPilotRadii.PillShape,
    fontWeight: FontWeight? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = if (height == null) modifier else modifier.height(height),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ),
        border = ButtonDefaults.outlinedButtonBorder(enabled),
        contentPadding = ButtonDefaults.ContentPadding
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = fontWeight
        )
    }
}

@Composable
fun TrashPilotDestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ),
        contentPadding = ButtonDefaults.TextButtonContentPadding
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun TrashPilotTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ),
        contentPadding = ButtonDefaults.TextButtonContentPadding
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun TrashPilotCard(
    modifier: Modifier = Modifier,
    shape: Shape = TrashPilotRadii.CardShape,
    colors: CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
    ),
    elevation: CardElevation = TrashPilotElevation.card(),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = colors,
        elevation = elevation,
        content = content
    )
}

@Composable
fun TrashPilotMetricCard(
    rows: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    padding: Dp = TrashPilotSpacing.Card
) {
    TrashPilotCard(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Medium)
        ) {
            rows.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth()) {
                    Text(label, modifier = Modifier.weight(1f))
                    Text(value, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun TrashPilotInfoCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    padding: Dp = TrashPilotSpacing.Card
) {
    TrashPilotCard(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        )
    ) {
        Column(
            Modifier.padding(padding),
            verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Medium)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TrashPilotSectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleLarge
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashPilotTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    navigationEnabled: Boolean = true,
    navigationContent: (@Composable () -> Unit)? = null,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { Text(title) },
        modifier = modifier,
        navigationIcon = {
            if (navigationContent != null) {
                navigationContent()
            } else if (onBack != null) {
                IconButton(onClick = onBack, enabled = navigationEnabled) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.navigate_back),
                        modifier = Modifier.size(TrashPilotIconSizes.Standard)
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun TrashPilotIconContainer(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    containerSize: Dp = TrashPilotComponentSizes.CardIconContainer,
    iconSize: Dp = TrashPilotIconSizes.Standard,
    shape: Shape = TrashPilotRadii.IconContainerShape
) {
    TrashPilotCard(
        modifier = modifier.size(containerSize),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun TrashPilotEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) = TrashPilotStateContent(title, body, modifier, actionText, onAction)

@Composable
fun TrashPilotLoadingState(
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Large)
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.secondaryContainer
        )
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TrashPilotErrorState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) = TrashPilotStateContent(title, body, modifier, actionText, onAction)

@Composable
private fun TrashPilotStateContent(
    title: String,
    body: String,
    modifier: Modifier,
    actionText: String?,
    onAction: (() -> Unit)?
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TrashPilotSpacing.Large)
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionText != null && onAction != null) {
            TrashPilotPrimaryButton(
                text = actionText,
                onClick = onAction,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun TrashPilotScanButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .size(TrashPilotHomeTokens.ScanButtonOuter)
            .clickable(role = Role.Button, onClick = onClick),
        shape = CircleShape,
        color = Color.White,
        shadowElevation = TrashPilotElevation.ScanDefault,
        border = androidx.compose.foundation.BorderStroke(
            TrashPilotHomeTokens.ScanRing,
            TrashPilotColors.HomeBlue
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(TrashPilotHomeTokens.ScanButtonInner),
                shape = CircleShape,
                color = TrashPilotColors.HomeBlue
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = text,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Calm, non-interactive copy that rotates only while this composable is on screen.
 *
 * Messages use ordinary text semantics so a focused message remains readable without behaving
 * like an alert or live notification when the phrase changes.
 */
@Composable
fun TrashPilotAmbientMessage(
    messages: List<String>,
    modifier: Modifier = Modifier
) {
    if (messages.isEmpty()) return

    var messageIndex by rememberSaveable(messages.size) { mutableIntStateOf(0) }
    LaunchedEffect(messages.size) {
        while (true) {
            kotlinx.coroutines.delay(TrashPilotMotion.AmbientMessageVisibleMillis)
            messageIndex = (messageIndex + 1) % messages.size
        }
    }

    Box(
        modifier = modifier.defaultMinSize(
            minHeight = TrashPilotHomeTokens.AmbientMessageMinimumHeight
        ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = messageIndex,
            transitionSpec = {
                fadeIn(tween(TrashPilotMotion.AmbientMessageFadeMillis)) togetherWith
                    fadeOut(tween(TrashPilotMotion.AmbientMessageFadeMillis))
            },
            label = "Ambient message"
        ) { index ->
            Text(
                text = messages[index],
                style = TrashPilotHomeTokens.AmbientTextStyle,
                color = Color.Black.copy(alpha = 0.30f),
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}
