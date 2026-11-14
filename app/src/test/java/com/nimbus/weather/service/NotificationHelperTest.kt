package com.nimbus.weather.service

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nimbus.weather.data.model.CurrentWeather
import com.nimbus.weather.data.model.WeatherResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
// SDK 36 в Robolectric требует Java 21, проект собирается на 17.
// Для логики уведомлений (API 33+) разницы с 35 нет.
@Config(sdk = [35])
class NotificationHelperTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager: NotificationManager = context.getSystemService(NotificationManager::class.java)

    private val response = WeatherResponse(
        latitude = 50.45,
        longitude = 30.52,
        timezone = "Europe/Kiev",
        current = CurrentWeather(
            time = "2026-11-14T12:00",
            temperature = 12.6,
            humidity = 68.0,
            apparentTemperature = 10.0,
            precipitation = 0.0,
            weatherCode = 61,
            pressure = 1008.0,
            windSpeed = 4.0,
            windDirection = 220.0,
            windGusts = 8.0,
            uvIndex = 1.0
        )
    )

    @Before
    fun grantNotifications() {
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @Test
    fun `channel is created with default importance`() {
        NotificationHelper.createChannel(context)

        val channel = manager.getNotificationChannel("weather_updates")
        assertNotNull(channel)
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, channel.importance)
    }

    @Test
    fun `notification contains rounded temperature and description`() {
        NotificationHelper.createChannel(context)

        NotificationHelper.showWeatherNotification(context, response)

        val posted = shadowOf(manager).allNotifications.single()
        val title = posted.extras.getString(Notification.EXTRA_TITLE)
        val text = posted.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

        assertTrue("title should contain 12, was $title", title?.contains("12") == true)
        assertTrue("text should not be empty", !text.isNullOrBlank())
        assertEquals("weather_updates", posted.channelId)
    }

    @Test
    fun `nothing is posted without post notifications permission`() {
        shadowOf(context as Application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        NotificationHelper.createChannel(context)

        NotificationHelper.showWeatherNotification(context, response)

        assertTrue(shadowOf(manager).allNotifications.isEmpty())
    }

    @Test
    fun `response without current block is ignored`() {
        NotificationHelper.createChannel(context)

        NotificationHelper.showWeatherNotification(context, response.copy(current = null))

        assertTrue(shadowOf(manager).allNotifications.isEmpty())
    }

    @Test
    fun `notification has pending intent back to the app`() {
        NotificationHelper.createChannel(context)

        NotificationHelper.showWeatherNotification(context, response)

        val posted = shadowOf(manager).allNotifications.single()
        assertNotNull(posted.contentIntent)
    }
}
