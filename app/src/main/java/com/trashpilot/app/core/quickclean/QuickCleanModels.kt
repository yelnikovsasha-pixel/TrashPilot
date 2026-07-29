package com.trashpilot.app.core.quickclean

enum class DisposableCategory {
    TEMPORARY_FILES,
    APP_CACHE,
    EMPTY_FOLDERS,
    APK_LEFTOVERS,
    LOG_FILES
}

data class DisposableCandidate(
    val uri: String,
    val name: String,
    val relativePath: String,
    val sizeBytes: Long,
    val category: DisposableCategory,
    val isDirectory: Boolean
)

data class FailedCleanItem(
    val candidate: DisposableCandidate,
    val reason: String?
)

data class CleaningReport(
    val reclaimedBytes: Long,
    val deletedByCategory: Map<DisposableCategory, Int>,
    val failedItems: List<FailedCleanItem>
) {
    val deletedItemCount: Int
        get() = deletedByCategory.values.sum()
}
