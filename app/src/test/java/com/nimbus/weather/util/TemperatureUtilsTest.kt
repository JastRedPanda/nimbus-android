package com.nimbus.weather.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TemperatureUtilsTest {

    @Test
    fun `celsius to celsius returns same`() {
        assertEquals(0.0, 0.0.toCelsiusOrFahrenheit(TemperatureUnit.CELSIUS), 0.001)
        assertEquals(25.0, 25.0.toCelsiusOrFahrenheit(TemperatureUnit.CELSIUS), 0.001)
        assertEquals(-10.0, (-10.0).toCelsiusOrFahrenheit(TemperatureUnit.CELSIUS), 0.001)
    }

    @Test
    fun `celsius to fahrenheit`() {
        assertEquals(32.0, 0.0.toCelsiusOrFahrenheit(TemperatureUnit.FAHRENHEIT), 0.001)
        assertEquals(212.0, 100.0.toCelsiusOrFahrenheit(TemperatureUnit.FAHRENHEIT), 0.001)
        assertEquals(-4.0, (-20.0).toCelsiusOrFahrenheit(TemperatureUnit.FAHRENHEIT), 0.001)
    }

    @Test
    fun `fahrenheit freezing point`() {
        assertEquals(32.0, 0.0.toCelsiusOrFahrenheit(TemperatureUnit.FAHRENHEIT), 0.001)
    }

    @Test
    fun `fahrenheit body temp`() {
        assertEquals(98.6, 37.0.toCelsiusOrFahrenheit(TemperatureUnit.FAHRENHEIT), 0.001)
    }

    @Test
    fun `displayString celsius`() {
        assertEquals("°C", TemperatureUnit.CELSIUS.displayString())
    }

    @Test
    fun `displayString fahrenheit`() {
        assertEquals("°F", TemperatureUnit.FAHRENHEIT.displayString())
    }
}
