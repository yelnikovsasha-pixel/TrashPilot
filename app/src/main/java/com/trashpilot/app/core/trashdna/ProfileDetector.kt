package com.trashpilot.app.core.trashdna

object ProfileDetector {
    fun detect(first: TrashDnaSessionEntity, latest: TrashDnaSessionEntity): TrashDnaProfile {
        val mediaGrowth = (latest.imageBytes + latest.videoBytes + latest.audioBytes) -
            (first.imageBytes + first.videoBytes + first.audioBytes)
        val specialized = listOf(
            TrashDnaProfile.MESSENGER_HEAVY to latest.messengerMediaBytes - first.messengerMediaBytes,
            TrashDnaProfile.DOWNLOAD_KEEPER to latest.downloadBytes - first.downloadBytes,
            TrashDnaProfile.SCREENSHOT_COLLECTOR to latest.screenshotBytes - first.screenshotBytes,
            TrashDnaProfile.LARGE_FILE_KEEPER to latest.largeFileBytes - first.largeFileBytes
        ).sortedByDescending { it.second }
        val top = specialized.first()
        val runnerUp = specialized.getOrNull(1)?.second ?: 0
        val specializedDominates = top.second > 0 &&
            (runnerUp <= 0 || top.second >= runnerUp + runnerUp / 4) &&
            (mediaGrowth <= 0 || top.second >= mediaGrowth / 2)
        if (specializedDominates) return top.first
        val strongestOther = specialized.maxOfOrNull { it.second } ?: 0
        return if (mediaGrowth > 0 &&
            (strongestOther <= 0 || mediaGrowth >= strongestOther + strongestOther / 4)
        ) TrashDnaProfile.MEDIA_COLLECTOR else TrashDnaProfile.BALANCED
    }
}
