package com.example.aplicacionparacelular.ui.dashboard

data class PlaytimeClock(
    val limitMinutes: Int,
    val sampledRemaining: Int,
    val sampledAtMs: Long,
    val running: Boolean,
) {
    fun remainingAt(nowMs: Long): Int {
        if (limitMinutes <= 0) return 0
        if (!running) return sampledRemaining.coerceAtLeast(0)
        val elapsed = ((nowMs - sampledAtMs).coerceAtLeast(0L) / 1000L).toInt()
        return (sampledRemaining - elapsed).coerceAtLeast(0)
    }

    fun absorb(limitMinutes: Int, piRemaining: Int, nowMs: Long, running: Boolean): PlaytimeClock {
        if (limitMinutes <= 0) {
            return PlaytimeClock(0, 0, nowMs, false)
        }
        val pi = piRemaining.coerceAtLeast(0)
        if (this.limitMinutes != limitMinutes) {
            return PlaytimeClock(limitMinutes, pi, nowMs, running)
        }
        val local = remainingAt(nowMs)
        val merged = if (pi > local + 10) pi else minOf(local, pi)
        return PlaytimeClock(limitMinutes, merged, nowMs, running)
    }

    companion object {
        fun fromPi(limitMinutes: Int, piRemaining: Int, nowMs: Long, running: Boolean): PlaytimeClock {
            if (limitMinutes <= 0) return PlaytimeClock(0, 0, nowMs, false)
            return PlaytimeClock(limitMinutes, piRemaining.coerceAtLeast(0), nowMs, running)
        }
    }
}
