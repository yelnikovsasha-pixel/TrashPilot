package com.trashpilot.app.features.results

import com.trashpilot.app.core.storage.ScannedFile
import com.trashpilot.app.core.storage.StorageScanResult

sealed interface ResultsUiState {
    data object Loading : ResultsUiState
    data object Empty : ResultsUiState
    data class Success(val result: StorageScanResult) : ResultsUiState
    data class Error(val message: String? = null) : ResultsUiState
}

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
