package com.nimbus.weather.data.repository

import android.content.Context
import com.nimbus.weather.data.api.ApiClient
import com.nimbus.weather.data.model.AirQualityResponse
import com.nimbus.weather.data.model.GeocodingResult
import com.nimbus.weather.data.model.WeatherResponse
import java.util.concurrent.ConcurrentHashMap

class WeatherRepository {

    companion object {
        const val DEFAULT_TTL_HOURS = 4

        internal fun buildLanguageChain(language: String): List<String> =
            (listOf(language, "en") + listOf("ru", "uk", "cs")).distinct()

        private val CYRILLIC_TO_LATIN = mapOf(
            'а' to "a", 'б' to "b", 'в' to "v", 'г' to "h", 'ґ' to "g",
            'д' to "d", 'е' to "e", 'є' to "ye", 'ё' to "yo", 'ж' to "zh",
            'з' to "z", 'и' to "y", 'і' to "i", 'ї' to "yi", 'й' to "y",
            'к' to "k", 'л' to "l", 'м' to "m", 'н' to "n", 'о' to "o",
            'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t", 'у' to "u",
            'ф' to "f", 'х' to "kh", 'ц' to "ts", 'ч' to "ch", 'ш' to "sh",
            'щ' to "shch", 'ъ' to "", 'ы' to "y", 'ь' to "", 'э' to "e",
            'ю' to "yu", 'я' to "ya"
        )

        /** Нижний регистр + транслит + только латиница: «Харків» → «kharkiv». */
        internal fun normalizeName(s: String): String = buildString {
            for (ch in s.lowercase()) {
                val mapped = CYRILLIC_TO_LATIN[ch] ?: ch.toString()
                for (c in mapped) {
                    if (c in 'a'..'z') append(c)
                }
            }
        }

        internal fun isExactMatch(query: String, name: String): Boolean =
            normalizeName(name) == normalizeName(query)

        /** Точные совпадения — вверх, затем содержащие запрос, затем остальные. */
        internal fun rankResults(query: String, results: List<GeocodingResult>): List<GeocodingResult> {
            val q = normalizeName(query)
            return results.sortedWith(
                compareBy(
                    { if (normalizeName(it.name) == q) 0 else 1 },
                    { if (normalizeName(it.name).contains(q)) 0 else 1 }
                )
            )
        }
    }

    private val weatherApi = ApiClient.weatherApi
    private val geocodingApi = ApiClient.geocodingApi
    private val airQualityApi = ApiClient.airQualityApi

    @Volatile
    private var cache: WeatherCache? = null

    @Volatile
    var showingCachedWeather: Boolean = false
        private set

    @Volatile
    private var ttlHours: Int = DEFAULT_TTL_HOURS

    fun setTtlHours(hours: Int) {
        ttlHours = hours.coerceAtLeast(1)
        cache?.setTtlHours(ttlHours)
    }

    private fun getCache(context: Context): WeatherCache {
        val existing = cache
        if (existing != null) return existing
        return WeatherCache(context).also {
            it.setTtlHours(ttlHours)
            cache = it
        }
    }

    suspend fun getWeather(lat: Double, lon: Double, context: Context? = null): WeatherResponse {
        val c = context?.let { getCache(it) }
        return try {
            val response = weatherApi.getForecast(latitude = lat, longitude = lon)
            showingCachedWeather = false
            c?.cacheWeather(response)
            response
        } catch (e: Exception) {
            val cached = c?.getCachedWeather(allowExpired = true)
            if (cached != null) {
                showingCachedWeather = true
                cached
            } else {
                showingCachedWeather = false
                throw e
            }
        }
    }

    suspend fun getAirQuality(lat: Double, lon: Double, context: Context? = null): AirQualityResponse {
        val c = context?.let { getCache(it) }
        return try {
            val response = airQualityApi.getAirQuality(latitude = lat, longitude = lon)
            c?.cacheAqi(response)
            response
        } catch (e: Exception) {
            val cached = c?.getCachedAqi(allowExpired = true)
            if (cached != null) cached else throw e
        }
    }

    suspend fun searchCities(query: String, language: String = "ru"): List<GeocodingResult> {
        return geocodingApi.searchCities(name = query, language = language).results.orEmpty()
    }

    /**
     * Поиск города с фолбэком по языкам: Open-Meteo ищет только по именам
     * на указанном языке, поэтому «Харків» с language=ru даёт пусто, а
     * «Харьков» с language=uk — мусор из однофамильцев.
     * Порядок: язык приложения → английский → остальные наши. Если родной
     * язык дал точное совпадение — дальше не идём. Иначе добираем цепочку,
     * дубли по id отбрасываем, точные совпадения поднимаем вверх.
     * Сравнение через транслит (кириллица→латиница), чтобы «Харків»
     * совпадал с «Kharkiv».
     */
    suspend fun searchCitiesWithFallback(query: String, language: String): List<GeocodingResult> {
        val merged = LinkedHashMap<Int, GeocodingResult>()
        for (lang in buildLanguageChain(language)) {
            val results = try {
                geocodingApi.searchCities(name = query, language = lang).results.orEmpty()
            } catch (_: Exception) {
                emptyList()
            }
            for (r in results) merged.putIfAbsent(r.id, r)
            if (merged.values.any { isExactMatch(query, it.name) }) break
        }
        return rankResults(query, merged.values.toList())
    }

    suspend fun translateCityName(name: String, lat: Double, lon: Double, toLang: String): String? {
        val key = TranslationKey(name, toLang, lat, lon)
        translationCache[key]?.let { return it }
        if (key in translationMisses) return null

        val radiusKm = 25.0
        fun bestMatch(results: List<GeocodingResult>): GeocodingResult? {
            var best: GeocodingResult? = null
            var bestDist = Double.MAX_VALUE
            for (r in results) {
                val d = distanceKm(r.latitude, r.longitude, lat, lon)
                if (d <= radiusKm && d < bestDist) {
                    best = r
                    bestDist = d
                }
            }
            return best
        }

        val targetResults = geocodingApi.searchCities(name = name, language = toLang).results.orEmpty()
        bestMatch(targetResults)?.let {
            translationCache[key] = it.name
            return it.name
        }
        val canonical = bestMatch(
            geocodingApi.searchCities(name = name, language = "en").results.orEmpty()
        ) ?: run {
            translationMisses.add(key)
            return null
        }
        val result = bestMatch(
            geocodingApi.searchCities(name = canonical.name, language = toLang).results.orEmpty()
        )?.name
        if (result != null) translationCache[key] = result else translationMisses.add(key)
        return result
    }

    private data class TranslationKey(
        val name: String,
        val lang: String,
        val lat: Double,
        val lon: Double
    )

    private val translationCache = ConcurrentHashMap<TranslationKey, String>()
    private val translationMisses = ConcurrentHashMap.newKeySet<TranslationKey>()

    private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2)
        return 2 * earthRadiusKm * Math.asin(Math.sqrt(a))
    }
}
