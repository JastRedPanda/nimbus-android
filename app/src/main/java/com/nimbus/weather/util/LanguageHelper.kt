package com.nimbus.weather.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LanguageHelper {

    private val CYRILLIC_FALLBACK = setOf(
        "be", "bg", "kk", "mk", "mn", "sr", "tg", "ky", "os", "ba", "cv", "ce"
    )

    fun resolveLocale(systemLocale: Locale = Locale.getDefault()): Locale {
        return when (val lang = systemLocale.language) {
            "ru" -> Locale("ru")
            "uk" -> Locale("uk")
            in CYRILLIC_FALLBACK -> Locale("uk")
            else -> Locale("en")
        }
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
            else -> "en"
        }
    }
}
