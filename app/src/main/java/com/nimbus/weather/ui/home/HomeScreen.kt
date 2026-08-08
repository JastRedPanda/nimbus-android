package com.nimbus.weather.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nimbus.weather.R
import com.nimbus.weather.data.local.SettingsDataStore.FavouriteCity
import com.nimbus.weather.ui.components.AqiCard
import com.nimbus.weather.ui.components.CurrentWeatherCard
import com.nimbus.weather.ui.components.DailyForecastCard
import com.nimbus.weather.ui.components.HourlyForecastBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSettingsClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadWeather()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.error_loading),
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadWeather() }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
                else -> {
                    val pages = remember(state.cityName, state.favouriteCities) {
                        buildList {
                            if (state.cityName.isNotBlank()) {
                                add(FavouriteCity(state.cityName, 0.0, 0.0, ""))
                            }
                            state.favouriteCities
                                .filter { city -> none { it.name == city.name } }
                                .forEach { add(it) }
                        }
                    }

                    if (pages.size > 1) {
                        val pagerState = rememberPagerState(pageCount = { pages.size })
                        val currentIndex = pages.indexOfFirst { it.name == state.cityName }
                            .coerceAtLeast(0)

                        LaunchedEffect(currentIndex) {
                            if (pagerState.currentPage != currentIndex) {
                                pagerState.animateScrollToPage(currentIndex)
                            }
                        }
                        LaunchedEffect(pagerState) {
                            snapshotFlow { pagerState.settledPage }.collect { page ->
                                val city = pages.getOrNull(page) ?: return@collect
                                if (city.name != state.cityName) {
                                    viewModel.switchToCity(city)
                                }
                            }
                        }

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            WeatherContent(
                                state = state,
                                onCitySwitch = { viewModel.switchToCity(it) }
                            )
                        }
                    } else {
                        WeatherContent(
                            state = state,
                            onCitySwitch = { viewModel.switchToCity(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherContent(
    state: HomeUiState,
    onCitySwitch: (com.nimbus.weather.data.local.SettingsDataStore.FavouriteCity) -> Unit
) {
    val aqi = state.aqi

    AnimatedVisibility(visible = true, enter = fadeIn()) {
        state.current?.let { current ->
            val config = LocalConfiguration.current
            val isTablet = config.screenWidthDp >= 600

            if (isTablet) {
                TabletLayout(state, current, aqi, onCitySwitch)
            } else {
                PhoneLayout(state, current, aqi, onCitySwitch)
            }
        }
    }
}

@Composable
private fun TabletLayout(
    state: HomeUiState,
    current: com.nimbus.weather.data.model.CurrentWeather,
    aqi: com.nimbus.weather.data.model.AirQualityCurrent?,
    onCitySwitch: (com.nimbus.weather.data.local.SettingsDataStore.FavouriteCity) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.favouriteCities.size > 1) {
                FavouriteCitiesRow(
                    cities = state.favouriteCities,
                    displayNames = state.favouriteDisplayNames,
                    currentCity = state.cityName,
                    onCityClick = onCitySwitch
                )
            }
            CurrentWeatherCard(
                current = current,
                cityName = state.cityName,
                sunrise = state.sunrise,
                sunset = state.sunset,
                tempUnit = state.tempUnit,
                modifier = Modifier.fillMaxWidth()
            )
            if (state.hourly.isNotEmpty()) {
                HourlyForecastBar(
                    hourly = state.hourly,
                    tempUnit = state.tempUnit,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (aqi != null) {
                AqiCard(aqi = aqi, modifier = Modifier.fillMaxWidth())
            }
            if (state.fromCache) {
                AssistChip(
                    onClick = { },
                    label = { Text(stringResource(R.string.cached_data)) }
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.forecast_7_days),
                style = MaterialTheme.typography.titleMedium
            )
            state.daily.forEach { day ->
                DailyForecastCard(day = day, tempUnit = state.tempUnit)
            }
        }
    }
}

@Composable
private fun PhoneLayout(
    state: HomeUiState,
    current: com.nimbus.weather.data.model.CurrentWeather,
    aqi: com.nimbus.weather.data.model.AirQualityCurrent?,
    onCitySwitch: (com.nimbus.weather.data.local.SettingsDataStore.FavouriteCity) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.favouriteCities.size > 1) {
            item {
                FavouriteCitiesRow(
                    cities = state.favouriteCities,
                    displayNames = state.favouriteDisplayNames,
                    currentCity = state.cityName,
                    onCityClick = onCitySwitch
                )
            }
        }
        if (state.fromCache) {
            item {
                AssistChip(
                    onClick = { },
                    label = { Text(stringResource(R.string.cached_data)) }
                )
            }
        }
        item {
            CurrentWeatherCard(
                current = current,
                cityName = state.cityName,
                sunrise = state.sunrise,
                sunset = state.sunset,
                tempUnit = state.tempUnit,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (state.hourly.isNotEmpty()) {
            item {
                HourlyForecastBar(
                    hourly = state.hourly,
                    tempUnit = state.tempUnit,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (aqi != null) {
            item {
                AqiCard(aqi = aqi, modifier = Modifier.fillMaxWidth())
            }
        }
        item {
            Text(
                text = stringResource(R.string.forecast_7_days),
                style = MaterialTheme.typography.titleMedium
            )
        }
        items(state.daily) { day ->
            DailyForecastCard(day = day, tempUnit = state.tempUnit)
        }
    }
}

@Composable
private fun FavouriteCitiesRow(
    cities: List<com.nimbus.weather.data.local.SettingsDataStore.FavouriteCity>,
    displayNames: Map<String, String>,
    currentCity: String,
    onCityClick: (com.nimbus.weather.data.local.SettingsDataStore.FavouriteCity) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        cities.forEach { city ->
            AssistChip(
                onClick = { onCityClick(city) },
                label = {
                    Text(
                        text = displayNames[city.name] ?: city.name,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            )
        }
    }
}
