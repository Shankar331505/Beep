package com.shankar.beep.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.shankar.beep.alert.Notifier
import com.shankar.beep.audio.MicUnavailableException
import com.shankar.beep.container
import com.shankar.beep.detect.Frame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * Foreground service (type "microphone") that keeps Beep listening with the
 * screen off or the app closed. The persistent notification is required by
 * Android and makes it obvious to the user that the mic is in use.
 */
class ListeningService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var captureJob: Job? = null
    private var resumeJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val notifier by lazy { Notifier(this) }
    private var lastUiUpdate = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val c = container
        scope.launch { c.engineConfig.collect { c.engine.config = it } }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action ?: ACTION_START) {
            ACTION_START, ACTION_RESUME -> start()
            ACTION_PAUSE -> pause(PAUSE_MINUTES)
            ACTION_STOP -> stopListening()
        }
        return START_STICKY
    }

    private fun start() {
        if (!goForeground(paused = false)) return
        resumeJob?.cancel()
        if (captureJob?.isActive == true) return
        acquireWakeLock()
        val c = container
        c.hub.live.update { it.copy(status = ListenStatus.Starting, startedAt = System.currentTimeMillis()) }
        captureJob = scope.launch {
            var backoff = 1_000L
            while (isActive) {
                try {
                    c.engine.reset()
                    c.mic.chunks().collect { chunk ->
                        backoff = 1_000L
                        handle(c.engine.process(chunk))
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: MicUnavailableException) {
                    Log.w(TAG, "Mic unavailable: ${e.message}")
                    c.hub.live.update { it.copy(status = ListenStatus.Error("Microphone busy — retrying…")) }
                    if (!c.mic.hasPermission()) {
                        c.hub.live.update { it.copy(status = ListenStatus.Error("Microphone permission was removed")) }
                        stopListening()
                        return@launch
                    }
                    delay(backoff)
                    backoff = (backoff * 2).coerceAtMost(30_000L)
                } catch (e: Exception) {
                    Log.e(TAG, "Listening failed", e)
                    c.hub.live.update { it.copy(status = ListenStatus.Error("Something went wrong — retrying…")) }
                    delay(backoff)
                    backoff = (backoff * 2).coerceAtMost(30_000L)
                }
            }
        }
    }

    private fun handle(frame: Frame) {
        val c = container
        c.hub.publishBands(frame.bands)
        val now = System.currentTimeMillis()
        if (frame.hearing != null || now - lastUiUpdate > 100) {
            lastUiUpdate = now
            c.hub.live.update { s ->
                s.copy(
                    status = ListenStatus.Listening,
                    levelDb = frame.levelDb,
                    baselineDb = c.engine.roomBaselineDb,
                    hearing = frame.hearing ?: s.hearing,
                    scores = frame.scores ?: s.scores,
                    resting = if (frame.hearing != null) frame.classifierSkipped else s.resting,
                )
            }
        }
        frame.detections.firstOrNull()?.let { detection ->
            c.eventLog.record(detection)
            c.settings.value?.let { c.alerter.fire(detection, it) }
        }
    }

    private fun pause(minutes: Int) {
        captureJob?.cancel()
        captureJob = null
        releaseWakeLock()
        val until = System.currentTimeMillis() + minutes * 60_000L
        container.hub.live.update { LiveState(status = ListenStatus.Paused(until)) }
        goForeground(paused = true, resumeAt = until)
        resumeJob?.cancel()
        resumeJob = scope.launch {
            delay(minutes * 60_000L)
            start()
        }
    }

    private fun stopListening() {
        resumeJob?.cancel()
        captureJob?.cancel()
        captureJob = null
        releaseWakeLock()
        container.hub.live.value = LiveState(status = ListenStatus.Idle)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun goForeground(paused: Boolean, resumeAt: Long = 0L): Boolean {
        val mode = container.settings.value?.mode?.title ?: "Custom"
        val time = if (paused) DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(resumeAt)) else null
        val notification = notifier.listening(mode, paused, time)
        return try {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
            ServiceCompat.startForeground(this, Notifier.LISTENING_ID, notification, type)
            true
        } catch (e: Exception) {
            // Android refuses to start a microphone service from the background.
            Log.w(TAG, "Could not start in foreground", e)
            container.hub.live.value = LiveState(status = ListenStatus.Error("Open Beep to start listening"))
            stopSelf()
            false
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Beep:listening").apply {
            setReferenceCounted(false)
            acquire(12 * 60 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    override fun onDestroy() {
        captureJob?.cancel()
        releaseWakeLock()
        scope.cancel()
        if (container.hub.live.value.status !is ListenStatus.Error) {
            container.hub.live.value = LiveState(status = ListenStatus.Idle)
        }
        NotificationManagerCompat.from(this).cancel(Notifier.LISTENING_ID)
        super.onDestroy()
    }

    companion object {
        private const val TAG = "BeepService"
        const val ACTION_START = "com.shankar.beep.START"
        const val ACTION_STOP = "com.shankar.beep.STOP"
        const val ACTION_PAUSE = "com.shankar.beep.PAUSE"
        const val ACTION_RESUME = "com.shankar.beep.RESUME"
        const val PAUSE_MINUTES = 15

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, ListeningService::class.java).setAction(ACTION_START))
        }

        fun stop(context: Context) {
            context.startService(Intent(context, ListeningService::class.java).setAction(ACTION_STOP))
        }

        fun pendingAction(context: Context, action: String): PendingIntent =
            PendingIntent.getService(
                context, action.hashCode(),
                Intent(context, ListeningService::class.java).setAction(action),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
    }
}
