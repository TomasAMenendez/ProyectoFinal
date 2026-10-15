package com.example.aplicacionparacelular.ui

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

object SystemBarInsets {
    fun applyTop(view: View) = apply(view, top = true, bottom = false)

    fun applyBottom(view: View) = apply(view, top = false, bottom = true)

    fun applyVertical(view: View) = apply(view, top = true, bottom = true)

    private fun apply(view: View, top: Boolean, bottom: Boolean) {
        val initialLeft = view.paddingLeft
        val initialTop = view.paddingTop
        val initialRight = view.paddingRight
        val initialBottom = view.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(view) { target, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            target.setPadding(
                initialLeft,
                initialTop + if (top) bars.top else 0,
                initialRight,
                initialBottom + if (bottom) bars.bottom else 0,
            )
            insets
        }
        ViewCompat.requestApplyInsets(view)
    }
}
