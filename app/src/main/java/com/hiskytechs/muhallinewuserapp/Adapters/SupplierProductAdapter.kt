package com.hiskytechs.muhallinewuserapp.Adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hiskytechs.muhallinewuserapp.Models.Product
import com.hiskytechs.muhallinewuserapp.Ui.loadMarketplaceImage
import com.hiskytechs.muhallinewuserapp.Utill.CartManager
import com.hiskytechs.muhallinewuserapp.databinding.ItemSupplierProductBinding
import com.hiskytechs.muhallinewuserapp.network.CurrencyFormatter

class SupplierProductAdapter(
    private val products: List<Product>,
    private val supplierName: String,
    private val onQuantityChanged: (Product, Int) -> Unit
) : RecyclerView.Adapter<SupplierProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(val binding: ItemSupplierProductBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemSupplierProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.binding.apply {
            tvProductName.text = product.name
            tvPrice.text = CurrencyFormatter.format(product.displayPrice)
            if (product.hasOffer) {
                tvOriginalPrice.visibility = View.VISIBLE
                tvOriginalPrice.paintFlags = tvOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                tvOriginalPrice.text = CurrencyFormatter.format(product.price)
            } else {
                tvOriginalPrice.visibility = View.GONE
            }
            ivProduct.loadMarketplaceImage(product.imageUrl)
            tvPackaging.text = listOf(product.packaging, product.unit)
                .filter { it.isNotBlank() }
                .joinToString(" / ")

            val cartQuantity = CartManager.getProductQuantity(product.id, supplierName)
            btnAddToCart.visibility = if (cartQuantity > 0) View.GONE else View.VISIBLE
            llProductQuantity.visibility = if (cartQuantity > 0) View.VISIBLE else View.GONE
            tvCartQuantity.text = cartQuantity.toString()

            btnAddToCart.setOnClickListener {
                onQuantityChanged(product, 1)
                notifyBoundItem(holder)
            }

            ivPlus.setOnClickListener {
                onQuantityChanged(product, 1)
                notifyBoundItem(holder)
            }

            ivMinus.setOnClickListener {
                onQuantityChanged(product, -1)
                notifyBoundItem(holder)
            }
        }
    }

    override fun getItemCount() = products.size

    private fun notifyBoundItem(holder: ProductViewHolder) {
        val position = holder.bindingAdapterPosition
        if (position != RecyclerView.NO_POSITION) {
            notifyItemChanged(position)
        }
    }
}
