package com.hiskytechs.muhallinewuserapp.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.Utill.SupplierCart
import com.hiskytechs.muhallinewuserapp.databinding.ItemCartSupplierBinding
import java.util.Locale

class CartSupplierAdapter(
    private var supplierCarts: List<SupplierCart>,
    private var selectedSupplierName: String?,
    private val onSupplierSelected: (String) -> Unit,
    private val onSupplierDoubleTapped: (String) -> Unit
) : RecyclerView.Adapter<CartSupplierAdapter.CartSupplierViewHolder>() {

    private var lastTappedSupplierName: String = ""
    private var lastTapTime: Long = 0L

    class CartSupplierViewHolder(val binding: ItemCartSupplierBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartSupplierViewHolder {
        val binding = ItemCartSupplierBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CartSupplierViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartSupplierViewHolder, position: Int) {
        val supplierCart = supplierCarts[position]
        val isSelected = supplierCart.supplierName.equals(selectedSupplierName, ignoreCase = true)
        val context = holder.binding.root.context

        holder.binding.apply {
            tvSupplierInitial.text = supplierCart.supplierName
                .trim()
                .take(1)
                .uppercase(Locale.getDefault())
            tvSupplierName.text = supplierCart.supplierName
            tvSupplierMeta.text = context.getString(
                R.string.cart_supplier_item_meta_format,
                supplierCart.lineItemCount,
                supplierCart.totalQuantity
            )
            updateCardSelection(cardSupplier, isSelected)

            root.setOnClickListener {
                val now = SystemClock.elapsedRealtime()
                val isDoubleTap = supplierCart.supplierName.equals(lastTappedSupplierName, ignoreCase = true) &&
                    now - lastTapTime <= DOUBLE_TAP_WINDOW_MS
                lastTappedSupplierName = supplierCart.supplierName
                lastTapTime = now

                if (isDoubleTap) {
                    onSupplierDoubleTapped(supplierCart.supplierName)
                } else {
                    onSupplierSelected(supplierCart.supplierName)
                }
            }
        }
    }

    override fun getItemCount(): Int = supplierCarts.size

    fun updateItems(newSupplierCarts: List<SupplierCart>, selectedSupplierName: String?) {
        supplierCarts = newSupplierCarts
        this.selectedSupplierName = selectedSupplierName
        notifyDataSetChanged()
    }

    private fun updateCardSelection(cardView: MaterialCardView, isSelected: Boolean) {
        val context = cardView.context
        val strokeWidth = (if (isSelected) 2 else 1) * context.resources.displayMetrics.density
        cardView.strokeWidth = strokeWidth.toInt()
        cardView.strokeColor = ContextCompat.getColor(
            context,
            if (isSelected) R.color.primary else R.color.divider
        )
        cardView.setCardBackgroundColor(
            ContextCompat.getColor(
                context,
                if (isSelected) R.color.status_transit_bg else R.color.white
            )
        )
    }

    private companion object {
        const val DOUBLE_TAP_WINDOW_MS = 350L
    }
}
