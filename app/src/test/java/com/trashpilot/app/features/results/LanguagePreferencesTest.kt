package com.trashpilot.app.features.settings

import com.trashpilot.app.core.settings.LanguagePreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguagePreferencesTest {
    @Test
    fun languageCatalog_hasSystemThenTwentyFiveLanguages() {
        assertEquals(LanguagePreference.SYSTEM, LanguagePreference.entries.first())
        assertEquals(26, LanguagePreference.entries.size)
        assertEquals(25, LanguagePreference.entries.count { it != LanguagePreference.SYSTEM })
    }

    @Test
    fun search_matchesNativeNameAndLocaleTag() {
        assertEquals(
            listOf(LanguagePreference.UKRAINIAN),
            filterLanguages("Україн")
        )
        assertTrue(LanguagePreference.PORTUGUESE_BRAZIL in filterLanguages("pt-BR"))
    }

    @Test
    fun blankSearch_preservesSystemLanguageFirst() {
        assertEquals(LanguagePreference.SYSTEM, filterLanguages(" ").first())
    }
}
