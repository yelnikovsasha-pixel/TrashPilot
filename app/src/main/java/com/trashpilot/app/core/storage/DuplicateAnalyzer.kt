package com.trashpilot.app.core.storage

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

data class DuplicateAnalysis(
    val duplicateFiles: List<ScannedFile>
) {
    val duplicateFileCount: Int = duplicateFiles.size
    val duplicateBytes: Long = duplicateFiles.sumOf(ScannedFile::sizeBytes)
}

class DuplicateAnalyzer(
    private val contentResolver: ContentResolver
) {
    suspend fun analyze(files: List<ScannedFile>): DuplicateAnalysis = withContext(Dispatchers.IO) {
        val candidates = files
            .filter { it.sizeBytes > 0L }
            .groupBy(ScannedFile::sizeBytes)
            .values
            .filter { it.size > 1 }
            .flatten()

        val duplicates = candidates
            .mapNotNull { file ->
                coroutineContext.ensureActive()
                hash(file)?.let { hash -> hash to file }
            }
            .groupBy({ it.first }, { it.second })
            .values
            .filter { it.size > 1 }
            .flatMap { matchingFiles ->
                matchingFiles.drop(1)
            }

        DuplicateAnalysis(duplicateFiles = duplicates)
    }

    private fun hash(file: ScannedFile): String? = runCatching {
        val digest = MessageDigest.getInstance("SHA-256")
        contentResolver.openInputStream(Uri.parse(file.uri))?.use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        } ?: return null
        digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }.getOrNull()
}
