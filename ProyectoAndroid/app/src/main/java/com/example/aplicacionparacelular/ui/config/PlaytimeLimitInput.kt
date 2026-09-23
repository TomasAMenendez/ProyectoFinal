package com.example.aplicacionparacelular.ui.config

object PlaytimeLimitInput {
    fun parseMinutes(raw: String): Int? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return 0
        val value = trimmed.toIntOrNull() ?: return null
        return value.coerceIn(0, 240)
    }
}
