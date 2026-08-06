package com.trashpilot.app.core.trashdna

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrashDnaAnalyzerTest {
    @Test
    fun `analysis stays empty until two completed metric scans exist`() {
        assertNull(TrashDnaAnalyzer.analyze(emptyList()))
        assertNull(TrashDnaAnalyzer.analyze(listOf(scan(1, used = 100))))
        assertNull(TrashDnaAnalyzer.analyze(listOf(scan(1, used = 0), scan(2, used = 100))))
    }

    @Test
    fun `calculates real storage and category change from latest scans`() {
        val analysis = checkNotNull(TrashDnaAnalyzer.analyze(listOf(
            scan(1, used = 1_000, downloads = 100),
            scan(2, used = 1_400, downloads = 500)
        )))

        assertEquals(400, analysis.storageTrend.changeBytes)
        assertEquals(TrashDnaCategory.DOWNLOADS, analysis.fastestGrowingCategory?.category)
        assertEquals(400L, analysis.fastestGrowingCategory?.changeBytes)
        assertEquals(TrashDnaInsight.DOWNLOADS_GROWTH, analysis.insight)
        assertEquals(TrashDnaRecommendation.REMOVE_OLD_DOWNLOADS, analysis.recommendation)
    }

    @Test
    fun `profile detector chooses dominant recorded pattern and otherwise balanced`() {
        val messenger = ProfileDetector.detect(
            scan(1, used = 100, messenger = 10),
            scan(2, used = 500, messenger = 410)
        )
        val balanced = ProfileDetector.detect(
            scan(1, used = 100, downloads = 10, screenshots = 10),
            scan(2, used = 200, downloads = 60, screenshots = 55)
        )

        assertEquals(TrashDnaProfile.MESSENGER_HEAVY, messenger)
        assertEquals(TrashDnaProfile.BALANCED, balanced)
    }

    @Test
    fun `history is chronological and attaches only real cleanup totals`() {
        val analysis = checkNotNull(TrashDnaAnalyzer.analyze(listOf(
            scan(200, used = 200, videos = 80),
            cleanup(150, reclaimed = 25),
            scan(100, used = 100, videos = 40),
            cleanup(250, reclaimed = 10)
        )))

        assertEquals(listOf(100L, 200L), analysis.history.map { it.timestampMillis })
        assertEquals(25, analysis.history[0].deletedBytes)
        assertEquals(10, analysis.history[1].deletedBytes)
        assertEquals(TrashDnaCategory.VIDEOS, analysis.history[1].largestCategory)
    }

    @Test
    fun `storage reduction produces calm stable recommendation`() {
        val analysis = checkNotNull(TrashDnaAnalyzer.analyze(listOf(
            scan(1, used = 500, hidden = 100),
            scan(2, used = 300, hidden = 50)
        )))

        assertTrue(analysis.storageTrend.changeBytes < 0)
        assertEquals(TrashDnaInsight.STORAGE_REDUCED, analysis.insight)
        assertEquals(TrashDnaRecommendation.KEEP_CURRENT_HABITS, analysis.recommendation)
    }

    private fun scan(
        time: Long,
        used: Long,
        downloads: Long = 0,
        screenshots: Long = 0,
        messenger: Long = 0,
        videos: Long = 0,
        hidden: Long = 0
    ) = TrashDnaSessionEntity(
        sessionType = TrashDnaSessionType.SCAN,
        timestampMillis = time,
        scannedFolderName = "Device",
        reclaimableBytes = 0,
        reclaimedBytes = 0,
        result = TrashDnaResult.ANALYZED,
        temporaryBytes = 0,
        cacheBytes = 0,
        emptyFolderCount = 0,
        apkLeftoverBytes = 0,
        logBytes = 0,
        usedStorageBytes = used,
        downloadBytes = downloads,
        screenshotBytes = screenshots,
        messengerMediaBytes = messenger,
        videoBytes = videos,
        hiddenFileBytes = hidden
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
