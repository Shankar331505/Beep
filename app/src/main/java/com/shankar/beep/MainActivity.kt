package com.shankar.beep

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.shankar.beep.data.UserPreferencesRepository
import com.shankar.beep.service.AudioMonitoringService
import com.shankar.beep.ui.navigation.BeepNavGraph
import com.shankar.beep.ui.theme.BeepTheme
import com.shankar.beep.ui.theme.DarkBackground

class MainActivity : ComponentActivity() {

    private lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        userPreferencesRepository = UserPreferencesRepository(applicationContext)

        setContent {
            val userSettings by userPreferencesRepository.userSettingsFlow.collectAsState(initial = null)
            val navController = rememberNavController()
            val context = LocalContext.current

            // Restore background listening after the process was killed
            LaunchedEffect(userSettings?.isMonitoringActive) {
                val settings = userSettings ?: return@LaunchedEffect
                val hasMic = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (settings.isMonitoringActive &&
                    !AudioMonitoringService.isServiceRunning.value &&
                    hasMic
                ) {
                    AudioMonitoringService.start(context)
                }
            }

            BeepTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    if (userSettings != null) {
                        BeepNavGraph(
                            navController = navController,
                            isOnboardingCompleted = userSettings!!.isOnboardingCompleted,
                            userPreferencesRepository = userPreferencesRepository
                        )
                    }
                }
            }
        }
    }
}
