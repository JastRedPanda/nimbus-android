package com.nimbus.weather.widget

import android.text.TextPaint

private const val MIN_FONT_SP = 10f
private const val MAX_FONT_SP = 400f
private const val SUB_RATIO = 0.52f
private const val ICON_RATIO = 1.1f
private const val GAP_EM = 0.4f
/** Отступ между «ощущается» и «макс/мин» — заметно шире пробела внутри группы. */
private const val SUB_GAP_EM = 0.9f

/** Паттерны даты виджета: ими же подписаны чипы в настройках. */
internal const val WIDGET_DATE_PATTERN_TEXT = "EEE, d MMM"
internal const val WIDGET_DATE_PATTERN_NUMERIC = "dd.MM"

/**
 * Подбирает базовый размер шрифта (верхняя строка) для двухстрочного виджета.
 *
 * Верхняя строка: «температура + иконка | время» — крупно (baseSp).
 * Нижняя строка: «ощущается | макс/мин | дата» — мельче (baseSp * SUB_RATIO).
 * Отступ между ощущается и макс/мин (SUB_GAP_EM) шире обычного пробела
 * внутри «↑макс ↓мин» — как на референсе.
 * Иконка погоды масштабируется от шрифта, поэтому её ширина тоже участвует
 * в замере верхней строки.
 *
 * Возвращает baseSp для верхней строки.
 */
internal fun fitBaseSp(
    timeText: String,
    dateText: String,
    tempText: String,
    feelsText: String,
    minMaxText: String,
    availPx: Float,
    density: Float,
    multiplier: Float
): Float {
    val paint = TextPaint()

    fun widthOf(text: String, ratio: Float, sp: Float): Float {
        paint.textSize = sp * ratio * density * multiplier
        return paint.measureText(text)
    }

    fun totalWidth(sp: Float): Float {
        val tempWidth = widthOf(tempText, 1f, sp)
        val iconWidth = sp * ICON_RATIO * density * multiplier + sp * GAP_EM * density * multiplier
        val timeWidth = widthOf(timeText, 1f, sp)
        val topWidth = tempWidth + iconWidth + timeWidth + sp * GAP_EM * density * multiplier

        val feelsWidth = widthOf(feelsText, SUB_RATIO, sp)
        val minMaxWidth = widthOf(minMaxText, SUB_RATIO, sp)
        val subGap = if (minMaxText.isNotEmpty()) sp * SUB_GAP_EM * density * multiplier else 0f
        val subWidth = feelsWidth + subGap + minMaxWidth
        val dateWidth = widthOf(dateText, SUB_RATIO, sp)
        val bottomWidth = subWidth + dateWidth + sp * GAP_EM * density * multiplier

        return maxOf(topWidth, bottomWidth)
    }

    var low = MIN_FONT_SP
    var high = MAX_FONT_SP
    var best = MIN_FONT_SP
    repeat(20) {
        val mid = (low + high) / 2f
        if (totalWidth(mid) <= availPx) {
            best = mid
            low = mid
        } else {
            high = mid
        }
    }
    return best
}
