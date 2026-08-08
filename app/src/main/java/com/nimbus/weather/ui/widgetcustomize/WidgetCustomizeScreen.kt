package com.nimbus.weather.ui.widgetcustomize

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.BrightnessAuto
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.nimbus.weather.widget.resolveWidgetPalette

private val BG_PALETTE = listOf(
    "#FFFFFF",
    "#000000",
    "#1976D2",
    "#00695C",
    "#388E3C",
    "#F57C00",
    "#7B1FA2",
    "#C2185B",
    "#455A64"
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

            WidgetPreviewBox(palette, dark)

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
            }

            Spacer(modifier = Modifier.height(16.dp))

            SectionLabel(stringResource(R.string.widget_bg_transparency))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Slider(
                    value = state.bgAlpha.toFloat(),
                    onValueChange = { viewModel.onBgAlphaChanged(it.toInt()) },
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
    val bg = color?.let { parseHex(it) } ?: Color.Transparent
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

@Composable
private fun WidgetPreviewBox(
    palette: com.nimbus.weather.widget.WidgetPalette,
    dark: Boolean
) {
    val weather = remember { WidgetUpdateManager.getCachedWeather() }
    val tempText = if (weather?.current != null) {
        "${weather.current.temperature.toCelsiusOrFahrenheit(com.nimbus.weather.util.TemperatureUnit.CELSIUS).toInt()}°C"
    } else "--°C"

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(palette.background)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "00:00",
                color = palette.text,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = tempText,
                color = palette.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun parseHex(hex: String): Color {
    return try {
        val value = if (hex.startsWith("#")) hex else "#$hex"
        Color(android.graphics.Color.parseColor(value))
    } catch (_: Exception) {
        Color.Transparent
    }
}