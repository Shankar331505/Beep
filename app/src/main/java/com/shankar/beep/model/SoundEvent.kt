package com.shankar.beep.model

import java.util.UUID

data class SoundEvent(
    val id: String = UUID.randomUUID().toString(),
    val soundId: String,
    val soundName: String,
    val category: SoundCategory,
    val confidence: Float,
    val decibel: Float,
    val timestamp: Long = System.currentTimeMillis()
)
