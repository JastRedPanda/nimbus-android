package com.nimbus.weather.widget

import android.text.TextPaint

private const val MIN_FONT_SP = 10f
private const val MAX_FONT_SP = 400f
private const val SUB_RATIO = 0.52f
/** Высота иконки погоды от базового шрифта (битмап растится под шрифт). */
internal const val ICON_RATIO = 1.3f
/** Высота иконки «ощущается» от базового шрифта. */
internal const val FEELS_ICON_RATIO = 0.65f
private const val GAP_EM = 0.4f
/** Отступ между «ощущается» и «макс/мин» — заметно шире пробела внутри группы. */
private const val SUB_GAP_EM = 0.9f

/** Паттерны даты виджета: ими же подписаны чипы в настройках. */
internal const val WIDGET_DATE_PATTERN_TEXT = "EEE, d MMM"
internal const val WIDGET_DATE_PATTERN_NUMERIC = "dd.MM"

/**
 * Подбирает базовый размер шрифта (верхняя строка) для двухстрочного виджета.
 *
 * Колонки стоят группой по центру: левая — макс. из (температура + иконка,
 * ощущается + макс/мин), правая — макс. из (время, дата), между ними
 * фиксированный зазор columnGapPx. Верхняя строка крупно (baseSp), нижняя —
 * мельче (baseSp * SUB_RATIO). Отступ между ощущается и макс/мин (SUB_GAP_EM)
 * шире обычного пробела внутри «↑макс ↓мин» — как на референсе.
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
    multiplier: Float,
    columnGapPx: Float
): Float {
    val paint = TextPaint()

    fun widthOf(text: String, ratio: Float, sp: Float): Float {
        paint.textSize = sp * ratio * density * multiplier
        return paint.measureText(text)
    }

    fun totalWidth(sp: Float): Float {
        val leftTop = widthOf(tempText, 1f, sp) +
            sp * ICON_RATIO * density * multiplier + sp * GAP_EM * density * multiplier
        val subGap = if (minMaxText.isNotEmpty()) sp * SUB_GAP_EM * density * multiplier else 0f
        val leftBottom = sp * FEELS_ICON_RATIO * density * multiplier +
            widthOf(feelsText, SUB_RATIO, sp) + subGap +
            widthOf(minMaxText, SUB_RATIO, sp)
        val left = maxOf(leftTop, leftBottom)
        val right = maxOf(widthOf(timeText, 1f, sp), widthOf(dateText, SUB_RATIO, sp))
        return left + columnGapPx * multiplier + right
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
