package com.trashpilot.app.core.privacy

import android.Manifest

object PrivacyPermissionAnalyzer {
    private val permissionMap = mapOf(
        PrivacyPermissionCategory.CAMERA to setOf(Manifest.permission.CAMERA),
        PrivacyPermissionCategory.MICROPHONE to setOf(Manifest.permission.RECORD_AUDIO),
        PrivacyPermissionCategory.LOCATION to setOf(
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION
        ),
        PrivacyPermissionCategory.CONTACTS to setOf(
            Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.GET_ACCOUNTS
        ),
        PrivacyPermissionCategory.PHOTOS_STORAGE to setOf(
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        ),
        PrivacyPermissionCategory.NOTIFICATIONS to setOf(Manifest.permission.POST_NOTIFICATIONS),
        PrivacyPermissionCategory.ACCESSIBILITY to setOf(Manifest.permission.BIND_ACCESSIBILITY_SERVICE),
        PrivacyPermissionCategory.BACKGROUND_ACTIVITY to setOf(
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.RECEIVE_BOOT_COMPLETED,
            Manifest.permission.WAKE_LOCK
        )
    )

    private val sensitiveCategories = setOf(
        PrivacyPermissionCategory.ACCESSIBILITY,
        PrivacyPermissionCategory.BACKGROUND_ACTIVITY
    )

    fun analyze(installedApps: List<RawInstalledApp>): PrivacySnapshot {
        val apps = installedApps.map { raw ->
            PrivacyApp(
                label = raw.label,
                packageName = raw.packageName,
                declaredCategories = matchingCategories(raw.requestedPermissions),
                grantedCategories = matchingCategories(raw.grantedPermissions) - sensitiveCategories,
                sensitiveCategories = matchingCategories(raw.requestedPermissions) intersect sensitiveCategories
            )
        }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
        return PrivacySnapshot(installedApps.size, apps)
    }

    private fun matchingCategories(permissions: Set<String>) =
        permissionMap.filterValues { values -> values.any(permissions::contains) }.keys
}
