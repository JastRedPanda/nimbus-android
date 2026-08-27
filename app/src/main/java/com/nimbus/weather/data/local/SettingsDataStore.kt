package com.nimbus.weather.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nimbus.weather.util.LanguageHelper
import com.nimbus.weather.util.ThemeMode
import com.nimbus.weather.util.TemperatureUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        private val KEY_USE_FEELS_LIKE = booleanPreferencesKey("use_feels_like")
        private val KEY_TEMP_UNIT = stringPreferencesKey("temp_unit")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val KEY_CITY_NAME = stringPreferencesKey("city_name")
        private val KEY_LATITUDE = doublePreferencesKey("latitude")
        private val KEY_LONGITUDE = doublePreferencesKey("longitude")
        private val KEY_TIMEZONE = stringPreferencesKey("timezone")
        private val KEY_CITY_LOCAL_NAMES = stringPreferencesKey("city_local_names")
        private val KEY_UPDATE_INTERVAL_HOURS = intPreferencesKey("update_interval_hours")
        private val KEY_FAVOURITE_CITIES = stringPreferencesKey("favourite_cities")
        private val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        private val KEY_WIDGET_BG_COLOR = stringPreferencesKey("widget_bg_color")
        private val KEY_WIDGET_BG_ALPHA = intPreferencesKey("widget_bg_alpha")
        private val KEY_WIDGET_TEXT_COLOR = stringPreferencesKey("widget_text_color")
        private val KEY_WIDGET_DATE_FORMAT = stringPreferencesKey("widget_date_format")
        private val KEY_WIDGET_FONT_SCALE = intPreferencesKey("widget_font_scale")
        private val KEY_HOURLY_INTERVAL_HOURS = intPreferencesKey("hourly_interval_hours")
        private val KEY_SHOW_AQI = booleanPreferencesKey("show_aqi")
        private val KEY_RECENT_CITIES = stringPreferencesKey("recent_cities")

        const val DEFAULT_CITY = "Киев"
        const val DEFAULT_LAT = 50.4501
        const val DEFAULT_LON = 30.5234
        const val DEFAULT_TZ = "Europe/Kiev"
        const val DEFAULT_UPDATE_INTERVAL_HOURS = 2
        const val DEFAULT_HOURLY_INTERVAL_HOURS = 1
        const val DEFAULT_WIDGET_BG_ALPHA = 100
        const val DEFAULT_WIDGET_DATE_FORMAT = "numeric"
        const val DEFAULT_WIDGET_FONT_SCALE = 100
        const val MIN_WIDGET_FONT_SCALE = 50
        const val MAX_WIDGET_FONT_SCALE = 200
        const val MAX_RECENT_CITIES = 5
    }

    private inline fun <reified T> decode(raw: String?, fallback: T): T {
        if (raw.isNullOrBlank()) return fallback
        return try { json.decodeFromString<T>(raw) } catch (_: Exception) { fallback }
    }

    private fun Preferences.cities(key: Preferences.Key<String>): List<FavouriteCity> =
        decode(this[key], emptyList())

    private fun List<FavouriteCity>.upsert(city: FavouriteCity, atTop: Boolean = false): List<FavouriteCity> =
        buildList {
            addAll(this@upsert.filterNot { it.name == city.name })
            if (atTop) add(0, city) else add(city)
        }.take(if (atTop) MAX_RECENT_CITIES else size)

    private suspend fun updateCityList(
        key: Preferences.Key<String>,
        transform: (List<FavouriteCity>) -> List<FavouriteCity>
    ) {
        context.dataStore.edit { prefs ->
            prefs[key] = json.encodeToString(transform(prefs.cities(key)))
        }
    }

    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_DONE] ?: false
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        ThemeMode.valueOf(prefs[KEY_THEME_MODE] ?: "SYSTEM")
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_NOTIFICATIONS_ENABLED] ?: true
    }

    val useFeelsLike: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_USE_FEELS_LIKE] ?: false
    }

    val tempUnit: Flow<TemperatureUnit> = context.dataStore.data.map { prefs ->
        TemperatureUnit.valueOf(prefs[KEY_TEMP_UNIT] ?: "CELSIUS")
    }

    val cityName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CITY_NAME] ?: DEFAULT_CITY
    }

    val latitude: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[KEY_LATITUDE] ?: DEFAULT_LAT
    }

    val longitude: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[KEY_LONGITUDE] ?: DEFAULT_LON
    }

    val timezone: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_TIMEZONE] ?: DEFAULT_TZ
    }

    val updateIntervalHours: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_UPDATE_INTERVAL_HOURS] ?: DEFAULT_UPDATE_INTERVAL_HOURS
    }

    val appLanguage: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_APP_LANGUAGE] ?: LanguageHelper.AUTO
    }

    val cityLocalNames: Flow<Map<String, String>> = context.dataStore.data.map { prefs ->
        decode(prefs[KEY_CITY_LOCAL_NAMES], emptyMap())
    }

    val widgetBgColor: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_WIDGET_BG_COLOR]
    }

    val widgetBgAlpha: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_WIDGET_BG_ALPHA] ?: DEFAULT_WIDGET_BG_ALPHA
    }

    val widgetTextColor: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_WIDGET_TEXT_COLOR] ?: "auto"
    }

    val widgetDateFormat: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_WIDGET_DATE_FORMAT] ?: DEFAULT_WIDGET_DATE_FORMAT
    }

    val widgetFontScale: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_WIDGET_FONT_SCALE] ?: DEFAULT_WIDGET_FONT_SCALE
    }

    val hourlyIntervalHours: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_HOURLY_INTERVAL_HOURS] ?: DEFAULT_HOURLY_INTERVAL_HOURS
    }

    val showAqi: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SHOW_AQI] ?: true
    }

    suspend fun setOnboardingDone() {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_DONE] = true
        }
    }

    suspend fun resetAll() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }

    suspend fun setUpdateIntervalHours(hours: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_UPDATE_INTERVAL_HOURS] = hours
        }
    }

    suspend fun setAppLanguage(language: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_APP_LANGUAGE] = language
        }
    }

    suspend fun setWidgetBgColor(colorHex: String?) {
        context.dataStore.edit { prefs ->
            if (colorHex == null) prefs.remove(KEY_WIDGET_BG_COLOR)
            else prefs[KEY_WIDGET_BG_COLOR] = colorHex
        }
    }

    suspend fun setWidgetBgAlpha(alpha: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WIDGET_BG_ALPHA] = alpha.coerceIn(0, 100)
        }
    }

    suspend fun setWidgetTextColor(option: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WIDGET_TEXT_COLOR] = option
        }
    }

    suspend fun setWidgetDateFormat(format: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WIDGET_DATE_FORMAT] = format
        }
    }

    suspend fun setWidgetFontScale(scale: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WIDGET_FONT_SCALE] = scale.coerceIn(
                MIN_WIDGET_FONT_SCALE, MAX_WIDGET_FONT_SCALE
            )
        }
    }

    suspend fun setHourlyIntervalHours(hours: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HOURLY_INTERVAL_HOURS] = hours
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setShowAqi(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SHOW_AQI] = show
        }
    }

    suspend fun setUseFeelsLike(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USE_FEELS_LIKE] = value
        }
    }

    suspend fun setTempUnit(unit: TemperatureUnit) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TEMP_UNIT] = unit.name
        }
    }

    suspend fun setLocation(name: String, lat: Double, lon: Double, tz: String, localNames: Map<String, String> = emptyMap()) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CITY_NAME] = name
            prefs[KEY_LATITUDE] = lat
            prefs[KEY_LONGITUDE] = lon
            prefs[KEY_TIMEZONE] = tz
            if (localNames.isNotEmpty()) {
                prefs[KEY_CITY_LOCAL_NAMES] = json.encodeToString(localNames)
            }
        }
    }

    suspend fun updateCityTranslations(name: String, translations: Map<String, String>) {
        context.dataStore.edit { prefs ->
            if (prefs[KEY_CITY_NAME] == name) {
                val current = decode<Map<String, String>>(prefs[KEY_CITY_LOCAL_NAMES], emptyMap())
                prefs[KEY_CITY_LOCAL_NAMES] = json.encodeToString(current + translations)
            }
            for (key in listOf(KEY_FAVOURITE_CITIES, KEY_RECENT_CITIES)) {
                val cities = prefs.cities(key)
                var changed = false
                val updated = cities.map { city ->
                    if (city.name == name) {
                        changed = true
                        city.copy(localNames = city.localNames + translations)
                    } else city
                }
                if (changed) prefs[key] = json.encodeToString(updated)
            }
        }
    }

    suspend fun getLocationSnapshot(): LocationSnapshot {
        val prefs = context.dataStore.data.first()
        return LocationSnapshot(
            name = prefs[KEY_CITY_NAME] ?: DEFAULT_CITY,
            lat = prefs[KEY_LATITUDE] ?: DEFAULT_LAT,
            lon = prefs[KEY_LONGITUDE] ?: DEFAULT_LON,
            tz = prefs[KEY_TIMEZONE] ?: DEFAULT_TZ,
            localNames = decode(prefs[KEY_CITY_LOCAL_NAMES], emptyMap())
        )
    }

    data class LocationSnapshot(
        val name: String = DEFAULT_CITY,
        val lat: Double = DEFAULT_LAT,
        val lon: Double = DEFAULT_LON,
        val tz: String = DEFAULT_TZ,
        val localNames: Map<String, String> = emptyMap()
    )

    /**
     * Читает все поля, нужные для загрузки главного экрана, одним запросом к DataStore.
     * Заменяет ~8 последовательных .first() в HomeViewModel.performLoad.
     */
    data class HomeSettings(
        val cityName: String = DEFAULT_CITY,
        val lat: Double = DEFAULT_LAT,
        val lon: Double = DEFAULT_LON,
        val tz: String = DEFAULT_TZ,
        val localNames: Map<String, String> = emptyMap(),
        val updateIntervalHours: Int = DEFAULT_UPDATE_INTERVAL_HOURS,
        val tempUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
        val useFeelsLike: Boolean = false,
        val hourlyIntervalHours: Int = DEFAULT_HOURLY_INTERVAL_HOURS,
        val notificationsEnabled: Boolean = true,
        val appLanguage: String = LanguageHelper.AUTO,
        val showAqi: Boolean = true
    )

    suspend fun getHomeSettings(): HomeSettings {
        val prefs = context.dataStore.data.first()
        return HomeSettings(
            cityName = prefs[KEY_CITY_NAME] ?: DEFAULT_CITY,
            lat = prefs[KEY_LATITUDE] ?: DEFAULT_LAT,
            lon = prefs[KEY_LONGITUDE] ?: DEFAULT_LON,
            tz = prefs[KEY_TIMEZONE] ?: DEFAULT_TZ,
            localNames = decode(prefs[KEY_CITY_LOCAL_NAMES], emptyMap()),
            updateIntervalHours = prefs[KEY_UPDATE_INTERVAL_HOURS] ?: DEFAULT_UPDATE_INTERVAL_HOURS,
            tempUnit = decode(prefs[KEY_TEMP_UNIT], TemperatureUnit.CELSIUS),
            useFeelsLike = prefs[KEY_USE_FEELS_LIKE] ?: false,
            hourlyIntervalHours = prefs[KEY_HOURLY_INTERVAL_HOURS] ?: DEFAULT_HOURLY_INTERVAL_HOURS,
            notificationsEnabled = prefs[KEY_NOTIFICATIONS_ENABLED] ?: true,
            appLanguage = prefs[KEY_APP_LANGUAGE] ?: LanguageHelper.AUTO,
            showAqi = prefs[KEY_SHOW_AQI] ?: true
        )
    }

    @Serializable
    data class FavouriteCity(
        val name: String,
        val lat: Double,
        val lon: Double,
        val tz: String,
        val localNames: Map<String, String> = emptyMap()
    )

    val favouriteCities: Flow<List<FavouriteCity>> = context.dataStore.data.map { prefs ->
        prefs.cities(KEY_FAVOURITE_CITIES)
    }

    suspend fun addFavouriteCity(city: FavouriteCity) =
        updateCityList(KEY_FAVOURITE_CITIES) { it.upsert(city) }

    suspend fun removeFavouriteCity(name: String) =
        updateCityList(KEY_FAVOURITE_CITIES) { list -> list.filterNot { it.name == name } }

    suspend fun setFavouriteCities(cities: List<FavouriteCity>) =
        updateCityList(KEY_FAVOURITE_CITIES) { cities }

    val recentCities: Flow<List<FavouriteCity>> = context.dataStore.data.map { prefs ->
        prefs.cities(KEY_RECENT_CITIES)
    }

    suspend fun addRecentCity(city: FavouriteCity) =
        updateCityList(KEY_RECENT_CITIES) { it.upsert(city, atTop = true) }

    suspend fun removeRecentCity(name: String) =
        updateCityList(KEY_RECENT_CITIES) { list -> list.filterNot { it.name == name } }
}
