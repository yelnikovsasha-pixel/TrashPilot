package com.trashpilot.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.trashpilot.app.features.scanner.ScannerScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    var latestScan by remember { mutableStateOf<StorageScanResult?>(null) }

    NavHost(
        navController = navController,
        startDestination = "splash"
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
                onOpenPrivacy = { navController.navigate("privacy") },
                onOpenReports = {
                    navController.navigate(if (latestScan == null) "reports" else "results")
                },
                onOpenSettings = { navController.navigate("settings") },
                latestScan = latestScan
            )
        }
        composable("scanner") {
            ScannerScreen(
                onBack = { navController.popBackStack() },
                onScanComplete = { result ->
                    latestScan = result
                    navController.navigate("results") {
                        popUpTo("scanner") { inclusive = true }
                    }
                }
            )
        }
        composable("results") {
            latestScan?.let { result ->
                ResultsScreen(
                    result = result,
                    onBack = { navController.popBackStack("home", inclusive = false) },
                    onScanAgain = { navController.navigate("scanner") }
                )
            } ?: PlaceholderDestinationScreen(
                title = R.string.results_screen_title,
                message = R.string.results_missing,
                onBack = { navController.popBackStack("home", inclusive = false) }
            )
        }
        composable("privacy") {
            PlaceholderDestinationScreen(
                title = R.string.nav_privacy,
                message = R.string.privacy_placeholder,
                onBack = { navController.popBackStack() }
            )
        }
        composable("reports") {
            PlaceholderDestinationScreen(
                title = R.string.nav_reports,
                message = R.string.reports_placeholder,
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
