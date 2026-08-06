package com.trashpilot.app.core.reports

import com.trashpilot.app.core.storage.FileCategory
import com.trashpilot.app.core.trashdna.TrashDnaSessionEntity
import com.trashpilot.app.core.trashdna.TrashDnaSessionType

data class ReportsSummary(
    val scans: List<ScanReport>,
    val totalScans: Int,
    val totalAnalyzedBytes: Long?,
    val totalCleanedBytes: Long,
    val lastScanMillis: Long,
    val averageCleanedPerScanBytes: Long
)

data class ScanReport(
    val id: Long,
    val timestampMillis: Long,
    val scannedBytes: Long?,
    val cleanedBytes: Long,
    val largestCategory: FileCategory?,
    val durationMillis: Long?,
    val details: ScanReportDetails
)

data class ScanReportDetails(
    val cacheBytes: Long?,
    val hiddenBytes: Long?,
    val largeFileBytes: Long?,
    val largeFileCount: Long?,
    val emptyFolderCount: Long?,
    val socialMediaBytes: Long?,
    val socialMediaFileCount: Long?,
    val duplicateBytes: Long?
)

object ReportsAnalyzer {
    fun summarize(history: List<TrashDnaSessionEntity>): ReportsSummary? {
        val scanEntities = history.filter { it.sessionType == TrashDnaSessionType.SCAN }
            .sortedBy { it.timestampMillis }
        if (scanEntities.isEmpty()) return null
        val cleanups = history.filter { it.sessionType == TrashDnaSessionType.CLEANUP }
        val reports = scanEntities.mapIndexed { index, scan ->
            val nextScanAt = scanEntities.getOrNull(index + 1)?.timestampMillis ?: Long.MAX_VALUE
            val cleaned = cleanups.filter {
                it.timestampMillis >= scan.timestampMillis && it.timestampMillis < nextScanAt
            }.sumOf { it.reclaimedBytes }
            scan.toReport(cleaned)
        }
        val allMetricsRecorded = scanEntities.all { it.reportMetricsRecorded }
        val totalCleaned = reports.sumOf { it.cleanedBytes }
        return ReportsSummary(
            scans = reports.sortedByDescending { it.timestampMillis },
            totalScans = reports.size,
            totalAnalyzedBytes = if (allMetricsRecorded) reports.sumOf { it.scannedBytes ?: 0 } else null,
            totalCleanedBytes = totalCleaned,
            lastScanMillis = scanEntities.last().timestampMillis,
            averageCleanedPerScanBytes = totalCleaned / reports.size
        )
    }

    private fun TrashDnaSessionEntity.toReport(cleanedBytes: Long): ScanReport {
        val recorded = reportMetricsRecorded
        return ScanReport(
            id = id,
            timestampMillis = timestampMillis,
            scannedBytes = scannedBytes.takeIf { recorded },
            cleanedBytes = cleanedBytes,
            largestCategory = if (recorded) largestCategory() else null,
            durationMillis = scanDurationMillis.takeIf { it > 0 },
            details = ScanReportDetails(
                cacheBytes = cacheBytes.takeIf { recorded },
                hiddenBytes = hiddenFileBytes.takeIf { recorded },
                largeFileBytes = largeFileBytes.takeIf { recorded },
                largeFileCount = largeFileCount.takeIf { recorded },
                emptyFolderCount = emptyFolderCount.takeIf { recorded },
                socialMediaBytes = messengerMediaBytes.takeIf { recorded },
                socialMediaFileCount = socialMediaFileCount.takeIf { recorded },
                duplicateBytes = null
            )
        )
    }

    private fun TrashDnaSessionEntity.largestCategory(): FileCategory? = listOf(
        FileCategory.IMAGES to imageBytes,
        FileCategory.VIDEOS to videoBytes,
        FileCategory.AUDIO to audioBytes,
        FileCategory.DOCUMENTS to documentBytes,
        FileCategory.APK_FILES to apkBytes,
        FileCategory.DOWNLOADS to downloadBytes,
        FileCategory.OTHER to otherBytes
    ).maxByOrNull { it.second }?.takeIf { it.second > 0 }?.first
}
