package com.trashpilot.app.core.screenshots

import android.content.ContentResolver
import android.content.ContentUris
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

data class ScreenshotScanProgress(val inspectedImages: Int, val totalImages: Int?)

class MediaStoreScreenshotRepository(private val resolver: ContentResolver) {
    suspend fun scan(onProgress: suspend (ScreenshotItem?, ScreenshotScanProgress) -> Unit = { _, _ -> }): List<ScreenshotItem> =
        withContext(Dispatchers.IO) {
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val pathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Images.Media.RELATIVE_PATH
            else @Suppress("DEPRECATION") MediaStore.Images.Media.DATA
            val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE, MediaStore.Images.Media.DATE_TAKEN, MediaStore.Images.Media.DATE_MODIFIED,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore.Images.Media.WIDTH, MediaStore.Images.Media.HEIGHT, pathColumn)
            val results = mutableListOf<ScreenshotItem>()
            val cursor = resolver.query(collection, projection, null, null, null) ?: throw SecurityException("MediaStore images unavailable")
            cursor.use {
                val id = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val name = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val size = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val taken = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val modified = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                val bucket = it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val width = it.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val height = it.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val path = it.getColumnIndexOrThrow(pathColumn)
                var inspected = 0
                while (it.moveToNext()) {
                    coroutineContext.ensureActive()
                    inspected++
                    val fileName = it.getString(name).orEmpty()
                    val rawPath = it.getString(path).orEmpty()
                    val folder = it.getString(bucket).orEmpty()
                    if (!isConfidentScreenshotPath(rawPath, folder, fileName)) {
                        onProgress(null, ScreenshotScanProgress(inspected, cursor.count))
                        continue
                    }
                    val dateTaken = if (it.isNull(taken)) 0 else it.getLong(taken)
                    val dateModified = if (it.isNull(modified)) 0 else it.getLong(modified) * 1_000L
                    val item = ScreenshotItem(
                        uri = ContentUris.withAppendedId(collection, it.getLong(id)).toString(),
                        name = fileName,
                        sizeBytes = if (it.isNull(size)) 0 else it.getLong(size),
                        timestampMillis = dateTaken.takeIf { value -> value > 0 } ?: dateModified,
                        folderName = folder,
                        relativePath = rawPath,
                        width = if (it.isNull(width) || it.getInt(width) <= 0) null else it.getInt(width),
                        height = if (it.isNull(height) || it.getInt(height) <= 0) null else it.getInt(height)
                    )
                    results += item
                    onProgress(item, ScreenshotScanProgress(inspected, cursor.count))
                }
            }
            results
        }
}
