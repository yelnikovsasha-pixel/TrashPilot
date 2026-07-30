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
import com.trashpilot.app.features.results.ResultsScreen
import com.trashpilot.app.features.results.ResultsUiState
import com.trashpilot.app.features.results.CategoryFilesScreen
import com.trashpilot.app.core.storage.FileCategory
import com.trashpilot.app.features.scanner.ScannerScreen
import com.trashpilot.app.features.quickclean.QuickCleanScreen
import com.trashpilot.app.core.trashdna.TrashDnaDatabase
import com.trashpilot.app.core.trashdna.TrashDnaRepository
import com.trashpilot.app.features.trashdna.TrashDnaScreen
import com.trashpilot.app.features.privacy.PrivacyMonitorScreen
import com.trashpilot.app.features.reports.ReportsScreen
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
                onOpenQuickClean = { navController.navigate("quick-clean") },
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
                    latestScan = result
                    scope.launch { historyRepository.recordScan(result) }
                    navController.navigate("results") {
                        popUpTo("scanner") { inclusive = true }
                    }
                }
            )
        }
        composable("results") {
            ResultsScreen(
                state = latestScan?.let(ResultsUiState::Success) ?: ResultsUiState.Empty,
                onBack = { navController.popBackStack("home", inclusive = false) },
                onScanAgain = { navController.navigate("scanner") },
                onQuickClean = { navController.navigate("quick-clean") },
                onOpenCategory = { category ->
                    selectedCategory = category
                    navController.navigate("category-files")
                }
            )
        }
        composable("quick-clean") {
            val result = latestScan
            if (result != null) {
                QuickCleanScreen(
                    scanResult = result,
                    onBack = { navController.popBackStack() },
                    onDone = { navController.popBackStack("results", inclusive = false) },
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
                onBack = { navController.popBackStack() }
            )
        }
        composable("reports") {
            ReportsScreen(
                repository = historyRepository,
                onBack = { navController.popBackStack() }
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
