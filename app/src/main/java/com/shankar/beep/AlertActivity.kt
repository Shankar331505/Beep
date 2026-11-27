package com.shankar.beep

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import android.graphics.Color
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.shankar.beep.data.ThemeChoice
import com.shankar.beep.detect.Category
import com.shankar.beep.detect.Detection
import com.shankar.beep.ui.alert.AlertContent
import com.shankar.beep.ui.alert.AlertInfo
import com.shankar.beep.ui.theme.BeepTheme

/** Full-screen alert shown over the lock screen when Beep hears something. */
class AlertActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        showOverLockScreen()
        val info = intent.toInfo() ?: run { finish(); return }
        setContent {
            BeepTheme(ThemeChoice.DARK) {
                AlertContent(
                    info,
                    onDismiss = {
                        container.alerter.dismiss()
                        finish()
                    },
                    onSnooze = {
                        container.engine.snooze(info.soundId, 10)
                        container.alerter.dismiss()
                        finish()
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    companion object {
        private const val ID = "id"; private const val TITLE = "title"; private const val DESC = "desc"
        private const val CAT = "cat"; private const val ICON = "icon"; private const val CONF = "conf"
        private const val DB = "db"; private const val TS = "ts"

        fun intent(context: Context, d: Detection): Intent =
            Intent(context, AlertActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
                .putExtra(ID, d.spec.id)
                .putExtra(TITLE, d.spec.title)
                .putExtra(DESC, d.spec.description)
                .putExtra(CAT, d.spec.category.name)
                .putExtra(ICON, d.spec.icon)
                .putExtra(CONF, d.confidence)
                .putExtra(DB, d.levelDb)
                .putExtra(TS, d.timestamp)

        private fun Intent.toInfo(): AlertInfo? {
            val id = getStringExtra(ID) ?: return null
            return AlertInfo(
                soundId = id,
                title = getStringExtra(TITLE) ?: "Sound",
                description = getStringExtra(DESC) ?: "",
                category = runCatching { Category.valueOf(getStringExtra(CAT)!!) }.getOrDefault(Category.SAFETY),
                icon = getStringExtra(ICON) ?: "loud",
                confidence = getFloatExtra(CONF, 0f),
                levelDb = getFloatExtra(DB, 0f),
                timestamp = getLongExtra(TS, System.currentTimeMillis()),
            )
        }

        fun Detection.toInfo() = AlertInfo(
            spec.id, spec.title, spec.description, spec.category, spec.icon, confidence, levelDb, timestamp,
        )
    }
}
