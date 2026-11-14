package com.nimbus.weather.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.nimbus.weather.data.model.AirQualityCurrent
import com.nimbus.weather.ui.theme.NimbusWeatherTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose-тест без устройства: Robolectric поднимает реальную композицию.
 * SDK 36 в Robolectric требует Java 21, поэтому 35.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AqiCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `shows european aqi value and pollutants`() {
        composeRule.setContent {
            NimbusWeatherTheme {
                AqiCard(
                    aqi = AirQualityCurrent(
                        europeanAqi = 42.0,
                        usAqi = 45.0,
                        pm25 = 12.0,
                        pm10 = 21.0,
                        ozone = 54.0
                    )
                )
            }
        }

        composeRule.onNodeWithText("42").assertIsDisplayed()
        composeRule.onNodeWithText("PM2.5").assertIsDisplayed()
        composeRule.onNodeWithText("PM10").assertIsDisplayed()
    }

    @Test
    fun `falls back to us aqi when european is missing`() {
        composeRule.setContent {
            NimbusWeatherTheme {
                AqiCard(aqi = AirQualityCurrent(usAqi = 77.0))
            }
        }

        composeRule.onNodeWithText("77").assertIsDisplayed()
    }
}
