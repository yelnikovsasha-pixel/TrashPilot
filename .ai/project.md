# project
- Single-module Android app (com.trashpilot.app) using Kotlin, Jetpack Compose, Material 3, and Navigation Compose; min SDK 24, compile/target SDK 36. MainActivity applies TrashPilotTheme and AppNavigation currently displays SplashScreen.
- Navigation uses Navigation Compose with splash as the start destination, followed by Home, Scanner, Results, Privacy, Reports, and Settings destinations.
- Home is a Material 3 screen with a TrashPilot/Smart Cleaner header, one 168 dp circular Scan action, rounded Storage, Trash DNA, Privacy Monitor, and Reports cards, offline/privacy assurance, and bottom navigation.
- Compose Material Icons Extended supplies monochrome icons for Home cards and navigation.
- `core/storage` owns read-only document-tree scanning, file categorization, byte formatting, and immutable scan models. Scanner and Results UI live in focused feature packages.
- AndroidX Core 1.18.0, Lifecycle 2.10.0, and Activity Compose 1.12.2 are pinned to remain compatible with the installed compile SDK 36.1.
