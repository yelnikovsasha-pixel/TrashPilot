package com.trashpilot.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object TrashPilotColors {
    val Background: Color = TrashPilotBackground
    val Primary: Color = TrashPilotPrimary
    val OnPrimary: Color = TrashPilotOnPrimary
    val Text: Color = TrashPilotText
    val TextSecondary: Color = TrashPilotTextSecondary
    val Card: Color = TrashPilotCard
    val AccentSurface: Color = TrashPilotAccentSurface
    val Outline: Color = TrashPilotOutline
    val SplashInk = Color(0xFF17212B)
    val SplashTextSecondary = Color(0xFF66727D)
    val White = Color(0xFFFFFFFF)
}

object TrashPilotSpacing {
    val None = 0.dp
    val Hairline = 1.dp
    val XSmall = 2.dp
    val Small = 3.dp
    val Compact = 4.dp
    val MediumCompact = 6.dp
    val Medium = 8.dp
    val MediumLarge = 10.dp
    val Standard = 12.dp
    val HomeCard = 14.dp
    val Large = 16.dp
    val CardDense = 18.dp
    val Card = 20.dp
    val Screen = 24.dp
    val HeroAfter = 28.dp
    val ExtraLarge = 32.dp
}

object TrashPilotRadii {
    val Small = 12.dp
    val IconContainer = 16.dp
    val Brand = 18.dp
    val Control = 18.dp
    val CompactCard = 20.dp
    val Card = 24.dp
    val Pill = 26.dp
    val Large = 28.dp

    val SmallShape = RoundedCornerShape(Small)
    val IconContainerShape = RoundedCornerShape(IconContainer)
    val BrandShape = RoundedCornerShape(Brand)
    val ControlShape = RoundedCornerShape(Control)
    val CompactCardShape = RoundedCornerShape(CompactCard)
    val CardShape = RoundedCornerShape(Card)
    val PillShape = RoundedCornerShape(Pill)
    val LargeShape = RoundedCornerShape(Large)
}

object TrashPilotElevation {
    val None = 0.dp
    val ScanDefault = 8.dp
    val ScanPressed = 3.dp

    @Composable
    fun card() = CardDefaults.cardElevation(defaultElevation = None)
}

object TrashPilotIconSizes {
    val SettingsRow = 20.dp
    val Navigation = 22.dp
    val Standard = 24.dp
    val ScannerHero = 56.dp
}

object TrashPilotComponentSizes {
    val MinimumTouchTarget = 48.dp
    val CardIconContainer = 48.dp
    val BrandMark = 52.dp
    val PrimaryButtonHeight = 52.dp
    val LanguageRowHeight = 58.dp
    val HomeFeatureCardHeight = 82.dp
    val HomeStorageCardHeight = 110.dp
    val HomeAssuranceHeight = 59.dp
    val HomeAssuranceTopPadding = 21.dp
    val MetricRowMinimumHeight = 34.dp
    val TopAppBarHeight = 64.dp
    val BottomNavigationHeight = 80.dp
    val SplashMark = 88.dp
    val AboutMark = 96.dp
    val ScanButton = 168.dp
    val ReportChartHeight = 150.dp
    val BottomNavigationIndicatorWidth = 64.dp
    val BottomNavigationIndicatorHeight = 32.dp
}

object TrashPilotHomeTokens {
    val HeaderToHeroSpace = 20.dp
    val HeroToStorageSpace = 20.dp
    val SectionSpace = 20.dp
    val SectionTitleToContentSpace = 10.dp
    val QuickActionGap = 10.dp
    val QuickActionHeight = 84.dp
    val QuickActionIconContainer = 40.dp
    val QuickActionIcon = 22.dp
    val StorageCardHeight = 92.dp
    val StorageProgressHeight = 4.dp
    val StorageProgressTopSpace = 6.dp
    val TrustCardPadding = 12.dp
    val TrustItemGap = 6.dp
    val TrustIcon = 20.dp
}

object TrashPilotMotion {
    const val SplashFadeMillis = 700
    const val SplashHoldMillis = 2_000L
}

object TrashPilotTypography {
    val values: Typography = Typography
    val FeatureHeading = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    )
    val ReportActivityHeading = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp
    )

    @Composable
    fun current(): Typography = MaterialTheme.typography
}

/**
 * Compatibility aliases retained while feature code migrates to semantic token groups.
 */
object TrashPilotDimensions {
    val ScreenPadding = TrashPilotSpacing.Screen
    val ContentTopPadding = TrashPilotSpacing.Card
    val CardGap = TrashPilotSpacing.Standard
    val CardRadius = TrashPilotRadii.Card
    val CardShape = TrashPilotRadii.CardShape
    val CardPadding = TrashPilotSpacing.HomeCard
    val CardIconContainer = TrashPilotComponentSizes.CardIconContainer
    val CardIcon = TrashPilotIconSizes.Standard
    val CardIconRadius = TrashPilotRadii.IconContainer
    val BottomBarHeight = TrashPilotComponentSizes.BottomNavigationHeight
    val BottomBarIcon = TrashPilotIconSizes.Navigation
    val BottomBarIndicatorWidth = TrashPilotComponentSizes.BottomNavigationIndicatorWidth
    val BottomBarIndicatorHeight = TrashPilotComponentSizes.BottomNavigationIndicatorHeight
    val PrimaryButtonHeight = TrashPilotComponentSizes.PrimaryButtonHeight
    val PrimaryButtonRadius = TrashPilotRadii.Control
    val TopBarHeight = TrashPilotComponentSizes.TopAppBarHeight
}
