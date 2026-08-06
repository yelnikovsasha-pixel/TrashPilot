package com.trashpilot.app.core.trashdna

import com.trashpilot.app.core.quickclean.CleaningReport
import com.trashpilot.app.core.quickclean.DisposableCategory
import com.trashpilot.app.core.privacy.PrivacySnapshot
import com.trashpilot.app.core.storage.StorageScanResult
import com.trashpilot.app.core.storage.FileCategory
import com.trashpilot.app.core.storage.SocialMediaAnalyzer
import com.trashpilot.app.core.storage.DuplicateCleaningReport
import com.trashpilot.app.core.cache.CacheSnapshot
import com.trashpilot.app.core.cache.CacheCleaningReport

interface HistoryRepository {
    suspend fun loadTrashDnaHistory(): List<TrashDnaSessionEntity>
    suspend fun resetTrashDnaHistory(timestampMillis: Long = System.currentTimeMillis())
}

class TrashDnaRepository(private val dao: TrashDnaDao) : HistoryRepository {
    suspend fun recordScan(result: StorageScanResult, timestampMillis: Long = System.currentTimeMillis()) {
        val grouped = result.disposableCandidates.groupBy { it.category }
        val messengerGroups = SocialMediaAnalyzer.groups(result.files)
        val messengerSource = messengerGroups.maxByOrNull { it.totalBytes }
        val largeFiles = result.files.filter { it.sizeBytes >= LARGE_FILE_MIN_BYTES }
        dao.insert(
            TrashDnaSessionEntity(
                sessionType = TrashDnaSessionType.SCAN,
                timestampMillis = timestampMillis,
                scannedFolderName = result.selectedRootName,
                reclaimableBytes = result.disposableCandidates.sumOf { it.sizeBytes },
                reclaimedBytes = 0,
                result = TrashDnaResult.ANALYZED,
                temporaryBytes = grouped.bytes(DisposableCategory.TEMPORARY_FILES),
                cacheBytes = grouped.bytes(DisposableCategory.APP_CACHE),
                emptyFolderCount = grouped[DisposableCategory.EMPTY_FOLDERS]?.size?.toLong() ?: 0,
                apkLeftoverBytes = grouped.bytes(DisposableCategory.APK_LEFTOVERS),
                logBytes = grouped.bytes(DisposableCategory.LOG_FILES),
                scannedFileCount = result.scannedFileCount.toLong(),
                scanDurationMillis = result.scanDurationMillis,
                usedStorageBytes = result.usedBytes,
                imageBytes = result.categoryBytes[FileCategory.IMAGES] ?: 0,
                videoBytes = result.categoryBytes[FileCategory.VIDEOS] ?: 0,
                audioBytes = result.categoryBytes[FileCategory.AUDIO] ?: 0,
                documentBytes = result.categoryBytes[FileCategory.DOCUMENTS] ?: 0,
                downloadBytes = result.categoryBytes[FileCategory.DOWNLOADS] ?: 0,
                messengerMediaBytes = messengerGroups.sumOf { it.totalBytes },
                screenshotBytes = result.files.filter(::isScreenshot).sumOf { it.sizeBytes },
                largeFileBytes = largeFiles.sumOf { it.sizeBytes },
                largeVideoBytes = largeFiles.filter { it.category == FileCategory.VIDEOS }.sumOf { it.sizeBytes },
                hiddenFileBytes = result.files.filter(::isHidden).sumOf { it.sizeBytes },
                messengerSourceName = messengerSource?.applicationName.orEmpty(),
                reportMetricsRecorded = true,
                scannedBytes = result.files.sumOf { it.sizeBytes },
                apkBytes = result.categoryBytes[FileCategory.APK_FILES] ?: 0,
                otherBytes = result.categoryBytes[FileCategory.OTHER] ?: 0,
                largeFileCount = largeFiles.size.toLong(),
                socialMediaFileCount = messengerGroups.sumOf { it.files.size }.toLong()
            )
        )
    }

    suspend fun recordCleanup(
        scan: StorageScanResult,
        report: CleaningReport,
        timestampMillis: Long = System.currentTimeMillis()
    ) {
        dao.insert(
            TrashDnaSessionEntity(
                sessionType = TrashDnaSessionType.CLEANUP,
                timestampMillis = timestampMillis,
                scannedFolderName = scan.selectedRootName,
                reclaimableBytes = scan.disposableCandidates.sumOf { it.sizeBytes },
                reclaimedBytes = report.reclaimedBytes,
                result = if (report.failedItems.isEmpty()) TrashDnaResult.CLEANED else TrashDnaResult.PARTIAL,
                temporaryBytes = 0,
                cacheBytes = 0,
                emptyFolderCount = 0,
                apkLeftoverBytes = 0,
                logBytes = 0
            )
        )
    }

