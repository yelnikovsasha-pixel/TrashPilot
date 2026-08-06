package com.trashpilot.app.core.trashdna

enum class TrashDnaCategory {
    MESSENGER_MEDIA, SCREENSHOTS, DOWNLOADS, LARGE_FILES, HIDDEN_FILES, CACHE,
    IMAGES, VIDEOS, AUDIO, DOCUMENTS
}

enum class TrashDnaProfile {
    BALANCED, MEDIA_COLLECTOR, MESSENGER_HEAVY, DOWNLOAD_KEEPER,
    SCREENSHOT_COLLECTOR, LARGE_FILE_KEEPER
}

enum class TrashDnaInsight {
    MESSENGER_GROWTH, DOWNLOADS_GROWTH, SCREENSHOTS_GROWTH,
    LARGE_VIDEOS_DOMINATE, STORAGE_STABLE, CATEGORY_GROWTH, STORAGE_REDUCED
}

enum class TrashDnaRecommendation {
    REVIEW_MESSENGER_MEDIA, REMOVE_OLD_DOWNLOADS, CHECK_SCREENSHOTS,
    REVIEW_LARGE_VIDEOS, REVIEW_FASTEST_CATEGORY, KEEP_CURRENT_HABITS
}

data class CategoryTrend(val category: TrashDnaCategory, val changeBytes: Long)

data class StorageTrend(val changeBytes: Long) {
    val direction: TrendDirection = when {
        changeBytes > 0 -> TrendDirection.UP
        changeBytes < 0 -> TrendDirection.DOWN
        else -> TrendDirection.STABLE
    }
}

enum class TrendDirection { UP, DOWN, STABLE }

data class TrashDnaHistoryItem(
    val timestampMillis: Long,
    val totalStorageBytes: Long,
    val deletedBytes: Long,
    val largestCategory: TrashDnaCategory?
)

data class TrashDnaAnalysis(
    val profile: TrashDnaProfile,
    val storageTrend: StorageTrend,
    val fastestGrowingCategory: CategoryTrend?,
    val mainSourceCategory: TrashDnaCategory?,
    val mainSourceName: String?,
    val insight: TrashDnaInsight,
    val recommendation: TrashDnaRecommendation,
    val history: List<TrashDnaHistoryItem>,
    val cleanupCount: Int,
    val averageDaysBetweenCleanups: Double?
)
