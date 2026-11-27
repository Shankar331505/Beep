package com.shankar.beep.data

import android.content.Context
import com.shankar.beep.detect.CustomSound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.util.UUID

/**
 * Stores what Beep learned from the user: MFCC templates of their name and
 * fingerprints of sounds they taught it. These are numbers derived from the
 * audio, not recordings — the original audio is discarded immediately.
 */
class VoiceStore(context: Context) {

    private val nameDir = File(context.filesDir, "name").apply { mkdirs() }
    private val customDir = File(context.filesDir, "custom").apply { mkdirs() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _nameTemplates = MutableStateFlow<List<Array<FloatArray>>>(emptyList())
    val nameTemplates: StateFlow<List<Array<FloatArray>>> = _nameTemplates.asStateFlow()

    private val _customSounds = MutableStateFlow<List<CustomSound>>(emptyList())
    val customSounds: StateFlow<List<CustomSound>> = _customSounds.asStateFlow()

    init {
        scope.launch {
            _nameTemplates.value = readMatrices(File(nameDir, NAME_FILE))
            _customSounds.value = loadCustomSounds()
        }
    }

    suspend fun saveNameTemplates(templates: List<Array<FloatArray>>) = withContext(Dispatchers.IO) {
        writeMatrices(File(nameDir, NAME_FILE), templates)
        _nameTemplates.value = templates
    }

    suspend fun addCustomSound(name: String, icon: String, prints: List<FloatArray>): CustomSound =
        withContext(Dispatchers.IO) {
            val sound = CustomSound(
                id = "custom:" + UUID.randomUUID().toString().take(8),
                name = name.trim(),
                icon = icon,
                prints = prints,
                createdAt = System.currentTimeMillis(),
            )
            val dir = File(customDir, sound.id.removePrefix("custom:")).apply { mkdirs() }
            File(dir, "meta.json").writeText(
                JSONObject().put("id", sound.id).put("name", sound.name).put("icon", sound.icon)
                    .put("created", sound.createdAt).toString()
            )
            writeMatrices(File(dir, "prints.bin"), listOf(prints.toTypedArray()))
            _customSounds.value = _customSounds.value + sound
            sound
        }

    suspend fun deleteCustomSound(id: String) = withContext(Dispatchers.IO) {
        File(customDir, id.removePrefix("custom:")).deleteRecursively()
        _customSounds.value = _customSounds.value.filterNot { it.id == id }
    }

    private fun loadCustomSounds(): List<CustomSound> =
        customDir.listFiles()?.mapNotNull { dir ->
            runCatching {
                val meta = JSONObject(File(dir, "meta.json").readText())
                val prints = readMatrices(File(dir, "prints.bin")).firstOrNull()?.toList() ?: emptyList()
                CustomSound(
                    id = meta.getString("id"),
                    name = meta.getString("name"),
                    icon = meta.optString("icon", "custom"),
                    prints = prints,
                    createdAt = meta.optLong("created"),
                )
            }.getOrNull()
        }?.sortedBy { it.createdAt } ?: emptyList()

    companion object {
        private const val NAME_FILE = "templates.bin"

        internal fun writeMatrices(file: File, matrices: List<Array<FloatArray>>) {
            val tmp = File(file.parentFile, file.name + ".tmp")
            DataOutputStream(tmp.outputStream().buffered()).use { out ->
                out.writeInt(matrices.size)
                for (m in matrices) {
                    out.writeInt(m.size)
                    out.writeInt(m.firstOrNull()?.size ?: 0)
                    for (row in m) for (v in row) out.writeFloat(v)
                }
            }
            tmp.renameTo(file)
        }

        internal fun readMatrices(file: File): List<Array<FloatArray>> = runCatching {
            if (!file.exists()) return emptyList()
            DataInputStream(file.inputStream().buffered()).use { input ->
                List(input.readInt()) {
                    val rows = input.readInt()
                    val cols = input.readInt()
                    Array(rows) { FloatArray(cols) { input.readFloat() } }
                }
            }
        }.getOrDefault(emptyList())
    }
}
