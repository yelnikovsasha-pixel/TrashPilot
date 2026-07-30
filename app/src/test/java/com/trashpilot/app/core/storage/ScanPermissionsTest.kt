package com.trashpilot.app.core.storage

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Test

class ScanPermissionsTest {
    @Test
    fun `uses scoped media permissions on Android 13 and newer`() {
        assertEquals(
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            ),
            requiredScanPermissions(33)
        )
    }

    @Test
    fun `includes selected visual media access on Android 14 and newer`() {
        assertEquals(
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            ),
            requiredScanPermissions(34)
        )
    }

    @Test
    fun `uses legacy read permission before Android 13`() {
        assertEquals(
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            requiredScanPermissions(32)
        )
    }
}
