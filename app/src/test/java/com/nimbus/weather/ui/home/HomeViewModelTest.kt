package com.nimbus.weather.ui.home

import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.data.model.AirQualityCurrent
import com.nimbus.weather.data.model.AirQualityResponse
import com.nimbus.weather.data.model.CurrentWeather
import com.nimbus.weather.data.model.WeatherResponse
import com.nimbus.weather.data.repository.WeatherRepository
import com.nimbus.weather.service.WeatherEffects
import com.nimbus.weather.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settings = mockk<SettingsDataStore>()
    private val repository = mockk<WeatherRepository>()
    private val effects = mockk<WeatherEffects>(relaxed = true)

    private val uiSettingsFlow = MutableStateFlow(SettingsDataStore.UiSettings())

    private var homeSettings = SettingsDataStore.HomeSettings(
        cityName = "Киев",
        lat = 50.4501,
        lon = 30.5234,
        updateIntervalHours = 2,
        hourlyIntervalHours = 1,
        notificationsEnabled = true,
        appLanguage = "auto",
        showAqi = true
    )

    private val sampleResponse = WeatherResponse(
        latitude = 50.4501,
        longitude = 30.5234,
        timezone = "Europe/Kiev",
        current = CurrentWeather(
            time = "2026-11-14T09:00",
            temperature = 21.5,
            humidity = 50.0,
            apparentTemperature = 20.0,
            precipitation = 0.0,
            weatherCode = 0,
            pressure = 1013.0,
            windSpeed = 3.0,
            windDirection = 180.0,
            windGusts = 5.0,
            uvIndex = 2.0
        )
    )

    @Before
    fun setup() {
        every { settings.uiSettings } returns uiSettingsFlow
        every { settings.appLanguage } returns flowOf("auto")
        every { settings.favouriteCities } returns flowOf(emptyList())
        coEvery { settings.getLocationSnapshot() } returns SettingsDataStore.LocationSnapshot()
        coEvery { settings.getHomeSettings() } answers { homeSettings }
        every { repository.setTtlHours(any()) } just runs
        every { repository.showingCachedWeather } returns false
        coEvery { repository.translateCityName(any(), any(), any(), any()) } returns null
    }

    @Test
    fun `loads weather into state`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { repository.getWeather(any(), any(), any()) } returns sampleResponse
        coEvery { repository.getAirQuality(any(), any(), any()) } returns
            AirQualityResponse(current = AirQualityCurrent(europeanAqi = 42.0))

        val viewModel = HomeViewModel(settings, repository, effects)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(21.5, state.current?.temperature ?: 0.0, 0.01)
        assertEquals("Киев", state.cityName)
        assertEquals(42.0, state.aqi?.europeanAqi ?: 0.0, 0.01)
        assertFalse(state.loading)
        assertNull(state.error)
        assertFalse(state.fromCache)
        coVerify { effects.onWeatherLoaded(sampleResponse, true) }
    }

    @Test
    fun `network error surfaces and keeps loading off`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { repository.getWeather(any(), any(), any()) } throws IOException("offline")

        val viewModel = HomeViewModel(settings, repository, effects)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.loading)
        assertNull(state.current)
        assertNotNull(state.error)
        coVerify(exactly = 0) { effects.onWeatherLoaded(any(), any()) }
    }

    @Test
    fun `cached fallback is reported`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { repository.getWeather(any(), any(), any()) } returns sampleResponse
        coEvery { repository.getAirQuality(any(), any(), any()) } returns AirQualityResponse()
        every { repository.showingCachedWeather } returns true

        val viewModel = HomeViewModel(settings, repository, effects)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.fromCache)
    }

    @Test
    fun `aqi is skipped when disabled`() = runTest(mainDispatcherRule.testDispatcher) {
        homeSettings = homeSettings.copy(showAqi = false)
        coEvery { repository.getWeather(any(), any(), any()) } returns sampleResponse

        val viewModel = HomeViewModel(settings, repository, effects)
        advanceUntilIdle()

        assertNull(viewModel.state.value.aqi)
        coVerify(exactly = 0) { repository.getAirQuality(any(), any(), any()) }
    }

    @Test
    fun `language change reloads weather`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { repository.getWeather(any(), any(), any()) } returns sampleResponse
        coEvery { repository.getAirQuality(any(), any(), any()) } returns AirQualityResponse()

        val viewModel = HomeViewModel(settings, repository, effects)
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.getWeather(any(), any(), any()) }

        uiSettingsFlow.value = uiSettingsFlow.value.copy(appLanguage = "en")
        advanceUntilIdle()

        coVerify(exactly = 2) { repository.getWeather(any(), any(), any()) }
    }
}
