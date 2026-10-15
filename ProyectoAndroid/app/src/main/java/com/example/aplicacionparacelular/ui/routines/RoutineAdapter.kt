package com.example.aplicacionparacelular.ui.routines

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.ItemRoutineBinding

class RoutineAdapter(
    private val onToggle: (RoutineItem) -> Unit,
    private val onEdit: (RoutineItem) -> Unit,
    private val onDelete: (RoutineItem) -> Unit,
) : RecyclerView.Adapter<RoutineAdapter.ViewHolder>() {

    private var items: List<RoutineItem> = emptyList()

    fun submitList(newItems: List<RoutineItem>) {
        items = RoutineDisplayOrder.sorted(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRoutineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], onToggle, onEdit, onDelete)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(private val binding: ItemRoutineBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            item: RoutineItem,
            onToggle: (RoutineItem) -> Unit,
            onEdit: (RoutineItem) -> Unit,
            onDelete: (RoutineItem) -> Unit,
        ) {
            val context = binding.root.context
            binding.txtTime.text = item.time
            binding.txtName.text = item.name
            binding.txtMessage.text = item.reminderMessage
            binding.root.setCardBackgroundColor(ContextCompat.getColor(context, RoutineCardTone.surface(item.enabled)))
            val emphasis = ContextCompat.getColor(context, RoutineCardTone.emphasis(item.enabled))
            binding.txtTime.setTextColor(emphasis)
            binding.txtName.setTextColor(emphasis)
            binding.txtMessage.setTextColor(ContextCompat.getColor(context, RoutineCardTone.message(item.enabled)))
            binding.root.alpha = 1f
            binding.switchEnabled.isEnabled = true
            binding.btnEdit.isEnabled = true
            binding.btnDelete.isEnabled = true
            binding.switchEnabled.contentDescription = item.name
            ViewCompat.setStateDescription(
                binding.switchEnabled,
                context.getString(if (item.enabled) R.string.routines_enabled else R.string.routines_disabled),
            )
            binding.switchEnabled.setOnCheckedChangeListener(null)
            binding.switchEnabled.isChecked = item.enabled
            binding.switchEnabled.setOnCheckedChangeListener { _, _ -> onToggle(item) }
            binding.btnEdit.setOnClickListener { onEdit(item) }
            binding.btnDelete.setOnClickListener { onDelete(item) }
        }
    }
}
