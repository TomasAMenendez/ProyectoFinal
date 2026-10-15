package com.example.aplicacionparacelular.ui

import android.view.View
import android.widget.ScrollView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.max

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
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val bottomInset = if (bottom) max(bars.bottom, ime) else 0
            target.setPadding(
                initialLeft,
                initialTop + if (top) bars.top else 0,
                initialRight,
                initialBottom + bottomInset,
            )
            if (bottom && ime > 0) {
                target.post { target.post { revealFocused(target, ime) } }
            }
            insets
        }
        ViewCompat.requestApplyInsets(view)
    }

    private fun revealFocused(container: View, imeBottom: Int) {
        val focused = container.findFocus() ?: return
        if (container !is ScrollView) return
        val location = IntArray(2)
        focused.getLocationOnScreen(location)
        val gap = (24 * container.resources.displayMetrics.density).toInt()
        val limit = container.rootView.height - imeBottom - gap
        val overlap = location[1] + focused.height - limit
        if (overlap > 0) container.scrollBy(0, overlap)
    }
}
