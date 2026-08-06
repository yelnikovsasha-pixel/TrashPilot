package com.trashpilot.app.core.trashdna

import com.trashpilot.app.core.trashdna.TrendCalculator.bytes

object TrashDnaAnalyzer {
    const val minimumScans = 2

    fun analyze(history: List<TrashDnaSessionEntity>): TrashDnaAnalysis? {
        val scans = history.filter {
            it.sessionType == TrashDnaSessionType.SCAN && it.usedStorageBytes > 0
        }.sortedBy { it.timestampMillis }
        if (scans.size < minimumScans) return null
        val previous = scans[scans.lastIndex - 1]
        val latest = scans.last()
        val storageTrend = TrendCalculator.storage(previous, latest)
        val fastest = TrendCalculator.fastestGrowing(previous, latest)
        val insight = InsightGenerator.generate(previous, latest, storageTrend, fastest)
        val mainCategory = fastest?.category ?: latest.largestCategory()
        val cleanupTimes = history.filter { it.sessionType == TrashDnaSessionType.CLEANUP }
            .map { it.timestampMillis }.sorted()
        return TrashDnaAnalysis(
            profile = ProfileDetector.detect(scans.first(), latest),
            storageTrend = storageTrend,
            fastestGrowingCategory = fastest,
            mainSourceCategory = mainCategory,
            mainSourceName = latest.messengerSourceName.takeIf {
                mainCategory == TrashDnaCategory.MESSENGER_MEDIA && it.isNotBlank()
            },
            insight = insight,
            recommendation = RecommendationGenerator.generate(insight),
            history = scans.mapIndexed { index, scan ->
                val nextTimestamp = scans.getOrNull(index + 1)?.timestampMillis ?: Long.MAX_VALUE
                TrashDnaHistoryItem(
                    timestampMillis = scan.timestampMillis,
                    totalStorageBytes = scan.usedStorageBytes,
                    deletedBytes = history.filter {
                        it.sessionType == TrashDnaSessionType.CLEANUP &&
                            it.timestampMillis >= scan.timestampMillis && it.timestampMillis < nextTimestamp
                    }.sumOf { it.reclaimedBytes },
                    largestCategory = scan.largestCategory()
                )
            },
            cleanupCount = cleanupTimes.size,
            averageDaysBetweenCleanups = cleanupTimes.zipWithNext { first, second -> second - first }
                .takeIf { it.isNotEmpty() }?.average()?.div(MILLIS_PER_DAY)
        )
    }

    private fun TrashDnaSessionEntity.largestCategory(): TrashDnaCategory? =
        TrashDnaCategory.entries.map { it to bytes(it) }.maxByOrNull { it.second }
            ?.takeIf { it.second > 0 }?.first

    private const val MILLIS_PER_DAY = 86_400_000.0
}
