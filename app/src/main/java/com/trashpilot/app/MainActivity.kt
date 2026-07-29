package com.trashpilot.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.trashpilot.app.core.navigation.AppNavigation
import com.trashpilot.app.core.settings.SettingsPreferences
import com.trashpilot.app.core.settings.ThemePreference
import com.trashpilot.app.core.settings.LanguagePreferences
import com.trashpilot.app.core.settings.applyLanguagePreference
import com.trashpilot.app.ui.theme.TrashPilotTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val language = runBlocking { LanguagePreferences(this@MainActivity).selectedLanguage.first() }
        applyLanguagePreference(this, language)
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            val theme = SettingsPreferences(this).theme
            TrashPilotTheme(
                darkTheme = when (theme) {
                    ThemePreference.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                    ThemePreference.LIGHT -> false
                    ThemePreference.DARK -> true
                }
            ) {
                AppNavigation()
            }
        }
    }
}
