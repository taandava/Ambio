package com.taandava.ambio.model

import com.taandava.ambio.data.model.MixChannel
import com.taandava.ambio.data.model.SavedMix
import com.taandava.ambio.data.model.SoundItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundModelsTest {

    @Test
    fun testMixChannelDefaultValues() {
        val channel = MixChannel(channelId = 0)
        assertEquals(0, channel.channelId)
        assertNull(channel.sound)
        assertNull(channel.customName)
        assertEquals(50, channel.volume)
        assertFalse(channel.isMuted)
        assertEquals(0, channel.repeatIntervalSec)
        assertFalse(channel.isFuzzyRepeat)
        assertEquals(1.0f, channel.pitch, 0.001f)
        assertEquals(1.0f, channel.speed, 0.001f)
    }

    @Test
    fun testMixChannelDisplayNameFallbackToChannelNumber() {
        val channel = MixChannel(channelId = 3)
        assertEquals("Channel 4", channel.displayName)
    }

    @Test
    fun testMixChannelDisplayNameUsesSoundNameWhenCustomNameBlank() {
        val sound = SoundItem(id = "s1", name = "Ocean Waves", uriString = "content://audio/1")
        val channel = MixChannel(channelId = 0, sound = sound, customName = "")
        assertEquals("Ocean Waves", channel.displayName)
    }

    @Test
    fun testMixChannelDisplayNamePrioritizesCustomName() {
        val sound = SoundItem(id = "s1", name = "Ocean Waves", uriString = "content://audio/1")
        val channel = MixChannel(channelId = 0, sound = sound, customName = "Deep Ocean Surf")
        assertEquals("Deep Ocean Surf", channel.displayName)
    }

    @Test
    fun testSavedMixCreation() {
        val sound = SoundItem(id = "s2", name = "Campfire", uriString = "content://audio/2")
        val channel = MixChannel(channelId = 1, sound = sound, volume = 80)
        val mix = SavedMix(name = "Night Camp", channels = listOf(channel))

        assertNotNull(mix.id)
        assertTrue(mix.id.isNotBlank())
        assertEquals("Night Camp", mix.name)
        assertEquals(1, mix.channels.size)
        assertEquals("Campfire", mix.channels[0].sound?.name)
        assertEquals(80, mix.channels[0].volume)
    }
}
