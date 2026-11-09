package com.pushpyns.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.pushpyns.ui.theme.PushpynsBlue
import com.pushpyns.ui.theme.PushpynsCyan
import com.pushpyns.ui.theme.PushpynsDeep

@Composable
fun PushpynsMark(modifier: Modifier = Modifier, hole: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val headRadius = w * 0.34f
        val cx = w / 2f
        val cy = headRadius + size.height * 0.04f
        val brush =
            Brush.linearGradient(
                colors = listOf(PushpynsCyan, PushpynsBlue, PushpynsDeep),
                start = Offset(cx - headRadius, cy - headRadius),
                end = Offset(cx + headRadius, cy + headRadius),
            )
        drawCircle(brush = brush, radius = headRadius, center = Offset(cx, cy))
        drawCircle(color = hole, radius = headRadius * 0.42f, center = Offset(cx, cy))
        val tip =
            Path().apply {
                moveTo(cx - headRadius * 0.46f, cy + headRadius * 0.62f)
                lineTo(cx + headRadius * 0.46f, cy + headRadius * 0.55f)
                lineTo(cx, size.height * 0.96f)
                close()
            }
        drawPath(tip, brush)
    }
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier, action: @Composable () -> Unit = {}) {
    Column(
        modifier = modifier.padding(horizontal = 28.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PushpynsMark(
            modifier = Modifier.size(72.dp),
            hole = MaterialTheme.colorScheme.background,
        )
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        action()
    }
}

@Composable
fun PlaceholderMap(pins: List<MapPin>, origin: MapPin?, caption: String, modifier: Modifier = Modifier, onPinClick: (Long) -> Unit = {}) {
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier =
        modifier.background(
            Brush.linearGradient(
                listOf(Color(0xFFD7EEF8), Color(0xFFE7F0E4), surface),
            ),
        ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val step = 28.dp.toPx()
            var x = 0f
            val grid = Color(0x220C6FBE)
            while (x < size.width) {
                drawLine(grid, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                y += step
            }
            val points =
                pins.mapNotNull { pin ->
                    val lat = pin.latitude ?: return@mapNotNull null
                    val lng = pin.longitude ?: return@mapNotNull null
                    Triple(pin.id, lat, lng)
                }
            if (points.isEmpty() && origin?.latitude == null) return@Canvas
            val lats = points.map { it.second } + listOfNotNull(origin?.latitude)
            val lngs = points.map { it.third } + listOfNotNull(origin?.longitude)
            val minLat = lats.min()
            val maxLat = lats.max()
            val minLng = lngs.min()
            val maxLng = lngs.max()

            fun nx(lng: Double): Float {
                val span = (maxLng - minLng).takeIf { it > 0.0001 } ?: 0.02
                return ((lng - minLng) / span).toFloat().coerceIn(0.08f, 0.92f) * size.width
            }

            fun ny(lat: Double): Float {
                val span = (maxLat - minLat).takeIf { it > 0.0001 } ?: 0.02
                return (1f - ((lat - minLat) / span).toFloat().coerceIn(0.08f, 0.92f)) * size.height
            }
            points.forEach { (_, lat, lng) ->
                val center = Offset(nx(lng), ny(lat))
                drawCircle(primary, radius = 10.dp.toPx(), center = center)
                drawCircle(Color.White, radius = 4.dp.toPx(), center = center)
            }
            val oLat = origin?.latitude
            val oLng = origin?.longitude
            if (oLat != null && oLng != null) {
                drawCircle(PushpynsCyan, radius = 8.dp.toPx(), center = Offset(nx(oLng), ny(oLat)))
            }
        }
        Text(
            caption,
            modifier =
            Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

data class MapPin(val id: Long, val latitude: Double?, val longitude: Double?)
