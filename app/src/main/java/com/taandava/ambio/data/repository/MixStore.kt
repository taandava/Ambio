package com.taandava.ambio.data.repository

import android.content.Context
import androidx.core.content.edit
import com.taandava.ambio.data.model.MixChannel
import com.taandava.ambio.data.model.SavedMix
import com.taandava.ambio.data.model.SoundItem
import com.taandava.ambio.data.model.ThemeMode
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class MixStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("ambio_mix_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SAVED_MIXES = "key_saved_mixes"
        private const val KEY_ACTIVE_MIX_ID = "key_active_mix_id"
        private const val KEY_CUSTOM_SOUNDS = "key_custom_sounds"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_MASTER_VOLUME = "key_master_volume"
        const val MAX_CHANNELS = 10
    }

    fun getThemeMode(): ThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, ThemeMode.DARK.name)
        return try { ThemeMode.valueOf(name!!) } catch (_: Exception) { ThemeMode.DARK }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit { putString(KEY_THEME_MODE, mode.name) }
    }

    fun getMasterVolume(): Int {
        return prefs.getInt(KEY_MASTER_VOLUME, 100)
    }

    fun setMasterVolume(vol: Int) {
        prefs.edit { putInt(KEY_MASTER_VOLUME, vol.coerceIn(0, 100)) }
    }

    fun loadSavedMixes(): List<SavedMix> {
        val raw = prefs.getString(KEY_SAVED_MIXES, null)
        if (raw.isNullOrBlank()) return emptyList()

        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val chArray = obj.optJSONArray("channels") ?: JSONArray()
                    val chList = (0 until MAX_CHANNELS).map { idx ->
                        val chObj = chArray.optJSONObject(idx)
                        if (chObj != null) channelFromJson(chObj, idx) else
                            MixChannel(idx)
                    }
                    add(
                        SavedMix(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            name = obj.optString("name", "Mix ${i + 1}"),
                            channels = chList,
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getActiveMixId(): String? {
        return prefs.getString(KEY_ACTIVE_MIX_ID, null)
    }

    fun setActiveMixId(id: String?) {
        prefs.edit { putString(KEY_ACTIVE_MIX_ID, id) }
    }

    fun getActiveMix(): SavedMix? {
        val mixes = loadSavedMixes()
        if (mixes.isEmpty()) return null
        val activeId = getActiveMixId()
        return mixes.find { it.id == activeId } ?: mixes.firstOrNull()
    }

    fun saveMix(mix: SavedMix) {
        val current = loadSavedMixes().toMutableList()
        val index = current.indexOfFirst { it.id == mix.id }
        if (index >= 0) {
            current[index] = mix
        } else {
            current.add(0, mix)
        }
        persistSavedMixes(current)
        setActiveMixId(mix.id)
    }

    fun createNewMix(name: String): SavedMix {
        val defaultChannels = (0 until MAX_CHANNELS).map { MixChannel(channelId = it) }
        val newMix = SavedMix(name = name, channels = defaultChannels)
        saveMix(newMix)
        return newMix
    }

    fun deleteMix(id: String) {
        val updated = loadSavedMixes().filterNot { it.id == id }
        persistSavedMixes(updated)
        if (getActiveMixId() == id) {
            setActiveMixId(updated.firstOrNull()?.id)
        }
    }

    private fun persistSavedMixes(list: List<SavedMix>) {
        val array = JSONArray()
        for ((id, name, channels) in list) {
            val mObj = JSONObject()
            mObj.put("id", id)
            mObj.put("name", name)
            val chArr = JSONArray()
            for (ch in channels) {
                chArr.put(channelToJson(ch))
            }
            mObj.put("channels", chArr)
            array.put(mObj)
        }
        prefs.edit { putString(KEY_SAVED_MIXES, array.toString()) }
    }

    fun loadCustomSounds(): List<SoundItem> {
        val raw = prefs.getString(KEY_CUSTOM_SOUNDS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    add(
                        SoundItem(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            uriString = obj.getString("uri"),
                            artist = obj.optString("artist", ""),
                            durationMs = obj.optLong("durationMs", 0L)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveCustomSound(sound: SoundItem) {
        val current = loadCustomSounds().toMutableList()
        if (current.none { it.uriString == sound.uriString }) {
            current.add(0, sound)
            val array = JSONArray()
            for ((id, name, uriString, artist, durationMs) in current) {
                val obj = JSONObject()
                obj.put("id", id)
                obj.put("name", name)
                obj.put("uri", uriString)
                obj.put("artist", artist)
                obj.put("durationMs", durationMs)
                array.put(obj)
            }
            prefs.edit { putString(KEY_CUSTOM_SOUNDS, array.toString()) }
        }
    }

    fun deleteCustomSound(id: String) {
        val updated = loadCustomSounds().filterNot { it.id == id }
        val array = JSONArray()
        for ((id, name, uriString, artist, durationMs) in updated) {
            val obj = JSONObject()
            obj.put("id", id)
            obj.put("name", name)
            obj.put("uri", uriString)
            obj.put("artist", artist)
            obj.put("durationMs", durationMs)
            array.put(obj)
        }
        prefs.edit { putString(KEY_CUSTOM_SOUNDS, array.toString()) }
    }

    private fun channelToJson(ch: MixChannel): JSONObject {
        val obj = JSONObject()
        obj.put("channelId", ch.channelId)
        obj.put("volume", ch.volume)
        obj.put("isMuted", ch.isMuted)
        obj.put("repeatIntervalSec", ch.repeatIntervalSec)
        obj.put("isFuzzyRepeat", ch.isFuzzyRepeat)
        obj.put("customName", ch.customName ?: "")
        obj.put("pitch", ch.pitch.toDouble())
        obj.put("speed", ch.speed.toDouble())
        if (ch.sound != null) {
            val sObj = JSONObject()
            sObj.put("id", ch.sound.id)
            sObj.put("name", ch.sound.name)
            sObj.put("uri", ch.sound.uriString)
            sObj.put("artist", ch.sound.artist)
            sObj.put("durationMs", ch.sound.durationMs)
            obj.put("sound", sObj)
        }
        return obj
    }

    private fun channelFromJson(obj: JSONObject, defaultId: Int): MixChannel {
        val channelId = obj.optInt("channelId", defaultId)
        val volume = obj.optInt("volume", 50)
        val isMuted = obj.optBoolean("isMuted", false)
        val repeatSec = obj.optInt("repeatIntervalSec", 0)
        val fuzzy = obj.optBoolean("isFuzzyRepeat", false)
        val customName = obj.optString("customName").takeIf { it.isNotBlank() }
        val pitch = obj.optDouble("pitch", 1.0).toFloat().coerceIn(0.5f, 1.5f)
        val speed = obj.optDouble("speed", 1.0).toFloat().coerceIn(0.5f, 1.5f)
        val sObj = obj.optJSONObject("sound")
        val sound = if (sObj != null) {
            SoundItem(
                id = sObj.optString("id"),
                name = sObj.optString("name", "Audio"),
                uriString = sObj.optString("uri"),
                artist = sObj.optString("artist", ""),
                durationMs = sObj.optLong("durationMs", 0L)
            )
        } else null

        return MixChannel(
            channelId = channelId,
            sound = sound,
            customName = customName,
            volume = volume,
            isMuted = isMuted,
            repeatIntervalSec = repeatSec,
            isFuzzyRepeat = fuzzy,
            pitch = pitch,
            speed = speed
        )
    }
}
