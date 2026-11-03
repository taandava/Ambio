package com.taandava.ambio.service

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.roundToInt

class SleepTimerLogicTest {

    private fun computeFadeFactor(remainingMs: Long): Float {
        return (remainingMs.toFloat() / 15000f).coerceIn(0f, 1f)
    }

    private fun computeFadedVolume(masterVolume: Int, remainingMs: Long): Int {
        val factor = computeFadeFactor(remainingMs)
        return (masterVolume * factor).roundToInt()
    }

    @Test
    fun testFadeFactorAboveFifteenSecondsIsFullVolume() {
        val factor = computeFadeFactor(30000L) // 30s remaining
        assertEquals(1.0f, factor, 0.001f)
        assertEquals(80, computeFadedVolume(80, 30000L))
    }

    @Test
    fun testFadeFactorAtExactFifteenSecondsBoundary() {
        val factor = computeFadeFactor(15000L)
        assertEquals(1.0f, factor, 0.001f)
        assertEquals(100, computeFadedVolume(100, 15000L))
    }

    @Test
    fun testFadeFactorMidwayThroughFadeWindow() {
        val factor = computeFadeFactor(7500L) // halfway through 15s fade
        assertEquals(0.5f, factor, 0.001f)
        assertEquals(50, computeFadedVolume(100, 7500L))
    }

    @Test
    fun testFadeFactorNearEndIsZero() {
        val factor = computeFadeFactor(0L)
        assertEquals(0.0f, factor, 0.001f)
        assertEquals(0, computeFadedVolume(100, 0L))
    }

    @Test
    fun testNegativeRemainingIsClampedToZero() {
        val factor = computeFadeFactor(-5000L)
        assertEquals(0.0f, factor, 0.001f)
        assertEquals(0, computeFadedVolume(90, -5000L))
    }
}
