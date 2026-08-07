package com.trashpilot.app.core.settings

import org.junit.Assert.*
import org.junit.Test

class SettingsModelsTest {
    @Test fun themeAndLanguageStoredNamesRestoreDeterministically() {
        assertEquals(LanguagePreference.POLISH, LanguagePreference.fromStoredName("POLISH"))
        assertEquals(LanguagePreference.SYSTEM, LanguagePreference.fromStoredName("missing"))
        assertEquals(ThemePreference.DARK, ThemePreference.fromStoredName("DARK"))
        assertEquals(ThemePreference.SYSTEM, ThemePreference.fromStoredName("missing"))
    }

    @Test fun photoPermissionMappingDistinguishesLimitedAccess() {
        assertEquals(PermissionAccessState.GRANTED, mediaAccessState(true, true))
        assertEquals(PermissionAccessState.LIMITED, mediaAccessState(false, true))
        assertEquals(PermissionAccessState.NOT_GRANTED, mediaAccessState(false, false))
    }

    @Test fun usageAndSafStatesRemainTruthful() {
        assertEquals(PermissionAccessState.UNSUPPORTED, usageAccessState(false, false))
        assertEquals(PermissionAccessState.GRANTED, usageAccessState(true, true))
        assertEquals(PermissionAccessState.MANAGED_BY_ANDROID, safAccessState(0))
        assertEquals(PermissionAccessState.GRANTED, safAccessState(2))
    }

    @Test fun unavailableSettingsIntentIsReported() {
        assertFalse(canOpenExternalSettings(false))
        assertTrue(canOpenExternalSettings(true))
    }

    @Test fun everyDestructiveLocalActionRequiresConfirmation() {
        assertTrue(SettingsResetAction.entries.all(::resetActionRequiresConfirmation))
    }

    @Test fun settingsNavigationIsDeterministicIncludingIntroduction() {
        SettingsDestination.entries.forEach { assertEquals(it, destinationForSettingsRow(it)) }
    }
}
