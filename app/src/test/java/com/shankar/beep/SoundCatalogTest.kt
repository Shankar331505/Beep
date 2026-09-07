package com.shankar.beep

import com.shankar.beep.data.SoundCatalog
import com.shankar.beep.model.SoundCategory
import org.junit.Assert.*
import org.junit.Test

class SoundCatalogTest {

    @Test
    fun testCatalogHasEssentialSounds() {
        val catalog = SoundCatalog.DEFAULT_CATALOG
        assertTrue(catalog.isNotEmpty())

        val nameSound = catalog.find { it.id == SoundCatalog.SOUND_ID_NAME }
        assertNotNull("User name sound must exist in catalog", nameSound)
        assertEquals(SoundCategory.SOCIAL, nameSound?.category)

        val knockSound = catalog.find { it.id == SoundCatalog.SOUND_ID_KNOCK }
        assertNotNull("Knock sound must exist in catalog", knockSound)

        val fireAlarmSound = catalog.find { it.id == SoundCatalog.SOUND_ID_FIRE_ALARM }
        assertNotNull("Fire alarm sound must exist in catalog", fireAlarmSound)
        assertEquals(SoundCategory.EMERGENCY, fireAlarmSound?.category)

        val clapSound = catalog.find { it.id == SoundCatalog.SOUND_ID_CLAP }
        assertNotNull("Clap sound must exist in catalog", clapSound)
    }

    @Test
    fun testThresholdRangesAreValid() {
        for (sound in SoundCatalog.DEFAULT_CATALOG) {
            assertTrue(
                "Threshold for ${sound.id} must be between 0.1 and 1.0",
                sound.defaultConfidenceThreshold in 0.1f..1.0f
            )
            assertTrue(
                "Cooldown for ${sound.id} must be >= 1 second",
                sound.cooldownSeconds >= 1
            )
        }
    }
}
