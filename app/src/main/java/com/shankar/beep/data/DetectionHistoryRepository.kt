package com.shankar.beep.data

import com.shankar.beep.model.SoundEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DetectionHistoryRepository {

    private val maxEvents = 50
    private val _events = MutableStateFlow<List<SoundEvent>>(emptyList())
    val events: StateFlow<List<SoundEvent>> = _events.asStateFlow()

    private val _lastAlert = MutableStateFlow<SoundEvent?>(null)
    val lastAlert: StateFlow<SoundEvent?> = _lastAlert.asStateFlow()

    fun addEvent(event: SoundEvent) {
        val currentList = _events.value.toMutableList()
        currentList.add(0, event)
        if (currentList.size > maxEvents) {
            currentList.removeAt(currentList.lastIndex)
        }
        _events.value = currentList
        _lastAlert.value = event
    }

    fun clearAlert() {
        _lastAlert.value = null
    }

    fun clearHistory() {
        _events.value = emptyList()
        _lastAlert.value = null
    }

    companion object {
        val instance: DetectionHistoryRepository by lazy { DetectionHistoryRepository() }
    }
}
