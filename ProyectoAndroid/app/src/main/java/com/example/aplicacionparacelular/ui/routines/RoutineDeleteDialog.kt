package com.example.aplicacionparacelular.ui.routines

import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import com.example.aplicacionparacelular.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object RoutineDeleteDialog {
    fun show(
        context: Context,
        name: String,
        title: String = context.getString(R.string.routines_delete_title),
        question: String = RoutineDraft.deleteQuestion(name),
        onConfirm: () -> Unit,
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_delete_routine, null)
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(view)
            .create()
        view.findViewById<TextView>(R.id.txt_delete_title).text = title
        view.findViewById<TextView>(R.id.txt_delete_message).text = question
        view.findViewById<android.view.View>(R.id.btn_delete_cancel).setOnClickListener { dialog.dismiss() }
        view.findViewById<android.view.View>(R.id.btn_delete_confirm).setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(R.drawable.teo_dialog_surface)
    }
}
