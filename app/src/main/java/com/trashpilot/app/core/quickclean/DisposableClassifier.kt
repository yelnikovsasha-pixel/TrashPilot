package com.trashpilot.app.core.quickclean

import com.trashpilot.app.core.storage.FileCategory

object DisposableClassifier {
    private val protectedCategories = setOf(
        FileCategory.IMAGES,
        FileCategory.VIDEOS,
        FileCategory.AUDIO,
        FileCategory.DOCUMENTS,
        FileCategory.DOWNLOADS
    )
    private val temporaryExtensions = setOf("tmp", "temp", "part")
    private val apkLeftoverMarkers = listOf("old", "backup", "update", "copy")

    fun classify(
        fileName: String,
        pathSegments: List<String>,
        fileCategory: FileCategory
    ): DisposableCategory? {
        if (fileCategory in protectedCategories) return null
        val lowerName = fileName.lowercase()
        val extension = lowerName.substringAfterLast('.', "")
        val parentSegments = pathSegments.dropLast(1).map(String::lowercase)

        return when {
            extension == "log" -> DisposableCategory.LOG_FILES
            extension in temporaryExtensions -> DisposableCategory.TEMPORARY_FILES
            parentSegments.any { it == "cache" || it == "caches" } ->
                DisposableCategory.APP_CACHE
            extension == "apk" && apkLeftoverMarkers.any(lowerName::contains) ->
                DisposableCategory.APK_LEFTOVERS
            else -> null
        }
    }
}
