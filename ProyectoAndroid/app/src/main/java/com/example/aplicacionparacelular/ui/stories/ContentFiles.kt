package com.example.aplicacionparacelular.ui.stories

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import java.util.concurrent.Executors

data class PickedFile(
    val name: String,
    val bytes: ByteArray,
)

data class FileSummary(
    val name: String,
    val sizeBytes: Long,
)

object ContentFiles {
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    fun read(context: Context, uri: Uri, fallbackName: String, onResult: (PickedFile?) -> Unit) {
        val appContext = context.applicationContext
        executor.execute {
            val picked = try {
                val name = displayName(appContext, uri, fallbackName)
                val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null) null else PickedFile(name, bytes)
            } catch (_: Exception) {
                null
            }
            main.post { onResult(picked) }
        }
    }

    fun describe(context: Context, uri: Uri, fallbackName: String, onResult: (FileSummary?) -> Unit) {
        val appContext = context.applicationContext
        executor.execute {
            val summary = try {
                val name = displayName(appContext, uri, fallbackName)
                val declared = declaredSize(appContext, uri)
                val size = if (declared >= 0) declared else countBytes(appContext, uri)
                if (size < 0) null else FileSummary(name, size)
            } catch (_: Exception) {
                null
            }
            main.post { onResult(summary) }
        }
    }

    private fun displayName(context: Context, uri: Uri, fallback: String): String {
        val cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    val name = it.getString(index)?.trim().orEmpty()
                    if (name.isNotEmpty()) return name
                }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { fallback } ?: fallback
    }

    private fun declaredSize(context: Context, uri: Uri): Long {
        val cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.SIZE)
                if (index >= 0 && !it.isNull(index)) return it.getLong(index)
            }
        }
        return -1
    }

    private fun countBytes(context: Context, uri: Uri): Long {
        val stream = context.contentResolver.openInputStream(uri) ?: return -1
        stream.use { input ->
            val buffer = ByteArray(8192)
            var total = 0L
            while (true) {
                val read = input.read(buffer)
                if (read < 0) return total
                total += read
            }
        }
    }
}
