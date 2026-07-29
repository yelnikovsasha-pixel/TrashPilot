package com.trashpilot.app.core.trashdna

import com.trashpilot.app.core.quickclean.CleaningReport
import com.trashpilot.app.core.quickclean.DisposableCategory
import com.trashpilot.app.core.privacy.PrivacySnapshot
import com.trashpilot.app.core.storage.StorageScanResult

class TrashDnaRepository(private val dao: TrashDnaDao) {
    suspend fun recordScan(result: StorageScanResult, timestampMillis: Long = System.currentTimeMillis()) {
        val grouped = result.disposableCandidates.groupBy { it.category }
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
                scanDurationMillis = result.scanDurationMillis
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

    suspend fun loadReportHistory(): List<TrashDnaSessionEntity> = dao.loadAll()

    suspend fun resetLocalHistory() = dao.clearAll()

    suspend fun replaceLocalHistory(sessions: List<TrashDnaSessionEntity>) {
        dao.replaceAll(sessions.map { it.copy(id = 0) })
    }

    private fun Map<DisposableCategory, List<com.trashpilot.app.core.quickclean.DisposableCandidate>>.bytes(
        category: DisposableCategory
    ): Long = get(category)?.sumOf { it.sizeBytes } ?: 0
}
