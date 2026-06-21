package com.hiskytechs.muhallinewuserapp.supplier.Adapters

import android.graphics.Paint
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.Ui.loadMarketplaceImage
import com.hiskytechs.muhallinewuserapp.databinding.ItemSupplierCatalogProductBinding
import com.hiskytechs.muhallinewuserapp.databinding.ItemSupplierCategoryBinding
import com.hiskytechs.muhallinewuserapp.databinding.ItemSupplierInventoryBinding
import com.hiskytechs.muhallinewuserapp.databinding.ItemSupplierStoreProductBinding
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierCatalogProduct
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierCategory
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierProduct
import com.hiskytechs.muhallinewuserapp.supplier.Utill.formatPkr
import com.hiskytechs.muhallinewuserapp.supplier.Utill.initials
import com.hiskytechs.muhallinewuserapp.supplier.Utill.inventoryBackground
import com.hiskytechs.muhallinewuserapp.supplier.Utill.stockBackground
import com.hiskytechs.muhallinewuserapp.supplier.Utill.stockTextColor

class SupplierStoreProductAdapter(
    private var items: List<SupplierProduct>,
    private val onEdit: (SupplierProduct) -> Unit,
    private val onPriceEdit: (SupplierProduct) -> Unit,
    private val onOffer: (SupplierProduct) -> Unit,
    private val onToggle: (SupplierProduct, Boolean) -> Unit
) : RecyclerView.Adapter<SupplierStoreProductAdapter.SupplierStoreProductViewHolder>() {

    inner class SupplierStoreProductViewHolder(
        private val binding: ItemSupplierStoreProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SupplierProduct) {
            val context = binding.root.context
            binding.tvThumbInitial.text = initials(item.name)
            binding.ivProductImage.loadMarketplaceImage(item.imageUrl)
            binding.tvThumbInitial.alpha = if (item.imageUrl.isBlank()) 1f else 0f
            binding.tvThumbInitial.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, item.accentColorRes)
            )
            binding.tvProductName.text = item.name
            binding.tvPrice.text = context.getString(
                R.string.supplier_price_format,
                formatPkr(item.displayPricePkr),
                item.unitLabel
            )
            binding.tvOriginalPrice.visibility = if (item.hasActiveOffer) View.VISIBLE else View.GONE
            binding.tvOfferLimit.visibility = if (item.hasActiveOffer && item.maximumOfferQuantity > 0) {
                View.VISIBLE
            } else {
                View.GONE
            }
            if (item.hasActiveOffer) {
                binding.tvOriginalPrice.text = context.getString(
                    R.string.supplier_price_format,
                    formatPkr(item.pricePkr),
                    item.unitLabel
                )
                binding.tvOriginalPrice.paintFlags =
                    binding.tvOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvOfferLimit.text = context.getString(
                    R.string.offer_limit_format,
                    item.maximumOfferQuantity
                )
            } else {
                binding.tvOriginalPrice.paintFlags =
                    binding.tvOriginalPrice.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
            binding.tvStockState.text = when (item.stockState.name) {
                "IN_STOCK" -> context.getString(R.string.supplier_in_stock)
                "LOW_STOCK" -> context.getString(R.string.supplier_low_stock)
                else -> context.getString(R.string.supplier_out_of_stock)
            }
            binding.tvStockState.setBackgroundResource(stockBackground(item.stockState))
            binding.tvStockState.setTextColor(
                ContextCompat.getColor(context, stockTextColor(item.stockState))
            )
            binding.tvStockCount.text = context.getString(R.string.supplier_stock_label, item.stock)
            binding.tvDeliveryDays.text = context.getString(R.string.supplier_delivery_label, item.deliveryDays)
            binding.switchAvailability.setOnCheckedChangeListener(null)
            binding.switchAvailability.isChecked = item.isActive
            binding.switchAvailability.setOnCheckedChangeListener { _, checked ->
                onToggle(item, checked)
            }
            binding.ivEdit.setOnClickListener { onEdit(item) }
            binding.ivPriceEdit.setOnClickListener { onPriceEdit(item) }
            binding.btnAddOffer.text = if (item.isOnOffer) {
                context.getString(R.string.supplier_on_offer)
            } else {
                context.getString(R.string.supplier_add_to_offers)
            }
            binding.btnAddOffer.setOnClickListener { onOffer(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SupplierStoreProductViewHolder {
        val binding = ItemSupplierStoreProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SupplierStoreProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SupplierStoreProductViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<SupplierProduct>) {
        val previous = items
        items = newItems
        if (previous.size + newItems.size > LARGE_PRODUCT_DIFF_THRESHOLD) {
            notifyDataSetChanged()
            return
        }
        DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = previous.size
            override fun getNewListSize(): Int = newItems.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return previous[oldItemPosition].id == newItems[newItemPosition].id
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return previous[oldItemPosition] == newItems[newItemPosition]
            }
        }).dispatchUpdatesTo(this)
    }

    private companion object {
        private const val LARGE_PRODUCT_DIFF_THRESHOLD = 80
    }
}

