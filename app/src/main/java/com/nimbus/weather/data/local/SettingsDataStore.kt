package com.nimbus.weather.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        private val KEY_USE_FEELS_LIKE = booleanPreferencesKey("use_feels_like")
        private val KEY_CITY_NAME = stringPreferencesKey("city_name")
        private val KEY_LATITUDE = doublePreferencesKey("latitude")
        private val KEY_LONGITUDE = doublePreferencesKey("longitude")
        private val KEY_TIMEZONE = stringPreferencesKey("timezone")

        const val DEFAULT_CITY = "Киев"
        const val DEFAULT_LAT = 50.4501
        const val DEFAULT_LON = 30.5234
        const val DEFAULT_TZ = "Europe/Kiev"
    }

    val useFeelsLike: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_USE_FEELS_LIKE] ?: false
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

    suspend fun setUseFeelsLike(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USE_FEELS_LIKE] = value
        }
    }

    suspend fun setLocation(name: String, lat: Double, lon: Double, tz: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CITY_NAME] = name
            prefs[KEY_LATITUDE] = lat
            prefs[KEY_LONGITUDE] = lon
            prefs[KEY_TIMEZONE] = tz
        }
    }

    suspend fun getLocationSnapshot(): LocationSnapshot {
        val prefs = context.dataStore.data.first()
        return LocationSnapshot(
            name = prefs[KEY_CITY_NAME] ?: DEFAULT_CITY,
            lat = prefs[KEY_LATITUDE] ?: DEFAULT_LAT,
            lon = prefs[KEY_LONGITUDE] ?: DEFAULT_LON,
            tz = prefs[KEY_TIMEZONE] ?: DEFAULT_TZ
        )
    }

    data class LocationSnapshot(
        val name: String = DEFAULT_CITY,
        val lat: Double = DEFAULT_LAT,
        val lon: Double = DEFAULT_LON,
        val tz: String = DEFAULT_TZ
    )
}
