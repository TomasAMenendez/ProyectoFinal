package com.example.aplicacionparacelular.network

object ParentInbox {
    private val types = setOf("aviso", "logro", "pedido", "crisis")

    fun accepts(type: String): Boolean = type.trim().lowercase() in types

    fun formatLine(type: String, message: String): String {
        val prefix = when (type.lowercase()) {
            "crisis" -> "⚠️ Crisis"
            "pedido" -> "📞 Pedido"
            "musica" -> "🎵 Música"
            "logro" -> "🎉 Logro"
            "vocabulario" -> "📚 Vocabulario"
            "rutina" -> "🗓️ Rutina"
            "vacuna" -> "💉 Vacuna"
            "turno" -> "🩺 Turno"
            else -> "ℹ️ Aviso"
        }
        return "$prefix: $message"
    }

    fun visibleLines(items: List<Pair<String, String>>): List<String> {
        return items.mapNotNull { (type, message) ->
            if (!accepts(type) || message.isBlank()) null
            else formatLine(type, message)
        }
    }
}
