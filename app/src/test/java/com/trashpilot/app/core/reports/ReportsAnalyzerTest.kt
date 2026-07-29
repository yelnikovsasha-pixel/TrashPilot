package com.trashpilot.app.core.reports

import com.trashpilot.app.core.trashdna.TrashDnaSessionEntity
import com.trashpilot.app.core.trashdna.TrashDnaSessionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class ReportsAnalyzerTest {
    @Test
    fun `summary separates real event types and sums reclaimed cleanup bytes`() {
        val summary = ReportsAnalyzer.summarize(
            listOf(
                session(TrashDnaSessionType.SCAN),
                session(TrashDnaSessionType.CLEANUP, reclaimed = 120),
                session(TrashDnaSessionType.PRIVACY_REVIEW),
                session(TrashDnaSessionType.CLEANUP, reclaimed = 30)
            )
        )

        assertEquals(1, summary.scans.size)
        assertEquals(2, summary.cleanups.size)
        assertEquals(1, summary.privacyReviews.size)
        assertEquals(150, summary.reclaimedBytes)
    }

    @Test
    fun `export contains recorded metadata and excludes file-level fields`() {
        val summary = ReportsAnalyzer.summarize(
            listOf(
                session(
                    type = TrashDnaSessionType.SCAN,
                    folder = "Download",
                    fileCount = 9,
                    duration = 450
                )
            )
        )
        val export = ReportExporter.create(
            summary,
            SimpleDateFormat("yyyy-MM-dd", Locale.US)
        )

        assert(export.contains("Download | 9 files | 450 ms"))
        assertFalse(export.contains("content://"))
        assertFalse(export.contains("secret.jpg"))
    }

    private fun session(
        type: String,
        reclaimed: Long = 0,
        folder: String = "",
        fileCount: Long = 0,
        duration: Long = 0
    ) = TrashDnaSessionEntity(
        sessionType = type,
        timestampMillis = 1_700_000_000_000,
        scannedFolderName = folder,
        reclaimableBytes = 0,
        reclaimedBytes = reclaimed,
        result = "OK",
        temporaryBytes = 0,
        cacheBytes = 0,
        emptyFolderCount = 0,
        apkLeftoverBytes = 0,
        logBytes = 0,
        scannedFileCount = fileCount,
        scanDurationMillis = duration
    )
}
