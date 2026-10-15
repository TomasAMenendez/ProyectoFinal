package com.example.aplicacionparacelular.ui.routines

object RoutineDisplayOrder {
    fun sorted(items: List<RoutineItem>): List<RoutineItem> = items.sortedBy { item ->
        val parts = item.time.trim().split(':')
        val hour = parts.getOrNull(0)?.toIntOrNull()
        val minute = parts.getOrNull(1)?.toIntOrNull()
        if (parts.size == 2 && hour != null && hour in 0..23 && minute != null && minute in 0..59) {
            hour * 60 + minute
        } else {
            Int.MAX_VALUE
        }
    }
}
