package com.trashpilot.app.core.reports

import com.trashpilot.app.core.trashdna.TrashDnaSessionEntity
import com.trashpilot.app.core.trashdna.TrashDnaSessionType

data class ReportsSummary(
    val scans: List<TrashDnaSessionEntity>,
    val cleanups: List<TrashDnaSessionEntity>,
    val privacyReviews: List<TrashDnaSessionEntity>,
    val reclaimedBytes: Long
)

object ReportsAnalyzer {
    fun summarize(history: List<TrashDnaSessionEntity>) = ReportsSummary(
        scans = history.filter { it.sessionType == TrashDnaSessionType.SCAN },
        cleanups = history.filter { it.sessionType == TrashDnaSessionType.CLEANUP },
        privacyReviews = history.filter { it.sessionType == TrashDnaSessionType.PRIVACY_REVIEW },
        reclaimedBytes = history
            .filter { it.sessionType == TrashDnaSessionType.CLEANUP }
            .sumOf { it.reclaimedBytes }
    )
}

