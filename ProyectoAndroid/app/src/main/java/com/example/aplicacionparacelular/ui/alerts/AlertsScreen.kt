package com.example.aplicacionparacelular.ui.alerts

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.network.ParentNotice

object AlertsScreen {
    fun bind(
        list: LinearLayout,
        empty: TextView,
        offline: TextView,
        notices: List<ParentNotice>,
        connected: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        offline.visibility = if (connected) View.GONE else View.VISIBLE
        empty.visibility = if (notices.isEmpty()) View.VISIBLE else View.GONE
        list.visibility = if (notices.isEmpty()) View.GONE else View.VISIBLE
        list.removeAllViews()
        val inflater = LayoutInflater.from(list.context)
        for (notice in notices) {
            val row = inflater.inflate(R.layout.item_parent_notice, list, false)
            ParentNoticeRow.bind(row, notice, nowMillis)
            list.addView(row)
        }
    }
}
