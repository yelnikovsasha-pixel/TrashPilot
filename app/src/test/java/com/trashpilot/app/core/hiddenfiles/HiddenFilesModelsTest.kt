package com.trashpilot.app.core.hiddenfiles

import com.trashpilot.app.core.largefiles.LargeFileSort
import com.trashpilot.app.core.storage.FileCategory
import com.trashpilot.app.core.storage.ScannedFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HiddenFilesModelsTest {
    @Test fun `dot file and file inside dot folder are hidden`() {
        assertTrue(file("Root/Pictures/.photo.jpg").isHiddenUserFile())
        assertTrue(file("Root/Pictures/.private/photo.jpg").isHiddenUserFile())
        assertFalse(file("Root/Pictures/photo.jpg").isHiddenUserFile())
    }

    @Test fun `protected Android and OS locations are excluded`() {
        assertTrue(isProtectedStoragePath(listOf("Root", "Android", "data")))
        assertTrue(isProtectedStoragePath(listOf("Root", "Android", "obb", "app")))
        assertTrue(isProtectedStoragePath(listOf("Root", "system")))
        assertFalse(isProtectedStoragePath(listOf("Root", "Pictures", ".private")))
        assertFalse(file("Root/Android/data/.secret").isHiddenUserFile())
        assertTrue(isProtectedTreeDocumentId("primary:Android/data"))
        assertTrue(isProtectedTreeDocumentId("primary:system"))
        assertFalse(isProtectedTreeDocumentId("primary:Pictures/.private"))
    }

    @Test fun `view searches filters and sorts real metadata`() {
        val older = file("Root/.hidden/alpha.jpg", size = 10, modified = 10).toHiddenFileItem()
        val newer = file("Root/.hidden/beta.jpg", size = 20, modified = 20).toHiddenFileItem()
        val result = listOf(older, newer).hiddenFilesView(null, ".jpg", LargeFileSort.NEWEST)
        assertEquals(listOf(newer.file.uri, older.file.uri), result.map { it.file.uri })
    }

    @Test fun `nomedia makes files in its user folder hidden`() {
        val marker = file("Root/Movies/private/.nomedia")
        val media = file("Root/Movies/private/movie.jpg")
        assertEquals(listOf(marker, media), hiddenUserFiles(listOf(marker, media)))
    }

    private fun file(path: String, size: Long = 1, modified: Long = 0): ScannedFile {
        val name = path.substringAfterLast('/')
        return ScannedFile(name, size, modified, "content://test/$name/$size", FileCategory.IMAGES, path)
    }
}
