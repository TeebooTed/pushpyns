package com.pushpyns.data.media

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppFiles
@Inject
constructor(@ApplicationContext private val context: Context) {
    fun file(relativePath: String): File = File(context.filesDir, relativePath)

    fun exists(relativePath: String): Boolean = file(relativePath).exists()

    fun delete(relativePath: String) {
        file(relativePath).delete()
    }

    fun allocate(folder: String, prefix: String, extension: String): String {
        val dir = File(context.filesDir, folder)
        dir.mkdirs()
        val name = "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.$extension"
        val relative = "$folder/$name"
        file(relative).createNewFile()
        return relative
    }

    fun write(relativePath: String, bytes: ByteArray) {
        val target = file(relativePath)
        target.parentFile?.mkdirs()
        target.writeBytes(bytes)
    }

    fun copyFrom(uri: Uri, folder: String): String {
        val relative = allocate(folder, "img", "jpg")
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Couldn't read the selected photo" }
            file(relative).outputStream().use { output -> input.copyTo(output) }
        }
        return relative
    }

    fun duplicate(relativePath: String, folder: String): String {
        val source = file(relativePath)
        val ext = source.extension.ifBlank { "jpg" }
        val copy = allocate(folder, "copy", ext)
        source.inputStream().use { input ->
            file(copy).outputStream().use { output -> input.copyTo(output) }
        }
        return copy
    }

    fun contentUri(relativePath: String): Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file(relativePath),
    )
}
