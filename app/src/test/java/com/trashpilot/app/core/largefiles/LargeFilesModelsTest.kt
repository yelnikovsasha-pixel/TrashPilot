package com.trashpilot.app.core.largefiles

import com.trashpilot.app.core.storage.FileCategory
import com.trashpilot.app.core.storage.ScannedFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LargeFilesModelsTest {
    @Test fun `thresholds use binary storage units`() {
        assertEquals(50L * 1024 * 1024, LargeFileThreshold.MB_50.bytes)
        assertEquals(1024L * 1024 * 1024, LargeFileThreshold.GB_1.bytes)
    }

    @Test fun `archives are detected before document category`() {
        assertEquals(LargeFileType.ARCHIVES, file("backup.zip", 60, FileCategory.DOCUMENTS).toLargeFileItem().type)
        assertEquals(LargeFileType.APK, file("release.apk", 60, FileCategory.OTHER).toLargeFileItem().type)
    }

    @Test fun `view filters real size type and filename then sorts`() {
        val small = file("small.jpg", 49, FileCategory.IMAGES)
        val first = file("Holiday.jpg", 70, FileCategory.IMAGES, modified = 10)
        val second = file("holiday-2.jpg", 90, FileCategory.IMAGES, modified = 20)
        val result = listOf(small, first, second).map(ScannedFile::toLargeFileItem).largeFilesView(
            threshold = LargeFileThreshold.MB_50,
            filter = LargeFileType.IMAGES,
            search = "holiday",
            sort = LargeFileSort.NEWEST
        )
        assertEquals(listOf(second.uri, first.uri), result.map { it.file.uri })
    }

    @Test fun `folder name never exposes full path`() {
        val item = file("movie.mp4", 70, FileCategory.VIDEOS, path = "Movies/Trips/movie.mp4").toLargeFileItem()
        assertEquals("Trips", item.folderName)
        assertTrue('/' !in item.folderName)
    }

    private fun file(
        name: String,
        megabytes: Long,
        category: FileCategory,
        modified: Long = 0,
        path: String = "Downloads/$name"
    ) = ScannedFile(name, megabytes * 1024 * 1024, modified, "content://test/$name", category, path)
}
