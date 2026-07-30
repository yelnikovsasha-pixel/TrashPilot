package com.trashpilot.app.features.results

import com.trashpilot.app.core.quickclean.DisposableCandidate
import com.trashpilot.app.core.quickclean.DisposableCategory
import com.trashpilot.app.core.storage.FileCategory
import com.trashpilot.app.core.storage.ScannedFile
import com.trashpilot.app.core.storage.StorageScanResult
import org.junit.Assert.assertEquals
import org.junit.Test

class ResultsModelsTest {
    @Test
    fun `sorts files by size name and date`() {
        val files = listOf(
            file("Zulu", 20, 100),
            file("alpha", 10, 300),
            file("Beta", 30, 200)
        )

        assertEquals(listOf("Beta", "Zulu", "alpha"), files.sortedFor(FileSortOption.SIZE).names())
        assertEquals(listOf("alpha", "Beta", "Zulu"), files.sortedFor(FileSortOption.NAME).names())
        assertEquals(listOf("alpha", "Beta", "Zulu"), files.sortedFor(FileSortOption.DATE).names())
    }

    @Test
    fun `overview uses only values present in the scan result`() {
        val visible = file("video.bin", 120L * 1024 * 1024, 100)
        val hidden = file(".private.bin", 20, 200, "Downloads/.private/.private.bin")
        val social = file(
            "photo.jpg",
            40,
            300,
            "Android/media/com.whatsapp/WhatsApp/Media/photo.jpg",
            FileCategory.IMAGES
        )
        val cache = DisposableCandidate(
            uri = "content://test/cache",
            name = "cache.tmp",
            relativePath = "cache/cache.tmp",
            sizeBytes = 30,
            category = DisposableCategory.APP_CACHE,
            isDirectory = false
        )
        val emptyFolder = DisposableCandidate(
            uri = "content://test/empty",
            name = "empty",
            relativePath = "empty",
            sizeBytes = 0,
            category = DisposableCategory.EMPTY_FOLDERS,
            isDirectory = true
        )
        val overview = StorageScanResult(
            totalBytes = 1_000,
            usedBytes = 500,
            freeBytes = 500,
            categoryBytes = emptyMap(),
            files = listOf(visible, hidden, social),
            disposableCandidates = listOf(cache, emptyFolder),
            scannedFileCount = 3,
            selectedRootName = "Test"
        ).toResultsOverview()

        assertEquals(120L * 1024 * 1024 + 60, overview.scannedBytes)
        assertEquals(30, overview.cacheBytes)
        assertEquals(1, overview.largeFileCount)
        assertEquals(20, overview.hiddenBytes)
        assertEquals(1, overview.emptyFolderCount)
        assertEquals(listOf(social), overview.socialFiles)
        assertEquals(40, overview.socialBytes)
    }

    @Test
    fun `nothing found requires no removable or reviewable files`() {
        val ordinary = result(files = listOf(file("note.txt", 20, 0)))
        assertEquals(false, ordinary.hasReviewableItems())

        val hidden = result(
            files = listOf(file(".hidden", 20, 0, "Pictures/.hidden"))
        )
        assertEquals(true, hidden.hasReviewableItems())

        val large = result(
            files = listOf(file("large.bin", 100L * 1024 * 1024, 0))
        )
        assertEquals(true, large.hasReviewableItems())
    }

    private fun result(files: List<ScannedFile>) = StorageScanResult(
        totalBytes = 1_000,
        usedBytes = 500,
        freeBytes = 500,
        categoryBytes = emptyMap(),
        files = files,
        disposableCandidates = emptyList(),
        scannedFileCount = files.size,
        selectedRootName = "Test"
    )

    private fun file(
        name: String,
        size: Long,
        modified: Long,
        path: String = name,
        category: FileCategory = FileCategory.OTHER
    ) = ScannedFile(
        name = name,
        sizeBytes = size,
        lastModifiedMillis = modified,
        uri = "content://test/$name",
        category = category,
        relativePath = path
    )

    private fun List<ScannedFile>.names() = map(ScannedFile::name)
}
