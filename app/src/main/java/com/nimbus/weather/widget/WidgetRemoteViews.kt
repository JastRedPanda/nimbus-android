package com.nimbus.weather.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import com.nimbus.weather.MainActivity
import com.nimbus.weather.R
import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.data.repository.WeatherCache
import com.nimbus.weather.service.WidgetUpdateManager
import com.nimbus.weather.util.isDayNowByTime
import com.nimbus.weather.util.toCelsiusOrFahrenheit
import com.nimbus.weather.util.weatherIcon
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Date
import java.util.Locale

private const val FALLBACK_WIDTH_DP = 250
private const val FALLBACK_HEIGHT_DP = 110
private const val CORNER_RADIUS_DP = 24
private const val SUB_RATIO = 0.52f
private const val TIME_PATTERN = "HH:mm"

/**
 * Собирает RemoteViews двухстрочного виджета по референсу:
 * верх — текущая температура + иконка погоды | время,
 * низ — ощущается + макс/мин | дата.
 * Время и дату рисует сам лаунчер через TextClock (тикают без нашего
 * процесса), здесь задаются только тексты погоды, иконка, шрифты,
 * цвета, фон и форматы даты.
 */
suspend fun buildWidgetViews(context: Context, appWidgetId: Int): RemoteViews {
    val settings = SettingsDataStore(context)
    val theme = settings.themeMode.first()
    val palette = resolveWidgetPalette(
        dark = isDarkTheme(context, theme),
        bgColorHex = settings.widgetBgColor.first(),
        bgAlpha = settings.widgetBgAlpha.first(),
        textOption = settings.widgetTextColor.first()
    )
    val dateFormat = settings.widgetDateFormat.first()
    val fontScaleSetting = settings.widgetFontScale.first()
    val tempUnit = settings.tempUnit.first()

    val weather = WidgetUpdateManager.getCachedWeather()
        ?: runCatching { WeatherCache(context).getCachedWidgetWeather(allowExpired = true) }.getOrNull()
        ?: runCatching { WeatherCache(context).getCachedWeather(allowExpired = true) }.getOrNull()

    val resources = context.resources
    val density = resources.displayMetrics.density
    val systemFontScale = resources.configuration.fontScale

    val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
    val widthDp = options.getInt(
        AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, FALLBACK_WIDTH_DP
    ).coerceAtLeast(1)
    val heightDp = options.getInt(
        AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, FALLBACK_HEIGHT_DP
    ).coerceAtLeast(1)

    val now = System.currentTimeMillis()
    val timeText = SimpleDateFormat(TIME_PATTERN, Locale.getDefault()).format(Date(now))
    // TextClock форматирует дату системной локалью в процессе лаунчера,
    // поэтому и ширину меряем по ней же, а не по языку приложения.
    val systemLocale = runCatching {
        Resources.getSystem().configuration.locales.get(0)
    }.getOrNull() ?: Locale.getDefault()
    val datePattern = if (dateFormat == "text") WIDGET_DATE_PATTERN_TEXT else WIDGET_DATE_PATTERN_NUMERIC
    val dateText = SimpleDateFormat(datePattern, systemLocale).format(Date(now))

    val current = weather?.current
    val tempText = current?.let {
        "${it.temperature.toCelsiusOrFahrenheit(tempUnit).toInt()}°"
    } ?: "--°"
    val feelsText = current?.let {
        "${it.apparentTemperature.toCelsiusOrFahrenheit(tempUnit).toInt()}°"
    } ?: ""
    val minMaxText = current?.let {
        val max = weather.daily?.temperatureMax?.firstOrNull()
            ?.toCelsiusOrFahrenheit(tempUnit)?.toInt()
        val min = weather.daily?.temperatureMin?.firstOrNull()
            ?.toCelsiusOrFahrenheit(tempUnit)?.toInt()
        buildString {
            if (max != null) append("↑${max}°")
            if (min != null) {
                if (isNotEmpty()) append(" ")
                append("↓${min}°")
            }
        }
    } ?: ""

    val weatherCode = current?.weatherCode
    val sunrise = weather?.daily?.sunrise?.firstOrNull() ?: ""
    val sunset = weather?.daily?.sunset?.firstOrNull() ?: ""
    val isDay = runCatching { isDayNowByTime(LocalTime.now(), sunrise, sunset) }.getOrDefault(true)
    val iconRes = weatherCode?.let { weatherIcon(it, isDay) }

    val availPx = (widthDp * density - 24f * density).coerceAtLeast(10f)
    // Фит меряем только с системным масштабом: пользовательский применяется
    // после (иначе замер и итог взаимно гасятся и настройка ни на что не влияет).
    val baseSp = fitBaseSp(
        timeText = timeText,
        dateText = dateText,
        tempText = tempText,
        feelsText = feelsText,
        minMaxText = minMaxText,
        availPx = availPx,
        density = density,
        multiplier = systemFontScale,
        columnGapPx = 24f * density
    )
    val userScale = fontScaleSetting / 100f

    val textArgb = palette.text.toArgb()
    val views = RemoteViews(context.packageName, R.layout.widget_clock_temp)

    views.setCharSequence(R.id.widget_time, "setFormat12Hour", TIME_PATTERN)
    views.setCharSequence(R.id.widget_time, "setFormat24Hour", TIME_PATTERN)
    views.setCharSequence(R.id.widget_date, "setFormat12Hour", datePattern)
    views.setCharSequence(R.id.widget_date, "setFormat24Hour", datePattern)

    views.setTextViewTextSize(
        R.id.widget_time, TypedValue.COMPLEX_UNIT_PX, baseSp * density * systemFontScale * userScale
    )
    views.setTextViewTextSize(
        R.id.widget_temp, TypedValue.COMPLEX_UNIT_PX, baseSp * density * systemFontScale * userScale
    )
    views.setTextViewTextSize(
        R.id.widget_date, TypedValue.COMPLEX_UNIT_PX, baseSp * SUB_RATIO * density * systemFontScale * userScale
    )
    views.setTextViewTextSize(
        R.id.widget_sub, TypedValue.COMPLEX_UNIT_PX, baseSp * SUB_RATIO * density * systemFontScale * userScale
    )
    views.setTextViewTextSize(
        R.id.widget_minmax, TypedValue.COMPLEX_UNIT_PX, baseSp * SUB_RATIO * density * systemFontScale * userScale
    )
    views.setTextColor(R.id.widget_time, textArgb)
    views.setTextColor(R.id.widget_date, textArgb)
    views.setTextColor(R.id.widget_temp, textArgb)
    views.setTextColor(R.id.widget_sub, textArgb)
    views.setTextColor(R.id.widget_minmax, textArgb)

    views.setTextViewText(R.id.widget_temp, tempText)
    views.setTextViewText(R.id.widget_sub, feelsText)
    views.setTextViewText(R.id.widget_minmax, minMaxText)
    views.setViewVisibility(
        R.id.widget_minmax, if (minMaxText.isNotEmpty()) View.VISIBLE else View.GONE
    )
    // Иконка «человечек с градусником» белая в векторе — красим под цвет текста.
    views.setImageViewResource(R.id.widget_feels_icon, R.drawable.ic_widget_feels_like)
    views.setInt(R.id.widget_feels_icon, "setColorFilter", textArgb)
    if (iconRes != null) {
        views.setImageViewResource(R.id.widget_icon, iconRes)
    }

    // Размер иконки фиксирован в layout (34dp): RemoteViews позволяет менять его
    // только с API 31, а minSdk — 26, поэтому адаптив под шрифт не делаем.

    views.setImageViewBitmap(R.id.widget_bg, roundedBackground(widthDp, heightDp, density, palette))

    val tapIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_root, tapIntent)

    return views
}

private fun roundedBackground(
    widthDp: Int,
    heightDp: Int,
    density: Float,
    palette: WidgetPalette
): Bitmap {
    val w = (widthDp * density).toInt().coerceAtLeast(1)
    val h = (heightDp * density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = palette.background.toArgb()
    }
    val radius = CORNER_RADIUS_DP * density
    canvas.drawRoundRect(0f, 0f, w.toFloat(), h.toFloat(), radius, radius, paint)
    return bitmap
}
