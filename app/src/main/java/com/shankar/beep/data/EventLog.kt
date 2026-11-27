package com.shankar.beep.data

import android.content.Context
import com.shankar.beep.detect.Category
import com.shankar.beep.detect.Detection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** One alert in the timeline. Only the label is kept — never the audio. */
data class LoggedEvent(
    val timestamp: Long,
    val soundId: String,
    val title: String,
    val category: Category,
    val icon: String,
    val confidence: Float,
    val levelDb: Float,
)

/** A small on-device journal of what Beep heard, stored as JSON in app-private storage. */
class EventLog(context: Context) {

    private val file = File(context.filesDir, "events.json")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val _events = MutableStateFlow<List<LoggedEvent>>(emptyList())
    val events: StateFlow<List<LoggedEvent>> = _events.asStateFlow()

    init {
        scope.launch {
            mutex.withLock {
                val stored = read()
                _events.update { fresh -> (fresh + stored).take(MAX_EVENTS) }
            }
        }
    }

    fun record(detection: Detection) {
        val event = LoggedEvent(
            timestamp = detection.timestamp,
            soundId = detection.spec.id,
            title = detection.spec.title,
            category = detection.spec.category,
            icon = detection.spec.icon,
            confidence = detection.confidence,
            levelDb = detection.levelDb,
        )
        _events.update { (listOf(event) + it).take(MAX_EVENTS) }
        persist()
    }

    fun clear() {
        _events.value = emptyList()
        persist()
    }

    private fun persist() {
        val snapshot = _events.value
        scope.launch {
            mutex.withLock {
                val array = JSONArray()
                snapshot.forEach { e ->
                    array.put(
                        JSONObject()
                            .put("t", e.timestamp)
                            .put("id", e.soundId)
                            .put("title", e.title)
                            .put("cat", e.category.name)
                            .put("icon", e.icon)
                            .put("conf", e.confidence.toDouble())
                            .put("db", e.levelDb.toDouble())
                    )
                }
                val tmp = File(file.parentFile, file.name + ".tmp")
                tmp.writeText(array.toString())
                tmp.renameTo(file)
            }
        }
    }

    private fun read(): List<LoggedEvent> = runCatching {
        if (!file.exists()) return emptyList()
        val array = JSONArray(file.readText())
        List(array.length()) { i ->
            val o = array.getJSONObject(i)
            LoggedEvent(
                timestamp = o.getLong("t"),
                soundId = o.getString("id"),
                title = o.getString("title"),
                category = runCatching { Category.valueOf(o.getString("cat")) }.getOrDefault(Category.CUSTOM),
                icon = o.optString("icon", "custom"),
                confidence = o.optDouble("conf", 0.0).toFloat(),
                levelDb = o.optDouble("db", 0.0).toFloat(),
            )
        }
    }.getOrDefault(emptyList())

    companion object {
        const val MAX_EVENTS = 1000
    }
}
