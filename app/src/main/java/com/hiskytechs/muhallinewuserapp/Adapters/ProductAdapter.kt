package com.hiskytechs.muhallinewuserapp.Adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hiskytechs.muhallinewuserapp.Models.Product
import com.hiskytechs.muhallinewuserapp.Ui.loadMarketplaceImage
import com.hiskytechs.muhallinewuserapp.databinding.ItemProductBinding
import com.hiskytechs.muhallinewuserapp.network.CurrencyFormatter

class ProductAdapter(private val products: List<Product>, private val onAddClick: (Product) -> Unit) :
    RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
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
            tvProductUnit.text = product.unit
            tvProductPrice.text = CurrencyFormatter.format(product.displayPrice)
            if (product.hasOffer) {
                tvOriginalPrice.visibility = View.VISIBLE
                tvOriginalPrice.paintFlags = tvOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                tvOriginalPrice.text = CurrencyFormatter.format(product.price)
            } else {
                tvOriginalPrice.visibility = View.GONE
            }
            ivProduct.loadMarketplaceImage(product.imageUrl)
            
            btnAddToCart.setOnClickListener {
                onAddClick(product)
            }
        }
    }

    override fun getItemCount() = products.size
}
