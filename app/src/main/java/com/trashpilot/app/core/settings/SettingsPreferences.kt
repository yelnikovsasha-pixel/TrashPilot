package com.trashpilot.app.core.settings

import android.content.Context
import android.content.res.Resources
import android.os.LocaleList
import java.util.Locale

enum class ThemePreference { SYSTEM, LIGHT, DARK;
    companion object {
        fun fromStoredName(value: String?): ThemePreference = entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

enum class LanguagePreference(val tag: String, val label: String) {
    SYSTEM("", "System language"),
    ENGLISH("en", "English"),
    SPANISH("es", "Español"),
    PORTUGUESE_BRAZIL("pt-BR", "Português (Brasil)"),
    FRENCH("fr", "Français"),
    GERMAN("de", "Deutsch"),
    ITALIAN("it", "Italiano"),
    POLISH("pl", "Polski"),
    UKRAINIAN("uk", "Українська"),
    RUSSIAN("ru", "Русский"),
    TURKISH("tr", "Türkçe"),
    ARABIC("ar", "العربية"),
    HINDI("hi", "हिन्दी"),
    BENGALI("bn", "বাংলা"),
    INDONESIAN("id", "Bahasa Indonesia"),
    VIETNAMESE("vi", "Tiếng Việt"),
    THAI("th", "ไทย"),
    JAPANESE("ja", "日本語"),
    KOREAN("ko", "한국어"),
    CHINESE_SIMPLIFIED("zh-CN", "简体中文"),
    CHINESE_TRADITIONAL("zh-TW", "繁體中文"),
    DUTCH("nl", "Nederlands"),
    SWEDISH("sv", "Svenska"),
    CZECH("cs", "Čeština"),
    ROMANIAN("ro", "Română"),
    GREEK("el", "Ελληνικά");

    companion object {
        fun fromStoredName(value: String?): LanguagePreference =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

class SettingsPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    var theme: ThemePreference
        get() = ThemePreference.fromStoredName(preferences.getString(KEY_THEME, null))
        set(value) { preferences.edit().putString(KEY_THEME, value.name).apply() }

    fun exportValues(language: LanguagePreference): Map<String, String> = mapOf(
        KEY_THEME to theme.name,
        KEY_LANGUAGE to language.name
    )

    fun restoreValues(values: Map<String, String>): LanguagePreference {
        theme = ThemePreference.fromStoredName(values[KEY_THEME])
        return LanguagePreference.fromStoredName(values[KEY_LANGUAGE])
    }

    private companion object {
        const val FILE_NAME = "trashpilot-settings"
        const val KEY_THEME = "theme"
        const val KEY_LANGUAGE = "language"
    }
}

@Suppress("DEPRECATION")
fun applyLanguagePreference(context: Context, preference: LanguagePreference) {
    val locale = if (preference == LanguagePreference.SYSTEM) {
        Resources.getSystem().configuration.locales[0]
    } else {
        Locale.forLanguageTag(preference.tag)
    }
    Locale.setDefault(locale)
    val configuration = context.resources.configuration
    configuration.setLocales(LocaleList(locale))
    context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
}
