package com.example.aplicacionparacelular.ui.dashboard

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.doOnLayout
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.FragmentDashboardBinding
import com.example.aplicacionparacelular.network.ParentInbox
import com.example.aplicacionparacelular.network.ParentNotice
import java.text.DateFormat
import java.util.Date

object HomeScreenRenderer {
    fun status(binding: FragmentDashboardBinding, state: HomeStatusView, waiting: Boolean) {
        binding.txtConnection.text = state.connectionLabel
        binding.txtActivity.text = state.activityLabel
        val connection = HomeChips.connectionColors(state.connected)
        HomeChips.apply(binding.txtConnection, connection.first, connection.second, R.drawable.teo_ic_dot)
        val activity = HomeChips.activityColors(state.activity)
        HomeChips.apply(
            binding.txtActivity,
            activity.first,
            activity.second,
            HomeChips.activityIcon(state.activity),
        )
        binding.btnPower.text = state.actionLabel
        binding.btnPower.isEnabled = state.actionEnabled && !waiting
        val celebrate = state.connected && !waiting
        binding.btnCelebrate.isEnabled = celebrate
        binding.btnCelebrate.isClickable = celebrate
        binding.btnCelebrate.alpha = if (celebrate) 1f else 0.55f
        if (binding.statusChips.width > 0) {
            stack(binding, binding.root.resources.configuration.fontScale, binding.root.resources.configuration.screenWidthDp)
        }
    }

    fun interactions(binding: FragmentDashboardBinding, text: String) {
        binding.txtInteractions.text = text
        val compact = text.length > 4
        binding.txtInteractions.setTextAppearance(
            if (compact) R.style.TextAppearance_Teo_Home_Section else R.style.TextAppearance_Teo_Home_Metric,
        )
        val word = binding.root.context.getString(R.string.home_interactions_word)
        binding.interactionsBubble.contentDescription = "$text $word"
    }

    fun signal(binding: FragmentDashboardBinding, text: String, known: Boolean) {
        binding.txtSignal.text = text
        val colors = if (known) {
            R.color.teo_cyan to R.color.teo_on_cyan
        } else {
            R.color.teo_neutral to R.color.teo_on_neutral
        }
        HomeChips.apply(binding.txtSignal, colors.first, colors.second, 0)
    }

    fun playback(binding: FragmentDashboardBinding, view: PlaybackView, waiting: Boolean) {
        binding.txtNowPlaying.text = view.title
        binding.btnMusicPlay.text = view.actionLabel
        binding.btnMusicPlay.isEnabled = view.playEnabled && !waiting
        binding.btnMusicStop.isEnabled = view.stopEnabled && !waiting
        binding.txtMusicHint.visibility = if (view.hint == null) View.GONE else View.VISIBLE
        binding.txtMusicHint.text = view.hint
    }

    fun notice(binding: FragmentDashboardBinding, notice: ParentNotice?) {
        val empty = notice == null
        binding.txtAlertEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        binding.alertRow.visibility = if (empty) View.GONE else View.VISIBLE
        if (notice == null) {
            binding.txtAlertTime.visibility = View.GONE
            return
        }
        binding.txtAlertLabel.text = ParentInbox.label(notice.type)
        binding.txtAlertMessage.text = notice.message
        val colors = HomeChips.noticeColors(notice.type)
        HomeChips.apply(binding.txtAlertLabel, colors.first, colors.second, 0)
        val eventAt = notice.eventAtMillis
        if (eventAt == null) {
            binding.txtAlertTime.visibility = View.GONE
        } else {
            binding.txtAlertTime.visibility = View.VISIBLE
            binding.txtAlertTime.text = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(Date(eventAt))
        }
    }

    fun stack(binding: FragmentDashboardBinding, fontScale: Float, widthDp: Int) {
        val stack = fontScale >= 1.3f || widthDp < 360
        val gap = binding.root.resources.getDimensionPixelSize(R.dimen.teo_home_control_gap)
        val statusWidth = binding.statusChips.width
        if (statusWidth == 0) {
            binding.statusChips.doOnLayout { stack(binding, fontScale, widthDp) }
        }
        val chipWidth = (statusWidth - gap) / 2f
        val stackStatus = stack || (statusWidth > 0 &&
            (!fitsStatusChip(binding.txtConnection, chipWidth) || !fitsStatusChip(binding.txtActivity, chipWidth)))
        binding.statusChips.orientation = if (stackStatus) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        val connection = binding.txtConnection.layoutParams as LinearLayout.LayoutParams
        val activity = binding.txtActivity.layoutParams as LinearLayout.LayoutParams
        if (stackStatus) {
            connection.width = LinearLayout.LayoutParams.MATCH_PARENT
            connection.weight = 0f
            activity.width = LinearLayout.LayoutParams.MATCH_PARENT
            activity.weight = 0f
            activity.marginStart = 0
            activity.topMargin = gap
        } else {
            connection.width = 0
            connection.weight = 1f
            activity.width = 0
            activity.weight = 1f
            activity.marginStart = gap
            activity.topMargin = 0
        }
        binding.txtConnection.layoutParams = connection
        binding.txtActivity.layoutParams = activity

        binding.playbackActions.orientation = if (stack) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        val play = binding.btnMusicPlay.layoutParams as LinearLayout.LayoutParams
        val stop = binding.btnMusicStop.layoutParams as LinearLayout.LayoutParams
        if (stack) {
            play.width = LinearLayout.LayoutParams.MATCH_PARENT
            play.weight = 0f
            stop.width = LinearLayout.LayoutParams.MATCH_PARENT
            stop.weight = 0f
            play.marginEnd = 0
            stop.topMargin = gap
        } else {
            play.width = 0
            play.weight = 1f
            stop.width = LinearLayout.LayoutParams.WRAP_CONTENT
            stop.weight = 0f
            play.marginEnd = gap
            stop.topMargin = 0
        }
        binding.btnMusicPlay.layoutParams = play
        binding.btnMusicStop.layoutParams = stop
    }

    private fun fitsStatusChip(view: TextView, availableWidth: Float): Boolean =
        view.paint.measureText(view.text.toString()) + view.compoundPaddingLeft + view.compoundPaddingRight <= availableWidth
}
