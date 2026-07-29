package com.trashpilot.app.core.trashdna

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trash_dna_sessions")
data class TrashDnaSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionType: String,
    val timestampMillis: Long,
    val scannedFolderName: String,
    val reclaimableBytes: Long,
    val reclaimedBytes: Long,
    val result: String,
    val temporaryBytes: Long,
    val cacheBytes: Long,
    val emptyFolderCount: Long,
    val apkLeftoverBytes: Long,
    val logBytes: Long,
    val scannedFileCount: Long = 0,
    val scanDurationMillis: Long = 0,
    val privacyAppsChecked: Long = 0,
    val privacySensitiveAppCount: Long = 0
)

object TrashDnaSessionType {
    const val SCAN = "SCAN"
    const val CLEANUP = "CLEANUP"
    const val PRIVACY_REVIEW = "PRIVACY_REVIEW"
}

object TrashDnaResult {
    const val ANALYZED = "ANALYZED"
    const val CLEANED = "CLEANED"
    const val PARTIAL = "PARTIAL"
}
