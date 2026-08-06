package com.trashpilot.app.core.privacy

enum class PrivacyPermissionCategory {
    CAMERA, MICROPHONE, LOCATION, CONTACTS, PHOTOS_STORAGE, NOTIFICATIONS,
    ACCESSIBILITY, BACKGROUND_ACTIVITY
}

enum class PrivacyPermissionStatus { NOT_GRANTED, GRANTED, SENSITIVE }

data class RawInstalledApp(
    val label: String,
    val packageName: String,
    val requestedPermissions: Set<String>,
    val grantedPermissions: Set<String>
)

data class PrivacyApp(
    val label: String,
    val packageName: String,
    val declaredCategories: Set<PrivacyPermissionCategory>,
    val grantedCategories: Set<PrivacyPermissionCategory>,
    val sensitiveCategories: Set<PrivacyPermissionCategory>
) {
    fun status(category: PrivacyPermissionCategory): PrivacyPermissionStatus = when {
        category in sensitiveCategories -> PrivacyPermissionStatus.SENSITIVE
        category in grantedCategories -> PrivacyPermissionStatus.GRANTED
        else -> PrivacyPermissionStatus.NOT_GRANTED
    }
}

data class PrivacySnapshot(
    val appsChecked: Int,
    val apps: List<PrivacyApp>
) {
    val categoryCounts: Map<PrivacyPermissionCategory, Int> =
        PrivacyPermissionCategory.entries.associateWith { category ->
            apps.count { category in it.declaredCategories }
        }
    val sensitiveAppCount: Int = apps.count { it.declaredCategories.isNotEmpty() }
}
