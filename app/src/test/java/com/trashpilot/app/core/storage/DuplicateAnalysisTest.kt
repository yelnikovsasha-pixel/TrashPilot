package com.trashpilot.app.core.storage

import org.junit.Assert.assertEquals
import org.junit.Test

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

        val analysis = DuplicateAnalysis(listOf(duplicate))

        assertEquals(1, analysis.duplicateFileCount)
        assertEquals(42, analysis.duplicateBytes)
    }
}
