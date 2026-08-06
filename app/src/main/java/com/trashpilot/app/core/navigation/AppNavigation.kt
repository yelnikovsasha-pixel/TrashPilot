package com.trashpilot.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val historyRepository = remember(context) {
        TrashDnaRepository(TrashDnaDatabase.get(context).trashDnaDao())
    }
    val scope = rememberCoroutineScope()
    var latestScan by remember { mutableStateOf<StorageScanResult?>(null) }
    var selectedCategory by remember { mutableStateOf<FileCategory?>(null) }
    var selectedCleanUris by remember { mutableStateOf(emptySet<String>()) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            TrashPilotBottomBar(currentRoute = currentRoute) { route ->
                if (route != currentRoute) {
                    navController.navigate(route) {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
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
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
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
                onOpenHiddenFiles = { navController.navigate("hidden-files-manager") }
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
                onBack = { navController.popBackStack() }
            )
        }
        }
    }
}
