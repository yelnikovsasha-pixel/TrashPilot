package com.trashpilot.app.core.navigation

enum class TopLevelDestination(val route: String) {
    HOME("home"),
    PRIVACY("privacy"),
    REPORTS("reports"),
    SETTINGS("settings")
}

enum class ReviewGroup { APPS, PHOTOS, FILES }

enum class ReviewFeature(val route: String, val parent: ReviewGroup) {
    APP_CACHE("cache-analyzer", ReviewGroup.APPS),
    SOCIAL_MEDIA("social-media-files", ReviewGroup.APPS),
    SCREENSHOTS("screenshots-cleaner", ReviewGroup.PHOTOS),
    DUPLICATES("duplicate-scanner", ReviewGroup.PHOTOS),
    PHOTO_REVIEW("photo-quality-analyzer", ReviewGroup.PHOTOS),
    LARGE_FILES("large-files-manager", ReviewGroup.FILES),
    DOWNLOADS("downloads-cleaner", ReviewGroup.FILES),
    APK_INSTALLERS("apk-manager", ReviewGroup.FILES),
    HIDDEN_FILES("hidden-files-manager", ReviewGroup.FILES),
    EMPTY_FOLDERS("empty-folders-cleaner", ReviewGroup.FILES)
}

enum class HomeAction { SCAN, QUICK_CLEAN, TRASH_DNA, PRIVACY }

fun reviewFeatures(group: ReviewGroup): List<ReviewFeature> =
    ReviewFeature.entries.filter { it.parent == group }

fun topLevelParent(route: String?): TopLevelDestination = when (route) {
    TopLevelDestination.PRIVACY.route -> TopLevelDestination.PRIVACY
    TopLevelDestination.REPORTS.route -> TopLevelDestination.REPORTS
    TopLevelDestination.SETTINGS.route, "about", "introduction" -> TopLevelDestination.SETTINGS
    else -> TopLevelDestination.HOME
}

fun homeActionRoute(action: HomeAction, hasScan: Boolean): String = when (action) {
    HomeAction.SCAN -> "scanner"
    HomeAction.QUICK_CLEAN -> if (hasScan) "quick-clean" else "scanner"
    HomeAction.TRASH_DNA -> "trash-dna"
    HomeAction.PRIVACY -> TopLevelDestination.PRIVACY.route
}

fun reviewFeatureForRoute(route: String): ReviewFeature? =
    ReviewFeature.entries.firstOrNull { it.route == route }
