package com.trashpilot.app.core.storage

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlinx.coroutines.runBlocking

class DuplicateAnalysisTest {
    @Test
    fun `reports only redundant copies`() {
        val duplicate = ScannedFile(
            name = "copy.jpg",
            sizeBytes = 42,
            lastModifiedMillis = 0,
            uri = "content://test/copy",
            category = FileCategory.IMAGES
        )

        val original = duplicate.copy(uri = "content://original", lastModifiedMillis = 1)
        val analysis = DuplicateAnalysis(listOf(DuplicateGroup("hash", listOf(original, duplicate))))

        assertEquals(1, analysis.duplicateFileCount)
        assertEquals(42, analysis.duplicateBytes)
    }

    @Test
    fun `hashes only files whose sizes match and groups only matching fingerprints`() = runBlocking {
        val files = listOf(
            file("old.jpg", 10, 1), file("copy.jpg", 10, 2),
            file("different.jpg", 10, 3), file("unique.mp3", 20, 4)
        )
        val hashed = mutableListOf<String>()

        val analysis = DuplicateGroupingEngine.analyze(files, fingerprint = {
            hashed += it.name
            if (it.name == "different.jpg") "different" else "same"
        })

        assertEquals(listOf("old.jpg", "copy.jpg", "different.jpg"), hashed)
        assertEquals(1, analysis.groups.size)
        assertEquals("old.jpg", analysis.groups.single().keptFile.name)
        assertEquals(setOf("copy.jpg"), analysis.duplicateFiles.map { it.name }.toSet())
    }

    @Test
    fun `processes large candidate sets incrementally`() = runBlocking {
        val files = (1..2_000).map { file("$it.jpg", 10, it.toLong()) }
        var lastProgress = DuplicateScanProgress(0, 0)

        val analysis = DuplicateGroupingEngine.analyze(files, { lastProgress = it }) { "same" }

        assertEquals(2_000, lastProgress.processedFiles)
        assertEquals(2_000, lastProgress.totalFiles)
        assertEquals(1_999, analysis.duplicateFileCount)
    }

    @Test
    fun `empty storage returns no groups without hashing`() = runBlocking {
        var hashes = 0
        val analysis = DuplicateGroupingEngine.analyze(emptyList(), fingerprint = {
            hashes += 1
            "unused"
        })

        assertEquals(0, hashes)
        assertEquals(0, analysis.groups.size)
        assertEquals(0, analysis.recoverableBytes)
    }

    private fun file(name: String, size: Long, modified: Long) = ScannedFile(
        name = name, sizeBytes = size, lastModifiedMillis = modified,
        uri = "content://test/$name", category = FileCategory.IMAGES
    )
}
