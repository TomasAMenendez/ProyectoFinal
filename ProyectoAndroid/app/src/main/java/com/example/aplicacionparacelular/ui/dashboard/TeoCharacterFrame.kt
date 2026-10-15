package com.example.aplicacionparacelular.ui.dashboard

data class OpaqueBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int get() = (right - left).coerceAtLeast(1)
    val height: Int get() = (bottom - top).coerceAtLeast(1)
}

data class FramePlacement(
    val scale: Float,
    val translateX: Float,
    val translateY: Float,
)

object TeoCharacterFrame {
    fun bounds(width: Int, height: Int, isOpaque: (x: Int, y: Int) -> Boolean): OpaqueBounds? {
        if (width <= 0 || height <= 0) return null
        var left = width
        var top = height
        var right = -1
        var bottom = -1
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (!isOpaque(x, y)) continue
                if (x < left) left = x
                if (y < top) top = y
                if (x > right) right = x
                if (y > bottom) bottom = y
            }
        }
        if (right < 0 || bottom < 0) return null
        return OpaqueBounds(left, top, right + 1, bottom + 1)
    }

    fun uniformScale(contentWidth: Int, contentHeight: Int, slotWidth: Int, slotHeight: Int): Float {
        if (contentWidth <= 0 || contentHeight <= 0 || slotWidth <= 0 || slotHeight <= 0) return 1f
        return minOf(slotWidth.toFloat() / contentWidth, slotHeight.toFloat() / contentHeight)
    }

    fun placement(
        bounds: OpaqueBounds,
        slotWidth: Int,
        slotHeight: Int,
    ): FramePlacement {
        val scale = uniformScale(bounds.width, bounds.height, slotWidth, slotHeight)
        val drawnWidth = bounds.width * scale
        val drawnHeight = bounds.height * scale
        return FramePlacement(
            scale = scale,
            translateX = (slotWidth - drawnWidth) / 2f - bounds.left * scale,
            translateY = (slotHeight - drawnHeight) / 2f - bounds.top * scale,
        )
    }
}
