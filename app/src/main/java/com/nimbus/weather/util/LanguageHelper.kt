package com.nimbus.weather.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LanguageHelper {

    const val AUTO = "auto"

    private const val CACHE_PREFS = "language_cache"
    private const val CACHE_KEY = "app_language"

    private val CYRILLIC_FALLBACK = setOf(
        "be", "bg", "kk", "mk", "mn", "sr", "tg", "ky", "os", "ba", "cv", "ce"
    )

    /**
     * Язык из SharedPreferences — синхронно и без DataStore.
     * `null`, если кэша ещё нет (первый старт после обновления
     * с версии, где язык жил только в DataStore).
     *
     * `attachBaseContext` вызывается до того, как в процессе появится
     * хоть один scope, и читать DataStore там приходилось через
     * `runBlocking`, то есть поднимать корутинный движок и парсить protobuf
     * прямо на главном потоке холодного старта. Кэш пишется рядом
     * с `SettingsDataStore.setAppLanguage`.
     */
    fun readCachedLanguage(context: Context): String? =
        context.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE)
            .getString(CACHE_KEY, null)

    fun writeCachedLanguage(context: Context, language: String) {
        context.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE)
            .edit().putString(CACHE_KEY, language).commit()
    }

    fun resolveLocale(systemLocale: Locale = Locale.getDefault()): Locale {
        return when (val lang = systemLocale.language) {
            "ru" -> Locale("ru")
            "uk" -> Locale("uk")
            "cs" -> Locale("cs")
            in CYRILLIC_FALLBACK -> Locale("uk")
            else -> Locale("en")
        }
    }

    fun resolveLocale(languageCode: String): Locale {
        return when (languageCode) {
            "ru" -> Locale("ru")
            "uk" -> Locale("uk")
            "cs" -> Locale("cs")
            "en" -> Locale("en")
            else -> resolveLocale(Locale.getDefault())
        }
    }

    fun resolve(appLanguage: String): String {
        return if (appLanguage == AUTO) resolveLocale().language else appLanguage
    }

    fun createContextWithLocale(context: Context, locale: Locale = resolveLocale()): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun getLocaleTag(locale: Locale = resolveLocale()): String {
        return when (locale.language) {
            "ru" -> "ru"
            "uk" -> "uk"
            "cs" -> "cs"
            else -> "en"
        }
    }
}
