package com.example.aplicacionparacelular.ui.routines

import androidx.annotation.ColorRes
import com.example.aplicacionparacelular.R

object RoutineCardTone {
    @ColorRes
    fun surface(enabled: Boolean): Int =
        if (enabled) R.color.teo_surface else R.color.teo_routine_inactive_surface

    @ColorRes
    fun emphasis(enabled: Boolean): Int =
        if (enabled) R.color.teo_text else R.color.teo_routine_inactive_text

    @ColorRes
    fun message(enabled: Boolean): Int =
        if (enabled) R.color.teo_secondary else R.color.teo_routine_inactive_message
}
