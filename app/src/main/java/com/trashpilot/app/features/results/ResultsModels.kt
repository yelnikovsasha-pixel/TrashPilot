package com.trashpilot.app.features.results

import com.trashpilot.app.core.storage.ScannedFile
import com.trashpilot.app.core.storage.StorageScanResult
import com.trashpilot.app.core.storage.SocialMediaAnalyzer

sealed interface ResultsUiState {
    data object Scanning : ResultsUiState
    data class Results(val result: StorageScanResult) : ResultsUiState
    data class NothingFound(val result: StorageScanResult) : ResultsUiState
    data class Error(val message: String? = null) : ResultsUiState
}

fun StorageScanResult.hasReviewableItems(): Boolean =
    disposableCandidates.isNotEmpty() ||
        files.any { it.sizeBytes >= LARGE_FILE_MIN_BYTES } ||
        files.any { file ->
            file.relativePath.replace('\\', '/').split('/').any { it.startsWith(".") }
        } ||
        SocialMediaAnalyzer.filesInSupportedFolders(files).isNotEmpty()

private const val LARGE_FILE_MIN_BYTES = 100L * 1024L * 1024L

enum class FileSortOption {
    SIZE,
    NAME,
    DATE
}

fun List<ScannedFile>.sortedFor(option: FileSortOption): List<ScannedFile> = when (option) {
    FileSortOption.SIZE -> sortedWith(
        compareByDescending<ScannedFile> { it.sizeBytes }.thenBy { it.name.lowercase() }
    )
    FileSortOption.NAME -> sortedWith(
        compareBy<ScannedFile> { it.name.lowercase() }.thenByDescending { it.sizeBytes }
    )
    FileSortOption.DATE -> sortedWith(
        compareByDescending<ScannedFile> { it.lastModifiedMillis }
            .thenBy { it.name.lowercase() }
    )
}
