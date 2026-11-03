package com.taandava.ambio.theme

import com.taandava.ambio.data.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {

    @Test
    fun testThemeModeLabels() {
        assertEquals("Follow System", ThemeMode.SYSTEM.label)
        assertEquals("Pure Light", ThemeMode.LIGHT.label)
        assertEquals("Pure Dark", ThemeMode.DARK.label)
    }

    @Test
    fun testThemeModeParsingValid() {
        assertEquals(ThemeMode.DARK, ThemeMode.valueOf("DARK"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.valueOf("LIGHT"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.valueOf("SYSTEM"))
    }

    @Test
    fun testThemeModeFallbackOnInvalid() {
        val raw = "INVALID_MODE"
        val parsed = try {
            ThemeMode.valueOf(raw)
        } catch (_: Exception) {
            ThemeMode.DARK
        }
        assertEquals(ThemeMode.DARK, parsed)
    }
}
