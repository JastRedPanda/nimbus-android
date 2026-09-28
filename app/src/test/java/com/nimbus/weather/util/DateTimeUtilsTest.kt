package com.nimbus.weather.util

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DateTimeUtilsTest {

    @Test
    fun `formatTime parses iso time`() {
        assertEquals("14:30", formatTime("2026-07-31T14:30:00"))
    }

    @Test
    fun `formatTime handles edge case`() {
        assertEquals("00:00", formatTime("2026-01-01T00:00:00"))
    }

    @Test
    fun `formatTime falls back to last 5 chars on error`() {
        assertEquals("23:59", formatTime("23:59"))
    }

    @Test
    fun `isToday returns true for today`() {
        assertEquals(true, isToday(LocalDate.now().toString()))
    }

    @Test
    fun `isToday returns false for other date`() {
        assertEquals(false, isToday("2025-01-01"))
    }

    @Test
    fun `isToday returns false for invalid date`() {
        assertEquals(false, isToday("not-a-date"))
    }

    @Test
    fun `isDayNowByTime uses system time not observation date`() {
        // Данные вчерашние вечерние, смотрим утром — должен быть день.
        assertTrue(
            isDayNowByTime(
                LocalTime.of(7, 50),
                "2026-09-26T06:30",
                "2026-09-26T18:20"
            )
        )
        assertFalse(
            isDayNowByTime(
                LocalTime.of(22, 12),
                "2026-09-27T06:30",
                "2026-09-27T18:20"
            )
        )
    }

    @Test
    fun `isDayNowByTime defaults to day on bad input`() {
        assertTrue(isDayNowByTime(LocalTime.NOON, "", ""))
    }
}
