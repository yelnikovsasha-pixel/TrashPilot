package com.trashpilot.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.trashpilot.app.R
import com.trashpilot.app.features.home.HomeScreen
import com.trashpilot.app.features.placeholder.PlaceholderDestinationScreen
import com.trashpilot.app.features.settings.SettingsScreen
import com.trashpilot.app.features.about.AboutScreen
import com.trashpilot.app.core.settings.SettingsDestination
import com.trashpilot.app.features.splash.SplashScreen
import com.trashpilot.app.core.storage.StorageScanResult
import com.trashpilot.app.features.results.ImprovedResultsScreen
import com.trashpilot.app.features.results.ResultsUiState
import com.trashpilot.app.features.results.hasReviewableItems
import com.trashpilot.app.features.results.CategoryFilesScreen
import com.trashpilot.app.features.socialcleaner.SocialMediaCleanerScreen
import com.trashpilot.app.core.storage.FileCategory
import com.trashpilot.app.features.scanner.ScannerScreen
import com.trashpilot.app.features.quickclean.QuickCleanScreen
import com.trashpilot.app.core.trashdna.TrashDnaDatabase
import com.trashpilot.app.core.trashdna.TrashDnaRepository
import com.trashpilot.app.features.trashdna.TrashDnaScreen
import com.trashpilot.app.features.privacy.PrivacyMonitorScreen
import com.trashpilot.app.features.reports.ReportsScreen
import com.trashpilot.app.features.duplicates.DuplicateScannerScreen
import com.trashpilot.app.features.cache.RealCacheAnalyzerScreen
import com.trashpilot.app.features.largefiles.LargeFilesManagerScreen
import com.trashpilot.app.features.hiddenfiles.HiddenFilesManagerScreen
import com.trashpilot.app.features.apkmanager.ApkManagerScreen
import com.trashpilot.app.features.downloads.DownloadsCleanerScreen
import com.trashpilot.app.features.emptyfolders.EmptyFoldersCleanerScreen
import com.trashpilot.app.features.screenshots.ScreenshotsCleanerScreen
import com.trashpilot.app.features.photoquality.PhotoQualityAnalyzerScreen
import com.trashpilot.app.core.onboarding.OnboardingPreferences
import com.trashpilot.app.core.onboarding.StartupDestination
import com.trashpilot.app.core.onboarding.startupDestination
import com.trashpilot.app.core.onboarding.completeOnboarding
import com.trashpilot.app.features.onboarding.OnboardingScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val historyRepository = remember(context) {
        TrashDnaRepository(TrashDnaDatabase.get(context).trashDnaDao())
    }
    val onboardingPreferences = remember(context) { OnboardingPreferences(context) }
    val scope = rememberCoroutineScope()
    var latestScan by remember { mutableStateOf<StorageScanResult?>(null) }
    var selectedCategory by remember { mutableStateOf<FileCategory?>(null) }
    var selectedCleanUris by remember { mutableStateOf(emptySet<String>()) }
    var requestedSettingsDestination by rememberSaveable { mutableStateOf<SettingsDestination?>(null) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (currentRoute !in setOf("splash", "onboarding", "introduction")) {
                TrashPilotBottomBar(currentRoute = currentRoute) { route ->
                    if (route != currentRoute) {
                        navController.navigate(route) {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }
            }
        }
    ) { appPadding ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(appPadding)
        ) {
        composable("splash") {
            SplashScreen(
                onFinished = {
                    val destination = when (startupDestination(onboardingPreferences.isCompleted())) {
                        StartupDestination.ONBOARDING -> "onboarding"
                        StartupDestination.HOME -> "home"
                    }
                    navController.navigate(destination) {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
        composable("onboarding") {
            OnboardingScreen(
                onComplete = {
                    if (completeOnboarding(onboardingPreferences) == StartupDestination.HOME) {
                        navController.navigate("home") {
                            popUpTo("onboarding") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }
        composable("introduction") {
            OnboardingScreen(onComplete = { navController.popBackStack() })
        }
        composable("home") {
            HomeScreen(
                onScan = { navController.navigate("scanner") },
                onOpenQuickClean = {
                    navController.navigate("cache-analyzer")
                },
                onOpenTrashDna = { navController.navigate("trash-dna") },
                onOpenPrivacy = { navController.navigate("privacy") },
                onOpenSettings = { navController.navigate("settings") },
                latestScan = latestScan
            )
        }
        composable("scanner") {
            ScannerScreen(
                onBack = { navController.popBackStack() },
                onScanComplete = { result ->
                    Log.d(
                        "TrashPilotScan",
                        "Navigation received files=${result.scannedFileCount} " +
                            "bytes=${result.files.sumOf { it.sizeBytes }}; opening results"
                    )
                    latestScan = result
                    scope.launch { historyRepository.recordScan(result) }
                    navController.navigate("results") {
                        popUpTo("scanner") { inclusive = true }
                    }
                }
            )
        }
        composable("results") {
            ImprovedResultsScreen(
                state = latestScan?.let { result ->
                    val reviewable = result.hasReviewableItems()
                    Log.d(
                        "TrashPilotScan",
                        "Results model files=${result.files.size} " +
                            "bytes=${result.files.sumOf { it.sizeBytes }} " +
                            "candidates=${result.disposableCandidates.size} " +
                            "reviewable=$reviewable"
                    )
                    if (reviewable) {
                        ResultsUiState.Results(result)
                    } else {
                        ResultsUiState.NothingFound(result)
                    }
                } ?: ResultsUiState.Error(),
                onBack = { navController.popBackStack("home", inclusive = false) },
                onScanAgain = { navController.navigate("scanner") },
                onQuickClean = { selectedUris ->
                    selectedCleanUris = selectedUris
                    navController.navigate("quick-clean")
                },
                onOpenCategory = { category ->
                    selectedCategory = category
                    navController.navigate("category-files")
                },
                onOpenSocialMedia = { navController.navigate("social-media-files") },
                onOpenDuplicates = { navController.navigate("duplicate-scanner") },
                onOpenLargeFiles = { navController.navigate("large-files-manager") },
                onOpenHiddenFiles = { navController.navigate("hidden-files-manager") },
                onOpenApkManager = { navController.navigate("apk-manager") },
                onOpenDownloads = { navController.navigate("downloads-cleaner") },
                onOpenEmptyFolders = { navController.navigate("empty-folders-cleaner") },
                onOpenScreenshots = { navController.navigate("screenshots-cleaner") },
                onOpenPhotoQuality = { navController.navigate("photo-quality-analyzer") }
            )
        }
        composable("large-files-manager") {
            LargeFilesManagerScreen(
                onBack = { navController.popBackStack() },
                onFilesDeleted = { updated, report ->
                    val source = latestScan ?: updated
                    latestScan = updated
                    scope.launch { historyRepository.recordLargeFilesCleanup(source, report) }
                }
            )
        }
        composable("hidden-files-manager") {
            HiddenFilesManagerScreen(
                onBack = { navController.popBackStack() },
                onFilesDeleted = { updated, report ->
                    val source = latestScan ?: updated
                    latestScan = updated
                    scope.launch { historyRepository.recordHiddenFilesCleanup(source, report) }
                }
            )
        }
        composable("apk-manager") {
            ApkManagerScreen(
                onBack = { navController.popBackStack() },
                onFilesDeleted = { updated, report ->
                    val source = latestScan ?: updated
                    latestScan = updated
                    scope.launch { historyRepository.recordApkCleanup(source, report) }
                }
            )
        }
        composable("downloads-cleaner") {
            DownloadsCleanerScreen(
                onBack = { navController.popBackStack() },
                onFilesDeleted = { updated, report ->
                    val source = latestScan ?: updated
                    latestScan = updated
                    scope.launch { historyRepository.recordDownloadsCleanup(source, report) }
                }
            )
        }
        composable("empty-folders-cleaner") {
            EmptyFoldersCleanerScreen(
                onBack = { navController.popBackStack() },
                onFoldersDeleted = { deletedUris, deletedCount, failedCount ->
                    val source = latestScan
                    if (source != null) {
                        latestScan = source.copy(
                            disposableCandidates = source.disposableCandidates.filterNot { it.uri in deletedUris }
                        )
                        scope.launch {
                            historyRepository.recordEmptyFoldersCleanup(source, deletedCount, failedCount)
                        }
                    }
                }
            )
        }
        composable("screenshots-cleaner") {
            ScreenshotsCleanerScreen(
                onBack = { navController.popBackStack() },
                onDeleted = { deletedUris, report ->
                    val source = latestScan
                    if (source != null) {
                        val remaining = source.files.filterNot { it.uri in deletedUris }
                        latestScan = source.copy(
                            files = remaining,
                            scannedFileCount = remaining.size,
                            categoryBytes = FileCategory.entries.associateWith { category -> remaining.filter { it.category == category }.sumOf { it.sizeBytes } },
                            disposableCandidates = source.disposableCandidates.filterNot { it.uri in deletedUris }
                        )
                        scope.launch { historyRepository.recordScreenshotsCleanup(source, report) }
                    }
                }
            )
        }
        composable("photo-quality-analyzer") {
            PhotoQualityAnalyzerScreen(
                onBack = { navController.popBackStack() },
                onDeleted = { deletedUris, report ->
                    val source = latestScan
                    if (source != null) {
                        val remaining = source.files.filterNot { it.uri in deletedUris }
                        latestScan = source.copy(
                            files = remaining,
                            scannedFileCount = remaining.size,
                            categoryBytes = FileCategory.entries.associateWith { category -> remaining.filter { it.category == category }.sumOf { it.sizeBytes } },
                            disposableCandidates = source.disposableCandidates.filterNot { it.uri in deletedUris }
                        )
                        scope.launch { historyRepository.recordPhotoQualityCleanup(source, report) }
                    }
                }
            )
        }
        composable("duplicate-scanner") {
            val result = latestScan
            if (result != null) {
                DuplicateScannerScreen(
                    scanResult = result,
                    onBack = { navController.popBackStack() },
                    onScanAgain = { navController.navigate("scanner") },
                    onCleaningComplete = { updated, report ->
                        latestScan = updated
                        scope.launch { historyRepository.recordDuplicateCleanup(result, report) }
                    }
                )
            } else PlaceholderDestinationScreen(
                title = R.string.duplicate_scanner_title,
                message = R.string.results_missing,
                onBack = { navController.popBackStack() }
            )
        }
        composable("quick-clean") {
            val result = latestScan
            if (result != null) {
                QuickCleanScreen(
                    scanResult = result,
                    onBack = { navController.popBackStack() },
                    onDone = { navController.popBackStack("results", inclusive = false) },
                    initialSelectedUris = selectedCleanUris,
                    onCleaningComplete = { report ->
                        scope.launch { historyRepository.recordCleanup(result, report) }
                    }
                )
            } else {
                PlaceholderDestinationScreen(
                    title = R.string.quick_clean_title,
                    message = R.string.results_missing,
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable("cache-analyzer") {
            RealCacheAnalyzerScreen(
                onBack = { navController.popBackStack() },
                onCacheScan = { snapshot ->
                    scope.launch { historyRepository.recordCacheScan(snapshot) }
                },
                onCacheCleaned = { report ->
                    scope.launch { historyRepository.recordCacheCleanup(report) }
                }
            )
        }
        composable("category-files") {
            val result = latestScan
            val category = selectedCategory
            if (result != null && category != null) {
                CategoryFilesScreen(
                    result = result,
                    category = category,
                    onBack = { navController.popBackStack() }
                )
            } else {
                PlaceholderDestinationScreen(
                    title = R.string.results_screen_title,
                    message = R.string.results_missing,
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable("social-media-files") {
            SocialMediaCleanerScreen(
                onBack = { navController.popBackStack() },
                onFilesDeleted = { updated, report ->
                    val source = latestScan ?: updated
                    latestScan = updated
                    scope.launch { historyRepository.recordSocialMediaCleanup(source, report) }
                }
            )
        }
        composable("privacy") {
            PrivacyMonitorScreen(
                onBack = { navController.popBackStack() },
                onSnapshotLoaded = { snapshot ->
                    scope.launch { historyRepository.recordPrivacyReview(snapshot) }
                }
            )
        }
        composable("trash-dna") {
            TrashDnaScreen(
                repository = historyRepository,
                onBack = { navController.popBackStack() },
                onScanAgain = { navController.navigate("scanner") }
            )
        }
        composable("reports") {
            ReportsScreen(
                repository = historyRepository,
                onBack = { navController.popBackStack() },
                onScanNow = { navController.navigate("scanner") }
            )
        }
        composable("settings") {
            SettingsScreen(
                repository = historyRepository,
                onBack = { navController.popBackStack() },
                onViewIntroduction = { navController.navigate("introduction") },
                onOpenAbout = { navController.navigate("about") },
                requestedDestination = requestedSettingsDestination,
                onDestinationHandled = { requestedSettingsDestination = null }
            )
        }
        composable("about") {
            AboutScreen(
                onBack = { navController.popBackStack() },
                onOpenPermissions = {
                    requestedSettingsDestination = SettingsDestination.PRIVACY_PERMISSIONS
                    navController.popBackStack()
                },
                onViewIntroduction = { navController.navigate("introduction") }
            )
        }
        }
    }
}
