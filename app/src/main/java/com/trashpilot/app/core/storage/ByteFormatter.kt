package com.trashpilot.app.core.storage

import java.util.Locale

fun formatBytes(bytes: Long): String {
    if (bytes < 1_000L) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIndex = -1
    while (value >= 1_000 && unitIndex < units.lastIndex) {
        value /= 1_000
        unitIndex += 1
    }
    return String.format(Locale.getDefault(), "%.1f %s", value, units[unitIndex])
}
