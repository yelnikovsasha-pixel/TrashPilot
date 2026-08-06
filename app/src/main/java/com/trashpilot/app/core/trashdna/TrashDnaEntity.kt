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
    val privacySensitiveAppCount: Long = 0,
    val usedStorageBytes: Long = 0,
    val imageBytes: Long = 0,
    val videoBytes: Long = 0,
    val audioBytes: Long = 0,
    val documentBytes: Long = 0,
    val downloadBytes: Long = 0,
    val messengerMediaBytes: Long = 0,
    val screenshotBytes: Long = 0,
    val largeFileBytes: Long = 0,
    val largeVideoBytes: Long = 0,
    val hiddenFileBytes: Long = 0,
    val messengerSourceName: String = "",
    val reportMetricsRecorded: Boolean = false,
    val scannedBytes: Long = 0,
    val apkBytes: Long = 0,
    val otherBytes: Long = 0,
    val largeFileCount: Long = 0,
    val socialMediaFileCount: Long = 0
)

@Entity(tableName = "trash_dna_state")
data class TrashDnaStateEntity(
    @PrimaryKey val key: String = "state",
    val resetAtMillis: Long = 0
)

object TrashDnaSessionType {
    const val SCAN = "SCAN"
    const val CLEANUP = "CLEANUP"
    const val PRIVACY_REVIEW = "PRIVACY_REVIEW"
    const val CACHE_SCAN = "CACHE_SCAN"
}

object TrashDnaResult {
    const val ANALYZED = "ANALYZED"
    const val CLEANED = "CLEANED"
    const val PARTIAL = "PARTIAL"
}
