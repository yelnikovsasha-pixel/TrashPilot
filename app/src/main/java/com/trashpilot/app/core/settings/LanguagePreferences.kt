package com.trashpilot.app.core.settings

import android.content.Context
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.languageDataStore by preferencesDataStore(
    name = "language",
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(
                context = context,
                sharedPreferencesName = "trashpilot-settings",
                keysToMigrate = setOf("language")
            )
        )
    }
)

class LanguagePreferences(private val context: Context) {
    val selectedLanguage: Flow<LanguagePreference> =
        context.languageDataStore.data.map { values ->
            LanguagePreference.fromStoredName(values[LANGUAGE_KEY])
        }

    suspend fun setSelectedLanguage(language: LanguagePreference) {
        context.languageDataStore.edit { values ->
            values[LANGUAGE_KEY] = language.name
        }
    }

    private companion object {
        val LANGUAGE_KEY = stringPreferencesKey("language")
    }
}
