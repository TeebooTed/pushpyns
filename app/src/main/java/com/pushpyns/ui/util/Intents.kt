package com.pushpyns.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.net.URLEncoder

fun Context.shareText(text: String, title: String) {
    val intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
    startActivity(Intent.createChooser(intent, title))
}

fun Context.sharePdf(file: File, subject: String) {
    val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
    val intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    startActivity(Intent.createChooser(intent, subject))
}

fun Context.openDirections(latitude: Double, longitude: Double, label: String): Boolean {
    val encoded = URLEncoder.encode(label, Charsets.UTF_8.name())
    val maps =
        Uri.parse(
            "https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude&travelmode=driving",
        )
    val geo = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($encoded)")
    val mapsIntent = Intent(Intent.ACTION_VIEW, maps)
    val geoIntent = Intent(Intent.ACTION_VIEW, geo)
    return try {
        startActivity(mapsIntent)
        true
    } catch (_: ActivityNotFoundException) {
        try {
            startActivity(geoIntent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}

fun Context.openAppSettings() {
    val intent =
        Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
    startActivity(intent)
}
