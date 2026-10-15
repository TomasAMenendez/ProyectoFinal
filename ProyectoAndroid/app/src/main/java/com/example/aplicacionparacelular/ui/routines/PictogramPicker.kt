package com.example.aplicacionparacelular.ui.routines

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.aplicacionparacelular.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object PictogramPicker {
    fun show(context: Context, selectedId: String?, onConfirm: (String?) -> Unit) {
        val options = listOf<String?>(null) + PictogramCatalog.entries.map { it.id }
        var pending = selectedId?.trim()?.ifEmpty { null }
        val pad = (context.resources.displayMetrics.density * 16).toInt()
        val rows = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, 0)
        }
        rows.addView(TextView(context).apply {
            setText(R.string.routines_pictogram_label)
            setTextAppearance(R.style.TextAppearance_Teo_Home_Section)
            setTextColor(ContextCompat.getColor(context, R.color.teo_primary))
            setPadding(0, 0, 0, pad / 2)
        })
        val marks = mutableListOf<Pair<String?, View>>()
        val inflater = LayoutInflater.from(context)
        options.forEach { id ->
            val row = inflater.inflate(R.layout.item_pictogram_option, rows, false)
            val image = row.findViewById<ImageView>(R.id.img_option)
            val label = row.findViewById<TextView>(R.id.txt_option)
            val mark = row.findViewById<ImageView>(R.id.img_selected)
            mark.setColorFilter(ContextCompat.getColor(context, R.color.teo_primary))
            val entry = PictogramCatalog.find(id)
            if (entry == null) {
                image.setImageDrawable(null)
                image.visibility = View.INVISIBLE
                label.setText(R.string.routines_pictogram_none)
            } else {
                image.setImageResource(entry.drawableRes)
                image.visibility = View.VISIBLE
                label.setText(entry.labelRes)
            }
            marks += id to row
            row.setOnClickListener {
                pending = id
                refresh(marks, pending, context)
            }
            rows.addView(row)
        }
        refresh(marks, pending, context)
        val scroll = ScrollView(context).apply { addView(rows) }
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(scroll)
            .setNegativeButton(R.string.routines_cancel, null)
            .setPositiveButton(R.string.routines_pictogram_done) { _, _ -> onConfirm(pending) }
            .create()
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(R.drawable.teo_dialog_surface)
        val buttonColor = ContextCompat.getColor(context, R.color.teo_primary)
        listOf(AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_POSITIVE).forEach { which ->
            dialog.getButton(which)?.apply {
                isAllCaps = false
                letterSpacing = 0f
                setTextColor(buttonColor)
            }
        }
    }

    private fun refresh(rows: List<Pair<String?, View>>, pending: String?, context: Context) {
        val selected = ContextCompat.getColor(context, R.color.teo_background)
        rows.forEach { (id, row) ->
            val mark = row.findViewById<ImageView>(R.id.img_selected)
            val chosen = id == pending
            mark.visibility = if (chosen) View.VISIBLE else View.INVISIBLE
            row.setBackgroundColor(if (chosen) selected else android.graphics.Color.TRANSPARENT)
        }
    }
}
