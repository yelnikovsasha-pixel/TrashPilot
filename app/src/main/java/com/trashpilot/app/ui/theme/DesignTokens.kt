package com.trashpilot.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
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
    val HomeBlue: Color = TrashPilotHomeBlue
    val HomeInk: Color = TrashPilotHomeInk
    val HomeTextSecondary: Color = TrashPilotHomeTextSecondary
    val HomeOutline: Color = TrashPilotHomeOutline
    val HomeNavigation: Color = TrashPilotHomeNavigation
    val StatusNotGranted: Color = TrashPilotStatusNotGranted
    val StatusNotGrantedText: Color = TrashPilotStatusNotGrantedText
    val StatusGranted: Color = TrashPilotStatusGranted
    val StatusGrantedText: Color = TrashPilotStatusGrantedText
    val StatusSensitive: Color = TrashPilotStatusSensitive
    val StatusSensitiveText: Color = TrashPilotStatusSensitiveText
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
    val ScreenTop = 12.dp
    val ScreenBottom = 14.dp
    val HeaderHorizontalPadding = 14.dp
    val HeaderHeight = 43.dp
    val HeaderToHeroSpace = 27.dp
    val HeroToAmbientSpace = 27.dp
    val AmbientMessageMinimumHeight = 30.dp
    val AmbientToStorageSpace = 26.dp
    val CardHorizontalPadding = 24.dp
    val StorageCardHeight = 72.dp
    val StorageProgressHeight = 5.dp
    val StorageProgressWidth = 150.dp
    val FeatureCardHeight = 72.dp
    val FeatureCardGap = 5.dp
    val FeatureIconContainer = 32.dp
    val FeatureIcon = 22.dp
    val FeatureContentGap = 17.dp
    val CardRadius = 20.dp
    val CardShadow = 4.dp
    val ScanButtonOuter = 200.dp
    val ScanButtonInner = 168.dp
    val ScanRing = 6.dp

    val AmbientTextStyle = TextStyle(
        fontWeight = FontWeight.Light,
        fontStyle = FontStyle.Italic,
        fontSize = 24.sp
    )
    val FeatureTitleStyle = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 22.sp
    )
    val FeatureBodyStyle = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
    val StorageDetailStyle = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 12.sp
    )
}

object TrashPilotMotion {
    const val SplashFadeMillis = 700
    const val SplashHoldMillis = 2_000L
    const val AmbientMessageFadeMillis = 600
    const val AmbientMessageVisibleMillis = 6_500L
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
