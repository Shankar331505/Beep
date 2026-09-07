package com.shankar.beep.ui.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Dashboard : Screen("dashboard")
    data object Settings : Screen("settings")
    data object SoundDetail : Screen("sound_detail/{soundId}") {
        fun createRoute(soundId: String) = "sound_detail/$soundId"
    }
}
