package com.nimbus.weather.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.graphics.Color
import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.util.ThemeMode
import kotlinx.coroutines.flow.first

private const val DEFAULT_DARK_BG = 0xFF1C1B1F
private const val DEFAULT_LIGHT_BG = 0xFFFFFBFE

fun isDarkTheme(context: Context, themeMode: ThemeMode): Boolean {
    return when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> {
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        }
    }
}

data class WidgetPalette(
    val background: Color,
    val text: Color
) {
    companion object {
        val defaultLight = WidgetPalette(
            background = Color(0xFFFFFBFE),
            text = Color.Black
        )
    }
}

fun resolveWidgetPalette(
    dark: Boolean,
    bgColorHex: String?,
    bgAlpha: Int,
    textOption: String
): WidgetPalette {
    val base = when {
        bgColorHex != null -> parseHexColor(bgColorHex) ?: if (dark) Color(DEFAULT_DARK_BG) else Color(DEFAULT_LIGHT_BG)
        dark -> Color(DEFAULT_DARK_BG)
        else -> Color(DEFAULT_LIGHT_BG)
    }
    val background = base.copy(alpha = bgAlpha.coerceIn(0, 100) / 100f)

    val text = when (textOption) {
        "white" -> Color.White
        "black" -> Color.Black
        else -> if (background.luminance() > 0.5f) Color.Black else Color.White
    }
    return WidgetPalette(background = background, text = text)
}

private fun parseHexColor(hex: String): Color? {
    return try {
        val value = if (hex.startsWith("#")) hex else "#$hex"
        Color(android.graphics.Color.parseColor(value))
    } catch (_: Exception) {
        null
    }
}

private fun Color.luminance(): Float {
    val r = linearize(red)
    val g = linearize(green)
    val b = linearize(blue)
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}

private fun linearize(c: Float): Float {
    return if (c <= 0.03928f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
}

private fun Float.pow(exp: Float): Float = Math.pow(this.toDouble(), exp.toDouble()).toFloat()

suspend fun SettingsDataStore.readWidgetPalette(dark: Boolean): WidgetPalette {
    return resolveWidgetPalette(
        dark = dark,
        bgColorHex = widgetBgColor.first(),
        bgAlpha = widgetBgAlpha.first(),
        textOption = widgetTextColor.first()
    )
}