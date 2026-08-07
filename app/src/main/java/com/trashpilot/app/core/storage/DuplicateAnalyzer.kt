package com.trashpilot.app.core.storage

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

data class DuplicateGroup(val fingerprint: String, val files: List<ScannedFile>) {
    val keptFile: ScannedFile = files.minWithOrNull(
        compareBy<ScannedFile> { file ->
            (file.createdMillis.takeIf { it > 0 } ?: file.lastModifiedMillis)
                .takeIf { it > 0 } ?: Long.MAX_VALUE
        }
            .thenBy { it.uri }
    ) ?: error("A duplicate group cannot be empty")
    val redundantFiles: List<ScannedFile> = files.filterNot { it.uri == keptFile.uri }
    val recoverableBytes: Long = redundantFiles.sumOf(ScannedFile::sizeBytes)
}

data class DuplicateAnalysis(val groups: List<DuplicateGroup>) {
    val initialSelection: Set<String> = emptySet()
    val duplicateFileCount: Int = groups.sumOf { it.redundantFiles.size }
    val recoverableBytes: Long = groups.sumOf(DuplicateGroup::recoverableBytes)
    val duplicateBytes: Long get() = recoverableBytes
    val duplicateFiles: List<ScannedFile> get() = groups.flatMap(DuplicateGroup::redundantFiles)
}

data class DuplicateScanProgress(val processedFiles: Int, val totalFiles: Int)

class DuplicateAnalyzer(private val contentResolver: ContentResolver) {
    suspend fun analyze(
        files: List<ScannedFile>,
        onProgress: suspend (DuplicateScanProgress) -> Unit = {}
    ): DuplicateAnalysis = withContext(Dispatchers.IO) {
        var candidateCount = 0
        var readableCount = 0
        val analysis = DuplicateGroupingEngine.analyze(
            files.filter(::isSupportedSharedFile),
            onProgress = {
                candidateCount = it.totalFiles
                onProgress(it)
            },
            fingerprint = { file -> hash(file)?.also { readableCount += 1 } }
        )
        if (candidateCount > 0 && readableCount == 0) throw DuplicateStorageUnavailableException()
        analysis
    }

    private fun isSupportedSharedFile(file: ScannedFile): Boolean {
        if (file.sizeBytes < 0 || file.category !in SUPPORTED_CATEGORIES) return false
        val path = file.relativePath.replace('\\', '/').lowercase()
        return EXCLUDED_PATHS.none(path::contains)
    }

    private suspend fun hash(file: ScannedFile): String? = try {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = contentResolver.openInputStream(Uri.parse(file.uri)) ?: return null
        input.use {
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                coroutineContext.ensureActive()
                val read = it.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    } catch (_: IOException) {
        null
    }

    private companion object {
        val SUPPORTED_CATEGORIES = setOf(
            FileCategory.IMAGES, FileCategory.VIDEOS, FileCategory.DOCUMENTS, FileCategory.AUDIO
        )
        val EXCLUDED_PATHS = setOf("android/data/", "android/obb/", "com.trashpilot.app")
    }
}

class DuplicateStorageUnavailableException : IOException("Shared storage files could not be read")

object DuplicateGroupingEngine {
    suspend fun analyze(
        files: List<ScannedFile>,
        onProgress: suspend (DuplicateScanProgress) -> Unit = {},
        fingerprint: suspend (ScannedFile) -> String?
    ): DuplicateAnalysis {
        val candidates = files.groupBy(ScannedFile::sizeBytes).values.filter { it.size > 1 }.flatten()
        onProgress(DuplicateScanProgress(0, candidates.size))
        var processed = 0
        val matches = mutableMapOf<Pair<Long, String>, MutableList<ScannedFile>>()
        candidates.forEach { file ->
            coroutineContext.ensureActive()
            fingerprint(file)?.let { digest -> matches.getOrPut(file.sizeBytes to digest, ::mutableListOf) += file }
            processed += 1
            onProgress(DuplicateScanProgress(processed, candidates.size))
        }
        return DuplicateAnalysis(
            matches.asSequence().filter { it.value.size > 1 }
                .map { (key, matching) -> DuplicateGroup(key.second, matching.toList()) }
                .sortedByDescending(DuplicateGroup::recoverableBytes).toList()
        )
    }
}
