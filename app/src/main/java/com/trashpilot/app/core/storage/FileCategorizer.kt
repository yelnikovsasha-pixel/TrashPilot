package com.trashpilot.app.core.storage

internal object FileCategorizer {
    private val documentExtensions = setOf(
        "csv", "doc", "docx", "epub", "html", "htm", "md", "odt", "pdf",
        "ppt", "pptx", "rtf", "txt", "xls", "xlsx", "xml"
    )

    fun categorize(
        fileName: String,
        mimeType: String?,
        pathSegments: List<String>
    ): FileCategory {
        if (pathSegments.any { it.equals("download", true) || it.equals("downloads", true) }) {
            return FileCategory.DOWNLOADS
        }

        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        return when {
            extension == "apk" ||
                mimeType == "application/vnd.android.package-archive" -> FileCategory.APK_FILES
            mimeType?.startsWith("image/") == true -> FileCategory.IMAGES
            mimeType?.startsWith("video/") == true -> FileCategory.VIDEOS
            mimeType?.startsWith("audio/") == true -> FileCategory.AUDIO
            mimeType?.startsWith("text/") == true ||
                mimeType == "application/pdf" ||
                extension in documentExtensions -> FileCategory.DOCUMENTS
            else -> FileCategory.OTHER
        }
    }
}
