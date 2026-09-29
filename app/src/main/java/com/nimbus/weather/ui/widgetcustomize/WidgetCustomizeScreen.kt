package com.nimbus.weather.ui.widgetcustomize

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nimbus.weather.R
import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.service.WidgetUpdateManager
import com.nimbus.weather.util.ThemeMode
import com.nimbus.weather.util.displayString
import com.nimbus.weather.util.toCelsiusOrFahrenheit
import com.nimbus.weather.widget.isDarkTheme
import com.nimbus.weather.widget.parseHexColor
import com.nimbus.weather.widget.resolveWidgetPalette

private val BG_PALETTE = listOf(
    "#FFFFFF",
    "#000000",
    "#1976D2",
    "#F57C00",
    "#7B1FA2",
    "#455A64"
)

/** Сетка диалога своего цвета: 12 тонов × 5 рядов насыщенности/яркости. */
private val PICKER_HUES = (0 until 12).map { it * 30f }
private val PICKER_ROWS = listOf(
    1.00f to 1.00f,
    0.75f to 1.00f,
    0.50f to 1.00f,
    0.38f to 0.88f,
    0.28f to 0.68f
)
private val PICKER_GRAYS = listOf(1.00f, 0.85f, 0.70f, 0.55f, 0.40f, 0.25f, 0.10f)