    suspend fun recordDuplicateCleanup(
        scan: StorageScanResult,
        report: DuplicateCleaningReport,
        timestampMillis: Long = System.currentTimeMillis()
    ) {
        dao.insert(
            TrashDnaSessionEntity(
                sessionType = TrashDnaSessionType.CLEANUP,
                timestampMillis = timestampMillis,
                scannedFolderName = scan.selectedRootName,
                reclaimableBytes = report.deletedFiles.sumOf { it.sizeBytes },
                reclaimedBytes = report.reclaimedBytes,
                result = if (report.failedFiles.isEmpty()) TrashDnaResult.CLEANED else TrashDnaResult.PARTIAL,
                temporaryBytes = 0, cacheBytes = 0, emptyFolderCount = 0,
                apkLeftoverBytes = 0, logBytes = 0
            )
        )
    }

    suspend fun recordCacheScan(snapshot: CacheSnapshot) {
        dao.insert(
            TrashDnaSessionEntity(
                sessionType = TrashDnaSessionType.CACHE_SCAN,
                timestampMillis = snapshot.timestampMillis,
                scannedFolderName = "",
                reclaimableBytes = snapshot.totalCacheBytes,
                reclaimedBytes = 0,
                result = TrashDnaResult.ANALYZED,
                temporaryBytes = 0,
                cacheBytes = snapshot.totalCacheBytes,
                emptyFolderCount = 0,
                apkLeftoverBytes = 0,
                logBytes = 0,
                scannedFileCount = snapshot.measurableAppCount.toLong()
            )
        )
    }

    suspend fun recordLargeFilesCleanup(
        scan: StorageScanResult,
        report: DuplicateCleaningReport,
        timestampMillis: Long = System.currentTimeMillis()
    ) = recordDuplicateCleanup(scan, report, timestampMillis)

    suspend fun recordCacheCleanup(
        report: CacheCleaningReport,
        timestampMillis: Long = System.currentTimeMillis()
    ) {
        if (report.cleanedBytes <= 0 || report.cleanedAppsCount <= 0) return
        dao.insert(
            TrashDnaSessionEntity(
                sessionType = TrashDnaSessionType.CLEANUP,
                timestampMillis = timestampMillis,
                scannedFolderName = "",
                reclaimableBytes = report.cleanedBytes,
                reclaimedBytes = report.cleanedBytes,
                result = TrashDnaResult.CLEANED,
                temporaryBytes = 0,
                cacheBytes = report.cleanedBytes,
                emptyFolderCount = 0,
                apkLeftoverBytes = 0,
                logBytes = 0,
                scannedFileCount = report.cleanedAppsCount.toLong()
            )
        )
    }

    suspend fun recordPrivacyReview(
        snapshot: PrivacySnapshot,
        timestampMillis: Long = System.currentTimeMillis()
    ) {
        dao.insert(
            TrashDnaSessionEntity(
                sessionType = TrashDnaSessionType.PRIVACY_REVIEW,
                timestampMillis = timestampMillis,
                scannedFolderName = "",
                reclaimableBytes = 0,
                reclaimedBytes = 0,
                result = TrashDnaResult.ANALYZED,
                temporaryBytes = 0,
                cacheBytes = 0,
                emptyFolderCount = 0,
                apkLeftoverBytes = 0,
                logBytes = 0,
                privacyAppsChecked = snapshot.appsChecked.toLong(),
                privacySensitiveAppCount = snapshot.sensitiveAppCount.toLong()
            )
        )
    }

    suspend fun loadHistory(): List<TrashDnaSessionEntity> = dao.loadAll()
        .filter { it.sessionType != TrashDnaSessionType.PRIVACY_REVIEW }

    override suspend fun loadTrashDnaHistory(): List<TrashDnaSessionEntity> {
        val resetAt = dao.loadResetAtMillis() ?: 0
        return dao.loadAll().filter {
            it.timestampMillis > resetAt && it.sessionType in setOf(
                TrashDnaSessionType.SCAN, TrashDnaSessionType.CLEANUP
            )
        }
    }

    suspend fun loadReportHistory(): List<TrashDnaSessionEntity> = dao.loadAll()

    suspend fun clearReportHistory() = dao.clearReportHistory()

    suspend fun resetLocalHistory() = dao.clearAll()

    override suspend fun resetTrashDnaHistory(timestampMillis: Long) {
        dao.saveState(TrashDnaStateEntity(resetAtMillis = timestampMillis))
    }

    suspend fun replaceLocalHistory(sessions: List<TrashDnaSessionEntity>) {
        dao.replaceAll(sessions.map { it.copy(id = 0) })
    }

    private fun Map<DisposableCategory, List<com.trashpilot.app.core.quickclean.DisposableCandidate>>.bytes(
        category: DisposableCategory
    ): Long = get(category)?.sumOf { it.sizeBytes } ?: 0

    private fun isScreenshot(file: com.trashpilot.app.core.storage.ScannedFile): Boolean =
        file.relativePath.replace('\\', '/').split('/').any { it.equals("Screenshots", true) } ||
            file.name.startsWith("Screenshot", true)

    private fun isHidden(file: com.trashpilot.app.core.storage.ScannedFile): Boolean =
        file.relativePath.replace('\\', '/').split('/').any { it.startsWith(".") }

    private companion object {
        const val LARGE_FILE_MIN_BYTES = 100L * 1024L * 1024L
    }
}
