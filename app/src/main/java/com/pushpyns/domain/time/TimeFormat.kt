package com.pushpyns.domain.time

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

fun relativeTime(fromMillis: Long, nowMillis: Long): String {
    val delta = (nowMillis - fromMillis).coerceAtLeast(0)
    val minutes = delta / 60_000
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 60 * 24 -> {
            val hours = minutes / 60
            if (hours == 1L) "1 hour ago" else "$hours hours ago"
        }
        minutes < 60 * 24 * 2 -> "Yesterday"
        minutes < 60 * 24 * 7 -> "${minutes / (60 * 24)} days ago"
        else -> formatDateTime(fromMillis)
    }
}

fun formatDateTime(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
    val formatter =
        DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(Locale.getDefault())
    return Instant.ofEpochMilli(millis).atZone(zone).format(formatter)
}

fun formatDate(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
    return Instant.ofEpochMilli(millis).atZone(zone).format(formatter)
}
