package com.trashpilot.app.core.apkmanager

import android.graphics.Bitmap
import com.trashpilot.app.core.storage.ScannedFile

data class ApkMetadata(
    val packageName: String,
    val versionName: String?,
    val appLabel: String?,
    val icon: Bitmap?
)

data class ApkFileItem(
    val file: ScannedFile,
    val metadata: ApkMetadata?
) {
    val metadataVerified: Boolean = metadata != null
    val parentFolder: String = file.relativePath.replace('\\', '/').split('/')
        .filter(String::isNotBlank).dropLast(1).lastOrNull().orEmpty()
}

enum class ApkSort { LARGEST, SMALLEST, NEWEST, OLDEST, NAME }
enum class ApkFilter { ALL, VALID, UNREADABLE }

fun ScannedFile.isApkInstaller(): Boolean = name.endsWith(".apk", ignoreCase = true)

fun List<ApkFileItem>.apkView(query: String, sort: ApkSort, filter: ApkFilter): List<ApkFileItem> {
    val eligible = asSequence()
        .filter {
            query.isBlank() || it.file.name.contains(query, ignoreCase = true) ||
                it.metadata?.appLabel?.contains(query, ignoreCase = true) == true
        }
        .filter {
            when (filter) {
                ApkFilter.ALL -> true
                ApkFilter.VALID -> it.metadataVerified
                ApkFilter.UNREADABLE -> !it.metadataVerified
            }
        }
    val comparator = when (sort) {
        ApkSort.LARGEST -> compareByDescending<ApkFileItem> { it.file.sizeBytes }
        ApkSort.SMALLEST -> compareBy { it.file.sizeBytes }
        ApkSort.NEWEST -> compareByDescending { it.file.lastModifiedMillis }
        ApkSort.OLDEST -> compareBy { it.file.lastModifiedMillis.takeIf { value -> value > 0 } ?: Long.MAX_VALUE }
        ApkSort.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { item: ApkFileItem -> item.file.name }
    }
    return eligible.sortedWith(comparator.thenBy { it.file.uri }).toList()
}
