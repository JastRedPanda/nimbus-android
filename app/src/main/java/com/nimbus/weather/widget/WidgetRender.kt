package com.nimbus.weather.widget

import android.content.Context
import android.text.TextPaint
import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.util.TemperatureUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

private const val MIN_FONT_SP = 10f
private const val MAX_FONT_SP = 400f
private const val TEMP_RATIO = 0.92f
private const val GAP_EM = 0.55f

internal data class WidgetRenderData(
    val fontScale: Int = SettingsDataStore.DEFAULT_WIDGET_FONT_SCALE,
    val dateFormat: String = SettingsDataStore.DEFAULT_WIDGET_DATE_FORMAT,
    val useFeelsLike: Boolean = false,
    val tempUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val palette: WidgetPalette = WidgetPalette.defaultLight
)

internal fun renderDataFlow(context: Context, settings: SettingsDataStore): Flow<WidgetRenderData> {
    val paletteFlow = combine(
        settings.themeMode,
        settings.widgetBgColor,
        settings.widgetBgAlpha,
        settings.widgetTextColor
    ) { theme, bg, alpha, text ->
        resolveWidgetPalette(
            dark = isDarkTheme(context, theme),
            bgColorHex = bg,
            bgAlpha = alpha,
            textOption = text
        )
    }
    return combine(
        paletteFlow,
        settings.widgetDateFormat,
        settings.widgetFontScale,
        settings.useFeelsLike,
        settings.tempUnit
    ) { palette, dateFormat, fontScale, useFeelsLike, tempUnit ->
        WidgetRenderData(
            fontScale = fontScale,
            dateFormat = dateFormat,
            useFeelsLike = useFeelsLike,
            tempUnit = tempUnit,
            palette = palette
        )
    }
}

/**
 * Подбирает базовый размер шрифта так, чтобы строка «время + дата + температура»
 * влезла в доступную ширину. Дата скрывается, если без неё шрифт крупнее.
 */
internal fun fitBaseSp(
    timeText: String,
    dateText: String,
    tempText: String,
    availPx: Float,
    density: Float,
    multiplier: Float
): Pair<Float, Boolean> {
    val paint = TextPaint()

    fun widthOf(text: String, ratio: Float, sp: Float): Float {
        paint.textSize = sp * ratio * density * multiplier
        return paint.measureText(text)
    }

    fun totalWidth(sp: Float, withDate: Boolean): Float {
        val timeWidth = widthOf(timeText, 1f, sp)
        val tempWidth = widthOf(tempText, TEMP_RATIO, sp)
        val dateWidth = if (withDate) widthOf(dateText, 1f, sp) else 0f
        val gaps = (if (withDate) 2 else 1) * sp * GAP_EM * density * multiplier
        return timeWidth + tempWidth + dateWidth + gaps
    }

    fun maxSp(withDate: Boolean): Float {
        var low = MIN_FONT_SP
        var high = MAX_FONT_SP
        var best = MIN_FONT_SP
        repeat(36) {
            val mid = (low + high) / 2f
            if (totalWidth(mid, withDate) <= availPx) {
                best = mid
                low = mid
            } else {
                high = mid
            }
        }
        return best
    }

    val withDateBest = maxSp(withDate = true)
    if (withDateBest > MIN_FONT_SP) return withDateBest to true
    return maxSp(withDate = false) to false
}
