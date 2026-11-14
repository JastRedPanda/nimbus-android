package com.nimbus.weather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nimbus.weather.data.model.AirQualityCurrent
import com.nimbus.weather.data.model.CurrentWeather
import com.nimbus.weather.ui.theme.NimbusWeatherTheme
import com.nimbus.weather.ui.theme.SkyPalette
import com.nimbus.weather.util.TemperatureUnit

private val previewCurrent = CurrentWeather(
    time = "2026-11-14T12:00",
    temperature = 12.4,
    humidity = 68.0,
    apparentTemperature = 10.1,
    precipitation = 0.2,
    weatherCode = 61,
    pressure = 1008.0,
    windSpeed = 4.7,
    windDirection = 220.0,
    windGusts = 9.3,
    uvIndex = 1.6
)

private val previewAqi = AirQualityCurrent(
    europeanAqi = 42.0,
    usAqi = 45.0,
    pm25 = 12.0,
    pm10 = 21.0,
    nitrogenDioxide = 9.0,
    sulphurDioxide = 2.0,
    carbonMonoxide = 180.0,
    ozone = 54.0
)

private val previewHourly = List(8) { index ->
    HourlyForecastData(
        time = "2026-11-14T${(12 + index).toString().padStart(2, '0')}:00",
        temperature = 11.0 + index * 0.4,
        precipitation = if (index % 3 == 0) 0.3 else 0.0,
        weatherCode = if (index % 3 == 0) 61 else 3,
        windSpeed = 4.0 + index * 0.2,
        windDirection = 220.0,
        humidity = 65.0,
        apparentTemperature = 9.5 + index * 0.3,
        uvIndex = 1.2
    )
}

private val previewDay = DailyForecastData(
    date = "2026-11-14",
    weatherCode = 61,
    tempMax = 13.0,
    tempMin = 5.0,
    feelsLikeMax = 11.0,
    feelsLikeMin = 3.0,
    sunrise = "2026-11-14T07:12",
    sunset = "2026-11-14T16:28",
    precipitation = 2.4,
    precipProbability = 70,
    windMax = 7.0,
    windGusts = 14.0,
    windDirection = 220.0,
    uvIndexMax = 1.4
)

@Composable
private fun PreviewBackground(weatherCode: Int = 3, isDay: Boolean = true, content: @Composable () -> Unit) {
    NimbusWeatherTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SkyPalette.resolveSkyBrush(weatherCode, isDay))
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            content()
        }
    }
}

@Preview(name = "Current, day", showBackground = true, widthDp = 360)
@Composable
private fun CurrentWeatherCardPreview() {
    PreviewBackground {
        CurrentWeatherCard(
            current = previewCurrent,
            cityName = "Киев",
            sunrise = "2026-11-14T07:12",
            sunset = "2026-11-14T16:28",
            isDay = true,
            modifier = Modifier.width(328.dp)
        )
    }
}

@Preview(name = "Current, night, °F", showBackground = true, widthDp = 360, locale = "en")
@Composable
private fun CurrentWeatherCardNightPreview() {
    PreviewBackground(weatherCode = 0, isDay = false) {
        CurrentWeatherCard(
            current = previewCurrent.copy(weatherCode = 0, temperature = 4.0),
            cityName = "Kyiv",
            sunrise = "2026-11-14T07:12",
            sunset = "2026-11-14T16:28",
            tempUnit = TemperatureUnit.FAHRENHEIT,
            isDay = false,
            modifier = Modifier.width(328.dp)
        )
    }
}

@Preview(name = "Hourly", showBackground = true, widthDp = 360)
@Composable
private fun HourlyForecastBarPreview() {
    PreviewBackground {
        HourlyForecastBar(
            hourly = previewHourly,
            sunrise = "2026-11-14T07:12",
            sunset = "2026-11-14T16:28",
            modifier = Modifier.width(328.dp)
        )
    }
}

@Preview(name = "Daily", showBackground = true, widthDp = 360)
@Composable
private fun DailyForecastCardPreview() {
    PreviewBackground {
        DailyForecastCard(day = previewDay, modifier = Modifier.width(328.dp))
    }
}

@Preview(name = "Week", showBackground = true, widthDp = 360)
@Composable
private fun WeeklyForecastBarPreview() {
    PreviewBackground {
        WeeklyForecastBar(
            days = List(7) { index -> previewDay.copy(date = "2026-11-${14 + index}") },
            modifier = Modifier.width(328.dp)
        )
    }
}

@Preview(name = "AQI", showBackground = true, widthDp = 360)
@Composable
private fun AqiCardPreview() {
    PreviewBackground {
        AqiCard(aqi = previewAqi, modifier = Modifier.width(328.dp))
    }
}
