package com.hiskytechs.muhallinewuserapp.supplier.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.hiskytechs.muhallinewuserapp.databinding.ItemSupplierOrderBinding
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierOrder
import com.hiskytechs.muhallinewuserapp.supplier.Utill.formatPkr
import com.hiskytechs.muhallinewuserapp.supplier.Utill.orderStatusBackground
import com.hiskytechs.muhallinewuserapp.supplier.Utill.orderStatusTextColor

class SupplierOrderAdapter(
    private var items: List<SupplierOrder>,
    private val onClick: (SupplierOrder) -> Unit
) : RecyclerView.Adapter<SupplierOrderAdapter.SupplierOrderViewHolder>() {

    inner class SupplierOrderViewHolder(
        private val binding: ItemSupplierOrderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SupplierOrder) {
            val context = binding.root.context
            binding.tvOrderId.text = "#${item.id}"
            binding.tvStatus.text = item.status.label
            binding.tvStatus.setBackgroundResource(orderStatusBackground(item.status))
            binding.tvStatus.setTextColor(
                ContextCompat.getColor(context, orderStatusTextColor(item.status))
            )
            binding.tvRetailerName.text = item.retailerName
            binding.tvOrderDate.text = item.orderDate
            binding.tvItemsCount.text = "${item.itemsCount} items"
            binding.tvExpectedDelivery.text = "${context.getString(com.hiskytechs.muhallinewuserapp.R.string.supplier_expected_delivery)} ${item.expectedDeliveryDate}"
            binding.tvAmount.text = formatPkr(item.amountPkr)
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SupplierOrderViewHolder {
        val binding = ItemSupplierOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SupplierOrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SupplierOrderViewHolder, position: Int) {
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
