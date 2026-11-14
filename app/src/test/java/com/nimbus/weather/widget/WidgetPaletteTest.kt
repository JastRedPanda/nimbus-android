package com.nimbus.weather.widget

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetPaletteTest {

    @Test
    fun `light theme gets light background and dark text`() {
        val palette = resolveWidgetPalette(
            dark = false, bgColorHex = null, bgAlpha = 100, textOption = "auto"
        )

        assertEquals(1f, palette.background.alpha)
        assertEquals(Color.Black, palette.text)
    }

    @Test
    fun `dark theme gets white text`() {
        val palette = resolveWidgetPalette(
            dark = true, bgColorHex = null, bgAlpha = 100, textOption = "auto"
        )

        assertEquals(Color.White, palette.text)
    }

    @Test
    fun `background alpha is clamped to 0-100`() {
        val over = resolveWidgetPalette(false, null, 150, "auto")
        val under = resolveWidgetPalette(false, null, -10, "auto")

        assertEquals(1f, over.background.alpha)
        assertEquals(0f, under.background.alpha)
    }

    @Test
    fun `explicit text color overrides luminance`() {
        val white = resolveWidgetPalette(dark = false, bgColorHex = null, bgAlpha = 100, textOption = "white")
        val black = resolveWidgetPalette(dark = true, bgColorHex = null, bgAlpha = 100, textOption = "black")

        assertEquals(Color.White, white.text)
        assertEquals(Color.Black, black.text)
    }

    @Test
    fun `content height covers both rows`() {
        val heightEm = widgetContentHeightEm()

        assertTrue(heightEm >= LINE_EM + SUB_RATIO * LINE_EM)
    }
}
