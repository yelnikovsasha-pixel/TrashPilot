package com.trashpilot.app.features.results

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.trashpilot.app.core.quickclean.DisposableCandidate
import com.trashpilot.app.core.quickclean.DisposableCategory
import com.trashpilot.app.core.storage.StorageScanResult
import com.trashpilot.app.ui.theme.TrashPilotTheme
import org.junit.Rule
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun resultsHubGroupsReviewAndQuickCleanStartsWithoutInlineSelection() {
        var quickCleanRequested = false
        composeRule.setContent {
            TrashPilotTheme {
                ImprovedResultsScreen(
                    state = ResultsUiState.Results(resultWithCacheCandidate()),
                    onBack = {},
                    onScanAgain = {},
                    onQuickClean = { quickCleanRequested = true },
                    onOpenCache = {},
                    onOpenSocialMedia = {},
                    onOpenDuplicates = {},
                    onOpenLargeFiles = {},
                    onOpenHiddenFiles = {},
                    onOpenApkManager = {},
                    onOpenDownloads = {},
                    onOpenEmptyFolders = {},
                    onOpenScreenshots = {},
                    onOpenPhotoQuality = {}
                )
            }
        }

        composeRule.onNodeWithText("Apps").assertExists()
        composeRule.onNodeWithText("Photos").assertExists()
        composeRule.onNodeWithText("Files").assertExists()
        composeRule.onNodeWithText("Review Quick Clean").performClick()
        composeRule.runOnIdle { assertTrue(quickCleanRequested) }
    }

    private fun resultWithCacheCandidate() = StorageScanResult(
        totalBytes = 1_000,
        usedBytes = 500,
        freeBytes = 500,
        categoryBytes = emptyMap(),
        files = emptyList(),
        disposableCandidates = listOf(
            DisposableCandidate(
                uri = "content://test/cache",
                name = "cache.tmp",
                relativePath = "cache/cache.tmp",
                sizeBytes = 20,
                category = DisposableCategory.APP_CACHE,
                isDirectory = false
            )
        ),
        scannedFileCount = 0,
        selectedRootName = "Test storage"
    )
}
