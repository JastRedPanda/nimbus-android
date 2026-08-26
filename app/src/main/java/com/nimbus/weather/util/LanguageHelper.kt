package com.nimbus.weather.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LanguageHelper {

    const val AUTO = "auto"

    private val CYRILLIC_FALLBACK = setOf(
        "be", "bg", "kk", "mk", "mn", "sr", "tg", "ky", "os", "ba", "cv", "ce"
    )

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
