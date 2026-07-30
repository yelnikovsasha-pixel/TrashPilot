package com.trashpilot.app.core.storage

data class SocialMediaGroup(
    val applicationName: String,
    val files: List<ScannedFile>
) {
    val totalBytes: Long = files.sumOf(ScannedFile::sizeBytes)
}

object SocialMediaAnalyzer {
    private val supportedApplications = listOf(
        SupportedApplication("WhatsApp", "whatsapp", "com.whatsapp"),
        SupportedApplication("Telegram", "telegram", "org.telegram.messenger"),
        SupportedApplication("Messenger", "messenger", "com.facebook.orca"),
        SupportedApplication("Signal", "signal", "org.thoughtcrime.securesms"),
        SupportedApplication("Viber", "viber", "com.viber.voip"),
        SupportedApplication("Discord", "discord", "com.discord"),
        SupportedApplication("Instagram", "instagram", "com.instagram.android"),
        SupportedApplication("Facebook", "facebook", "com.facebook.katana"),
        SupportedApplication("Snapchat", "snapchat", "com.snapchat.android"),
        SupportedApplication("LINE", "line", "jp.naver.line.android"),
        SupportedApplication("WeChat", "wechat", "com.tencent.mm"),
        SupportedApplication("TikTok", "tiktok", "com.zhiliaoapp.musically"),
        SupportedApplication("X / Twitter", "twitter", "com.twitter.android")
    )

    fun groups(files: List<ScannedFile>): List<SocialMediaGroup> {
        val grouped = linkedMapOf<SupportedApplication, MutableList<ScannedFile>>()
        files.forEach { file ->
            identifyApplication(file)?.let { application ->
                grouped.getOrPut(application, ::mutableListOf) += file
            }
        }
        return grouped.map { (application, matchingFiles) ->
            SocialMediaGroup(
                applicationName = application.displayName,
                files = matchingFiles
            )
        }
    }

    fun filesInSupportedFolders(files: List<ScannedFile>): List<ScannedFile> =
        groups(files).flatMap(SocialMediaGroup::files)

    fun isSupportedSocialMedia(file: ScannedFile): Boolean =
        identifyApplication(file) != null

    private fun identifyApplication(file: ScannedFile): SupportedApplication? {
        if (file.category !in ACCESSIBLE_MEDIA_CATEGORIES) return null
        val directorySegments = file.relativePath
            .replace('\\', '/')
            .split('/')
            .dropLast(1)
            .map { it.lowercase() }
        return supportedApplications.firstOrNull { application ->
            directorySegments.any(application::matches)
        }
    }

    private class SupportedApplication(
        val displayName: String,
        vararg val markers: String
    ) {
        fun matches(segment: String): Boolean = markers.any { marker ->
            segment == marker || segment.startsWith("$marker ")
        }
    }

    private val ACCESSIBLE_MEDIA_CATEGORIES = setOf(
        FileCategory.IMAGES,
        FileCategory.VIDEOS,
        FileCategory.AUDIO,
        FileCategory.DOWNLOADS
    )
}
