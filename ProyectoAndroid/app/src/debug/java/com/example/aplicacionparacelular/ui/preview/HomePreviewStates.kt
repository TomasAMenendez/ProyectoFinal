package com.example.aplicacionparacelular.ui.preview

import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.network.ParentNotice
import com.example.aplicacionparacelular.ui.dashboard.HomeStatus
import com.example.aplicacionparacelular.ui.dashboard.HomeStatusView
import com.example.aplicacionparacelular.ui.dashboard.NowPlaying
import com.example.aplicacionparacelular.ui.dashboard.PlaybackSnapshot
import com.example.aplicacionparacelular.ui.dashboard.PlaybackView

enum class HomePreviewState(val labelRes: Int) {
    AWAKE(R.string.preview_state_awake),
    ASLEEP(R.string.preview_state_asleep),
    PREPARING(R.string.preview_state_preparing),
    DISCONNECTED(R.string.preview_state_disconnected),
    MUSIC(R.string.preview_state_music),
    STORY(R.string.preview_state_story),
    NO_DATA(R.string.preview_state_no_data),
}

data class HomePreviewSnapshot(
    val status: HomeStatusView,
    val playback: PlaybackView,
    val interactions: String?,
    val signal: String?,
    val notice: ParentNotice?,
)

object HomePreviewStates {
    fun snapshot(state: HomePreviewState): HomePreviewSnapshot {
        val status = when (state) {
            HomePreviewState.ASLEEP -> HomeStatus.from(true, false, true, false)
            HomePreviewState.PREPARING -> HomeStatus.from(true, true, false, true)
            HomePreviewState.DISCONNECTED -> HomeStatus.from(false, false, false, false)
            else -> HomePreviewScenario.status
        }
        val playback = when (state) {
            HomePreviewState.MUSIC -> PlaybackSnapshot(null, null, "musica_para_jugar.mp3")
            HomePreviewState.STORY -> PlaybackSnapshot("El bosque de las luces", "ejemplo-cuento", null)
            else -> PlaybackSnapshot(null, null, null)
        }
        val hasData = state != HomePreviewState.DISCONNECTED && state != HomePreviewState.NO_DATA
        return HomePreviewSnapshot(
            status = status,
            playback = NowPlaying.view(status.connected, playback, if (hasData) 1 else -1),
            interactions = HomePreviewScenario.INTERACTIONS.takeIf { hasData },
            signal = HomePreviewScenario.SIGNAL.takeIf { hasData },
            notice = HomePreviewScenario.notice.takeIf { hasData },
        )
    }
}
