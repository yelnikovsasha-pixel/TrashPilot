package com.trashpilot.app.features.results

import com.trashpilot.app.core.storage.FileCategory
import com.trashpilot.app.core.storage.ScannedFile
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

    private fun file(name: String, size: Long, modified: Long) = ScannedFile(
        name = name,
        sizeBytes = size,
        lastModifiedMillis = modified,
        uri = "content://test/$name",
        category = FileCategory.OTHER
    )

    private fun List<ScannedFile>.names() = map(ScannedFile::name)
}
