package com.example.aplicacionparacelular.ui.preview

import com.example.aplicacionparacelular.network.ParentNotice
import com.example.aplicacionparacelular.ui.dashboard.HomeStatus
import com.example.aplicacionparacelular.ui.dashboard.HomeStatusView
import com.example.aplicacionparacelular.ui.dashboard.NowPlaying
import com.example.aplicacionparacelular.ui.dashboard.PlaybackSnapshot
import com.example.aplicacionparacelular.ui.dashboard.PlaybackView

object HomePreviewScenario {
    const val INTERACTIONS = "12"
    const val SIGNAL = "Tranquilo"

    val status: HomeStatusView = HomeStatus.from(
        connected = true,
        powerOn = true,
        modelsReady = true,
        pendingWake = false,
    )

    val playback: PlaybackView = NowPlaying.view(
        connected = true,
        snapshot = PlaybackSnapshot(storyTitle = null, storyId = null, songName = null),
        catalogCount = 1,
    )

    val notice = ParentNotice(
        type = "logro",
        message = "Ejemplo: el nene logró quedarse tranquilo.",
        eventAtMillis = 1_760_457_600_000L,
        receivedAtMillis = 1_760_457_600_000L,
    )
}
