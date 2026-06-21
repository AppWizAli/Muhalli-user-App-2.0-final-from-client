package com.hiskytechs.muhallinewuserapp.supplier.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.hiskytechs.muhallinewuserapp.databinding.ItemSupplierTransactionBinding
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierTransaction
import com.hiskytechs.muhallinewuserapp.supplier.Utill.formatPositivePkr

class SupplierTransactionAdapter(
    private var items: List<SupplierTransaction>
) : RecyclerView.Adapter<SupplierTransactionAdapter.SupplierTransactionViewHolder>() {

    inner class SupplierTransactionViewHolder(
        private val binding: ItemSupplierTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SupplierTransaction) {
            binding.tvRetailerName.text = item.retailerName
            binding.tvOrderId.text = "Order #${item.orderId}"
            binding.tvAmount.text = formatPositivePkr(item.amountPkr)
            binding.tvDate.text = item.date
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SupplierTransactionViewHolder {
        val binding = ItemSupplierTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SupplierTransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SupplierTransactionViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<SupplierTransaction>) {
        val previous = items
        items = newItems
        DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = previous.size
            override fun getNewListSize(): Int = newItems.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return previous[oldItemPosition].orderId == newItems[newItemPosition].orderId
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return previous[oldItemPosition] == newItems[newItemPosition]
            }
        }).dispatchUpdatesTo(this)
    }
}
