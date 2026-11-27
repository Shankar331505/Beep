package com.shankar.beep

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shankar.beep.AlertActivity.Companion.toInfo
import com.shankar.beep.detect.Mode
import com.shankar.beep.service.ListenStatus
import com.shankar.beep.service.ListeningService
import com.shankar.beep.ui.MainViewModel
import com.shankar.beep.ui.alert.AlertContent
import com.shankar.beep.ui.beepViewModel
import com.shankar.beep.ui.home.HomeScreen
import com.shankar.beep.ui.onboarding.OnboardingScreen
import com.shankar.beep.ui.record.NameScreen
import com.shankar.beep.ui.record.TeachScreen
import com.shankar.beep.ui.settings.SettingsScreen
import com.shankar.beep.ui.sounds.SoundsScreen
import com.shankar.beep.ui.timeline.TimelineScreen
import com.shankar.beep.ui.theme.Beep
import com.shankar.beep.ui.theme.BeepTheme

class MainActivity : ComponentActivity() {

    private var startAfterPermission = false

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result[Manifest.permission.RECORD_AUDIO] == true && startAfterPermission) {
            ListeningService.start(this)
        } else if (result[Manifest.permission.RECORD_AUDIO] == false) {
            Toast.makeText(this, "Beep needs the microphone to listen.", Toast.LENGTH_LONG).show()
        }
        startAfterPermission = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleStartExtra(intent)
        setContent {
            val vm = beepViewModel { MainViewModel(it) }
            val settings by vm.settings.collectAsStateWithLifecycle()
            val s = settings
            BeepTheme(s?.theme ?: com.shankar.beep.data.ThemeChoice.SYSTEM) {
                val alertShowing by vm.activeAlert.collectAsStateWithLifecycle()
                val lightBars = !Beep.palette.isDark && alertShowing == null
                val view = LocalView.current
                SideEffect {
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = lightBars
                        isAppearanceLightNavigationBars = lightBars
                    }
                }
                Box(Modifier.fillMaxSize().background(Beep.palette.canvas)) {
                    when {
                        s == null -> Unit // settings loading: plain canvas for a frame
                        !s.onboarded -> OnboardingScreen(vm) {
                            vm.completeOnboarding(s.mode ?: Mode.EVERYDAY)
                            startListening()
                        }
                        else -> BeepMain(vm, ::toggleListening)
                    }
                    AlertOverlay(vm)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleStartExtra(intent)
    }

    override fun onStart() {
        super.onStart()
        container.hub.appVisible.value = true
    }

    override fun onStop() {
        container.hub.appVisible.value = false
        super.onStop()
    }

    private fun handleStartExtra(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_START_LISTENING, false) == true) {
            intent.removeExtra(EXTRA_START_LISTENING)
            startListening()
        }
    }

    private fun toggleListening() {
        when (container.hub.live.value.status) {
            ListenStatus.Listening, ListenStatus.Starting, is ListenStatus.Paused -> ListeningService.stop(this)
            else -> startListening()
        }
    }

    private fun startListening() {
        val needed = buildList {
            if (!granted(Manifest.permission.RECORD_AUDIO)) add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= 33 && !granted(Manifest.permission.POST_NOTIFICATIONS)) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (granted(Manifest.permission.RECORD_AUDIO)) {
            ListeningService.start(this)
            if (needed.isNotEmpty()) permissionLauncher.launch(needed.toTypedArray())
        } else {
            startAfterPermission = true
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    companion object {
        const val EXTRA_START_LISTENING = "start_listening"
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("listen", "Listen", Icons.Rounded.Hearing),
    Tab("sounds", "Sounds", Icons.Rounded.GraphicEq),
    Tab("timeline", "Timeline", Icons.Rounded.History),
    Tab("settings", "Settings", Icons.Rounded.Settings),
)

@Composable
private fun BeepMain(vm: MainViewModel, onToggleListening: () -> Unit) {
    val p = Beep.palette
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    fun go(to: String) = nav.navigate(to) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    Scaffold(
        containerColor = p.canvas,
        bottomBar = {
            if (tabs.any { it.route == route }) {
                NavigationBar(containerColor = p.canvas, tonalElevation = androidx.compose.ui.unit.Dp(0f)) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { go(tab.route) },
                            icon = { Icon(tab.icon, null) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = p.onSignal,
                                selectedTextColor = p.text,
                                indicatorColor = p.signal,
                                unselectedIconColor = p.muted,
                                unselectedTextColor = p.muted,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "listen", modifier = Modifier.padding(padding)) {
            composable("listen") {
                HomeScreen(vm, onToggleListening, onOpenSounds = { go("sounds") }, onOpenTimeline = { go("timeline") })
            }
            composable("sounds") {
                SoundsScreen(vm, onOpenName = { nav.navigate("name") }, onTeach = { nav.navigate("teach") })
            }
            composable("timeline") { TimelineScreen(vm) }
            composable("settings") { SettingsScreen(vm, onOpenName = { nav.navigate("name") }) }
            composable("name") { NameScreen(onBack = { nav.popBackStack() }) }
            composable("teach") { TeachScreen(onBack = { nav.popBackStack() }) }
        }
    }
}

/** When the app is open, alerts take over the screen in-app instead of a notification. */
@Composable
private fun AlertOverlay(vm: MainViewModel) {
    val alert by vm.activeAlert.collectAsStateWithLifecycle()
    AnimatedVisibility(alert != null, enter = fadeIn() + scaleIn(initialScale = 0.96f), exit = fadeOut()) {
        val d = alert ?: return@AnimatedVisibility
        AlertContent(d.toInfo(), onDismiss = vm::dismissAlert, onSnooze = { vm.snooze(d.spec.id) })
    }
}
