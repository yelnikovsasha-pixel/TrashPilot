package com.trashpilot.app.core.reports

import com.trashpilot.app.core.storage.FileCategory
import com.trashpilot.app.core.trashdna.TrashDnaResult
import com.trashpilot.app.core.trashdna.TrashDnaSessionEntity
import com.trashpilot.app.core.trashdna.TrashDnaSessionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReportsAnalyzerTest {
    @Test
    fun `no scan history produces no report summary`() {
        assertNull(ReportsAnalyzer.summarize(emptyList()))
        assertNull(ReportsAnalyzer.summarize(listOf(cleanup(10, 20))))
    }

    @Test
    fun `summary uses only recorded scan and cleanup values`() {
        val summary = checkNotNull(ReportsAnalyzer.summarize(listOf(
            scan(id = 1, time = 100, scanned = 1_000, images = 700),
            cleanup(time = 150, reclaimed = 120),
            scan(id = 2, time = 200, scanned = 2_000, videos = 1_200),
            cleanup(time = 250, reclaimed = 80)
        )))

        assertEquals(2, summary.totalScans)
        assertEquals(3_000L, summary.totalAnalyzedBytes)
        assertEquals(200L, summary.totalCleanedBytes)
        assertEquals(200L, summary.lastScanMillis)
        assertEquals(100L, summary.averageCleanedPerScanBytes)
        assertEquals(listOf(2L, 1L), summary.scans.map { it.id })
        assertEquals(80L, summary.scans[0].cleanedBytes)
        assertEquals(FileCategory.VIDEOS, summary.scans[0].largestCategory)
    }

    @Test
    fun `legacy scan metrics remain explicitly unavailable`() {
        val legacy = scan(id = 1, time = 100, scanned = 0).copy(reportMetricsRecorded = false)
        val summary = checkNotNull(ReportsAnalyzer.summarize(listOf(legacy)))

        assertNull(summary.totalAnalyzedBytes)
        assertNull(summary.scans.single().scannedBytes)
        assertNull(summary.scans.single().details.cacheBytes)
        assertNull(summary.scans.single().largestCategory)
    }

    @Test
    fun `detail exposes exact aggregate categories and never invents duplicates`() {
        val report = checkNotNull(ReportsAnalyzer.summarize(listOf(
            scan(id = 1, time = 100, scanned = 1_000, cache = 20, hidden = 30,
                largeBytes = 400, largeCount = 2, emptyFolders = 3,
                socialBytes = 50, socialCount = 4)
        ))).scans.single()

        assertEquals(20L, report.details.cacheBytes)
        assertEquals(30L, report.details.hiddenBytes)
        assertEquals(400L, report.details.largeFileBytes)
        assertEquals(2L, report.details.largeFileCount)
        assertEquals(3L, report.details.emptyFolderCount)
        assertEquals(50L, report.details.socialMediaBytes)
        assertEquals(4L, report.details.socialMediaFileCount)
        assertNull(report.details.duplicateBytes)
    }

    private fun scan(
        id: Long,
        time: Long,
        scanned: Long,
        images: Long = 0,
        videos: Long = 0,
        cache: Long = 0,
        hidden: Long = 0,
        largeBytes: Long = 0,
        largeCount: Long = 0,
        emptyFolders: Long = 0,
        socialBytes: Long = 0,
        socialCount: Long = 0
    ) = TrashDnaSessionEntity(
        id = id,
        sessionType = TrashDnaSessionType.SCAN,
        timestampMillis = time,
        scannedFolderName = "Device",
        reclaimableBytes = 0,
        reclaimedBytes = 0,
        result = TrashDnaResult.ANALYZED,
        temporaryBytes = 0,
        cacheBytes = cache,
        emptyFolderCount = emptyFolders,
        apkLeftoverBytes = 0,
        logBytes = 0,
        reportMetricsRecorded = true,
        scannedBytes = scanned,
        imageBytes = images,
        videoBytes = videos,
        hiddenFileBytes = hidden,
        largeFileBytes = largeBytes,
        largeFileCount = largeCount,
        messengerMediaBytes = socialBytes,
        socialMediaFileCount = socialCount,
        scanDurationMillis = 500
    )

    private fun cleanup(time: Long, reclaimed: Long) = TrashDnaSessionEntity(
        sessionType = TrashDnaSessionType.CLEANUP,
        timestampMillis = time,
        scannedFolderName = "Device",
        reclaimableBytes = reclaimed,
        reclaimedBytes = reclaimed,
        result = TrashDnaResult.CLEANED,
        temporaryBytes = 0,
        cacheBytes = 0,
        emptyFolderCount = 0,
        apkLeftoverBytes = 0,
        logBytes = 0
    )
}
