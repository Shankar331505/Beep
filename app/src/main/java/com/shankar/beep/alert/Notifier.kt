package com.shankar.beep.alert

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.shankar.beep.AlertActivity
import com.shankar.beep.MainActivity
import com.shankar.beep.R
import com.shankar.beep.detect.Category
import com.shankar.beep.detect.Detection
import com.shankar.beep.service.ListeningService
import com.shankar.beep.ui.theme.categoryArgb
import kotlin.math.roundToInt

class Notifier(private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)

    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_LISTENING, "Listening status", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shows that Beep is listening, with quick pause and stop buttons"
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_SAFETY, "Safety alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Alarms, sirens, breaking glass and other urgent sounds"
                setSound(null, null) // Beep plays its own tones
                enableVibration(false) // and its own vibration patterns
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_ALERTS, "Sound alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Your name, doorbells, knocks and the other sounds you watch for"
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
    }

    fun listening(modeTitle: String, paused: Boolean, resumeAt: String? = null): Notification {
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(context, CH_LISTENING)
            .setSmallIcon(R.drawable.ic_stat_beep)
            .setColor(0xFFFF7A45.toInt())
            .setContentTitle(if (paused) "Beep is paused" else "Beep is listening")
            .setContentText(
                if (paused) "Resumes at $resumeAt" else "$modeTitle mode · audio stays on your phone"
            )
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        if (paused) {
            builder.addAction(0, "Resume now", ListeningService.pendingAction(context, ListeningService.ACTION_RESUME))
        } else {
            builder.addAction(0, "Pause 15 min", ListeningService.pendingAction(context, ListeningService.ACTION_PAUSE))
        }
        builder.addAction(0, "Stop", ListeningService.pendingAction(context, ListeningService.ACTION_STOP))
        return builder.build()
    }

    fun showAlert(d: Detection, fullScreen: Boolean) {
        val spec = d.spec
        val alertIntent = AlertActivity.intent(context, d)
        val contentIntent = PendingIntent.getActivity(
            context, 1, alertIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val detail = "Heard nearby · ${(d.confidence * 100).roundToInt()}% sure · ${d.levelDb.roundToInt()} dB"
        val builder = NotificationCompat.Builder(context, if (spec.category == Category.SAFETY) CH_SAFETY else CH_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_beep)
            .setColor(categoryArgb(spec.category))
            .setColorized(true)
            .setContentTitle("Look around · ${spec.title}")
            .setContentText(detail)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$detail\n${spec.description}"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(if (spec.category == Category.SAFETY) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setTimeoutAfter(10 * 60_000L)
            .setContentIntent(contentIntent)
            .setDeleteIntent(AlertActionReceiver.pending(context, AlertActionReceiver.ACTION_DISMISS, spec.id))
            .addAction(0, "Got it", AlertActionReceiver.pending(context, AlertActionReceiver.ACTION_DISMISS, spec.id))
            .addAction(0, "Mute 10 min", AlertActionReceiver.pending(context, AlertActionReceiver.ACTION_SNOOZE, spec.id))
        if (fullScreen && canUseFullScreen()) builder.setFullScreenIntent(contentIntent, true)
        if (manager.areNotificationsEnabled()) {
            try {
                manager.notify(ALERT_ID, builder.build())
            } catch (_: SecurityException) {
                // Notification permission revoked; in-app and haptic alerts still work.
            }
        }
    }

    fun cancelAlert() = manager.cancel(ALERT_ID)

    private fun canUseFullScreen(): Boolean =
        Build.VERSION.SDK_INT < 34 || context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    companion object {
        const val CH_LISTENING = "listening"
        const val CH_SAFETY = "alerts_safety"
        const val CH_ALERTS = "alerts"
        const val LISTENING_ID = 1
        const val ALERT_ID = 2
    }
}
