package com.trashpilot.app.core.quickclean

import com.trashpilot.app.core.storage.FileCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DisposableClassifierTest {
    @Test
    fun `classifies only conservative disposable patterns`() {
        assertEquals(
            DisposableCategory.TEMPORARY_FILES,
            DisposableClassifier.classify("render.tmp", listOf("root", "render.tmp"), FileCategory.OTHER)
        )
        assertEquals(
            DisposableCategory.APP_CACHE,
            DisposableClassifier.classify("blob.bin", listOf("root", "cache", "blob.bin"), FileCategory.OTHER)
        )
        assertEquals(
            DisposableCategory.APK_LEFTOVERS,
            DisposableClassifier.classify("update-old.apk", listOf("root", "update-old.apk"), FileCategory.APK_FILES)
        )
        assertEquals(
            DisposableCategory.LOG_FILES,
            DisposableClassifier.classify("scanner.log", listOf("root", "scanner.log"), FileCategory.OTHER)
        )
    }

    @Test
    fun `never offers personal media or documents`() {
        listOf(
            FileCategory.IMAGES,
            FileCategory.VIDEOS,
            FileCategory.AUDIO,
            FileCategory.DOCUMENTS,
            FileCategory.DOWNLOADS
        ).forEach { category ->
            assertNull(
                DisposableClassifier.classify(
                    "personal.tmp",
                    listOf("root", "cache", "personal.tmp"),
                    category
                )
            )
        }
    }

    @Test
    fun `does not classify normal apks or unknown files`() {
        assertNull(
            DisposableClassifier.classify(
                "application.apk",
                listOf("root", "application.apk"),
                FileCategory.APK_FILES
            )
        )
        assertNull(
            DisposableClassifier.classify(
                "notes.bin",
                listOf("root", "notes.bin"),
                FileCategory.OTHER
            )
        )
    }
}
