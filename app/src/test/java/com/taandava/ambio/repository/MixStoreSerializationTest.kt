package com.taandava.ambio.repository

import com.taandava.ambio.data.model.MixChannel
import com.taandava.ambio.data.model.SavedMix
import com.taandava.ambio.data.model.SoundItem
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MixStoreSerializationTest {

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

    @Test
    fun testEmptyChannelRoundTrip() {
        val original = MixChannel(channelId = 2)
        val json = channelToJson(original)
        val restored = channelFromJson(json, 2)

        assertEquals(original.channelId, restored.channelId)
        assertNull(restored.sound)
        assertNull(restored.customName)
        assertEquals(original.volume, restored.volume)
        assertEquals(original.isMuted, restored.isMuted)
        assertEquals(original.repeatIntervalSec, restored.repeatIntervalSec)
        assertEquals(original.isFuzzyRepeat, restored.isFuzzyRepeat)
        assertEquals(original.pitch, restored.pitch, 0.001f)
        assertEquals(original.speed, restored.speed, 0.001f)
    }

    @Test
    fun testFilledChannelWithModulationRoundTrip() {
        val sound = SoundItem(
            id = "content://media/123",
            name = "Gentle Rain",
            uriString = "content://media/123",
            artist = "Nature Sounds",
            durationMs = 45000L
        )
        val original = MixChannel(
            channelId = 4,
            sound = sound,
            customName = "Soothing Window Rain",
            volume = 75,
            isMuted = true,
            repeatIntervalSec = 30,
            isFuzzyRepeat = true,
            pitch = 1.25f,
            speed = 0.85f
        )

        val json = channelToJson(original)
        val restored = channelFromJson(json, 4)

        assertEquals(4, restored.channelId)
        assertNotNull(restored.sound)
        assertEquals("Gentle Rain", restored.sound?.name)
        assertEquals("content://media/123", restored.sound?.uriString)
        assertEquals("Nature Sounds", restored.sound?.artist)
        assertEquals(45000L, restored.sound?.durationMs)
        assertEquals("Soothing Window Rain", restored.customName)
        assertEquals(75, restored.volume)
        assertTrue(restored.isMuted)
        assertEquals(30, restored.repeatIntervalSec)
        assertTrue(restored.isFuzzyRepeat)
        assertEquals(1.25f, restored.pitch, 0.001f)
        assertEquals(0.85f, restored.speed, 0.001f)
    }

    @Test
    fun testPitchAndSpeedClampingFromCorruptedJson() {
        val json = JSONObject().apply {
            put("channelId", 0)
            put("pitch", 99.0) // Out of range (> 1.5)
            put("speed", -10.0) // Out of range (< 0.5)
        }
        val restored = channelFromJson(json, 0)
        assertEquals(1.5f, restored.pitch, 0.001f)
        assertEquals(0.5f, restored.speed, 0.001f)
    }

    @Test
    fun testSavedMixListSerializationRoundTrip() {
        val channels = (0 until 10).map { i ->
            if (i == 0) {
                MixChannel(
                    channelId = i,
                    sound = SoundItem(id = "s0", name = "White Noise", uriString = "uri:0"),
                    volume = 60
                )
            } else {
                MixChannel(channelId = i)
            }
        }
        val mix = SavedMix(id = "mix-abc-123", name = "Focus Zen", channels = channels)

        // Serialize
        val array = JSONArray()
        val mObj = JSONObject()
        mObj.put("id", mix.id)
        mObj.put("name", mix.name)
        val chArr = JSONArray()
        for (ch in mix.channels) {
            chArr.put(channelToJson(ch))
        }
        mObj.put("channels", chArr)
        array.put(mObj)

        val raw = array.toString()

        // Deserialize
        val restoredArray = JSONArray(raw)
        val obj = restoredArray.getJSONObject(0)
        assertEquals("mix-abc-123", obj.getString("id"))
        assertEquals("Focus Zen", obj.getString("name"))

        val restoredChArr = obj.getJSONArray("channels")
        assertEquals(10, restoredChArr.length())

        val ch0 = channelFromJson(restoredChArr.getJSONObject(0), 0)
        assertEquals(0, ch0.channelId)
        assertEquals("White Noise", ch0.sound?.name)
        assertEquals(60, ch0.volume)

        val ch1 = channelFromJson(restoredChArr.getJSONObject(1), 1)
        assertEquals(1, ch1.channelId)
        assertNull(ch1.sound)
    }
}

