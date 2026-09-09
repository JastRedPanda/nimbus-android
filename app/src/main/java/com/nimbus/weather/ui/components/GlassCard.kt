package com.nimbus.weather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nimbus.weather.ui.theme.LocalGlassDark

/**
 * Базовый компонент «матового стекла».
 *
 * Тонировка — от темы приложения ([LocalGlassDark]): тёмная тема —
 * тонировка в чёрное, светлая — в белое. Текст под неё подгоняется
 * там же, где читается [LocalGlassDark].
 *
 * Плюс тонкая градиентная фаска (1 dp), имитирующая отблеск
 * света на кромке стекла.
 *
 * @param cornerRadius скругление углов (по умолчанию 20 dp)
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val glassDark = LocalGlassDark.current
    val shape = RoundedCornerShape(cornerRadius)

    val fill = if (!glassDark) {
        Color.White.copy(alpha = 0.55f)
    } else {
        // Тёмная тема — стекло тонировано в чёрное, небо лишь просвечивает
        Color.Black.copy(alpha = 0.45f)
    }

    val borderBrush = Brush.verticalGradient(
        colors = if (glassDark) listOf(
            Color.White.copy(alpha = 0.35f),
            Color.White.copy(alpha = 0.08f)
        ) else listOf(
            Color.White.copy(alpha = 0.70f),
            Color.White.copy(alpha = 0.20f)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(fill)
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .padding(12.dp),
        content = content
    )
}
