package com.hiskytechs.muhallinewuserapp.Adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.hiskytechs.muhallinewuserapp.Models.Product
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.Ui.loadMarketplaceImage
import com.hiskytechs.muhallinewuserapp.databinding.ItemProductSearchResultBinding
import com.hiskytechs.muhallinewuserapp.network.CurrencyFormatter

class ProductSearchAdapter(
    private var items: List<Product>,
    private val onAddClick: (Product) -> Unit,
    private val onOpenSupplier: (Product) -> Unit
) : RecyclerView.Adapter<ProductSearchAdapter.ProductSearchViewHolder>() {

    class ProductSearchViewHolder(val binding: ItemProductSearchResultBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductSearchViewHolder {
        return ProductSearchViewHolder(
            ItemProductSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ProductSearchViewHolder, position: Int) {
        val product = items[position]
        val context = holder.binding.root.context
        holder.binding.apply {
            ivProduct.loadMarketplaceImage(product.imageUrl)
            tvProductName.text = product.name
            tvSupplierName.text = product.supplierName
            tvStock.text = context.getString(R.string.search_stock_format, product.stockQuantity)
            tvCategory.text = product.categoryName
            tvOfferPrice.text = CurrencyFormatter.format(product.displayPrice)

            if (product.hasOffer) {
                tvOriginalPrice.visibility = View.VISIBLE
                tvOriginalPrice.paintFlags = tvOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                tvOriginalPrice.text = CurrencyFormatter.format(product.price)
                tvOfferLimit.visibility = View.VISIBLE
                tvOfferLimit.text = context.getString(
                    R.string.offer_limit_format,
                    product.maximumOfferQuantity
                )
            } else {
                tvOriginalPrice.visibility = View.GONE
                tvOfferLimit.visibility = View.GONE
            }

            btnAddToCart.setOnClickListener { onAddClick(product) }
            root.setOnClickListener { onOpenSupplier(product) }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(updated: List<Product>) {
        val previous = items
        items = updated
        DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = previous.size
            override fun getNewListSize(): Int = updated.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return previous[oldItemPosition].id == updated[newItemPosition].id
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return previous[oldItemPosition] == updated[newItemPosition]
            }
        }).dispatchUpdatesTo(this)
    }
}
