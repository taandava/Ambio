package com.taandava.ambio.service

import com.taandava.ambio.data.model.MixChannel
import com.taandava.ambio.data.model.SoundItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt
import kotlin.random.Random

class VolumeAndPlaybackLogicTest {

    private fun computeEffectiveVolume(channel: MixChannel, masterVolume: Int): Float {
        if (channel.isMuted) return 0f
        val masterFactor = masterVolume.coerceIn(0, 100) / 100f
        return (channel.volume.coerceIn(0, 100) / 100f) * masterFactor
    }

    private fun computeFuzzyInterval(repeatIntervalSec: Int, isFuzzy: Boolean): Int {
        return if (isFuzzy) {
            val variance = 0.7 + (Random.nextDouble() * 0.6)
            (repeatIntervalSec * variance).roundToInt().coerceAtLeast(1)
        } else {
            repeatIntervalSec
        }
    }

    @Test
    fun testEffectiveVolumeFullMasterFullChannel() {
        val ch = MixChannel(channelId = 0, volume = 100, isMuted = false)
        val eff = computeEffectiveVolume(ch, 100)
        assertEquals(1.0f, eff, 0.001f)
    }

    @Test
    fun testEffectiveVolumeHalfMasterHalfChannel() {
        val ch = MixChannel(channelId = 0, volume = 50, isMuted = false)
        val eff = computeEffectiveVolume(ch, 50)
        assertEquals(0.25f, eff, 0.001f)
    }

    @Test
    fun testEffectiveVolumeMutedChannelIsAlwaysZero() {
        val ch = MixChannel(channelId = 0, volume = 100, isMuted = true)
        val eff = computeEffectiveVolume(ch, 100)
        assertEquals(0f, eff, 0.0f)
    }

    @Test
    fun testEffectiveVolumeZeroMasterIsZero() {
        val ch = MixChannel(channelId = 0, volume = 100, isMuted = false)
        val eff = computeEffectiveVolume(ch, 0)
        assertEquals(0f, eff, 0.0f)
    }

    @Test
    fun testMasterVolumeClamping() {
        val negative = (-25).coerceIn(0, 100)
        val excessive = 150.coerceIn(0, 100)
        val normal = 75.coerceIn(0, 100)

        assertEquals(0, negative)
        assertEquals(100, excessive)
        assertEquals(75, normal)
    }

    @Test
    fun testFuzzyIntervalStaysWithinBounds() {
        val baseInterval = 10
        // Variance formula: 0.7 + [0..0.6] -> range [0.7, 1.3]
        // Range for 10 is [7, 13]
        for (i in 0 until 100) {
            val fuzzy = computeFuzzyInterval(baseInterval, isFuzzy = true)
            assertTrue("Fuzzy interval $fuzzy out of range [7, 13]", fuzzy in 7..13)
        }
    }

    @Test
    fun testNonFuzzyIntervalIsExact() {
        val baseInterval = 15
        val exact = computeFuzzyInterval(baseInterval, isFuzzy = false)
        assertEquals(15, exact)
    }
}

