package com.trashpilot.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.trashpilot.app.core.navigation.AppNavigation
import com.trashpilot.app.ui.theme.TrashPilotTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            TrashPilotTheme {
                AppNavigation()
            }
        }
    }
}