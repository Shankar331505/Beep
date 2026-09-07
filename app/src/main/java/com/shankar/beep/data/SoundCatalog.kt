package com.shankar.beep.data

import com.shankar.beep.R
import com.shankar.beep.model.MonitoredSound
import com.shankar.beep.model.SoundCategory

data class ListeningPreset(
    val key: String,
    val label: String,
    val description: String
)

object SoundCatalog {

    const val SOUND_ID_NAME = "user_name"
    const val SOUND_ID_SUDDEN_LOUD = "sudden_loud_impact"
    const val SOUND_ID_GLASS = "glass_break"
    const val SOUND_ID_KNOCK = "door_knock"
    const val SOUND_ID_DOORBELL = "doorbell"
    const val SOUND_ID_FIRE_ALARM = "fire_smoke_alarm"
    const val SOUND_ID_SIREN = "emergency_siren"
    const val SOUND_ID_SCREAM = "scream"
    const val SOUND_ID_BABY_CRY = "baby_cry"
    const val SOUND_ID_DOG_BARK = "dog_bark"
    const val SOUND_ID_CLAP = "clapping"
    const val SOUND_ID_WHISTLE = "whistle"

    val DEFAULT_CATALOG = listOf(
        // Social & Attention
        MonitoredSound(
            id = SOUND_ID_NAME,
            displayName = "Your Name Called",
            description = "Listens for someone saying your name or wake-words",
            category = SoundCategory.SOCIAL,
            yamnetLabels = emptyList(), // handled by NameDetectorEngine
            defaultConfidenceThreshold = 0.70f,
            cooldownSeconds = 5,
            isEnabled = true,
            alertToneRes = R.raw.alert_name,
            iconName = "person"
        ),
        MonitoredSound(
            id = SOUND_ID_CLAP,
            displayName = "Clapping / Applause",
            description = "Hand clapping or physical applause cues",
            category = SoundCategory.SOCIAL,
            yamnetLabels = listOf("Clapping", "Applause", "Hands"),
            defaultConfidenceThreshold = 0.60f,
            cooldownSeconds = 3,
            isEnabled = true,
            alertToneRes = R.raw.alert_chime,
            iconName = "pan_tool"
        ),
        MonitoredSound(
            id = SOUND_ID_WHISTLE,
            displayName = "Whistling",
            description = "Human whistle or attention whistle",
            category = SoundCategory.SOCIAL,
            yamnetLabels = listOf("Whistling", "Whistle"),
            defaultConfidenceThreshold = 0.65f,
            cooldownSeconds = 4,
            isEnabled = false,
            alertToneRes = R.raw.alert_chime,
            iconName = "music_note"
        ),

        // Emergency & Danger
        MonitoredSound(
            id = SOUND_ID_FIRE_ALARM,
            displayName = "Smoke & Fire Alarm",
            description = "Continuous emergency beeps and high-pitch smoke alarms",
            category = SoundCategory.EMERGENCY,
            yamnetLabels = listOf("Fire alarm", "Smoke detector, smoke alarm", "Alarm", "Buzzer"),
            defaultConfidenceThreshold = 0.60f,
            cooldownSeconds = 5,
            isEnabled = true,
            alertToneRes = R.raw.alert_emergency,
            iconName = "warning"
        ),
        MonitoredSound(
            id = SOUND_ID_SIREN,
            displayName = "Emergency Siren",
            description = "Ambulance, police, or fire truck sirens",
            category = SoundCategory.EMERGENCY,
            yamnetLabels = listOf("Siren", "Emergency vehicle", "Police car (siren)", "Ambulance (siren)"),
            defaultConfidenceThreshold = 0.65f,
            cooldownSeconds = 5,
            isEnabled = true,
            alertToneRes = R.raw.alert_emergency,
            iconName = "local_police"
        ),
        MonitoredSound(
            id = SOUND_ID_SCREAM,
            displayName = "Scream / Shout",
            description = "Distress scream or loud human shout",
            category = SoundCategory.EMERGENCY,
            yamnetLabels = listOf("Screaming", "Shout", "Yell"),
            defaultConfidenceThreshold = 0.70f,
            cooldownSeconds = 4,
            isEnabled = true,
            alertToneRes = R.raw.alert_emergency,
            iconName = "record_voice_over"
        ),
        MonitoredSound(
            id = SOUND_ID_SUDDEN_LOUD,
            displayName = "Sudden Loud Impact",
            description = "Sudden high-energy audio anomaly (>85 dB)",
            category = SoundCategory.EMERGENCY,
            yamnetLabels = listOf("Explosion", "Bang", "Gunshot, gunfire", "Boom"),
            defaultConfidenceThreshold = 0.60f,
            cooldownSeconds = 4,
            isEnabled = true,
            alertToneRes = R.raw.alert_emergency,
            iconName = "bolt"
        ),

        // Home & Domestic
        MonitoredSound(
            id = SOUND_ID_KNOCK,
            displayName = "Door Knock",
            description = "Physical knocking on a door or surface",
            category = SoundCategory.DOMESTIC,
            yamnetLabels = listOf("Knock", "Door", "Tap", "Slap, smack"),
            defaultConfidenceThreshold = 0.60f,
            cooldownSeconds = 3,
            isEnabled = true,
            alertToneRes = R.raw.alert_chime,
            iconName = "sensor_door"
        ),
        MonitoredSound(
            id = SOUND_ID_DOORBELL,
            displayName = "Doorbell / Chime",
            description = "Electronic doorbell chime or ding-dong",
            category = SoundCategory.DOMESTIC,
            yamnetLabels = listOf("Doorbell", "Ding-dong", "Chime", "Bell"),
            defaultConfidenceThreshold = 0.65f,
            cooldownSeconds = 4,
            isEnabled = true,
            alertToneRes = R.raw.alert_chime,
            iconName = "notifications_active"
        ),
        MonitoredSound(
            id = SOUND_ID_GLASS,
            displayName = "Glass Breaking",
            description = "Shattering glass cups, windows, or dishes",
            category = SoundCategory.DOMESTIC,
            yamnetLabels = listOf("Glass", "Shatter", "Crack", "Breaking"),
            defaultConfidenceThreshold = 0.60f,
            cooldownSeconds = 4,
            isEnabled = true,
            alertToneRes = R.raw.alert_emergency,
            iconName = "broken_image"
        ),
        MonitoredSound(
            id = SOUND_ID_BABY_CRY,
            displayName = "Baby Crying",
            description = "Infant crying, babbling, or distress",
            category = SoundCategory.DOMESTIC,
            yamnetLabels = listOf("Baby cry, infant cry", "Crying, sobbing", "Wail, moan"),
            defaultConfidenceThreshold = 0.65f,
            cooldownSeconds = 6,
            isEnabled = false,
            alertToneRes = R.raw.alert_chime,
            iconName = "child_care"
        ),
        MonitoredSound(
            id = SOUND_ID_DOG_BARK,
            displayName = "Dog Barking",
            description = "Domestic canine barking or howling",
            category = SoundCategory.DOMESTIC,
            yamnetLabels = listOf("Dog", "Bark", "Bow-wow", "Howl"),
            defaultConfidenceThreshold = 0.65f,
            cooldownSeconds = 5,
            isEnabled = false,
            alertToneRes = R.raw.alert_chime,
            iconName = "pets"
        )
    )

    fun getSoundById(id: String): MonitoredSound? {
        return DEFAULT_CATALOG.find { it.id == id }
    }

    // Listening presets: quick one-tap sound bundles for common scenarios
    val LISTENING_PRESETS = listOf(
        ListeningPreset(
            key = "Focus / Study",
            label = "Focus",
            description = "Name, knock & fire alarm only"
        ),
        ListeningPreset(
            key = "Home / Relax",
            label = "Home",
            description = "Domestic + emergency sounds"
        ),
        ListeningPreset(
            key = "Outdoor / Commute",
            label = "Outdoor",
            description = "Sirens, loud impacts & shouts"
        ),
        ListeningPreset(
            key = "Balanced",
            label = "Balanced",
            description = "A well-rounded everyday mix"
        )
    )

    fun presetLabel(key: String): String {
        return LISTENING_PRESETS.find { it.key == key }?.label ?: key
    }
}
