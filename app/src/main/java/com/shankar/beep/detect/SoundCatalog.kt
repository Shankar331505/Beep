package com.shankar.beep.detect

/** Groups sounds so each kind of alert has its own colour, vibration and chime. */
enum class Category(val title: String, val priority: Int) {
    SAFETY("Safety", 3),
    PEOPLE("People", 2),
    HOME("Home", 1),
    STREET("Outside", 2),
    CUSTOM("Your sounds", 1),
}

/** How a sound behaves over time, which changes how many frames we need to be sure. */
enum class Shape {
    /** Short and sharp (knock, glass, bang): one confident frame is enough. */
    IMPULSE,
    /** Lasts a while (alarm, siren, crying): wait for it to persist to avoid false alarms. */
    SUSTAINED,
}

enum class Source { MODEL, NAME, LOUDNESS, CUSTOM }

data class SoundSpec(
    val id: String,
    val title: String,
    val description: String,
    val category: Category,
    val icon: String,
    /** Indices into the YAMNet / AudioSet class map. */
    val classes: IntArray = IntArray(0),
    val shape: Shape = Shape.IMPULSE,
    val defaultSensitivity: Float = 0.5f,
    val source: Source = Source.MODEL,
    /** Seconds before the same sound may alert again. */
    val cooldownSeconds: Int = 10,
) {
    override fun equals(other: Any?) = other is SoundSpec && other.id == id
    override fun hashCode() = id.hashCode()
}

object SoundCatalog {

    const val NAME = "name"
    const val LOUD = "loud"

    val all: List<SoundSpec> = listOf(
        // Safety
        SoundSpec("smoke", "Smoke or fire alarm", "Smoke detectors, fire alarms and other alarm tones",
            Category.SAFETY, "fire", intArrayOf(393, 394, 382), Shape.SUSTAINED, 0.65f, cooldownSeconds = 30),
        SoundSpec("siren", "Siren", "Ambulance, police, fire engine and civil defence sirens",
            Category.SAFETY, "siren", intArrayOf(316, 317, 318, 319, 390, 391), Shape.SUSTAINED, 0.55f, cooldownSeconds = 30),
        SoundSpec("glass", "Breaking glass", "Glass shattering or smashing",
            Category.SAFETY, "glass", intArrayOf(435, 437), Shape.IMPULSE, 0.5f),
        SoundSpec("crash", "Crash or bang", "Something heavy falling, a smash, a bang",
            Category.SAFETY, "crash", intArrayOf(463, 460, 420, 421), Shape.IMPULSE, 0.45f),
        SoundSpec("scream", "Scream", "Someone screaming",
            Category.SAFETY, "scream", intArrayOf(11), Shape.IMPULSE, 0.5f),
        SoundSpec("car_alarm", "Car alarm", "A car alarm going off",
            Category.SAFETY, "car_alarm", intArrayOf(304), Shape.SUSTAINED, 0.5f, cooldownSeconds = 60),
        SoundSpec(LOUD, "Sudden loud noise", "Any sudden noise much louder than the room — no AI needed",
            Category.SAFETY, "loud", source = Source.LOUDNESS, defaultSensitivity = 0.5f, cooldownSeconds = 8),

        // People
        SoundSpec(NAME, "Your name", "Someone calling your name",
            Category.PEOPLE, "name", source = Source.NAME, defaultSensitivity = 0.5f, cooldownSeconds = 6),
        SoundSpec("shout", "Someone shouting", "Shouting, yelling or calling out loudly",
            Category.PEOPLE, "shout", intArrayOf(6, 7, 9, 10), Shape.IMPULSE, 0.5f),
        SoundSpec("baby", "Baby crying", "A baby or child crying",
            Category.PEOPLE, "baby", intArrayOf(20, 19), Shape.SUSTAINED, 0.6f, cooldownSeconds = 30),
        SoundSpec("talking", "Someone talking", "Conversation nearby — useful if you want to know when someone speaks to you",
            Category.PEOPLE, "talking", intArrayOf(0, 1, 2), Shape.SUSTAINED, 0.35f, cooldownSeconds = 60),

        // Home
        SoundSpec("doorbell", "Doorbell", "Doorbells and ding-dong chimes",
            Category.HOME, "doorbell", intArrayOf(349, 350), Shape.IMPULSE, 0.6f),
        SoundSpec("knock", "Door knock", "Knocking on a door",
            Category.HOME, "knock", intArrayOf(353), Shape.IMPULSE, 0.6f),
        SoundSpec("phone", "Phone ringing", "A phone or ringtone",
            Category.HOME, "phone", intArrayOf(383, 384, 385), Shape.SUSTAINED, 0.5f, cooldownSeconds = 20),
        SoundSpec("appliance", "Timer or appliance beep", "Microwave, oven timer, alarm clock, buzzers and beeps",
            Category.HOME, "timer", intArrayOf(389, 392, 475, 362), Shape.SUSTAINED, 0.45f, cooldownSeconds = 20),
        SoundSpec("water", "Running water", "A tap or faucet left running",
            Category.HOME, "water", intArrayOf(364), Shape.SUSTAINED, 0.45f, cooldownSeconds = 120),
        SoundSpec("dog", "Dog barking", "Dogs barking or whimpering",
            Category.HOME, "dog", intArrayOf(69, 70, 75), Shape.IMPULSE, 0.5f, cooldownSeconds = 20),
        SoundSpec("cat", "Cat meowing", "A cat meowing",
            Category.HOME, "cat", intArrayOf(76, 78), Shape.IMPULSE, 0.5f, cooldownSeconds = 20),

        // Outside
        SoundSpec("horn", "Car horn", "Car, bus and truck horns",
            Category.STREET, "horn", intArrayOf(302, 312), Shape.IMPULSE, 0.5f, cooldownSeconds = 6),
        SoundSpec("reversing", "Reversing vehicle", "The beeping of a vehicle backing up",
            Category.STREET, "reversing", intArrayOf(313), Shape.SUSTAINED, 0.5f, cooldownSeconds = 15),
        SoundSpec("bicycle", "Bicycle bell", "A bike bell behind you",
            Category.STREET, "bicycle", intArrayOf(198), Shape.IMPULSE, 0.5f, cooldownSeconds = 6),
        SoundSpec("train", "Train horn", "Train horns and whistles",
            Category.STREET, "train", intArrayOf(324, 325), Shape.IMPULSE, 0.5f, cooldownSeconds = 15),
        SoundSpec("whistle", "Whistle", "Someone whistling or a whistle blown",
            Category.STREET, "whistle", intArrayOf(396, 35), Shape.IMPULSE, 0.45f, cooldownSeconds = 10),
    )

