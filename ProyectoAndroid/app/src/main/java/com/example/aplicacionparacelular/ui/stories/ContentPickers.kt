package com.example.aplicacionparacelular.ui.stories

import android.content.Intent
import android.widget.TextView
import androidx.core.view.isVisible
import com.example.aplicacionparacelular.R

object ContentPickers {
    fun audio(): Intent = Intent(Intent.ACTION_GET_CONTENT).apply {
        type = "audio/*"
        addCategory(Intent.CATEGORY_OPENABLE)
    }

    fun pdf(): Intent = Intent(Intent.ACTION_GET_CONTENT).apply {
        type = "application/pdf"
        addCategory(Intent.CATEGORY_OPENABLE)
    }

    fun bindPdf(name: TextView, meta: TextView, button: TextView, fileName: String?, sizeBytes: Long) {
        if (fileName.isNullOrBlank()) {
            name.setText(R.string.content_pdf_empty)
            meta.text = ""
            meta.isVisible = false
            button.setText(R.string.content_choose_pdf)
            return
        }
        name.text = fileName
        meta.isVisible = true
        meta.text = "PDF · ${ContentRules.sizeLabel(sizeBytes)}"
        button.setText(R.string.content_change_file)
    }
}
