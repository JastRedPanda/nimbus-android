package com.nimbus.weather.util

import org.junit.Assert.assertEquals
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
        assertEquals(true, isToday("2026-07-31"))
    }

    @Test
    fun `isToday returns false for other date`() {
        assertEquals(false, isToday("2025-01-01"))
    }

    @Test
    fun `isToday returns false for invalid date`() {
        assertEquals(false, isToday("not-a-date"))
    }
}
