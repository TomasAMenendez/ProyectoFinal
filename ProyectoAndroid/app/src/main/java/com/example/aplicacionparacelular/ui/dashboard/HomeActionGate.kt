package com.example.aplicacionparacelular.ui.dashboard

enum class HomeAction {
    POWER,
    PLAY,
    STOP,
    CELEBRATE,
}

class HomeActionGate {
    private val busy = linkedSetOf<HomeAction>()

    fun tryStart(action: HomeAction): Boolean {
        if (action in busy) return false
        busy.add(action)
        return true
    }

    fun finish(action: HomeAction) {
        busy.remove(action)
    }

    fun isBusy(action: HomeAction): Boolean = action in busy

    val waiting: Boolean get() = busy.isNotEmpty()
}
