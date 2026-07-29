package com.trashpilot.app.core.settings

import com.trashpilot.app.core.trashdna.TrashDnaSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SettingsBackupCodecTest {
    @Test
    fun roundTrip_preservesPreferencesAndMetadata() {
        val source = SettingsBackup(
            preferences = mapOf("theme" to "DARK", "language" to "POLISH"),
            sessions = listOf(
                TrashDnaSessionEntity(
                    sessionType = "SCAN",
                    timestampMillis = 42,
                    scannedFolderName = "Folder\tOne",
                    reclaimableBytes = 100,
                    reclaimedBytes = 0,
                    result = "ANALYZED",
                    temporaryBytes = 70,
                    cacheBytes = 30,
                    emptyFolderCount = 0,
                    apkLeftoverBytes = 0,
                    logBytes = 0,
                    scannedFileCount = 4,
                    scanDurationMillis = 50
                )
            )
        )

        val restored = SettingsBackupCodec.decode(SettingsBackupCodec.encode(source))

        assertEquals(source.preferences, restored.preferences)
        assertEquals(source.sessions, restored.sessions)
    }

    @Test
    fun decode_rejectsUnknownFile() {
        assertThrows(IllegalArgumentException::class.java) {
            SettingsBackupCodec.decode("not a backup")
        }
    }
}
