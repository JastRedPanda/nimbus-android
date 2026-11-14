package com.nimbus.weather.util

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitPreferencesTest {

    @Test
    fun `default units depend on language`() {
        assertEquals(PressureUnit.MMHG, defaultPressureUnit("ru"))
        assertEquals(PressureUnit.MMHG, defaultPressureUnit("uk"))
        assertEquals(PressureUnit.HPA, defaultPressureUnit("en"))
        assertEquals(PressureUnit.HPA, defaultPressureUnit("cs"))

        assertEquals(WindSpeedUnit.MS, defaultWindSpeedUnit("ru"))
        assertEquals(WindSpeedUnit.MS, defaultWindSpeedUnit("uk"))
        assertEquals(WindSpeedUnit.KMH, defaultWindSpeedUnit("en"))
        assertEquals(WindSpeedUnit.KMH, defaultWindSpeedUnit("cs"))
    }

    @Test
    fun `pressure conversion`() {
        val hpa = 1013.25
        val mmhg = hpa.toPressure(PressureUnit.MMHG)
        assertEquals(759.99, mmhg, 0.1)

        val back = mmhg.toPressure(PressureUnit.HPA) // если в HPA, умножения нет
        assertEquals(759.99, back, 0.1)
    }

    @Test
    fun `wind speed conversion`() {
        val ms = 10.0
        val kmh = ms.toWindSpeed(WindSpeedUnit.KMH)
        assertEquals(36.0, kmh, 0.01)

        val back = kmh.toWindSpeed(WindSpeedUnit.MS)
        assertEquals(36.0, back, 0.01)
    }
}
