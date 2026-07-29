package com.trashpilot.app.core.storage

import android.content.ContentResolver
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

class DocumentTreeStorageScanner(
    private val contentResolver: ContentResolver
) : StorageScanner {

    override suspend fun scan(treeUri: Uri): StorageScanResult = withContext(Dispatchers.IO) {
        val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
        val rootUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocumentId)
        val rootName = queryDisplayName(rootUri).orEmpty().ifBlank { "Selected storage" }
        val categoryBytes = FileCategory.entries.associateWith { 0L }.toMutableMap()
        val largestFiles = mutableListOf<ScannedFile>()
        var scannedFileCount = 0

        val pendingDirectories = ArrayDeque(
            listOf(PendingDirectory(rootDocumentId, listOf(rootName)))
        )
        while (pendingDirectories.isNotEmpty()) {
            coroutineContext.ensureActive()
            val directory = pendingDirectories.removeFirst()
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri,
                directory.documentId
            )
            contentResolver.query(
                childrenUri,
                DOCUMENT_PROJECTION,
                null,
                null,
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)

                while (cursor.moveToNext()) {
                    coroutineContext.ensureActive()
                    val documentId = cursor.getString(idIndex)
                    val name = cursor.getString(nameIndex).orEmpty().ifBlank { "Unnamed file" }
                    val mimeType = cursor.getString(mimeIndex)
                    val childPath = directory.pathSegments + name
                    if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                        pendingDirectories.addLast(PendingDirectory(documentId, childPath))
                        continue
                    }

                    val sizeBytes = if (cursor.isNull(sizeIndex)) 0L else cursor.getLong(sizeIndex)
                    val category = FileCategorizer.categorize(name, mimeType, childPath)
                    categoryBytes[category] = categoryBytes.getValue(category) + sizeBytes
                    scannedFileCount += 1

                    val scannedFile = ScannedFile(
                        name = name,
                        sizeBytes = sizeBytes,
                        uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId),
                        category = category
                    )
                    largestFiles += scannedFile
                    largestFiles.sortByDescending(ScannedFile::sizeBytes)
                    if (largestFiles.size > MAX_LARGEST_FILES) {
                        largestFiles.removeAt(largestFiles.lastIndex)
                    }
                }
            }
        }

        val storage = deviceStorage()
        StorageScanResult(
            totalBytes = storage.totalBytes,
            usedBytes = storage.usedBytes,
            freeBytes = storage.freeBytes,
            categoryBytes = categoryBytes.toMap(),
            largestFiles = largestFiles.toList(),
            scannedFileCount = scannedFileCount,
            selectedRootName = rootName
        )
    }

    private fun queryDisplayName(uri: Uri): String? =
        contentResolver.query(
            uri,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }

    @Suppress("DEPRECATION")
    private fun deviceStorage(): DeviceStorage {
        val statFs = StatFs(Environment.getExternalStorageDirectory().absolutePath)
        val totalBytes = statFs.totalBytes
        val freeBytes = statFs.availableBytes
        return DeviceStorage(
            totalBytes = totalBytes,
            usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L),
            freeBytes = freeBytes
        )
    }

    private data class PendingDirectory(
        val documentId: String,
        val pathSegments: List<String>
    )

    private data class DeviceStorage(
        val totalBytes: Long,
        val usedBytes: Long,
        val freeBytes: Long
    )

    private companion object {
        const val MAX_LARGEST_FILES = 10
        val DOCUMENT_PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        )
    }
}
