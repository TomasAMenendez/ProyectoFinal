package com.example.aplicacionparacelular.network

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

data class ParentNotice(
    val type: String,
    val message: String,
    val eventAtMillis: Long?,
    val receivedAtMillis: Long,
)

object ParentInbox {
    const val LIMIT = 20

    private val types = setOf("aviso", "logro", "pedido", "crisis")

    fun accepts(type: String): Boolean = type.trim().lowercase() in types

    fun label(type: String): String = when (type.trim().lowercase()) {
        "logro" -> "Logro"
        "pedido" -> "Pedido"
        "crisis" -> "Atención"
        else -> "Aviso"
    }

    fun parseEventMillis(raw: String?): Long? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty() || text.equals("null", ignoreCase = true)) return null
        val normalized = text.replace(Regex("""(\.\d{3})\d+"""), "$1")
        val patterns = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
        )
        for (pattern in patterns) {
            try {
                val format = SimpleDateFormat(pattern, Locale.US)
                format.isLenient = false
                return format.parse(normalized)?.time
            } catch (_: ParseException) {
            }
        }
        return null
    }

    fun notice(type: String, message: String, timestamp: String?, receivedAtMillis: Long): ParentNotice? {
        if (!accepts(type) || message.isBlank()) return null
        return ParentNotice(
            type = type.trim().lowercase(),
            message = message.trim(),
            eventAtMillis = parseEventMillis(timestamp),
            receivedAtMillis = receivedAtMillis,
        )
    }

    fun sortKey(notice: ParentNotice): Long = notice.eventAtMillis ?: notice.receivedAtMillis

    fun merge(existing: List<ParentNotice>, incoming: List<ParentNotice>, limit: Int = LIMIT): List<ParentNotice> {
        return (existing + incoming)
            .sortedWith(compareByDescending<ParentNotice> { sortKey(it) }.thenByDescending { it.receivedAtMillis })
            .take(limit)
    }
}
