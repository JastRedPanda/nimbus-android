package com.nimbus.weather.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class AnimatedSkyTest {

    @Test
    fun `thunder for 95 to 99`() {
        assertEquals(SkyEffect.THUNDER, skyEffectFor(95, true))
        assertEquals(SkyEffect.THUNDER, skyEffectFor(99, false))
    }

    @Test
    fun `snow for snow codes`() {
        assertEquals(SkyEffect.SNOW, skyEffectFor(71, true))
        assertEquals(SkyEffect.SNOW, skyEffectFor(86, false))
    }

    @Test
    fun `rain for drizzle rain showers`() {
        assertEquals(SkyEffect.RAIN, skyEffectFor(51, true))
        assertEquals(SkyEffect.RAIN, skyEffectFor(61, true))
        assertEquals(SkyEffect.RAIN, skyEffectFor(80, false))
    }

    @Test
    fun `fog for 45 and 48`() {
        assertEquals(SkyEffect.FOG, skyEffectFor(45, true))
        assertEquals(SkyEffect.FOG, skyEffectFor(48, false))
    }

    @Test
    fun `clouds for overcast`() {
        assertEquals(SkyEffect.CLOUDS, skyEffectFor(2, true))
        assertEquals(SkyEffect.CLOUDS, skyEffectFor(3, false))
    }

    @Test
    fun `stars on clear night, none on clear day`() {
        assertEquals(SkyEffect.STARS, skyEffectFor(0, false))
        assertEquals(SkyEffect.NONE, skyEffectFor(0, true))
        assertEquals(SkyEffect.NONE, skyEffectFor(1, true))
    }
}
