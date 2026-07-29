package com.trashpilot.app.core.privacy

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyPermissionAnalyzerTest {
    @Test
    fun `maps only permissions actually declared by each app`() {
        val snapshot = PrivacyPermissionAnalyzer.analyze(
            listOf(
                RawInstalledApp(
                    label = "Camera app",
                    packageName = "test.camera",
                    requestedPermissions = setOf(Manifest.permission.CAMERA),
                    grantedPermissions = emptySet()
                ),
                RawInstalledApp(
                    label = "No permissions",
                    packageName = "test.empty",
                    requestedPermissions = emptySet(),
                    grantedPermissions = emptySet()
                )
            )
        )

        assertEquals(2, snapshot.appsChecked)
        assertEquals(1, snapshot.sensitiveAppCount)
        assertEquals(1, snapshot.categoryCounts[PrivacyPermissionCategory.CAMERA])
        assertTrue(snapshot.apps.first { it.packageName == "test.empty" }.declaredCategories.isEmpty())
    }

    @Test
    fun `keeps background and foreground location distinct`() {
        val snapshot = PrivacyPermissionAnalyzer.analyze(
            listOf(
                RawInstalledApp(
                    label = "Background app",
                    packageName = "test.background",
                    requestedPermissions = setOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    grantedPermissions = setOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                )
            )
        )
        val app = snapshot.apps.single()

        assertTrue(PrivacyPermissionCategory.BACKGROUND_LOCATION in app.declaredCategories)
        assertFalse(PrivacyPermissionCategory.LOCATION in app.declaredCategories)
        assertTrue(PrivacyPermissionCategory.BACKGROUND_LOCATION in app.grantedCategories)
    }

    @Test
    fun `maps nearby devices and notifications without generating other categories`() {
        val snapshot = PrivacyPermissionAnalyzer.analyze(
            listOf(
                RawInstalledApp(
                    label = "Connected app",
                    packageName = "test.connected",
                    requestedPermissions = setOf(
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.POST_NOTIFICATIONS
                    ),
                    grantedPermissions = setOf(Manifest.permission.BLUETOOTH_CONNECT)
                )
            )
        )
        val app = snapshot.apps.single()

        assertEquals(
            setOf(
                PrivacyPermissionCategory.NEARBY_DEVICES,
                PrivacyPermissionCategory.NOTIFICATIONS
            ),
            app.declaredCategories
        )
        assertEquals(setOf(PrivacyPermissionCategory.NEARBY_DEVICES), app.grantedCategories)
    }
}

