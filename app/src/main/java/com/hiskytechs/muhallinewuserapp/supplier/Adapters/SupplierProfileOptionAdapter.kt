package com.hiskytechs.muhallinewuserapp.supplier.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.databinding.ItemSupplierProfileOptionBinding
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierProfileOption

class SupplierProfileOptionAdapter(
    private val items: List<SupplierProfileOption>,
    private val onClick: (SupplierProfileOption) -> Unit
) : RecyclerView.Adapter<SupplierProfileOptionAdapter.SupplierProfileOptionViewHolder>() {

    inner class SupplierProfileOptionViewHolder(
        private val binding: ItemSupplierProfileOptionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SupplierProfileOption) {
            val context = binding.root.context
            binding.ivOptionIcon.setImageResource(item.iconRes)
            binding.tvOptionTitle.text = item.title
            val color = ContextCompat.getColor(
                context,
                if (item.isDanger) R.color.status_cancelled_text else R.color.text_dark
            )
            binding.tvOptionTitle.setTextColor(color)
            binding.ivOptionIcon.imageTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    if (item.isDanger) R.color.status_cancelled_text else R.color.primary
                )
            )
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SupplierProfileOptionViewHolder {
        val binding = ItemSupplierProfileOptionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SupplierProfileOptionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SupplierProfileOptionViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
