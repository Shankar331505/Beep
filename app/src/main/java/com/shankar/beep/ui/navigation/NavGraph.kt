package com.shankar.beep.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.shankar.beep.data.UserPreferencesRepository
import com.shankar.beep.ui.dashboard.DashboardScreen
import com.shankar.beep.ui.dashboard.DashboardViewModel
import com.shankar.beep.ui.detail.SoundDetailScreen
import com.shankar.beep.ui.onboarding.OnboardingScreen
import com.shankar.beep.ui.settings.SettingsScreen

@Composable
fun BeepNavGraph(
    navController: NavHostController,
    isOnboardingCompleted: Boolean,
    userPreferencesRepository: UserPreferencesRepository,
    dashboardViewModel: DashboardViewModel = viewModel()
) {
    val startDestination = if (isOnboardingCompleted) {
        Screen.Dashboard.route
    } else {
        Screen.Onboarding.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onOnboardingFinished = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToSoundDetail = { soundId ->
                    navController.navigate(Screen.SoundDetail.createRoute(soundId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                viewModel = dashboardViewModel
            )
        }

        composable(
            route = Screen.SoundDetail.route,
            arguments = listOf(navArgument("soundId") { type = NavType.StringType })
        ) { backStackEntry ->
            val soundId = backStackEntry.arguments?.getString("soundId") ?: ""
            SoundDetailScreen(
                soundId = soundId,
                onBack = { navController.popBackStack() },
                viewModel = dashboardViewModel
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                viewModel = dashboardViewModel,
                userPreferencesRepository = userPreferencesRepository
            )
        }
    }
}
