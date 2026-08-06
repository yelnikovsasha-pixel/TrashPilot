package com.trashpilot.app.core.storage

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

data class DuplicateCleaningReport(
    val deletedFiles: List<ScannedFile>,
    val failedFiles: List<ScannedFile>,
    val permissionDenied: Boolean = false
) {
    val reclaimedBytes = deletedFiles.sumOf(ScannedFile::sizeBytes)
}

class DuplicateCleaner(private val contentResolver: ContentResolver) {
    suspend fun clean(files: List<ScannedFile>): DuplicateCleaningReport = withContext(Dispatchers.IO) {
        val deleted = mutableListOf<ScannedFile>()
        val failed = mutableListOf<ScannedFile>()
        var permissionDenied = false
        files.forEach { file ->
            coroutineContext.ensureActive()
            val uri = Uri.parse(file.uri)
            val removed = try {
                DocumentsContract.deleteDocument(contentResolver, uri)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                try {
                    contentResolver.delete(uri, null, null) > 0
                } catch (_: SecurityException) {
                    permissionDenied = true
                    false
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    false
                }
            }
            if (removed) deleted += file else failed += file
        }
        DuplicateCleaningReport(deleted, failed, permissionDenied)
    }
}
