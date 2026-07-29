package com.trashpilot.app.core.trashdna

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrashDnaAnalyzerTest {
    @Test
    fun `summary stays empty until two real scans exist`() {
        assertNull(TrashDnaAnalyzer.summary(emptyList()))
        assertNull(TrashDnaAnalyzer.summary(listOf(scan(1, temporary = 10))))
    }

    @Test
    fun `summary uses stored aggregates without inventing values`() {
        val history = listOf(
            scan(100, reclaimable = 100, temporary = 60, cache = 20),
            scan(200, reclaimable = 300, temporary = 120, cache = 30),
            cleanup(250, reclaimed = 80)
        )
        val summary = checkNotNull(TrashDnaAnalyzer.summary(history))
        assertEquals(2, summary.scansCompleted)
        assertEquals(1, summary.cleanupsCompleted)
        assertEquals(TrashDnaCategory.TEMPORARY_FILES, summary.mostCommonCategory)
        assertEquals(200, summary.averageReclaimableBytes)
        assertEquals(200, summary.lastScanMillis)
    }

    @Test
    fun `insights require three scans and supported patterns`() {
        val history = listOf(
            scan(1, reclaimable = 100, temporary = 10, apk = 5, logs = 2),
            scan(2, reclaimable = 200, temporary = 40, apk = 6, logs = 3),
            scan(3, reclaimable = 300, temporary = 100, apk = 0, logs = 4)
        )
        val insights = TrashDnaAnalyzer.insights(history)
        assertTrue(TrashDnaInsight.TEMPORARY_FILES_ACCUMULATE_FASTEST in insights)
        assertTrue(TrashDnaInsight.APK_LEFTOVERS_RECUR in insights)
        assertTrue(TrashDnaInsight.LOG_FILES_REMAIN_LOW in insights)
    }

    private fun scan(
        time: Long,
        reclaimable: Long = 0,
        temporary: Long = 0,
        cache: Long = 0,
        apk: Long = 0,
        logs: Long = 0
    ) = TrashDnaSessionEntity(
        sessionType = TrashDnaSessionType.SCAN,
        timestampMillis = time,
        scannedFolderName = "Folder",
        reclaimableBytes = reclaimable,
        reclaimedBytes = 0,
        result = TrashDnaResult.ANALYZED,
        temporaryBytes = temporary,
        cacheBytes = cache,
        emptyFolderCount = 0,
        apkLeftoverBytes = apk,
        logBytes = logs
    )

    private fun cleanup(time: Long, reclaimed: Long) = TrashDnaSessionEntity(
        sessionType = TrashDnaSessionType.CLEANUP,
        timestampMillis = time,
        scannedFolderName = "Folder",
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
