package com.nimbus.weather.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.text.font.FontWeight
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
    var favouriteListExpanded by rememberSaveable { mutableStateOf(false) }

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
                    if (state.favouriteCities.size > 1) {
                        FavouriteCitiesButton(
                            count = state.favouriteCities.size,
                            expanded = favouriteListExpanded,
                            onClick = { favouriteListExpanded = !favouriteListExpanded }
                        )
                    }
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
            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    state.loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
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
                                    .filter { it.name != state.cityName }
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
                                    onCitySwitch = { viewModel.switchToCity(it) },
                                    favouriteListExpanded = favouriteListExpanded,
                                    onCollapseFavourites = { favouriteListExpanded = false }
                                )
                            }
                        } else {
                            WeatherContent(
                                state = state,
                                onCitySwitch = { viewModel.switchToCity(it) },
                                favouriteListExpanded = favouriteListExpanded,
                                onCollapseFavourites = { favouriteListExpanded = false }
                            )
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
    onCitySwitch: (com.nimbus.weather.data.local.SettingsDataStore.FavouriteCity) -> Unit,
    favouriteListExpanded: Boolean,
    onCollapseFavourites: () -> Unit
) {
    val aqi = state.aqi

    AnimatedVisibility(visible = true, enter = fadeIn()) {
        state.current?.let { current ->
            val config = LocalConfiguration.current
            val isTablet = config.screenWidthDp >= 600

            if (isTablet) {
                TabletLayout(state, current, aqi, onCitySwitch, favouriteListExpanded, onCollapseFavourites)
            } else {
                PhoneLayout(state, current, aqi, onCitySwitch, favouriteListExpanded, onCollapseFavourites)
            }
        }
    }
}

@Composable
private fun TabletLayout(
    state: HomeUiState,
    current: com.nimbus.weather.data.model.CurrentWeather,
    aqi: com.nimbus.weather.data.model.AirQualityCurrent?,
    onCitySwitch: (com.nimbus.weather.data.local.SettingsDataStore.FavouriteCity) -> Unit,
    favouriteListExpanded: Boolean,
    onCollapseFavourites: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (favouriteListExpanded && state.favouriteCities.size > 1) {
            FavouriteCitiesList(
                cities = state.favouriteCities,
                displayNames = state.favouriteDisplayNames,
                currentCity = state.cityName,
                onCityClick = onCitySwitch,
                onCollapse = onCollapseFavourites
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
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
}

@Composable
private fun PhoneLayout(
    state: HomeUiState,
    current: com.nimbus.weather.data.model.CurrentWeather,
    aqi: com.nimbus.weather.data.model.AirQualityCurrent?,
    onCitySwitch: (com.nimbus.weather.data.local.SettingsDataStore.FavouriteCity) -> Unit = {},
    favouriteListExpanded: Boolean = false,
    onCollapseFavourites: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (favouriteListExpanded && state.favouriteCities.size > 1) {
            item {
                FavouriteCitiesList(
                    cities = state.favouriteCities,
                    displayNames = state.favouriteDisplayNames,
                    currentCity = state.cityName,
                    onCityClick = onCitySwitch,
                    onCollapse = onCollapseFavourites
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
private fun FavouriteCitiesButton(
    count: Int,
    expanded: Boolean,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        BadgedBox(
            badge = {
                if (count > 0) {
                    Badge { Text(count.toString()) }
                }
            }
        ) {
            Icon(
                imageVector = Icons.Default.Bookmark,
                contentDescription = stringResource(R.string.favourite_cities),
                tint = if (expanded) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun FavouriteCitiesList(
    cities: List<com.nimbus.weather.data.local.SettingsDataStore.FavouriteCity>,
    displayNames: Map<String, String>,
    currentCity: String,
    onCityClick: (com.nimbus.weather.data.local.SettingsDataStore.FavouriteCity) -> Unit,
    onCollapse: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.favourite_cities),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onCollapse) {
                    Icon(
                        imageVector = Icons.Default.ExpandLess,
                        contentDescription = stringResource(R.string.cancel)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            cities.forEach { city ->
                val displayName = displayNames[city.name] ?: city.name
                val isCurrent = city.name == currentCity
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCityClick(city) }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (isCurrent) {
                        Text(
                            text = stringResource(R.string.today).take(2),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
