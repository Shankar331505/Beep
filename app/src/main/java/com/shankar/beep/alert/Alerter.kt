package com.shankar.beep.alert

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.shankar.beep.BeepApp
import com.shankar.beep.data.Settings
import com.shankar.beep.detect.Detection
import com.shankar.beep.service.ListeningHub
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Turns a detection into something the user notices, through every channel
 * they enabled: paused media, a chime in their headphones, a vibration
 * pattern, a flashing torch, and a notification or full-screen alert.
 */
class Alerter(context: Context, private val hub: ListeningHub) {

    private val notifier = Notifier(context)
    private val haptics = Haptics(context)
    private val torch = Torch(context)
    private val media = MediaPauser(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var holdJob: Job? = null
    private var clearJob: Job? = null

    val hasFlash: Boolean get() = torch.available

    fun fire(d: Detection, settings: Settings) {
        hub.activeAlert.value = d
        val category = d.spec.category
        val paused = settings.pauseMedia && media.pause()

        scope.launch {
            if (paused) delay(250) // let the player fade out before the chime
            if (settings.vibrate) haptics.play(category)
            if (settings.chime) Chime.play(category)
        }
        if (settings.flash) scope.launch { torch.strobe(category) }

        // When Beep is open the in-app alert takes over the screen; otherwise notify (full screen if locked).
        if (!hub.appVisible.value) notifier.showAlert(d, fullScreen = settings.fullScreen)

        holdJob?.cancel()
        if (paused) {
            holdJob = scope.launch {
                delay(if (settings.holdUntilDismissed) MAX_HOLD_MS else Chime.durationMs(category) + RESUME_AFTER_MS)
                media.resume()
            }
        }
        clearJob?.cancel()
        clearJob = scope.launch {
            delay(BANNER_MS)
            if (hub.activeAlert.value == d) hub.activeAlert.value = null
        }
    }

    /** The user acknowledged the alert: resume media and clear it everywhere. */
    fun dismiss() {
        holdJob?.cancel()
        media.resume()
        hub.activeAlert.value = null
        notifier.cancelAlert()
    }

    companion object {
        private const val RESUME_AFTER_MS = 4_000L
        private const val MAX_HOLD_MS = 120_000L
        private const val BANNER_MS = 60_000L
    }
}

/** Handles the "Got it" and "Mute 10 min" buttons on alert notifications. */
class AlertActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as BeepApp
        when (intent.action) {
            ACTION_DISMISS -> app.container.alerter.dismiss()
            ACTION_SNOOZE -> {
                intent.getStringExtra(EXTRA_SOUND)?.let { app.container.engine.snooze(it, 10) }
                app.container.alerter.dismiss()
            }
        }
    }

    companion object {
        const val ACTION_DISMISS = "com.shankar.beep.DISMISS_ALERT"
        const val ACTION_SNOOZE = "com.shankar.beep.SNOOZE_SOUND"
        const val EXTRA_SOUND = "sound"

        fun pending(context: Context, action: String, soundId: String): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                action.hashCode(),
                Intent(context, AlertActionReceiver::class.java).setAction(action).putExtra(EXTRA_SOUND, soundId),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
    }
}
