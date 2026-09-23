package com.example.aplicacionparacelular.ui.dashboard

data class PlaytimeRing(
    val visible: Boolean,
    val fraction: Float,
    val label: String,
    val startAngle: Float,
    val sweepAngle: Float,
) {
    companion object {
        fun from(limitMinutes: Int, remainingSeconds: Int): PlaytimeRing {
            if (limitMinutes <= 0) {
                return PlaytimeRing(
                    visible = false,
                    fraction = 0f,
                    label = "",
                    startAngle = -90f,
                    sweepAngle = 0f,
                )
            }
            val cap = limitMinutes * 60
            val remaining = remainingSeconds.coerceAtLeast(0).coerceAtMost(cap)
            val fraction = if (cap <= 0) 0f else remaining.toFloat() / cap.toFloat()
            val startAngle = -90f + (1f - fraction) * 360f
            val minutes = remaining / 60
            val seconds = remaining % 60
            val label = "%d:%02d".format(minutes, seconds)
            return PlaytimeRing(
                visible = true,
                fraction = fraction,
                label = label,
                startAngle = startAngle,
                sweepAngle = fraction * 360f,
            )
        }
    }
}
