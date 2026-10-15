package com.example.aplicacionparacelular.ui.alerts

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.network.ParentInbox
import com.example.aplicacionparacelular.network.ParentNotice
import com.example.aplicacionparacelular.ui.dashboard.HomeChips

object ParentNoticeRow {
    fun bind(row: View, notice: ParentNotice, nowMillis: Long = System.currentTimeMillis()) {
        val colors = HomeChips.noticeColors(notice.type)
        val icon = row.findViewById<ImageView>(R.id.img_notice_icon)
        val foreground = ContextCompat.getColor(row.context, colors.second)
        val background = ContextCompat.getColor(row.context, colors.first)
        icon.setImageResource(iconFor(notice.type))
        icon.imageTintList = ColorStateList.valueOf(foreground)
        val density = row.resources.displayMetrics.density
        icon.background = GradientDrawable().apply {
            cornerRadius = 10f * density
            setColor(background)
        }
        val label = row.findViewById<TextView>(R.id.txt_notice_label)
        label.text = ParentInbox.label(notice.type)
        label.setTextColor(foreground)
        label.background = GradientDrawable().apply {
            cornerRadius = 10f * density
            setColor(background)
        }
        row.findViewById<TextView>(R.id.txt_notice_message).text = notice.message
        val time = row.findViewById<TextView>(R.id.txt_notice_time)
        val whenLabel = NoticeTime.label(notice.eventAtMillis, nowMillis)
        if (whenLabel == null) {
            time.visibility = View.GONE
        } else {
            time.visibility = View.VISIBLE
            time.text = whenLabel
        }
    }

    private fun iconFor(type: String): Int = when (type.trim().lowercase()) {
        "logro" -> R.drawable.teo_ic_star
        "pedido" -> R.drawable.teo_ic_message
        "crisis" -> R.drawable.teo_ic_bell
        else -> R.drawable.teo_ic_note
    }
}
