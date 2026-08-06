package com.trashpilot.app.core.storage

import android.net.Uri
import com.trashpilot.app.core.quickclean.DisposableCandidate

enum class FileCategory {
    IMAGES,
    VIDEOS,
    AUDIO,
    DOCUMENTS,
    APK_FILES,
    DOWNLOADS,
    OTHER
}

data class ScannedFile(
    val name: String,
    val sizeBytes: Long,
    val lastModifiedMillis: Long,
    val uri: String,
    val category: FileCategory,
    val relativePath: String = "",
    val createdMillis: Long = 0,
    val ownerPackageName: String? = null
)

data class StorageScanResult(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val categoryBytes: Map<FileCategory, Long>,
    val files: List<ScannedFile>,
    val disposableCandidates: List<DisposableCandidate>,
    val scannedFileCount: Int,
    val selectedRootName: String,
    val scanDurationMillis: Long = 0
) {
    val largestFiles: List<ScannedFile>
        get() = files.sortedByDescending(ScannedFile::sizeBytes).take(10)
}

data class StorageScanProgress(
    val scannedFiles: Int,
    val totalFiles: Int?
)

interface StorageScanner {
    suspend fun scan(treeUri: Uri): StorageScanResult
}
