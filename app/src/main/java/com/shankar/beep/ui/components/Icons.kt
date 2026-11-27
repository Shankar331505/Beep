package com.shankar.beep.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.DoorFront
import androidx.compose.material.icons.rounded.Doorbell
import androidx.compose.material.icons.rounded.Emergency
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.LocalTaxi
import androidx.compose.material.icons.rounded.Microwave
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PedalBike
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Sports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector

/** Maps the icon keys stored with each sound to Material icons. */
object SoundIcons {

    fun of(key: String): ImageVector = when (key) {
        "fire" -> Icons.Rounded.LocalFireDepartment
        "siren" -> Icons.Rounded.Emergency
        "glass" -> Icons.Rounded.BrokenImage
        "crash" -> Icons.Rounded.Bolt
        "scream" -> Icons.Rounded.RecordVoiceOver
        "car_alarm" -> Icons.Rounded.DirectionsCar
        "loud" -> Icons.AutoMirrored.Rounded.VolumeUp
        "name" -> Icons.Rounded.Badge
        "shout" -> Icons.Rounded.Campaign
        "baby" -> Icons.Rounded.ChildCare
        "talking" -> Icons.Rounded.Forum
        "doorbell" -> Icons.Rounded.Doorbell
        "knock" -> Icons.Rounded.DoorFront
        "phone" -> Icons.Rounded.PhoneInTalk
        "timer" -> Icons.Rounded.Timer
        "water" -> Icons.Rounded.WaterDrop
        "dog", "cat", "pet" -> Icons.Rounded.Pets
        "horn" -> Icons.Rounded.LocalTaxi
        "reversing" -> Icons.Rounded.LocalShipping
        "bicycle" -> Icons.Rounded.PedalBike
        "train" -> Icons.Rounded.Train
        "whistle" -> Icons.Rounded.Sports
        "microwave" -> Icons.Rounded.Microwave
        "kitchen" -> Icons.Rounded.Kitchen
        "music" -> Icons.Rounded.MusicNote
        "bell" -> Icons.Rounded.Notifications
        "star" -> Icons.Rounded.Star
        // Modes
        "everyday" -> Icons.Rounded.WbSunny
        "focus" -> Icons.Rounded.Headphones
        "home" -> Icons.Rounded.Home
        "outside" -> Icons.AutoMirrored.Rounded.DirectionsWalk
        "night" -> Icons.Rounded.Bedtime
        else -> Icons.Rounded.GraphicEq
    }

    /** Icons offered when the user teaches Beep a new sound. */
    val customChoices = listOf("custom", "bell", "microwave", "kitchen", "pet", "phone", "music", "star", "water", "timer")
}
