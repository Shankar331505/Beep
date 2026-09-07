package com.shankar.beep.model

data class MonitoredSound(
    val id: String,
    val displayName: String,
    val description: String,
    val category: SoundCategory,
    val yamnetLabels: List<String>,
    val defaultConfidenceThreshold: Float = 0.65f,
    val confidenceThreshold: Float = 0.65f,
    val cooldownSeconds: Int = 4,
    val isEnabled: Boolean = true,
    val alertToneRes: Int? = null,
    val iconName: String = "volume_up"
)
