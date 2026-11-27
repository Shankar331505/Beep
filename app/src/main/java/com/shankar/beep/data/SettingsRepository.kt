package com.shankar.beep.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shankar.beep.detect.Mode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeChoice(val title: String) { SYSTEM("Match system"), DARK("Dark"), LIGHT("Light") }

data class Settings(
    val onboarded: Boolean = false,
    val userName: String = "",
    /** The preset in use, or null once the user has customised their sounds. */
    val mode: Mode? = Mode.EVERYDAY,
    val enabled: Set<String> = Mode.EVERYDAY.sounds,
    val sensitivity: Map<String, Float> = emptyMap(),
    val vibrate: Boolean = true,
    val flash: Boolean = true,
    val chime: Boolean = true,
    val pauseMedia: Boolean = true,
    val holdUntilDismissed: Boolean = false,
    val fullScreen: Boolean = true,
    val theme: ThemeChoice = ThemeChoice.SYSTEM,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "beep_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val onboarded = booleanPreferencesKey("onboarded")
        val userName = stringPreferencesKey("user_name")
        val mode = stringPreferencesKey("mode")
        val enabled = stringSetPreferencesKey("enabled")
        val sensitivity = stringSetPreferencesKey("sensitivity")
        val vibrate = booleanPreferencesKey("vibrate")
        val flash = booleanPreferencesKey("flash")
        val chime = booleanPreferencesKey("chime")
        val pauseMedia = booleanPreferencesKey("pause_media")
        val hold = booleanPreferencesKey("hold_until_dismissed")
        val fullScreen = booleanPreferencesKey("full_screen")
        val theme = stringPreferencesKey("theme")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        val defaults = Settings()
        Settings(
            onboarded = p[Keys.onboarded] ?: defaults.onboarded,
            userName = p[Keys.userName] ?: defaults.userName,
            mode = p[Keys.mode].let { raw ->
                when (raw) {
                    null -> defaults.mode
                    CUSTOM -> null
                    else -> Mode.entries.firstOrNull { it.name == raw } ?: defaults.mode
                }
            },
            enabled = p[Keys.enabled] ?: defaults.enabled,
            sensitivity = p[Keys.sensitivity]?.let(::decodeSensitivity) ?: defaults.sensitivity,
            vibrate = p[Keys.vibrate] ?: defaults.vibrate,
            flash = p[Keys.flash] ?: defaults.flash,
            chime = p[Keys.chime] ?: defaults.chime,
            pauseMedia = p[Keys.pauseMedia] ?: defaults.pauseMedia,
            holdUntilDismissed = p[Keys.hold] ?: defaults.holdUntilDismissed,
            fullScreen = p[Keys.fullScreen] ?: defaults.fullScreen,
            theme = p[Keys.theme]?.let { raw -> ThemeChoice.entries.firstOrNull { it.name == raw } } ?: defaults.theme,
        )
    }

    suspend fun current(): Settings = settings.first()

    suspend fun completeOnboarding() = context.dataStore.edit { it[Keys.onboarded] = true }

    suspend fun setUserName(name: String) = context.dataStore.edit { it[Keys.userName] = name.trim() }

    suspend fun applyMode(mode: Mode, customIds: Collection<String> = emptyList()) = context.dataStore.edit {
        it[Keys.mode] = mode.name
        // Sounds the user taught Beep stay on regardless of the preset.
        it[Keys.enabled] = mode.sounds + customIds
    }

    suspend fun setEnabled(id: String, on: Boolean) = context.dataStore.edit { p ->
        val current = p[Keys.enabled] ?: Settings().enabled
        p[Keys.enabled] = if (on) current + id else current - id
        p[Keys.mode] = CUSTOM
    }

    suspend fun setSensitivity(id: String, value: Float) = context.dataStore.edit { p ->
        val map = (p[Keys.sensitivity]?.let(::decodeSensitivity) ?: emptyMap()).toMutableMap()
        map[id] = value.coerceIn(0f, 1f)
        p[Keys.sensitivity] = encodeSensitivity(map)
    }

    suspend fun removeSound(id: String) = context.dataStore.edit { p ->
        p[Keys.enabled] = (p[Keys.enabled] ?: Settings().enabled) - id
        val map = (p[Keys.sensitivity]?.let(::decodeSensitivity) ?: emptyMap()) - id
        p[Keys.sensitivity] = encodeSensitivity(map)
    }

    suspend fun update(transform: (Settings) -> Settings) {
        val next = transform(current())
        context.dataStore.edit { p ->
            p[Keys.vibrate] = next.vibrate
            p[Keys.flash] = next.flash
            p[Keys.chime] = next.chime
            p[Keys.pauseMedia] = next.pauseMedia
            p[Keys.hold] = next.holdUntilDismissed
            p[Keys.fullScreen] = next.fullScreen
            p[Keys.theme] = next.theme.name
        }
    }

    companion object {
        private const val CUSTOM = "CUSTOM"

        internal fun encodeSensitivity(map: Map<String, Float>): Set<String> =
            map.map { (k, v) -> "$k=$v" }.toSet()

        internal fun decodeSensitivity(set: Set<String>): Map<String, Float> =
            set.mapNotNull { entry ->
                val i = entry.lastIndexOf('=')
                if (i <= 0) null else entry.substring(0, i) to (entry.substring(i + 1).toFloatOrNull() ?: return@mapNotNull null)
            }.toMap()
    }
}
