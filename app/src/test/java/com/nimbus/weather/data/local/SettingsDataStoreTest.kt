package com.nimbus.weather.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.nimbus.weather.data.local.SettingsDataStore.FavouriteCity
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SettingsDataStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var scope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var store: SettingsDataStore

    @Before
    fun setup() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(tempFolder.root, "settings.preferences_pb") }
        )
        store = SettingsDataStore(mockk<Context>(relaxed = true), dataStore)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `defaults are returned on empty preferences`() = runBlocking {
        assertEquals(true, store.notificationsEnabled.first())
        assertEquals(SettingsDataStore.DEFAULT_UPDATE_INTERVAL_HOURS, store.updateIntervalHours.first())
        assertEquals(SettingsDataStore.DEFAULT_CITY, store.cityName.first())
        assertTrue(store.favouriteCities.first().isEmpty())
    }

    @Test
    fun `favourite cities round-trip through json`() = runBlocking {
        val kyiv = FavouriteCity("Киев", 50.45, 30.52, "Europe/Kiev")
        val lviv = FavouriteCity("Львов", 49.84, 24.03, "Europe/Kiev")

        store.addFavouriteCity(kyiv)
        store.addFavouriteCity(lviv)

        assertEquals(listOf(kyiv, lviv), store.favouriteCities.first())
    }

    @Test
    fun `adding existing city updates instead of duplicating`() = runBlocking {
        val kyiv = FavouriteCity("Киев", 50.45, 30.52, "Europe/Kiev")
        store.addFavouriteCity(kyiv)
        store.addFavouriteCity(kyiv.copy(lat = 50.46))

        val cities = store.favouriteCities.first()
        assertEquals(1, cities.size)
        assertEquals(50.46, cities.single().lat, 0.0001)
    }

    @Test
    fun `recent cities are capped and newest first`() = runBlocking {
        repeat(SettingsDataStore.MAX_RECENT_CITIES + 2) { index ->
            store.addRecentCity(FavouriteCity("City$index", index.toDouble(), 0.0, "UTC"))
        }

        val recent = store.recentCities.first()
        assertEquals(SettingsDataStore.MAX_RECENT_CITIES, recent.size)
        assertEquals("City6", recent.first().name)
    }

    @Test
    fun `location snapshot keeps coordinates and local names`() = runBlocking {
        store.setLocation("Харьков", 49.99, 36.23, "Europe/Kiev", mapOf("en" to "Kharkiv"))

        val snapshot = store.getLocationSnapshot()
        assertEquals("Харьков", snapshot.name)
        assertEquals(49.99, snapshot.lat, 0.0001)
        assertEquals(36.23, snapshot.lon, 0.0001)
        assertEquals("Kharkiv", snapshot.localNames["en"])
    }

    @Test
    fun `corrupted preferences file falls back to defaults`() = runBlocking {
        scope.cancel()
        File(tempFolder.root, "settings.preferences_pb").writeText("not-a-protobuf")

        val freshScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val freshDataStore = PreferenceDataStoreFactory.create(
                scope = freshScope,
                produceFile = { File(tempFolder.root, "settings.preferences_pb") },
                corruptionHandler = androidx.datastore.core.handlers.ReplaceFileCorruptionHandler {
                    emptyPreferences()
                }
            )
            val freshStore = SettingsDataStore(mockk<Context>(relaxed = true), freshDataStore)

            assertEquals(SettingsDataStore.DEFAULT_CITY, freshStore.cityName.first())
        } finally {
            freshScope.cancel()
        }
    }
}
