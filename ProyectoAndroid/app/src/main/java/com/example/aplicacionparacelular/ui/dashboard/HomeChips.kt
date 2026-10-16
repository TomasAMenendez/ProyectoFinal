package com.example.aplicacionparacelular.ui.dashboard

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.aplicacionparacelular.R

object HomeChips {
    fun connectionColors(connected: Boolean): Pair<Int, Int> = if (connected) {
        R.color.teo_success to R.color.teo_on_success
    } else {
        R.color.teo_neutral to R.color.teo_on_neutral
    }

    fun activityColors(activity: TeoActivity): Pair<Int, Int> = when (activity) {
        TeoActivity.AWAKE -> R.color.teo_gold to R.color.teo_on_gold
        TeoActivity.ASLEEP -> R.color.teo_sleep to R.color.teo_on_sleep
        TeoActivity.PREPARING -> R.color.teo_gold to R.color.teo_on_gold
        TeoActivity.UNAVAILABLE -> R.color.teo_neutral to R.color.teo_on_neutral
    }

    fun activityIcon(activity: TeoActivity): Int = when (activity) {
        TeoActivity.AWAKE -> R.drawable.teo_ic_star
        TeoActivity.ASLEEP -> R.drawable.teo_ic_moon
        else -> R.drawable.teo_ic_dot
    }

    fun noticeColors(type: String): Pair<Int, Int> = when (type.trim().lowercase()) {
        "logro" -> R.color.teo_gold to R.color.teo_on_gold
        "pedido" -> R.color.teo_cyan to R.color.teo_on_cyan
        "crisis" -> R.color.teo_error_bg to R.color.teo_error
        else -> R.color.teo_neutral to R.color.teo_on_neutral
    }

    fun apply(view: TextView, background: Int, foreground: Int, icon: Int) {
        val color = ContextCompat.getColor(view.context, foreground)
        view.setTextColor(color)
        val density = view.resources.displayMetrics.density
        view.background = GradientDrawable().apply {
            cornerRadius = 14f * density
            setColor(ContextCompat.getColor(view.context, background))
        }
        view.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0)
        view.compoundDrawableTintList = ColorStateList.valueOf(color)
    }
}