private fun Color.toHex(): String =
    "#%02X%02X%02X".format(
        (red * 255).toInt().coerceIn(0, 255),
        (green * 255).toInt().coerceIn(0, 255),
        (blue * 255).toInt().coerceIn(0, 255)
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetCustomizeScreen(
    onBackClick: () -> Unit,
    viewModel: WidgetCustomizeViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val settings = remember { SettingsDataStore(context.applicationContext) }
    val themeMode by settings.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val dark = isDarkTheme(context, themeMode)
    val palette = resolveWidgetPalette(dark, state.bgColorHex, state.bgAlpha, state.textOption)
    // Подписи чипов — сегодняшняя дата теми же паттернами, что рисует виджет.
    val today = remember { java.util.Date() }
    val dateNumericExample = remember {
        java.text.SimpleDateFormat(
            com.nimbus.weather.widget.WIDGET_DATE_PATTERN_NUMERIC,
            java.util.Locale.getDefault()
        ).format(today)
    }
    val dateTextExample = remember {
        java.text.SimpleDateFormat(
            com.nimbus.weather.widget.WIDGET_DATE_PATTERN_TEXT,
            java.util.Locale.getDefault()
        ).format(today)
    }

    var showPicker by remember { mutableStateOf(false) }
    // Свой цвет — выбранный hex, которого нет в готовой палитре.
    val customHex = state.bgColorHex?.takeIf { hex ->
        BG_PALETTE.none { it.equals(hex, ignoreCase = true) }
    }

    if (showPicker) {
        CustomColorDialog(
            initialHex = customHex,
            onDismiss = { showPicker = false },
            onApply = { hex ->
                showPicker = false
                viewModel.onBgColorSelected(hex)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.widgets)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SectionLabel(stringResource(R.string.widget_preview))
            Spacer(modifier = Modifier.height(8.dp))

            WidgetPreviewBox(palette, dark, state.dateFormat, state.fontScale)

            Spacer(modifier = Modifier.height(16.dp))

            SectionLabel(stringResource(R.string.widget_bg_color))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ColorDot(
                    color = null,
                    selected = state.bgColorHex == null,
                    onClick = { viewModel.onBgColorSelected(null) }
                )
                BG_PALETTE.forEach { hex ->
                    ColorDot(
                        color = hex,
                        selected = state.bgColorHex == hex,
                        onClick = { viewModel.onBgColorSelected(hex) }
                    )
                }
                if (customHex != null) {
                    ColorDot(
                        color = customHex,
                        selected = true,
                        onClick = { showPicker = true }
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            CircleShape
                        )
                        .clickable(onClick = { showPicker = true }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.widget_custom_color),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SectionLabel(stringResource(R.string.widget_bg_transparency))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Slider(
                    value = state.bgAlpha.toFloat(),
                    onValueChange = { viewModel.onBgAlphaPreview(it.toInt()) },
                    onValueChangeFinished = { viewModel.onBgAlphaCommit() },
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${state.bgAlpha}%",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.width(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            SectionLabel(stringResource(R.string.widget_text_color))
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextOptionChip(
                    label = stringResource(R.string.widget_text_auto),
                    selected = state.textOption == "auto",
                    onClick = { viewModel.onTextOptionSelected("auto") }
                )
                TextOptionChip(
                    label = stringResource(R.string.widget_text_black),
                    selected = state.textOption == "black",
                    onClick = { viewModel.onTextOptionSelected("black") }
                )
                TextOptionChip(
                    label = stringResource(R.string.widget_text_white),
                    selected = state.textOption == "white",
                    onClick = { viewModel.onTextOptionSelected("white") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SectionLabel(stringResource(R.string.widget_date_format))
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextOptionChip(
                    label = dateNumericExample,
                    selected = state.dateFormat == "numeric",
                    onClick = { viewModel.onDateFormatSelected("numeric") }
                )
                TextOptionChip(
                    label = dateTextExample,
                    selected = state.dateFormat == "text",
                    onClick = { viewModel.onDateFormatSelected("text") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SectionLabel(stringResource(R.string.widget_font_size))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Slider(
                    value = state.fontScale.toFloat(),
                    onValueChange = { viewModel.onFontScalePreview(it.toInt()) },
                    onValueChangeFinished = { viewModel.onFontScaleCommit() },
                    valueRange = SettingsDataStore.MIN_WIDGET_FONT_SCALE.toFloat()..
                        SettingsDataStore.MAX_WIDGET_FONT_SCALE.toFloat(),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${state.fontScale}%",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.width(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = viewModel::onReset,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.widget_reset))
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun ColorDot(
    color: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = color?.let { parseHexColor(it) } ?: Color.Transparent
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(bg)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = borderColor,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (color == null) {
            Icon(
                imageVector = Icons.Outlined.BrightnessAuto,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        } else if (selected) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .border(2.dp, Color.White.copy(alpha = 0.9f), CircleShape)
            )
        }
    }
}

@Composable
private fun TextOptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}

/**
 * Диалог своего цвета фона: сетка тонов (12 оттенков × 5 рядов) + ряд серого.
 * Выбранный hex пишется в ту же настройку, что и готовая палитра, — виджет
 * и превью подхватывают его без дополнительного кода.
 */
@Composable
private fun CustomColorDialog(
    initialHex: String?,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    var selected by remember(initialHex) {
        mutableStateOf(
            initialHex?.let { parseHexColor(it) } ?: Color.hsv(210f, 0.65f, 0.85f)
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.widget_custom_color)) },
        text = {
            Column {
                PICKER_ROWS.forEach { (saturation, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PICKER_HUES.forEach { hue ->
                            PickerCell(
                                color = Color.hsv(hue, saturation, value),
                                selectedColor = selected,
                                onClick = { selected = it }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PICKER_GRAYS.forEach { gray ->
                        PickerCell(
                            color = Color.hsv(0f, 0f, gray),
                            selectedColor = selected,
                            onClick = { selected = it }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(selected)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(8.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = selected.toHex(),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(selected.toHex()) }) {
                Text(stringResource(R.string.widget_custom_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun RowScope.PickerCell(
    color: Color,
    selectedColor: Color,
    onClick: (Color) -> Unit
) {
    val selected = color == selectedColor
    Box(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 2.dp else 0.5.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.White.copy(alpha = 0.25f)
                },
                shape = CircleShape
            )
            .clickable(onClick = { onClick(color) })
    )
}

@Composable
private fun WidgetPreviewBox(
    palette: com.nimbus.weather.widget.WidgetPalette,
    dark: Boolean,
    dateFormat: String,
    fontScale: Int
) {
    val weather = remember { WidgetUpdateManager.getCachedWeather() }
    val current = weather?.current
    val tempText = if (current != null) {
        "${current.temperature.toCelsiusOrFahrenheit(com.nimbus.weather.util.TemperatureUnit.CELSIUS).toInt()}°"
    } else "24°"
    val feelsText = if (current != null) {
        "${current.apparentTemperature.toCelsiusOrFahrenheit(com.nimbus.weather.util.TemperatureUnit.CELSIUS).toInt()}°"
    } else "23°"
    val minMaxText = if (current != null) {
        val max = weather.daily?.temperatureMax?.firstOrNull()
            ?.toCelsiusOrFahrenheit(com.nimbus.weather.util.TemperatureUnit.CELSIUS)?.toInt()
        val min = weather.daily?.temperatureMin?.firstOrNull()
            ?.toCelsiusOrFahrenheit(com.nimbus.weather.util.TemperatureUnit.CELSIUS)?.toInt()
        buildString {
            if (max != null) append("↑${max}°")
            if (min != null) {
                if (isNotEmpty()) append(" ")
                append("↓${min}°")
            }
        }
    } else "↑28° ↓16°"
    val datePattern = if (dateFormat == "text") {
        com.nimbus.weather.widget.WIDGET_DATE_PATTERN_TEXT
    } else {
        com.nimbus.weather.widget.WIDGET_DATE_PATTERN_NUMERIC
    }
    val dateText = remember(datePattern) {
        java.text.SimpleDateFormat(datePattern, java.util.Locale.getDefault())
            .format(java.util.Date())
    }
    val scale = fontScale / 100f
    val iconRes = current?.let {
        com.nimbus.weather.util.weatherIcon(it.weatherCode, true)
    } ?: com.nimbus.weather.R.drawable.ic_weather_partly_day

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(palette.background)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tempText,
                        color = palette.text,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = (30 * scale).sp
                        ),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size((34 * scale).dp)
                    )
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(
                            com.nimbus.weather.R.drawable.ic_widget_feels_like
                        ),
                        contentDescription = null,
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(palette.text),
                        // Виджет ставит низ иконки на базовую линию текста
                        // (baselineAlignBottom), а не на низ вьюхи: приподнимаем
                        // на высоту нижнего выноса, чтобы превью совпадало.
                        modifier = Modifier
                            .padding(bottom = (3 * scale).dp)
                            .size((18 * scale).dp)
                    )
                    Text(
                        text = feelsText,
                        color = palette.text,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = (15 * scale).sp
                        ),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Text(
                        text = minMaxText,
                        color = palette.text,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = (15 * scale).sp
                        ),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "10:35",
                    color = palette.text,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = (30 * scale).sp
                    ),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
                Text(
                    text = dateText,
                    color = palette.text,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = (15 * scale).sp
                    ),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
        }
    }
}
