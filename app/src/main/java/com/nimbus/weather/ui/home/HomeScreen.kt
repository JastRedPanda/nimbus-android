package com.nimbus.weather.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
import com.nimbus.weather.ui.theme.AnimatedSky
import com.nimbus.weather.ui.theme.GlassRainOverlay
import com.nimbus.weather.ui.theme.LocalGlassDark
import com.nimbus.weather.ui.theme.LocalSkyDark
import com.nimbus.weather.ui.theme.SkyEffect
import com.nimbus.weather.ui.theme.SkyPalette
import com.nimbus.weather.ui.theme.skyEffectFor
import com.nimbus.weather.ui.theme.skyTextColors
import com.nimbus.weather.util.formatUpdateTime
import com.nimbus.weather.util.isDayNowByTime
import kotlin.math.abs

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

    // Фон-небо под всем экраном, включая топбар.
    // День/ночь — по системным часам, а не по времени замера погоды:
    // иначе при несвежих данных утром показывается ночь.
    val isDay = state.current?.let {
        isDayNowByTime(java.time.LocalTime.now(), state.sunrise, state.sunset)
    } ?: false
    val weatherCode = state.current?.weatherCode ?: 0
    val skyBrush = SkyPalette.resolveSkyBrush(weatherCode, isDay)
    val skyDark = SkyPalette.isDarkSky(weatherCode, isDay)
    // Живой фон — одним тумблером в настройках, иначе статичный градиент.
    val skyAnimated = state.skyAnimationEnabled

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(skyBrush)
    ) {
    if (skyAnimated) {
        AnimatedSky(weatherCode = weatherCode, isDay = isDay)
        val effect = skyEffectFor(weatherCode = weatherCode, isDay = isDay)
        if (effect == SkyEffect.RAIN || effect == SkyEffect.THUNDER) {
            GlassRainOverlay()
        }
    }
    val t = skyTextColors(skyDark)
    // Тонировка стекла — от темы приложения (яркость surface),
    // а не от неба: тёмная тема — тёмное стекло, светлая — белое.
    val glassDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    CompositionLocalProvider(
        LocalSkyDark provides skyDark,
        LocalGlassDark provides glassDark
    ) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.app_name),
                        color = t.title
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = t.title
                        )
                    }
                }
            )
        }
    ) { padding ->
        // Свайп влево/вправо по контенту открывает выбор города.
        var cityPickerOpen by rememberSaveable { mutableStateOf(false) }
        val density = LocalDensity.current
        val swipeThresholdPx = remember { with(density) { 120.dp.toPx() } }
        if (cityPickerOpen && state.favouriteCities.isNotEmpty()) {
            CityPickerDialog(
                cities = state.favouriteCities,
                displayNames = state.favouriteDisplayNames,
                currentCity = state.cityName,
                onPick = {
                    cityPickerOpen = false
                    viewModel.switchToCity(it)
                },
                onDismiss = { cityPickerOpen = false }
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(swipeThresholdPx) {
                    var acc = 0f
                    var fired = false
                    detectHorizontalDragGestures(
                        onDragStart = { acc = 0f; fired = false },
                        onDragEnd = { acc = 0f; fired = false },
                        onDragCancel = { acc = 0f; fired = false },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            if (!fired) {
                                acc += amount
                                if (abs(acc) > swipeThresholdPx) {
                                    fired = true
                                    if (viewModel.state.value.favouriteCities.isNotEmpty()) {
                                        cityPickerOpen = true
                                    }
                                }
                            }
                        }
                    )
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (state.lastWeatherUpdateMillis > 0L) {
                    val updated = remember(state.lastWeatherUpdateMillis) {
                        formatUpdateTime(state.lastWeatherUpdateMillis)
                    }
                    Text(
                        text = stringResource(R.string.last_weather_update, updated),
                        style = MaterialTheme.typography.bodySmall,
                        color = t.subtle,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
                    )
                }
                PullToRefreshBox(
                    isRefreshing = state.refreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                when {
                    state.loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = t.title
                        )
                    }
                    state.error != null -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.error_loading),
                                color = t.title
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { viewModel.loadWeather() }) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }
                    else -> {
                                    WeatherContent(state = state, isDay = isDay)
                    }
                }
            }
        }
    }
    }
    }
    }
}

@Composable
private fun WeatherContent(
    state: HomeUiState,
    isDay: Boolean
) {
    val aqi = state.aqi

    AnimatedVisibility(visible = true, enter = fadeIn()) {
        state.current?.let { current ->
            val config = LocalConfiguration.current
            val isTablet = config.screenWidthDp >= 600

            if (isTablet) {
                TabletLayout(state, current, aqi, isDay)
            } else {
                PhoneLayout(state, current, aqi, isDay)
            }
        }
    }
}

@Composable
private fun TabletLayout(
    state: HomeUiState,
    current: com.nimbus.weather.data.model.CurrentWeather,
    aqi: com.nimbus.weather.data.model.AirQualityCurrent?,
    isDay: Boolean
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CurrentWeatherCard(
                    current = current,
                    cityName = state.cityName,
                    sunrise = state.sunrise,
                    sunset = state.sunset,
                    tempUnit = state.tempUnit,
                    isDay = isDay,
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.hourly.isNotEmpty()) {
                    HourlyForecastBar(
                        hourly = state.hourly,
                        tempUnit = state.tempUnit,
                        sunrise = state.sunrise,
                        sunset = state.sunset,
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
                    style = MaterialTheme.typography.titleMedium,
                    color = skyTextColors(LocalSkyDark.current).title
                )
                state.daily.forEach { day ->
                    DailyForecastCard(day = day, tempUnit = state.tempUnit)
                }
            }
        }
    }
}

@Composable
private fun PhoneLayout(
    state: HomeUiState,
    current: com.nimbus.weather.data.model.CurrentWeather,
    aqi: com.nimbus.weather.data.model.AirQualityCurrent?,
    isDay: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                isDay = isDay,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (state.hourly.isNotEmpty()) {
            item {
                HourlyForecastBar(
                    hourly = state.hourly,
                    tempUnit = state.tempUnit,
                    sunrise = state.sunrise,
                    sunset = state.sunset,
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
                style = MaterialTheme.typography.titleMedium,
                color = skyTextColors(LocalSkyDark.current).title
            )
        }
        items(state.daily) { day ->
            DailyForecastCard(day = day, tempUnit = state.tempUnit)
        }
    }
}

@Composable
private fun CityPickerDialog(
    cities: List<FavouriteCity>,
    displayNames: Map<String, String>,
    currentCity: String,
    onPick: (FavouriteCity) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.favourite_cities)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cities.forEach { city ->
                    val isCurrent = city.name == currentCity
                    if (isCurrent) {
                        Button(
                            onClick = { onPick(city) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(displayNames[city.name] ?: city.name)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onPick(city) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(displayNames[city.name] ?: city.name)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
