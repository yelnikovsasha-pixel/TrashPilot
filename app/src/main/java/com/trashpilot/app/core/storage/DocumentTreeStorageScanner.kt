package com.trashpilot.app.core.storage

import android.content.ContentResolver
import android.net.Uri
import android.os.Environment
import android.os.SystemClock
import android.os.StatFs
import android.provider.DocumentsContract
import android.util.Log
import com.trashpilot.app.core.quickclean.DisposableCandidate
import com.trashpilot.app.core.quickclean.DisposableCategory
import com.trashpilot.app.core.quickclean.DisposableClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

class DocumentTreeStorageScanner(
    private val contentResolver: ContentResolver,
    private val fallbackRootName: String,
    private val unknownFileName: String
) : StorageScanner {

    override suspend fun scan(treeUri: Uri): StorageScanResult = scan(treeUri) {}

    suspend fun scan(
        treeUri: Uri,
        onStage: suspend (ScanStage) -> Unit
    ): StorageScanResult = withContext(Dispatchers.IO) {
        val scanStartedAt = SystemClock.elapsedRealtime()
        onStage(ScanStage.STORAGE)
        val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
        val rootUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocumentId)
        val rootName = queryDisplayName(rootUri).orEmpty().ifBlank { fallbackRootName }
        val categoryBytes = FileCategory.entries.associateWith { 0L }.toMutableMap()
        val files = mutableListOf<ScannedFile>()
        val disposableCandidates = mutableListOf<DisposableCandidate>()
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
            var directoryWasReadable = false
            var hasChildren = false
            contentResolver.query(
                childrenUri,
                DOCUMENT_PROJECTION,
                null,
                null,
                null
            )?.use { cursor ->
                directoryWasReadable = true
                val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                val modifiedIndex = cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED
                )

                while (cursor.moveToNext()) {
                    coroutineContext.ensureActive()
                    hasChildren = true
                    val documentId = cursor.getString(idIndex)
                    val name = cursor.getString(nameIndex).orEmpty().ifBlank { unknownFileName }
                    val mimeType = cursor.getString(mimeIndex)
                    val childPath = directory.pathSegments + name
                    if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                        pendingDirectories.addLast(PendingDirectory(documentId, childPath))
                        continue
                    }

                    val sizeBytes = if (cursor.isNull(sizeIndex)) 0L else cursor.getLong(sizeIndex)
                    val lastModifiedMillis =
                        if (cursor.isNull(modifiedIndex)) 0L else cursor.getLong(modifiedIndex)
                    val category = FileCategorizer.categorize(name, mimeType, childPath)
                    categoryBytes[category] = categoryBytes.getValue(category) + sizeBytes
                    scannedFileCount += 1

                    val scannedFile = ScannedFile(
                        name = name,
                        sizeBytes = sizeBytes,
                        lastModifiedMillis = lastModifiedMillis,
                        uri = DocumentsContract.buildDocumentUriUsingTree(
                            treeUri,
                            documentId
                        ).toString(),
                        category = category,
                        relativePath = childPath.joinToString("/")
                    )
                    files += scannedFile
                    DisposableClassifier.classify(name, childPath, category)?.let {
                        disposableCandidates += DisposableCandidate(
                            uri = scannedFile.uri,
                            name = name,
                            relativePath = childPath.joinToString("/"),
                            sizeBytes = sizeBytes,
                            category = it,
                            isDirectory = false
                        )
                    }
                }
            }
            if (
                directoryWasReadable &&
                !hasChildren &&
                directory.pathSegments.size > 1
            ) {
                disposableCandidates += DisposableCandidate(
                    uri = DocumentsContract.buildDocumentUriUsingTree(
                        treeUri,
                        directory.documentId
                    ).toString(),
                    name = directory.pathSegments.last(),
                    relativePath = directory.pathSegments.joinToString("/"),
                    sizeBytes = 0L,
                    category = DisposableCategory.EMPTY_FOLDERS,
                    isDirectory = true
                )
            }
        }

        onStage(ScanStage.LARGE_FILES)
        files.count { it.sizeBytes >= LARGE_FILE_MIN_BYTES }
        onStage(ScanStage.HIDDEN_FILES)
        files.count { file ->
            file.relativePath.replace('\\', '/').split('/').any { it.startsWith(".") }
        }
        onStage(ScanStage.SOCIAL_MEDIA)
        SocialMediaAnalyzer.filesInSupportedFolders(files)
        onStage(ScanStage.EMPTY_FOLDERS)
        disposableCandidates.count { it.category == DisposableCategory.EMPTY_FOLDERS }
        onStage(ScanStage.FINALIZING)
        val storage = deviceStorage()
        Log.d(
            TAG,
            "Document tree complete files=$scannedFileCount " +
                "bytes=${files.sumOf(ScannedFile::sizeBytes)} " +
                "candidates=${disposableCandidates.size} categories=$categoryBytes"
        )
        StorageScanResult(
            totalBytes = storage.totalBytes,
            usedBytes = storage.usedBytes,
            freeBytes = storage.freeBytes,
            categoryBytes = categoryBytes.toMap(),
            files = files.toList(),
            disposableCandidates = disposableCandidates.toList(),
            scannedFileCount = scannedFileCount,
            selectedRootName = rootName,
            scanDurationMillis = SystemClock.elapsedRealtime() - scanStartedAt
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
        const val TAG = "TrashPilotScan"
        const val LARGE_FILE_MIN_BYTES = 100L * 1024L * 1024L
        val DOCUMENT_PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )
    }
}
