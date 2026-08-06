package com.trashpilot.app.core.storage

import android.content.ContentResolver
import android.content.ContentUris
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import com.trashpilot.app.core.quickclean.DisposableCandidate
import com.trashpilot.app.core.quickclean.DisposableClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * Scans the shared-storage records Android already exposes to this app.
 *
 * This is the default scan path. It does not request broad filesystem access and
 * does not infer files that Android has not made readable. SAF remains available
 * to the UI when this query is blocked and the user chooses to grant folder access.
 */
class MediaStoreStorageScanner(
    private val contentResolver: ContentResolver,
    private val rootName: String,
    private val unknownFileName: String
) {
    suspend fun scan(
        onStage: suspend (ScanStage) -> Unit = {}
    ): StorageScanResult = withContext(Dispatchers.IO) {
        val scanStartedAt = SystemClock.elapsedRealtime()
        val categoryBytes = FileCategory.entries.associateWith { 0L }.toMutableMap()
        val files = mutableListOf<ScannedFile>()
        val disposableCandidates = mutableListOf<DisposableCandidate>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Files.getContentUri("external")
        }
        val pathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.FileColumns.RELATIVE_PATH
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Files.FileColumns.DATA
        }
        val projection = BASE_PROJECTION + pathColumn

        onStage(ScanStage.STORAGE)
        Log.d(TAG, "Querying collection=$collection projection=${projection.joinToString()}")
        val cursor = contentResolver.query(
            collection,
            projection,
            null,
            null,
            null
        ) ?: throw StorageAccessRequiredException()
        cursor.use {
            Log.d(TAG, "MediaStore cursor count=${cursor.count}")
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val mediaTypeIndex =
                cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val modifiedIndex =
                cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val createdIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            val pathIndex = cursor.getColumnIndexOrThrow(pathColumn)

            while (cursor.moveToNext()) {
                coroutineContext.ensureActive()
                val mimeType = cursor.getString(mimeIndex)
                val mediaType = cursor.getInt(mediaTypeIndex)
                if (
                    mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_NONE &&
                    mimeType == null
                ) {
                    continue
                }
                val name = cursor.getString(nameIndex).orEmpty().ifBlank { unknownFileName }
                val storagePath = cursor.getString(pathIndex).orEmpty()
                val pathSegments = storagePath
                    .split('/')
                    .filter(String::isNotBlank) + name
                val sizeBytes = if (cursor.isNull(sizeIndex)) 0L else cursor.getLong(sizeIndex)
                val category = FileCategorizer.categorize(name, mimeType, pathSegments)
                val uri = ContentUris.withAppendedId(
                    collection,
                    cursor.getLong(idIndex)
                ).toString()
                val scannedFile = ScannedFile(
                    name = name,
                    sizeBytes = sizeBytes,
                    lastModifiedMillis = if (cursor.isNull(modifiedIndex)) {
                        0L
                    } else {
                        cursor.getLong(modifiedIndex) * MILLIS_PER_SECOND
                    },
                    uri = uri,
                    category = category,
                    relativePath = pathSegments.joinToString("/"),
                    createdMillis = if (cursor.isNull(createdIndex)) {
                        0L
                    } else {
                        cursor.getLong(createdIndex) * MILLIS_PER_SECOND
                    }
                )

                categoryBytes[category] = categoryBytes.getValue(category) + sizeBytes
                files += scannedFile
                DisposableClassifier.classify(name, pathSegments, category)?.let { disposable ->
                    disposableCandidates += DisposableCandidate(
                        uri = uri,
                        name = name,
                        relativePath = pathSegments.joinToString("/"),
                        sizeBytes = sizeBytes,
                        category = disposable,
                        isDirectory = false
                    )
                }
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
        onStage(ScanStage.FINALIZING)
        val storage = deviceStorage()
        Log.d(
            TAG,
            "Scan complete files=${files.size} bytes=${files.sumOf(ScannedFile::sizeBytes)} " +
                "candidates=${disposableCandidates.size} categories=$categoryBytes"
        )
        StorageScanResult(
            totalBytes = storage.totalBytes,
            usedBytes = storage.usedBytes,
            freeBytes = storage.freeBytes,
            categoryBytes = categoryBytes.toMap(),
            files = files,
            disposableCandidates = disposableCandidates,
            scannedFileCount = files.size,
            selectedRootName = rootName,
            scanDurationMillis = SystemClock.elapsedRealtime() - scanStartedAt
        )
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

    private data class DeviceStorage(
        val totalBytes: Long,
        val usedBytes: Long,
        val freeBytes: Long
    )

    private companion object {
        const val TAG = "TrashPilotScan"
        const val MILLIS_PER_SECOND = 1_000L
        const val LARGE_FILE_MIN_BYTES = 100L * 1024L * 1024L
        val BASE_PROJECTION = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.DATE_ADDED
        )
    }
}

class StorageAccessRequiredException : SecurityException(
    "Android did not expose shared storage to TrashPilot"
)
