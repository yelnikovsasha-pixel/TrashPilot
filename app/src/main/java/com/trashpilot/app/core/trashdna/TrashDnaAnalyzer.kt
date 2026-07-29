package com.trashpilot.app.core.trashdna

data class TrashDnaSummary(
    val scansCompleted: Int,
    val cleanupsCompleted: Int,
    val mostCommonCategory: TrashDnaCategory?,
    val averageReclaimableBytes: Long,
    val lastScanMillis: Long
)

enum class TrashDnaCategory {
    TEMPORARY_FILES, APP_CACHE, EMPTY_FOLDERS, APK_LEFTOVERS, LOG_FILES
}

enum class TrashDnaInsight {
    TEMPORARY_FILES_ACCUMULATE_FASTEST,
    APK_LEFTOVERS_RECUR,
    LOG_FILES_REMAIN_LOW
}

object TrashDnaAnalyzer {
    const val minimumSummaryScans = 2
    private const val minimumInsightScans = 3

    fun summary(history: List<TrashDnaSessionEntity>): TrashDnaSummary? {
        val scans = history.filter { it.sessionType == TrashDnaSessionType.SCAN }
        if (scans.size < minimumSummaryScans) return null
        val occurrences = categoryOccurrences(scans)
        val common = occurrences.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.key
        return TrashDnaSummary(
            scansCompleted = scans.size,
            cleanupsCompleted = history.count { it.sessionType == TrashDnaSessionType.CLEANUP },
            mostCommonCategory = common,
            averageReclaimableBytes = scans.sumOf { it.reclaimableBytes } / scans.size,
            lastScanMillis = scans.maxOf { it.timestampMillis }
        )
    }

    fun insights(history: List<TrashDnaSessionEntity>): Set<TrashDnaInsight> {
        val scans = history.filter { it.sessionType == TrashDnaSessionType.SCAN }
            .sortedBy { it.timestampMillis }
        if (scans.size < minimumInsightScans) return emptySet()
        val result = mutableSetOf<TrashDnaInsight>()
        val first = scans.first()
        val last = scans.last()
        val tempGrowth = last.temporaryBytes - first.temporaryBytes
        val competingGrowth = maxOf(
            last.cacheBytes - first.cacheBytes,
            last.apkLeftoverBytes - first.apkLeftoverBytes,
            last.logBytes - first.logBytes,
            0L
        )
        if (tempGrowth > 0 && tempGrowth > competingGrowth) {
            result += TrashDnaInsight.TEMPORARY_FILES_ACCUMULATE_FASTEST
        }
        if (scans.count { it.apkLeftoverBytes > 0 } >= 2) {
            result += TrashDnaInsight.APK_LEFTOVERS_RECUR
        }
        if (scans.all { it.logBytes <= it.reclaimableBytes / 10 }) {
            result += TrashDnaInsight.LOG_FILES_REMAIN_LOW
        }
        return result
    }

    private fun categoryOccurrences(scans: List<TrashDnaSessionEntity>) = mapOf(
        TrashDnaCategory.TEMPORARY_FILES to scans.count { it.temporaryBytes > 0 },
        TrashDnaCategory.APP_CACHE to scans.count { it.cacheBytes > 0 },
        TrashDnaCategory.EMPTY_FOLDERS to scans.count { it.emptyFolderCount > 0 },
        TrashDnaCategory.APK_LEFTOVERS to scans.count { it.apkLeftoverBytes > 0 },
        TrashDnaCategory.LOG_FILES to scans.count { it.logBytes > 0 }
    )
}
