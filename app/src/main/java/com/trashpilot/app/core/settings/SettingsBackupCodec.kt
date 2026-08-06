package com.trashpilot.app.core.settings

import com.trashpilot.app.core.trashdna.TrashDnaSessionEntity

data class SettingsBackup(
    val preferences: Map<String, String>,
    val sessions: List<TrashDnaSessionEntity>
)

object SettingsBackupCodec {
    private const val HEADER = "TRASHPILOT_BACKUP_V2"
    private const val LEGACY_HEADER = "TRASHPILOT_BACKUP_V1"

    fun encode(backup: SettingsBackup): String = buildString {
        appendLine(HEADER)
        backup.preferences.toSortedMap().forEach { (key, value) ->
            appendLine("P\t${safe(key)}\t${safe(value)}")
        }
        backup.sessions.forEach { session ->
            appendLine(
                listOf(
                    "S", session.sessionType, session.timestampMillis, safe(session.scannedFolderName),
                    session.reclaimableBytes, session.reclaimedBytes, session.result,
                    session.temporaryBytes, session.cacheBytes, session.emptyFolderCount,
                    session.apkLeftoverBytes, session.logBytes, session.scannedFileCount,
                    session.scanDurationMillis, session.privacyAppsChecked,
                    session.privacySensitiveAppCount, session.usedStorageBytes,
                    session.imageBytes, session.videoBytes, session.audioBytes,
                    session.documentBytes, session.downloadBytes, session.messengerMediaBytes,
                    session.screenshotBytes, session.largeFileBytes, session.largeVideoBytes,
                    session.hiddenFileBytes, safe(session.messengerSourceName)
                ).joinToString("\t")
            )
        }
    }

    fun decode(text: String): SettingsBackup {
        val lines = text.lineSequence().filter { it.isNotBlank() }.toList()
        val header = lines.firstOrNull()
        require(header == HEADER || header == LEGACY_HEADER) { "This is not a TrashPilot backup." }
        val preferences = mutableMapOf<String, String>()
        val sessions = mutableListOf<TrashDnaSessionEntity>()
        lines.drop(1).forEach { line ->
            val parts = line.split('\t')
            when (parts.firstOrNull()) {
                "P" -> if (parts.size == 3) preferences[unsafe(parts[1])] = unsafe(parts[2])
                "S" -> {
                    require(parts.size == 16 || parts.size == 28) {
                        "The backup contains an invalid history row."
                    }
                    sessions += TrashDnaSessionEntity(
                        sessionType = parts[1],
                        timestampMillis = parts[2].toLong(),
                        scannedFolderName = unsafe(parts[3]),
                        reclaimableBytes = parts[4].toLong(),
                        reclaimedBytes = parts[5].toLong(),
                        result = parts[6],
                        temporaryBytes = parts[7].toLong(),
                        cacheBytes = parts[8].toLong(),
                        emptyFolderCount = parts[9].toLong(),
                        apkLeftoverBytes = parts[10].toLong(),
                        logBytes = parts[11].toLong(),
                        scannedFileCount = parts[12].toLong(),
                        scanDurationMillis = parts[13].toLong(),
                        privacyAppsChecked = parts[14].toLong(),
                        privacySensitiveAppCount = parts[15].toLong(),
                        usedStorageBytes = parts.getOrNull(16)?.toLong() ?: 0,
                        imageBytes = parts.getOrNull(17)?.toLong() ?: 0,
                        videoBytes = parts.getOrNull(18)?.toLong() ?: 0,
                        audioBytes = parts.getOrNull(19)?.toLong() ?: 0,
                        documentBytes = parts.getOrNull(20)?.toLong() ?: 0,
                        downloadBytes = parts.getOrNull(21)?.toLong() ?: 0,
                        messengerMediaBytes = parts.getOrNull(22)?.toLong() ?: 0,
                        screenshotBytes = parts.getOrNull(23)?.toLong() ?: 0,
                        largeFileBytes = parts.getOrNull(24)?.toLong() ?: 0,
                        largeVideoBytes = parts.getOrNull(25)?.toLong() ?: 0,
                        hiddenFileBytes = parts.getOrNull(26)?.toLong() ?: 0,
                        messengerSourceName = parts.getOrNull(27)?.let(::unsafe).orEmpty()
                    )
                }
            }
        }
        return SettingsBackup(preferences, sessions)
    }

    private fun safe(value: String): String = value
        .replace("%", "%25")
        .replace("\t", "%09")
        .replace("\r", "%0D")
        .replace("\n", "%0A")

    private fun unsafe(value: String): String = value
        .replace("%0A", "\n")
        .replace("%0D", "\r")
        .replace("%09", "\t")
        .replace("%25", "%")
}
