package com.trashpilot.app.core.about

import com.trashpilot.app.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AboutModelsTest {
    @Test fun actualBuildVersionMapsWithoutFabrication() {
        val info = aboutVersionInfo(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toLong())
        assertEquals(BuildConfig.VERSION_NAME, info.versionName)
        assertEquals(BuildConfig.VERSION_CODE.toLong(), info.versionCode)
    }

    @Test fun unavailableVersionNameRemainsUnavailable() {
        val info = aboutVersionInfo("  ", -1)
        assertNull(info.versionName)
        assertEquals(0L, info.versionCode)
    }

    @Test fun missingPrivacyPolicyIsTheOnlyUnavailableLocalDestination() {
        assertEquals(
            AboutDestinationAvailability.UNAVAILABLE,
            aboutDestinationAvailability(AboutDestination.PRIVACY_POLICY, false)
        )
        assertTrue(
            AboutDestination.entries
                .filterNot { it == AboutDestination.PRIVACY_POLICY }
                .all { aboutDestinationAvailability(it, false) == AboutDestinationAvailability.AVAILABLE }
        )
    }

    @Test fun configuredAboutNavigationIsDeterministic() {
        assertTrue(
            AboutDestination.entries.all {
                aboutDestinationAvailability(it, true) == AboutDestinationAvailability.AVAILABLE
            }
        )
    }

    @Test fun settingsAboutAndBackNavigationAreDeterministic() {
        assertEquals(AboutNavigationTarget.ABOUT, aboutNavigationTarget(AboutNavigationAction.OPEN_ABOUT))
        assertEquals(
            AboutNavigationTarget.SETTINGS_OVERVIEW,
            aboutNavigationTarget(AboutNavigationAction.BACK_TO_SETTINGS)
        )
    }

    @Test fun introductionAndPermissionDestinationsReuseExistingFlows() {
        assertEquals(
            AboutNavigationTarget.INTRODUCTION,
            aboutNavigationTarget(AboutNavigationAction.VIEW_INTRODUCTION)
        )
        assertEquals(
            AboutNavigationTarget.SETTINGS_PERMISSIONS,
            aboutNavigationTarget(AboutNavigationAction.OPEN_PERMISSIONS)
        )
    }
}
