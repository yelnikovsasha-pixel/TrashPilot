package com.trashpilot.app.core.privacy

enum class PrivacyPermissionCategory {
    CAMERA, MICROPHONE, LOCATION, CONTACTS, CALENDAR, SMS, PHONE,
    NEARBY_DEVICES, NOTIFICATIONS, BACKGROUND_LOCATION
}

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
    val grantedCategories: Set<PrivacyPermissionCategory>
)

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

