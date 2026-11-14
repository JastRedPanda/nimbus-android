package com.nimbus.weather.ui.location

import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.data.model.GeocodingResult
import com.nimbus.weather.data.repository.WeatherRepository
import com.nimbus.weather.service.WeatherEffects
import com.nimbus.weather.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class LocationSearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settings = mockk<SettingsDataStore>()
    private val repository = mockk<WeatherRepository>()
    private val effects = mockk<WeatherEffects>(relaxed = true)

    private val kyiv = GeocodingResult(id = 1, name = "Kyiv", latitude = 50.45, longitude = 30.52)

    @Before
    fun setup() {
        every { settings.favouriteCities } returns flowOf(emptyList())
        every { settings.recentCities } returns flowOf(emptyList())
        every { settings.appLanguage } returns flowOf("ru")
        coEvery { repository.searchCitiesWithFallback(any(), any()) } returns listOf(kyiv)
    }

    @Test
    fun `search is debounced`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = LocationSearchViewModel(settings, repository, effects)

        viewModel.onQueryChanged("Ky")
        advanceTimeBy(299)
        coVerify(exactly = 0) { repository.searchCitiesWithFallback(any(), any()) }

        advanceTimeBy(2)
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.searchCitiesWithFallback("Ky", "ru") }
        assertEquals(listOf(kyiv), viewModel.state.value.results)
    }

    @Test
    fun `short query clears results without network`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = LocationSearchViewModel(settings, repository, effects)

        viewModel.onQueryChanged("K")
        advanceUntilIdle()

        assertTrue(viewModel.state.value.results.isEmpty())
        assertFalse(viewModel.state.value.loading)
        coVerify(exactly = 0) { repository.searchCitiesWithFallback(any(), any()) }
    }

    @Test
    fun `network failure marks no results`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { repository.searchCitiesWithFallback(any(), any()) } throws IOException("offline")
        val viewModel = LocationSearchViewModel(settings, repository, effects)

        viewModel.onQueryChanged("Kyiv")
        advanceUntilIdle()

        assertTrue(viewModel.state.value.noResults)
        assertFalse(viewModel.state.value.loading)
    }

    @Test
    fun `new query cancels previous search`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = LocationSearchViewModel(settings, repository, effects)

        viewModel.onQueryChanged("Ky")
        advanceTimeBy(100)
        viewModel.onQueryChanged("Kyiv")
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.searchCitiesWithFallback(any(), any()) }
        coVerify { repository.searchCitiesWithFallback("Kyiv", "ru") }
    }
}
