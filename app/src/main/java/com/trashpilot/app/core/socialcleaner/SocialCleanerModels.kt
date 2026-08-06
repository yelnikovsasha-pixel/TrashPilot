package com.trashpilot.app.core.socialcleaner

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import com.trashpilot.app.core.storage.FileCategory
import com.trashpilot.app.core.storage.ScannedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SupportedSocialApp(
    val name: String,
    val packageName: String,
    val pathMarkers: Set<String>
)

data class InstalledSocialApp(val definition: SupportedSocialApp)

enum class SocialMediaType { IMAGES, VIDEOS, AUDIO, DOCUMENTS, DOWNLOADS, VOICE_NOTES, GIFS, STICKERS }
enum class SocialMediaSort { LARGEST, NEWEST, OLDEST, FILE_TYPE, APPLICATION }

data class SocialMediaItem(
    val file: ScannedFile,
    val app: InstalledSocialApp,
    val type: SocialMediaType
)

data class SocialAppSummary(
    val app: InstalledSocialApp,
    val items: List<SocialMediaItem>
) {
    val totalBytes = items.sumOf { it.file.sizeBytes }
    fun bytes(type: SocialMediaType) = items.filter { it.type == type }.sumOf { it.file.sizeBytes }
}

class InstalledSocialAppsRepository(private val context: Context) {
    suspend fun installed(): List<InstalledSocialApp> = withContext(Dispatchers.IO) {
        SUPPORTED_SOCIAL_APPS.mapNotNull { definition ->
            val info = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getApplicationInfo(
                        definition.packageName,
                        android.content.pm.PackageManager.ApplicationInfoFlags.of(0)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getApplicationInfo(definition.packageName, 0)
                }
            }.getOrNull()
            info?.takeIf { it.flags and ApplicationInfo.FLAG_INSTALLED != 0 }
                ?.let { InstalledSocialApp(definition) }
        }
    }
}

fun socialMediaItem(file: ScannedFile, installedApps: List<InstalledSocialApp>): SocialMediaItem? {
    if (file.category !in SOCIAL_FILE_CATEGORIES) return null
    val ownerMatch = file.ownerPackageName?.let { owner ->
        installedApps.firstOrNull { it.definition.packageName == owner }
    }
    if (file.ownerPackageName != null && ownerMatch == null) return null
    val normalizedPath = file.relativePath.replace('\\', '/').lowercase()
    val segments = normalizedPath.split('/').filter(String::isNotBlank)
    val app = ownerMatch ?: installedApps.firstOrNull { installed ->
        installed.definition.pathMarkers.any { marker ->
            val normalizedMarker = marker.lowercase()
            segments.any { it == normalizedMarker || it.startsWith("$normalizedMarker ") }
        }
    } ?: return null
    val extension = file.name.substringAfterLast('.', "").lowercase()
    val type = when {
        segments.any { "sticker" in it } -> SocialMediaType.STICKERS
        extension == "gif" -> SocialMediaType.GIFS
        segments.any { it == "voice" || "voice note" in it || "voicenote" in it } -> SocialMediaType.VOICE_NOTES
        file.category == FileCategory.DOWNLOADS || segments.any { it == "download" || it == "downloads" } -> SocialMediaType.DOWNLOADS
        file.category == FileCategory.IMAGES -> SocialMediaType.IMAGES
        file.category == FileCategory.VIDEOS -> SocialMediaType.VIDEOS
        file.category == FileCategory.AUDIO -> SocialMediaType.AUDIO
        else -> SocialMediaType.DOCUMENTS
    }
    return SocialMediaItem(file, app, type)
}

fun socialAppSummaries(apps: List<InstalledSocialApp>, items: List<SocialMediaItem>): List<SocialAppSummary> =
    apps.map { app -> SocialAppSummary(app, items.filter { it.app.definition.packageName == app.definition.packageName }) }

fun List<SocialMediaItem>.socialMediaView(
    query: String,
    type: SocialMediaType?,
    appPackage: String?,
    sort: SocialMediaSort
): List<SocialMediaItem> {
    val eligible = asSequence()
        .filter { query.isBlank() || it.file.name.contains(query, ignoreCase = true) }
        .filter { type == null || it.type == type }
        .filter { appPackage == null || it.app.definition.packageName == appPackage }
    val comparator = when (sort) {
        SocialMediaSort.LARGEST -> compareByDescending<SocialMediaItem> { it.file.sizeBytes }
        SocialMediaSort.NEWEST -> compareByDescending { it.file.lastModifiedMillis }
        SocialMediaSort.OLDEST -> compareBy { it.file.lastModifiedMillis.takeIf { value -> value > 0 } ?: Long.MAX_VALUE }
        SocialMediaSort.FILE_TYPE -> compareBy<SocialMediaItem> { it.type.name }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.file.name }
        SocialMediaSort.APPLICATION -> compareBy(String.CASE_INSENSITIVE_ORDER) { item: SocialMediaItem -> item.app.definition.name }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.file.name }
    }
    return eligible.sortedWith(comparator.thenBy { it.file.uri }).toList()
}

val SUPPORTED_SOCIAL_APPS = listOf(
    SupportedSocialApp("WhatsApp", "com.whatsapp", setOf("WhatsApp", "com.whatsapp")),
    SupportedSocialApp("Telegram", "org.telegram.messenger", setOf("Telegram", "org.telegram.messenger")),
    SupportedSocialApp("Messenger", "com.facebook.orca", setOf("Messenger", "com.facebook.orca")),
    SupportedSocialApp("Instagram", "com.instagram.android", setOf("Instagram", "com.instagram.android")),
    SupportedSocialApp("Facebook", "com.facebook.katana", setOf("Facebook", "com.facebook.katana")),
    SupportedSocialApp("TikTok", "com.zhiliaoapp.musically", setOf("TikTok", "com.zhiliaoapp.musically")),
    SupportedSocialApp("X", "com.twitter.android", setOf("Twitter", "com.twitter.android")),
    SupportedSocialApp("Discord", "com.discord", setOf("Discord", "com.discord")),
    SupportedSocialApp("Signal", "org.thoughtcrime.securesms", setOf("Signal", "org.thoughtcrime.securesms")),
    SupportedSocialApp("Viber", "com.viber.voip", setOf("Viber", "com.viber.voip")),
    SupportedSocialApp("LINE", "jp.naver.line.android", setOf("LINE", "jp.naver.line.android")),
    SupportedSocialApp("WeChat", "com.tencent.mm", setOf("WeChat", "com.tencent.mm")),
    SupportedSocialApp("Snapchat", "com.snapchat.android", setOf("Snapchat", "com.snapchat.android"))
)

private val SOCIAL_FILE_CATEGORIES = setOf(
    FileCategory.IMAGES, FileCategory.VIDEOS, FileCategory.AUDIO,
    FileCategory.DOCUMENTS, FileCategory.DOWNLOADS
)
