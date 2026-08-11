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

    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_DONE] ?: false
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_THEME_MODE]
        if (raw == null) ThemeMode.SYSTEM
        else try { ThemeMode.valueOf(raw) } catch (_: Exception) { ThemeMode.SYSTEM }
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_NOTIFICATIONS_ENABLED] ?: true
    }

    val useFeelsLike: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_USE_FEELS_LIKE] ?: false
    }

    val tempUnit: Flow<TemperatureUnit> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_TEMP_UNIT]
        if (raw == null) TemperatureUnit.CELSIUS
        else try { TemperatureUnit.valueOf(raw) } catch (_: Exception) { TemperatureUnit.CELSIUS }
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
        prefs[KEY_APP_LANGUAGE] ?: "auto"
    }

    val cityLocalNames: Flow<Map<String, String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_CITY_LOCAL_NAMES]?.let {
            try { json.decodeFromString<Map<String, String>>(it) } catch (_: Exception) { emptyMap() }
        } ?: emptyMap()
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
                val current = prefs[KEY_CITY_LOCAL_NAMES]?.let {
                    try { json.decodeFromString<Map<String, String>>(it) } catch (_: Exception) { emptyMap() }
                } ?: emptyMap()
                prefs[KEY_CITY_LOCAL_NAMES] = json.encodeToString(current + translations)
            }
            val favourites = prefs[KEY_FAVOURITE_CITIES]?.let {
                try { json.decodeFromString<List<FavouriteCity>>(it) } catch (_: Exception) { null }
            } ?: emptyList()
            if (favourites.any { it.name == name }) {
                prefs[KEY_FAVOURITE_CITIES] = json.encodeToString(
                    favourites.map { if (it.name == name) it.copy(localNames = it.localNames + translations) else it }
                )
            }
            val recents = prefs[KEY_RECENT_CITIES]?.let {
                try { json.decodeFromString<List<FavouriteCity>>(it) } catch (_: Exception) { null }
            } ?: emptyList()
            if (recents.any { it.name == name }) {
                prefs[KEY_RECENT_CITIES] = json.encodeToString(
                    recents.map { if (it.name == name) it.copy(localNames = it.localNames + translations) else it }
                )
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
            localNames = prefs[KEY_CITY_LOCAL_NAMES]?.let {
                try { json.decodeFromString<Map<String, String>>(it) } catch (_: Exception) { emptyMap() }
            } ?: emptyMap()
        )
    }

    data class LocationSnapshot(
        val name: String = DEFAULT_CITY,
        val lat: Double = DEFAULT_LAT,
        val lon: Double = DEFAULT_LON,
        val tz: String = DEFAULT_TZ,
        val localNames: Map<String, String> = emptyMap()
    )

    @Serializable
    data class FavouriteCity(
        val name: String,
        val lat: Double,
        val lon: Double,
        val tz: String,
        val localNames: Map<String, String> = emptyMap()
    )

    val favouriteCities: Flow<List<FavouriteCity>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_FAVOURITE_CITIES]
        if (raw.isNullOrBlank()) emptyList()
        else try { json.decodeFromString<List<FavouriteCity>>(raw) } catch (_: Exception) { emptyList() }
    }

    suspend fun addFavouriteCity(city: FavouriteCity) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_FAVOURITE_CITIES]?.let {
                try { json.decodeFromString<List<FavouriteCity>>(it) } catch (_: Exception) { null }
            } ?: emptyList()
            val updated = current.toMutableList().apply {
                removeAll { it.name == city.name }
                add(city)
            }
            prefs[KEY_FAVOURITE_CITIES] = json.encodeToString(updated)
        }
    }

    suspend fun removeFavouriteCity(name: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_FAVOURITE_CITIES]?.let {
                try { json.decodeFromString<List<FavouriteCity>>(it) } catch (_: Exception) { null }
            } ?: emptyList()
            prefs[KEY_FAVOURITE_CITIES] = json.encodeToString(current.filter { it.name != name })
        }
    }

    suspend fun setFavouriteCities(cities: List<FavouriteCity>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FAVOURITE_CITIES] = json.encodeToString(cities)
        }
    }

    val recentCities: Flow<List<FavouriteCity>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_RECENT_CITIES]
        if (raw.isNullOrBlank()) emptyList()
        else try { json.decodeFromString<List<FavouriteCity>>(raw) } catch (_: Exception) { emptyList() }
    }

    suspend fun addRecentCity(city: FavouriteCity) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_RECENT_CITIES]?.let {
                try { json.decodeFromString<List<FavouriteCity>>(it) } catch (_: Exception) { null }
            } ?: emptyList()
            val updated = current.toMutableList().apply {
                removeAll { it.name == city.name }
                add(0, city)
            }.take(MAX_RECENT_CITIES)
            prefs[KEY_RECENT_CITIES] = json.encodeToString(updated)
        }
    }

    suspend fun removeRecentCity(name: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_RECENT_CITIES]?.let {
                try { json.decodeFromString<List<FavouriteCity>>(it) } catch (_: Exception) { null }
            } ?: emptyList()
            prefs[KEY_RECENT_CITIES] = json.encodeToString(current.filter { it.name != name })
        }
    }
}
