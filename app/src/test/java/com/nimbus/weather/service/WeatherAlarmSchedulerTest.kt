package com.nimbus.weather.service

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherAlarmSchedulerTest {

    @Test
    fun `next trigger is now plus interval`() {
        val now = 1_786_000_000_000L
        assertEquals(now + 2 * 3_600_000L, WeatherAlarmScheduler.nextTriggerMillis(now, 2))
        assertEquals(now + 12 * 3_600_000L, WeatherAlarmScheduler.nextTriggerMillis(now, 12))
        assertEquals(now + 24 * 3_600_000L, WeatherAlarmScheduler.nextTriggerMillis(now, 24))
    }

    @Test
    fun `interval below one hour coerced to one hour`() {
        val now = 1_786_000_000_000L
        assertEquals(now + 3_600_000L, WeatherAlarmScheduler.nextTriggerMillis(now, 0))
    }
}
