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
import com.nimbus.weather.util.displayString
import com.nimbus.weather.util.toCelsiusOrFahrenheit
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val FALLBACK_WIDTH_DP = 250
private const val FALLBACK_HEIGHT_DP = 40
private const val CORNER_RADIUS_DP = 16
private const val TEMP_RATIO = 0.92f
private const val TIME_PATTERN = "HH:mm"
private const val DATE_PATTERN_TEXT = "d MMMM yyyy"
private const val DATE_PATTERN_NUMERIC = "dd.MM.yyyy"

/**
 * Собирает RemoteViews виджета. Время и дату рисует сам лаунчер через
 * TextClock (тикают без нашего процесса), здесь задаются только текст
 * температуры, шрифты, цвета, фон и форматы.
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
    val useFeelsLike = settings.useFeelsLike.first()
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
    val datePattern = if (dateFormat == "text") DATE_PATTERN_TEXT else DATE_PATTERN_NUMERIC
    val dateText = SimpleDateFormat(datePattern, systemLocale).format(Date(now))
    val tempText = weather?.current?.let { current ->
        val t = if (useFeelsLike) current.apparentTemperature else current.temperature
        "${t.toCelsiusOrFahrenheit(tempUnit).toInt()}${tempUnit.displayString()}"
    } ?: "--°"

    val availPx = (widthDp * density - 16f * density).coerceAtLeast(10f)
    val multiplier = systemFontScale * (100f / fontScaleSetting)
    val (baseSp, showDate) = fitBaseSp(
        timeText = timeText,
        dateText = dateText,
        tempText = tempText,
        availPx = availPx,
        density = density,
        multiplier = multiplier
    )

    val textArgb = palette.text.toArgb()
    val views = RemoteViews(context.packageName, R.layout.widget_clock_temp)

    views.setCharSequence(R.id.widget_time, "setFormat12Hour", TIME_PATTERN)
    views.setCharSequence(R.id.widget_time, "setFormat24Hour", TIME_PATTERN)
    views.setCharSequence(R.id.widget_date, "setFormat12Hour", datePattern)
    views.setCharSequence(R.id.widget_date, "setFormat24Hour", datePattern)

    views.setTextViewTextSize(
        R.id.widget_time, TypedValue.COMPLEX_UNIT_PX, baseSp * density * multiplier
    )
    views.setTextViewTextSize(
        R.id.widget_date, TypedValue.COMPLEX_UNIT_PX, baseSp * density * multiplier
    )
    views.setTextViewTextSize(
        R.id.widget_temp, TypedValue.COMPLEX_UNIT_PX, baseSp * TEMP_RATIO * density * multiplier
    )
    views.setTextColor(R.id.widget_time, textArgb)
    views.setTextColor(R.id.widget_date, textArgb)
    views.setTextColor(R.id.widget_temp, textArgb)

    views.setViewVisibility(R.id.widget_date, if (showDate) View.VISIBLE else View.GONE)
    views.setTextViewText(R.id.widget_temp, tempText)

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
