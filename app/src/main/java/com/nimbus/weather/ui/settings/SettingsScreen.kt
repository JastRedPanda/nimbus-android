package com.nimbus.weather.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nimbus.weather.R
import com.nimbus.weather.util.ThemeMode
import com.nimbus.weather.util.TemperatureUnit

    @Composable
    private fun SectionHeader(title: String) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onCitySearchClick: () -> Unit,
    onWidgetCustomizeClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.reset_settings_confirm_title)) },
            text = { Text(stringResource(R.string.reset_settings_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    viewModel.resetSettings()
                }) {
                    Text(stringResource(R.string.reset_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Widget temperature mode
            SectionHeader(title = stringResource(R.string.widget_temp_mode))

            SettingsToggle(
                label = stringResource(R.string.actual_temp),
                checked = !state.useFeelsLike,
                onCheck = { viewModel.setUseFeelsLike(false) }
            )
            SettingsToggle(
                label = stringResource(R.string.feels_like_temp),
                checked = state.useFeelsLike,
                onCheck = { viewModel.setUseFeelsLike(true) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Temperature units
            SectionHeader(title = stringResource(R.string.units_temperature))

            SettingsToggle(
                label = stringResource(R.string.celsius),
                checked = state.tempUnit == TemperatureUnit.CELSIUS,
                onCheck = { viewModel.setTempUnit(TemperatureUnit.CELSIUS) }
            )
            SettingsToggle(
                label = stringResource(R.string.fahrenheit),
                checked = state.tempUnit == TemperatureUnit.FAHRENHEIT,
                onCheck = { viewModel.setTempUnit(TemperatureUnit.FAHRENHEIT) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Theme
            SectionHeader(title = stringResource(R.string.theme_mode))

            SettingsToggle(
                label = stringResource(R.string.theme_system),
                checked = state.themeMode == ThemeMode.SYSTEM,
                onCheck = { viewModel.setThemeMode(ThemeMode.SYSTEM) }
            )
            SettingsToggle(
                label = stringResource(R.string.theme_light),
                checked = state.themeMode == ThemeMode.LIGHT,
                onCheck = { viewModel.setThemeMode(ThemeMode.LIGHT) }
            )
            SettingsToggle(
                label = stringResource(R.string.theme_dark),
                checked = state.themeMode == ThemeMode.DARK,
                onCheck = { viewModel.setThemeMode(ThemeMode.DARK) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Notifications
            SectionHeader(title = stringResource(R.string.notifications))

            SettingsToggle(
                label = stringResource(R.string.notifications_enabled),
                checked = state.notificationsEnabled,
                onCheck = { viewModel.setNotificationsEnabled(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Update interval
            SectionHeader(title = stringResource(R.string.update_interval))

            SettingsToggle(
                label = stringResource(R.string.interval_2h),
                checked = state.updateIntervalHours == 2,
                onCheck = { viewModel.setUpdateIntervalHours(2) }
            )
            SettingsToggle(
                label = stringResource(R.string.interval_12h),
                checked = state.updateIntervalHours == 12,
                onCheck = { viewModel.setUpdateIntervalHours(12) }
            )
            SettingsToggle(
                label = stringResource(R.string.interval_24h),
                checked = state.updateIntervalHours == 24,
                onCheck = { viewModel.setUpdateIntervalHours(24) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Hourly forecast interval
            SectionHeader(title = stringResource(R.string.hourly_interval))

            SettingsToggle(
                label = stringResource(R.string.hourly_interval_1h),
                checked = state.hourlyIntervalHours == 1,
                onCheck = { viewModel.setHourlyIntervalHours(1) }
            )
            SettingsToggle(
                label = stringResource(R.string.hourly_interval_3h),
                checked = state.hourlyIntervalHours == 3,
                onCheck = { viewModel.setHourlyIntervalHours(3) }
            )
            SettingsToggle(
                label = stringResource(R.string.hourly_interval_6h),
                checked = state.hourlyIntervalHours == 6,
                onCheck = { viewModel.setHourlyIntervalHours(6) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Air quality
            SectionHeader(title = stringResource(R.string.air_quality))

            SettingsToggle(
                label = stringResource(R.string.show_aqi),
                checked = state.showAqi,
                onCheck = { viewModel.setShowAqi(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Language
            SectionHeader(title = stringResource(R.string.language))

            SettingsToggle(
                label = stringResource(R.string.language_auto),
                checked = state.appLanguage == "auto",
                onCheck = { viewModel.setAppLanguage("auto") }
            )
            SettingsToggle(
                label = stringResource(R.string.language_en),
                checked = state.appLanguage == "en",
                onCheck = { viewModel.setAppLanguage("en") }
            )
            SettingsToggle(
                label = stringResource(R.string.language_uk),
                checked = state.appLanguage == "uk",
                onCheck = { viewModel.setAppLanguage("uk") }
            )
            SettingsToggle(
                label = stringResource(R.string.language_ru),
                checked = state.appLanguage == "ru",
                onCheck = { viewModel.setAppLanguage("ru") }
            )
            SettingsToggle(
                label = stringResource(R.string.language_cs),
                checked = state.appLanguage == "cs",
                onCheck = { viewModel.setAppLanguage("cs") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Widgets
            SectionHeader(title = stringResource(R.string.widgets))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onWidgetCustomizeClick)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.widget_customize),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // City selection
            SectionHeader(title = stringResource(R.string.select_city))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCitySearchClick)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val langCode = if (state.appLanguage == "auto") {
                    com.nimbus.weather.util.LanguageHelper.resolveLocale().language
                } else {
                    state.appLanguage
                }
                Text(
                    text = com.nimbus.weather.util.CityNameResolver.displayName(
                        state.cityName,
                        state.cityLocalNames,
                        langCode
                    ),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Favourite cities
            SectionHeader(title = stringResource(R.string.favourite_cities))

            if (state.favouriteCities.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_favourite_cities),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                state.favouriteCities.forEach { city ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.favouriteDisplayNames[city.name] ?: city.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        IconButton(onClick = { viewModel.moveFavouriteCity(city.name, up = true) }) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.moveFavouriteCity(city.name, up = false) }) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.removeFavouriteCity(city.name) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.reset_settings))
            }
        }
    }
}

@Composable
private fun SettingsToggle(
    label: String,
    checked: Boolean,
    onCheck: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheck(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        Switch(checked = checked, onCheckedChange = onCheck)
    }
}
