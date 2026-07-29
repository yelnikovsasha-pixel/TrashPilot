package com.trashpilot.app.core.storage

import android.net.Uri

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
    val uri: Uri,
    val category: FileCategory
)

data class StorageScanResult(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val categoryBytes: Map<FileCategory, Long>,
    val largestFiles: List<ScannedFile>,
    val scannedFileCount: Int,
    val selectedRootName: String
)

interface StorageScanner {
    suspend fun scan(treeUri: Uri): StorageScanResult
}
