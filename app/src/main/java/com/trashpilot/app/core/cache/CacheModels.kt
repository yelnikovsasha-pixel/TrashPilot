package com.trashpilot.app.core.cache

enum class CacheSort { LARGEST, APP_NAME, RECENTLY_UPDATED }

data class CacheApp(
    val packageName: String,
    val label: String,
    val cacheBytes: Long?,
    val lastUpdatedMillis: Long
)

data class CacheSnapshot(
    val timestampMillis: Long,
    val apps: List<CacheApp>
) {
    val totalCacheBytes: Long = apps.sumOf { it.cacheBytes ?: 0 }
    val measurableAppCount: Int = apps.count { it.cacheBytes != null }
}

data class CacheScanProgress(val processedApps: Int, val totalApps: Int)

data class CacheCleaningReport(
    val cleanedBytes: Long,
    val cleanedAppsCount: Int
)

fun List<CacheApp>.filteredAndSorted(query: String, sort: CacheSort): List<CacheApp> {
    val filtered = if (query.isBlank()) this else filter {
        it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
    }
    return when (sort) {
        CacheSort.LARGEST -> filtered.sortedWith(
            compareByDescending<CacheApp> { it.cacheBytes ?: Long.MIN_VALUE }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.label }
        )
        CacheSort.APP_NAME -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
        CacheSort.RECENTLY_UPDATED -> filtered.sortedWith(
            compareByDescending<CacheApp> { it.lastUpdatedMillis }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.label }
        )
    }
}