    private val byId = all.associateBy { it.id }

    fun find(id: String): SoundSpec? = byId[id]

    /** YAMNet class 0 is "Speech": used to make sure the name engine only wakes for voices. */
    const val SPEECH_CLASS = 0

    fun customSpec(sound: CustomSound) = SoundSpec(
        id = sound.id,
        title = sound.name,
        description = "Taught by you · ${sound.prints.size} samples",
        category = Category.CUSTOM,
        icon = sound.icon,
        source = Source.CUSTOM,
        defaultSensitivity = 0.5f,
        cooldownSeconds = 10,
    )
}

/** A ready-made selection of sounds for a situation. */
enum class Mode(val title: String, val tagline: String, val icon: String, val sounds: Set<String>) {
    EVERYDAY(
        "Everyday", "A balanced mix for most days", "everyday",
        setOf("name", "smoke", "siren", "glass", "scream", "doorbell", "knock", "baby", "shout", "phone", "horn", "loud"),
    ),
    FOCUS(
        "Focus", "Headphones on, studying or working", "focus",
        setOf("name", "smoke", "knock", "doorbell", "shout", "phone", "glass"),
    ),
    HOME(
        "Home", "Chores, cooking and family", "home",
        setOf("name", "smoke", "glass", "crash", "doorbell", "knock", "baby", "dog", "appliance", "water", "phone"),
    ),
    OUTSIDE(
        "Outside", "Walking, commuting, cycling", "outside",
        setOf("name", "siren", "horn", "reversing", "bicycle", "train", "shout", "scream", "loud"),
    ),
    SLEEP(
        "Night", "Wakes you only for what matters", "night",
        setOf("smoke", "glass", "crash", "scream", "baby", "car_alarm", "siren"),
    ),
}