class SupplierCategoryAdapter(
    private var items: List<SupplierCategory>,
    private val onClick: (SupplierCategory) -> Unit
) : RecyclerView.Adapter<SupplierCategoryAdapter.SupplierCategoryViewHolder>() {

    var selectedCategoryId: String? = null

    inner class SupplierCategoryViewHolder(
        private val binding: ItemSupplierCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SupplierCategory) {
            val context = binding.root.context
            val selected = item.id == selectedCategoryId
            binding.tvCategoryInitial.text = initials(item.name)
            binding.tvCategoryInitial.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, item.accentColorRes)
            )
            binding.tvCategoryName.text = item.name
            binding.tvCategoryCount.text = context.getString(
                R.string.supplier_catalog_products_count_format,
                item.catalogCount
            )
            binding.root.setBackgroundResource(
                if (selected) R.drawable.bg_supplier_card_selected else R.drawable.bg_supplier_card
            )
            binding.tvCategoryName.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (selected) R.color.primary else R.color.text_dark
                )
            )
            binding.root.setOnClickListener {
                selectedCategoryId = item.id
                notifyDataSetChanged()
                onClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SupplierCategoryViewHolder {
        val binding = ItemSupplierCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SupplierCategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SupplierCategoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<SupplierCategory>) {
        items = newItems
        notifyDataSetChanged()
    }
}

class SupplierCatalogProductAdapter(
    private var items: List<SupplierCatalogProduct>,
    private val onClick: (SupplierCatalogProduct) -> Unit
) : RecyclerView.Adapter<SupplierCatalogProductAdapter.SupplierCatalogProductViewHolder>() {

    var selectedProductId: String? = null

    inner class SupplierCatalogProductViewHolder(
        private val binding: ItemSupplierCatalogProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SupplierCatalogProduct) {
            val context = binding.root.context
            val selected = item.id == selectedProductId
            binding.tvProductInitial.text = initials(item.name)
            binding.ivProductImage.loadMarketplaceImage(item.imageUrl)
            binding.tvProductInitial.alpha = if (item.imageUrl.isBlank()) 1f else 0f
            binding.tvProductInitial.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, item.accentColorRes)
            )
            binding.tvProductName.text = item.name
            binding.tvProductMeta.text = listOf(item.categoryName, item.packaging, item.unitLabel)
                .filter { it.isNotBlank() }
                .joinToString(" • ")
            binding.root.setBackgroundResource(
                if (selected) R.drawable.bg_supplier_card_selected else R.drawable.bg_supplier_card
            )
            binding.tvProductName.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (selected) R.color.primary else R.color.text_dark
                )
            )
            binding.root.setOnClickListener {
                selectedProductId = item.id
                notifyDataSetChanged()
                onClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SupplierCatalogProductViewHolder {
        val binding = ItemSupplierCatalogProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SupplierCatalogProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SupplierCatalogProductViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<SupplierCatalogProduct>) {
        items = newItems
        notifyDataSetChanged()
    }
}

class SupplierInventoryAdapter(
    private var items: List<SupplierProduct>,
    private val onAdjustStock: (SupplierProduct, Int) -> Unit,
    private val onUpdate: (SupplierProduct) -> Unit
) : RecyclerView.Adapter<SupplierInventoryAdapter.SupplierInventoryViewHolder>() {

    inner class SupplierInventoryViewHolder(
        private val binding: ItemSupplierInventoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SupplierProduct) {
            val context = binding.root.context
            binding.layoutContainer.setBackgroundResource(inventoryBackground(item.stockState))
            binding.tvThumbInitial.text = initials(item.name)
            binding.tvThumbInitial.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, item.accentColorRes)
            )
            binding.tvProductName.text = item.name
            binding.tvCategory.text = item.categoryName
            binding.tvStockCount.text = item.stock.toString()
            binding.tvUnit.text = if (item.unitLabel.equals("Bag", true)) {
                context.getString(R.string.supplier_bags)
            } else {
                context.getString(R.string.supplier_cartons)
            }
            binding.ivMinus.setOnClickListener { onAdjustStock(item, -1) }
            binding.ivPlus.setOnClickListener { onAdjustStock(item, 1) }
            binding.btnUpdate.setOnClickListener { onUpdate(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SupplierInventoryViewHolder {
        val binding = ItemSupplierInventoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SupplierInventoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SupplierInventoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<SupplierProduct>) {
        items = newItems
        notifyDataSetChanged()
    }
}
