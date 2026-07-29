package com.trashpilot.app.core.reports

import com.trashpilot.app.core.storage.formatBytes
import java.text.DateFormat
import java.util.Date

object ReportExporter {
    fun create(summary: ReportsSummary, dateFormat: DateFormat = DateFormat.getDateTimeInstance()): String =
        buildString {
            appendLine("TrashPilot local report")
            appendLine("Metadata only. No file names, paths, or file contents.")
            appendLine()
            appendLine("Summary")
            appendLine("Scans: ${summary.scans.size}")
            appendLine("Quick Clean sessions: ${summary.cleanups.size}")
            appendLine("Privacy Monitor reviews: ${summary.privacyReviews.size}")
            appendLine("Storage reclaimed: ${formatBytes(summary.reclaimedBytes)}")
            appendLine()
            appendLine("Scan history")
            summary.scans.forEach { session ->
                appendLine(
                    "${dateFormat.format(Date(session.timestampMillis))} | " +
                        "${session.scannedFolderName} | " +
                        if (session.scanDurationMillis > 0) {
                            "${session.scannedFileCount} files | ${session.scanDurationMillis} ms | "
                        } else {
                            "file count not recorded | duration not recorded | "
                        } +
                        "reclaimable ${formatBytes(session.reclaimableBytes)}"
                )
            }
            appendLine()
            appendLine("Cleaning history")
            summary.cleanups.forEach { session ->
                appendLine(
                    "${dateFormat.format(Date(session.timestampMillis))} | " +
                        "${session.scannedFolderName} | reclaimed ${formatBytes(session.reclaimedBytes)} | " +
                        session.result
                )
            }
            appendLine()
            appendLine("Privacy Monitor history")
            summary.privacyReviews.forEach { session ->
                appendLine(
                    "${dateFormat.format(Date(session.timestampMillis))} | " +
                        "${session.privacyAppsChecked} apps checked | " +
                        "${session.privacySensitiveAppCount} apps with sensitive permissions"
                )
            }
        }
}
