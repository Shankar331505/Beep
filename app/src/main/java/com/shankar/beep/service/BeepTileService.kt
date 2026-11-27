package com.shankar.beep.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.shankar.beep.MainActivity
import com.shankar.beep.container
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Quick Settings tile: turn listening on or off from the notification shade. */
class BeepTileService : TileService() {

    private var watch: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        watch = CoroutineScope(Dispatchers.Main).launch {
            container.hub.live.collect { refresh(it.status) }
        }
    }

    override fun onStopListening() {
        watch?.cancel()
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val status = container.hub.live.value.status
        if (status is ListenStatus.Listening || status is ListenStatus.Starting || status is ListenStatus.Paused) {
            ListeningService.stop(this)
            return
        }
        // Android only lets a microphone service start while the app is visible,
        // so open Beep and let it start listening.
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(MainActivity.EXTRA_START_LISTENING, true)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 7, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            )
        } else {
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }

    private fun refresh(status: ListenStatus) {
        val tile = qsTile ?: return
        tile.state = when (status) {
            is ListenStatus.Listening, is ListenStatus.Starting -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when (status) {
                is ListenStatus.Listening -> "Listening"
                is ListenStatus.Starting -> "Starting…"
                is ListenStatus.Paused -> "Paused"
                is ListenStatus.Error -> "Tap to open"
                ListenStatus.Idle -> "Off"
            }
        }
        tile.updateTile()
    }
}
