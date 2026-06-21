package com.hiskytechs.muhallinewuserapp.supplier.Adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.hiskytechs.muhallinewuserapp.databinding.ItemSupplierQuickActionBinding
import com.hiskytechs.muhallinewuserapp.databinding.ItemSupplierRecentOrderBinding
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierOrder
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierQuickAction
import com.hiskytechs.muhallinewuserapp.supplier.Utill.formatPkr
import com.hiskytechs.muhallinewuserapp.supplier.Utill.orderStatusBackground
import com.hiskytechs.muhallinewuserapp.supplier.Utill.orderStatusTextColor

class SupplierQuickActionAdapter(
    private val items: List<SupplierQuickAction>,
    private val onClick: (SupplierQuickAction) -> Unit
) : RecyclerView.Adapter<SupplierQuickActionAdapter.SupplierQuickActionViewHolder>() {

    inner class SupplierQuickActionViewHolder(
        private val binding: ItemSupplierQuickActionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SupplierQuickAction) {
            binding.ivAction.setImageResource(item.iconRes)
            binding.tvActionTitle.text = item.title
            binding.tvActionSubtitle.text = item.subtitle
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SupplierQuickActionViewHolder {
        val binding = ItemSupplierQuickActionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SupplierQuickActionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SupplierQuickActionViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}

class SupplierRecentOrderAdapter(
    private var items: List<SupplierOrder>,
    private val onClick: (SupplierOrder) -> Unit
) : RecyclerView.Adapter<SupplierRecentOrderAdapter.SupplierRecentOrderViewHolder>() {

    inner class SupplierRecentOrderViewHolder(
        private val binding: ItemSupplierRecentOrderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SupplierOrder) {
            val context = binding.root.context
            binding.tvOrderId.text = "#${item.id}"
            binding.tvRetailerName.text = item.retailerName
            binding.tvItemsCount.text = "${item.itemsCount} items"
            binding.tvAmount.text = formatPkr(item.amountPkr)
            binding.tvStatus.text = item.status.label
            binding.tvStatus.setBackgroundResource(orderStatusBackground(item.status))
            binding.tvStatus.setTextColor(
                ContextCompat.getColor(context, orderStatusTextColor(item.status))
            )
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SupplierRecentOrderViewHolder {
        val binding = ItemSupplierRecentOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SupplierRecentOrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SupplierRecentOrderViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<SupplierOrder>) {
        val previous = items
        items = newItems
        DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = previous.size
            override fun getNewListSize(): Int = newItems.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return previous[oldItemPosition].backendId == newItems[newItemPosition].backendId
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return previous[oldItemPosition] == newItems[newItemPosition]
            }
        }).dispatchUpdatesTo(this)
    }
}
