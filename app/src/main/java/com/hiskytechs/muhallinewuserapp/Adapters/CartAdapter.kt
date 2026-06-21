package com.hiskytechs.muhallinewuserapp.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.Models.CartItem
import com.hiskytechs.muhallinewuserapp.Ui.loadMarketplaceImage
import com.hiskytechs.muhallinewuserapp.databinding.ItemCartBinding
import com.hiskytechs.muhallinewuserapp.network.CurrencyFormatter

class CartAdapter(
    private var items: List<CartItem>,
    private val onQuantityChanged: (CartItem) -> Unit,
    private val onDeleteItem: (CartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(val binding: ItemCartBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = items[position]
        holder.binding.apply {
            tvProductName.text = item.name
            tvSupplier.text = item.supplier
            tvPrice.text = CurrencyFormatter.format(item.displayUnitPrice)
            tvOfferSplit.visibility = if (item.hasOffer) View.VISIBLE else View.GONE
            if (item.hasOffer) {
                tvOfferSplit.text = root.context.getString(
                    R.string.cart_hybrid_split_format,
                    item.offerQuantity,
                    CurrencyFormatter.format(item.offerPrice),
                    item.regularQuantity,
                    CurrencyFormatter.format(item.price)
                )
            }
            ivProduct.loadMarketplaceImage(item.imageUrl.orEmpty())
            tvQuantity.text = item.quantity.toString()
            tvItemSubtotal.text = CurrencyFormatter.format(item.subtotal)

            ivPlus.setOnClickListener {
                item.quantity++
                onQuantityChanged(item)
                notifyItemChanged(position)
            }

            ivMinus.setOnClickListener {
                if (item.quantity > 1) {
                    item.quantity--
                    onQuantityChanged(item)
                    notifyItemChanged(position)
                }
            }

            ivDelete.setOnClickListener {
                onDeleteItem(item)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<CartItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
