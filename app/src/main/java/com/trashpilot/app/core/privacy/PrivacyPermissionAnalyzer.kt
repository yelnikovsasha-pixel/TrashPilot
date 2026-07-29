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
        PrivacyPermissionCategory.CALENDAR to setOf(
            Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR
        ),
        PrivacyPermissionCategory.SMS to setOf(
            Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS, Manifest.permission.RECEIVE_MMS,
            Manifest.permission.RECEIVE_WAP_PUSH
        ),
        PrivacyPermissionCategory.PHONE to setOf(
            Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_PHONE_NUMBERS,
            Manifest.permission.CALL_PHONE, Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.ADD_VOICEMAIL, Manifest.permission.USE_SIP
        ),
        PrivacyPermissionCategory.NEARBY_DEVICES to setOf(
            Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.NEARBY_WIFI_DEVICES
        ),
        PrivacyPermissionCategory.NOTIFICATIONS to setOf(Manifest.permission.POST_NOTIFICATIONS),
        PrivacyPermissionCategory.BACKGROUND_LOCATION to
            setOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    )

    fun analyze(installedApps: List<RawInstalledApp>): PrivacySnapshot {
        val apps = installedApps.map { raw ->
            PrivacyApp(
                label = raw.label,
                packageName = raw.packageName,
                declaredCategories = matchingCategories(raw.requestedPermissions),
                grantedCategories = matchingCategories(raw.grantedPermissions)
            )
        }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
        return PrivacySnapshot(installedApps.size, apps)
    }

    private fun matchingCategories(permissions: Set<String>) =
        permissionMap.filterValues { values -> values.any(permissions::contains) }.keys
}

