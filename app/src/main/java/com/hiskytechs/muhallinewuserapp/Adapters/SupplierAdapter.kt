package com.hiskytechs.muhallinewuserapp.Adapters

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.hiskytechs.muhallinewuserapp.Models.Supplier
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.Ui.SupplierDetailsActivity
import com.hiskytechs.muhallinewuserapp.databinding.ItemSupplierBinding
import com.hiskytechs.muhallinewuserapp.network.CurrencyFormatter

class SupplierAdapter(
    private var suppliers: List<Supplier>,
    private val highlightedCategory: String? = null
) :
    RecyclerView.Adapter<SupplierAdapter.SupplierViewHolder>() {

    class SupplierViewHolder(val binding: ItemSupplierBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SupplierViewHolder {

        android.util.Log.e("SUPPLIER_DEBUG", "onCreateViewHolder called")

        val binding = ItemSupplierBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return SupplierViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SupplierViewHolder, position: Int) {

        val supplier = suppliers[position]
        holder.binding.apply {
            tvSupplierName.text = supplier.name
            tvLocation.text = supplier.location
            tvProductCount.text = supplier.productCount
            tvDeliveryTime.text = supplier.deliveryTime
            tvMinAmount.text = CurrencyFormatter.format(supplier.minimumAmount)
            tvMinQty.text = supplier.minimumQuantity.toString()
            tvVerified.visibility = if (supplier.isVerified) View.VISIBLE else View.GONE

            layoutHeader.setBackgroundColor(Color.parseColor(supplier.headerColor))

            root.setOnClickListener {
                val intent = Intent(it.context, SupplierDetailsActivity::class.java).apply {
                    putExtra("supplier_id", supplier.id)
                    putExtra("supplier_name", supplier.name)
                    putExtra("supplier_owner_name", supplier.ownerName)
                    putExtra("location", supplier.location)
                    putExtra("category_name", highlightedCategory.orEmpty())
                    putExtra("delivery_time", supplier.deliveryTime)
                    putExtra(
                        "min_amount",
                        CurrencyFormatter.format(supplier.minimumAmount)
                    )
                    putExtra("min_qty", supplier.minimumQuantity.toString())
                }
                it.context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int {
        android.util.Log.d("SupplierAdapter", "count = ${suppliers.size}")
        return suppliers.size
    }

    fun updateItems(updated: List<Supplier>) {
        val previous = suppliers
        val newList = updated.toList()

        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = previous.size

            override fun getNewListSize(): Int = newList.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return previous[oldItemPosition].id == newList[newItemPosition].id
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return previous[oldItemPosition] == newList[newItemPosition]
            }
        })

        suppliers = newList
        diffResult.dispatchUpdatesTo(this)
    }
}
