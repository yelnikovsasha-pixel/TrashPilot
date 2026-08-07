package com.trashpilot.app.core.photoquality

import android.content.ContentResolver
import android.content.ContentUris
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

data class PhotoAnalysisProgress(val analyzed: Int, val total: Int, val flagged: Int)
data class PhotoAnalysisResult(val analyzedCount: Int, val flagged: List<PhotoQualityItem>, val unreadableCount: Int)

class PhotoQualityRepository(
    private val resolver: ContentResolver,
    private val thresholds: PhotoQualityThresholds = PhotoQualityThresholds()
) {
    suspend fun analyze(onProgress: suspend (PhotoQualityItem?, PhotoAnalysisProgress) -> Unit = { _, _ -> }): PhotoAnalysisResult =
        withContext(Dispatchers.IO) {
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val pathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Images.Media.RELATIVE_PATH
            else @Suppress("DEPRECATION") MediaStore.Images.Media.DATA
            val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_TAKEN, MediaStore.Images.Media.DATE_MODIFIED, MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.WIDTH, MediaStore.Images.Media.HEIGHT, pathColumn)
            val flagged = mutableListOf<PhotoQualityItem>()
            var analyzed = 0
            var unreadable = 0
            val cursor = resolver.query(collection, projection, null, null, null) ?: throw SecurityException("MediaStore images unavailable")
            cursor.use {
                val id = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID); val name = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val size = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE); val taken = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val modified = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED); val bucket = it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val widthCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH); val heightCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val path = it.getColumnIndexOrThrow(pathColumn)
                while (it.moveToNext()) {
                    coroutineContext.ensureActive()
                    val uri = ContentUris.withAppendedId(collection, it.getLong(id))
                    val width = if (it.isNull(widthCol)) 0 else it.getInt(widthCol)
                    val height = if (it.isNull(heightCol)) 0 else it.getInt(heightCol)
                    val bitmap = runCatching {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) resolver.loadThumbnail(uri, Size(thresholds.analysisMaximumSide, thresholds.analysisMaximumSide), null)
                        else @Suppress("DEPRECATION") MediaStore.Images.Thumbnails.getThumbnail(resolver, it.getLong(id), MediaStore.Images.Thumbnails.MINI_KIND, null)
                    }.getOrNull()
                    val metrics = bitmap?.let(::metricsFromBitmap)
                    bitmap?.recycle()
                    analyzed++
                    if (metrics == null) unreadable++
                    val reasons = classifyPhoto(width, height, metrics, thresholds)
                    val item = if (reasons.isEmpty()) null else PhotoQualityItem(
                        uri.toString(), it.getString(name).orEmpty(), if (it.isNull(size)) 0 else it.getLong(size),
                        (if (it.isNull(taken)) 0 else it.getLong(taken)).takeIf { value -> value > 0 }
                            ?: if (it.isNull(modified)) 0 else it.getLong(modified) * 1_000L,
                        it.getString(bucket).orEmpty(), it.getString(path).orEmpty(), width, height, reasons, metrics)
                    if (item != null) flagged += item
                    onProgress(item, PhotoAnalysisProgress(analyzed, cursor.count, flagged.size))
                }
            }
            PhotoAnalysisResult(analyzed, flagged, unreadable)
        }

    private fun metricsFromBitmap(bitmap: Bitmap): PhotoMetrics? {
        if (bitmap.width < 3 || bitmap.height < 3) return null
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val luminance = IntArray(pixels.size) { index ->
            val color = pixels[index]
            ((77 * ((color shr 16) and 255) + 150 * ((color shr 8) and 255) + 29 * (color and 255)) shr 8)
        }
        return measureLuminance(luminance, bitmap.width, bitmap.height, thresholds)
    }
}
