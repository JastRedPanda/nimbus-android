package com.nimbus.weather.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

/* ─── Типографика колонок ───
 * Почасовой и недельный блоки используют один набор кеглей:
 * метка сверху, мелкая деталь, значение. */

/** Время часа или день недели. */
internal val ChipLabelText = TextStyle(fontSize = 10.sp, lineHeight = 12.sp)

/** Дата, вероятность осадков, ветер. */
internal val ChipMetaText = TextStyle(fontSize = 9.sp, lineHeight = 12.sp)

/** Температура. */
internal val ChipValueText = TextStyle(fontSize = 13.sp, lineHeight = 16.sp)

/**
 * Кубический сегмент Catmull-Rom между [p1] и [p2].
 * [p0]/[p3] — соседние точки (на краях ряда дублируются крайними),
 * по ним считаются контрольные точки: кривая проходит через все узлы
 * и не «выстреливает» на концах.
 */
internal fun smoothSegment(p0: Offset, p1: Offset, p2: Offset, p3: Offset): Path =
    Path().apply {
        moveTo(p1.x, p1.y)
        cubicTo(
            p1.x + (p2.x - p0.x) / 6f, p1.y + (p2.y - p0.y) / 6f,
            p2.x - (p3.x - p1.x) / 6f, p2.y - (p3.y - p1.y) / 6f,
            p2.x, p2.y
        )
    }

/**
 * Готовая геометрия спарклайна: пути сегментов и точки с цветами.
 * Собирается один раз на размер и данные, а не в каждом кадре прокрутки.
 */
internal class SparklineSeries(
    val segments: List<Path>,
    val segmentColors: List<Color>,
    val points: List<Offset>,
    val pointColors: List<Color>
)

/**
 * Собирает геометрию ряда по [points]: сегменты сглаженной линии
 * и точки с кольцом-обводкой. Цвет сегмента и точки задаёт вызывающий —
 * почасовой красит по температуре, недельный ведёт два ряда
 * с разными палитрами.
 */
internal fun buildSparklineSeries(
    points: List<Offset>,
    pointColor: (Int) -> Color,
    segmentColor: (Int) -> Color
): SparklineSeries {
    if (points.size < 2) {
        return SparklineSeries(emptyList(), emptyList(), emptyList(), emptyList())
    }
    val segments = ArrayList<Path>(points.size - 1)
    val colors = ArrayList<Color>(points.size - 1)
    for (i in 0 until points.size - 1) {
        val p0 = points.getOrElse(i - 1) { points[i] }
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = points.getOrElse(i + 2) { points[i + 1] }
        segments += smoothSegment(p0, p1, p2, p3)
        colors += segmentColor(i)
    }
    val pointColors = points.indices.map(pointColor)
    return SparklineSeries(segments, colors, points, pointColors)
}

/**
 * Рисует заранее собранный ряд: только команды отрисовки, без аллокаций.
 */
internal fun DrawScope.drawSparkline(
    series: SparklineSeries,
    ringColor: Color,
    strokeWidth: Float,
    dotRadius: Float,
    ringRadius: Float
) {
    if (series.points.size < 2) return
    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    series.segments.forEachIndexed { i, path ->
        drawPath(path = path, color = series.segmentColors[i], style = stroke)
    }
    series.points.forEachIndexed { i, pt ->
        drawCircle(ringColor, radius = ringRadius, center = pt)
        drawCircle(series.pointColors[i], radius = dotRadius, center = pt)
    }
}

/**
 * Цвет линии по температуре: ноль и ниже — синий (мороз),
 * выше нуля — оранжево-красный (тепло). Чем дальше от нуля,
 * тем насыщеннее. Порог — 0°C при любой единице отображения.
 */
internal fun tempLineColor(celsius: Double): Color {
    return if (celsius <= 0) {
        val f = ((-celsius) / 20.0).coerceIn(0.0, 1.0).toFloat()
        lerp(Color(0xFF8FCBFF), Color(0xFF1565C0), f)
    } else {
        val f = (celsius / 30.0).coerceIn(0.0, 1.0).toFloat()
        lerp(Color(0xFFFFB74D), Color(0xFFD32F2F), f)
    }
}

/**
 * Цвет линии минимумов: всегда холодная гамма, чтобы нижний ряд
 * недельного графика не сливался с верхним летом, когда обе
 * температуры положительные. Чем холоднее — тем насыщеннее.
 */
internal fun coldLineColor(celsius: Double): Color {
    val f = ((10.0 - celsius) / 30.0).coerceIn(0.0, 1.0).toFloat()
    return lerp(Color(0xFFB3DDFF), Color(0xFF1E6FD9), f)
}
