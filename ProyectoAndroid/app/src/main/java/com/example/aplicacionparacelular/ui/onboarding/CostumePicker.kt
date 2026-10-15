package com.example.aplicacionparacelular.ui.onboarding

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.DialogCostumePickerBinding
import com.example.aplicacionparacelular.databinding.ItemTeoCostumeBinding
import com.example.aplicacionparacelular.ui.character.TeoCostumeCatalog
import com.example.aplicacionparacelular.ui.dashboard.TeoCharacterPresenter
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object CostumePicker {
    fun show(context: Context, selectedId: String, onConfirm: (String) -> Unit): androidx.appcompat.app.AlertDialog {
        val binding = DialogCostumePickerBinding.inflate(LayoutInflater.from(context))
        val adapter = CostumeAdapter(selectedId)
        val metrics = context.resources.displayMetrics
        val columns = if (context.resources.configuration.screenWidthDp >= 360 &&
            context.resources.configuration.fontScale <= 1.3f) 3 else 2
        binding.costumeGrid.layoutManager = GridLayoutManager(context, columns)
        binding.costumeGrid.adapter = adapter
        val rows = (TeoCostumeCatalog.entries.size + columns - 1) / columns
        val desired = (rows * 144 * metrics.density).toInt()
        binding.costumeGrid.layoutParams.height = minOf(desired, (metrics.heightPixels * 0.58f).toInt())
        val dialog = MaterialAlertDialogBuilder(context).setView(binding.root).create()
        binding.btnCancel.setOnClickListener { dialog.dismiss() }
        binding.btnChoose.setOnClickListener {
            onConfirm(adapter.selectedId)
            dialog.dismiss()
        }
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(R.drawable.teo_dialog_surface)
        return dialog
    }

    private class CostumeAdapter(var selectedId: String) : RecyclerView.Adapter<CostumeAdapter.Holder>() {
        class Holder(val binding: ItemTeoCostumeBinding) : RecyclerView.ViewHolder(binding.root)
        override fun getItemCount() = TeoCostumeCatalog.entries.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
            ItemTeoCostumeBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        )
        override fun onBindViewHolder(holder: Holder, position: Int) {
            val entry = TeoCostumeCatalog.entries[position]
            val binding = holder.binding
            val chosen = entry.id == selectedId
            binding.txtCostume.setText(entry.labelRes)
            TeoCharacterPresenter.applyFront(binding.imgCostume, entry.id)
            binding.root.apply {
                isChecked = chosen
                isSelected = chosen
                contentDescription = context.getString(entry.labelRes)
                setCardBackgroundColor(ContextCompat.getColor(context, if (chosen) R.color.teo_cyan else R.color.teo_surface))
                strokeColor = ContextCompat.getColor(context, if (chosen) R.color.teo_primary else R.color.teo_disabled)
                strokeWidth = (resources.displayMetrics.density * if (chosen) 2 else 1).toInt()
                setOnClickListener {
                    val old = TeoCostumeCatalog.entries.indexOfFirst { it.id == selectedId }
                    selectedId = entry.id
                    notifyItemChanged(old)
                    notifyItemChanged(position)
                }
            }
        }
    }
}
