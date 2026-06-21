package com.hiskytechs.muhallinewuserapp.Ui

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.chip.Chip
import com.hiskytechs.muhallinewuserapp.Adapters.SupplierProductAdapter
import com.hiskytechs.muhallinewuserapp.Data.AppData
import com.hiskytechs.muhallinewuserapp.Models.CartItem
import com.hiskytechs.muhallinewuserapp.Models.Product
import com.hiskytechs.muhallinewuserapp.Models.Supplier
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.Utill.CartManager
import com.hiskytechs.muhallinewuserapp.Utill.SupplierCart
import com.hiskytechs.muhallinewuserapp.databinding.ActivitySupplierDetailsBinding
import com.hiskytechs.muhallinewuserapp.network.CurrencyFormatter

class SupplierDetailsActivity : AppCompatActivity() {

    private companion object {
        private const val DEBUG_TAG = "MH_SUPPLIER_DEBUG"
    }

    private lateinit var binding: ActivitySupplierDetailsBinding
    private var supplierId: Int = 0
    private var supplierName: String = ""
    private var supplier: Supplier? = null
    private lateinit var productAdapter: SupplierProductAdapter
    private var activeLoadingCount = 0
    private var currentQuery: String = ""
    private var selectedCategoryName: String = ""
    private var latestProductsRequestId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySupplierDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supplierId = intent.getIntExtra("supplier_id", 0)
        supplierName = intent.getStringExtra("supplier_name").orEmpty()
        selectedCategoryName = intent.getStringExtra("category_name").orEmpty()
        Log.e(
            DEBUG_TAG,
            "onCreate supplierId=$supplierId supplierName='$supplierName' category='$selectedCategoryName' location='${intent.getStringExtra("location").orEmpty()}'"
        )
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.text_dark))
        bindHeaderFallback()

        setupRecycler()
        setupInteractions()
        loadSupplier()
    }

    override fun onResume() {
        super.onResume()
        updateOrderProgress()
    }

    private fun setupRecycler() {
        productAdapter = SupplierProductAdapter(emptyList(), currentSupplierName(), ::onProductQuantityChanged)
        binding.rvProducts.layoutManager = GridLayoutManager(this, 2)
        binding.rvProducts.adapter = productAdapter
    }

    private fun setupInteractions() {
        binding.etSellerSearch.addTextChangedListener {
            currentQuery = it?.toString().orEmpty()
            loadProducts()
        }
    }

    private fun loadSupplier() {
        showInlineLoading(R.string.loading_supplier_catalog)
        AppData.loadSuppliers(
            searchQuery = "",
            cityFilter = "",
            forceRefresh = true,
            onSuccess = { suppliers ->
                hideInlineLoading()
                supplier = suppliers.firstOrNull { it.id == supplierId }
                    ?: suppliers.firstOrNull { item ->
                        val requested = supplierName.trim()
                        requested.isNotBlank() && (
                            item.name.equals(requested, ignoreCase = true) ||
                                item.ownerName.equals(requested, ignoreCase = true) ||
                                item.name.contains(requested, ignoreCase = true) ||
                                item.ownerName.contains(requested, ignoreCase = true)
                            )
                    }
                    ?: AppData.findSupplierById(supplierId)
                    ?: AppData.findSupplierByName(supplierName)
                if (supplier == null) {
                    Log.e(DEBUG_TAG, "loadSupplier no match for supplierId=$supplierId supplierName='$supplierName'")
                    Toast.makeText(this, R.string.supplier_not_found, Toast.LENGTH_SHORT).show()
                    finish()
                    return@loadSuppliers
                }
                Log.e(
                    DEBUG_TAG,
                    "loadSupplier resolved id=${supplier?.id} name='${supplier?.name}' owner='${supplier?.ownerName}' categories=${supplier?.categories?.size ?: 0}"
                )
                bindSupplier(requireNotNull(supplier))
                loadProducts()
                updateOrderProgress()
            },
            onError = { message ->
                hideInlineLoading()
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun bindSupplier(item: Supplier) {
        val displayName = item.name.ifBlank { item.ownerName }.ifBlank { supplierName }
        supplierName = displayName
        binding.tvSupplierName.text = displayName
        binding.toolbar.title = displayName
        binding.tvLocation.text = item.location.ifBlank { intent.getStringExtra("location").orEmpty() }
        binding.tvDeliveryTime.text = item.deliveryTime
        binding.tvMinAmount.text = CurrencyFormatter.format(item.minimumAmount)
        binding.tvMinQty.text = getString(R.string.supplier_items_count_format, item.minimumQuantity)
        binding.tvVerified.visibility = if (item.isVerified) android.view.View.VISIBLE else android.view.View.GONE

        binding.layoutCategories.removeAllViews()
        item.categories.ifEmpty { listOf(getString(R.string.general_category)) }.forEach { category ->
            val chip = Chip(this).apply {
                text = category
                textSize = 12f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                isCheckable = false
                isClickable = true
                isFocusable = true
                chipMinHeight = 36f
                chipStartPadding = 12f
                chipEndPadding = 12f
                textStartPadding = 0f
                textEndPadding = 0f
                shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                    .setAllCornerSizes(18f)
                    .build()
                chipStrokeWidth = 0f
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = resources.getDimensionPixelSize(R.dimen.supplier_category_chip_spacing)
                }
                setOnClickListener {
                    selectedCategoryName = if (selectedCategoryName.equals(category, ignoreCase = true)) {
                        ""
                    } else {
                        category
                    }
                    Log.e(
                        DEBUG_TAG,
                        "categoryClick category='$category' selected='$selectedCategoryName' supplierId=$supplierId"
                    )
                    bindSupplier(item)
                    loadProducts()
                }
            }
            val isSelected = selectedCategoryName.equals(category, ignoreCase = true)
            chip.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (isSelected) R.color.white else R.color.primary
                )
            )
            chip.chipBackgroundColor = ColorStateList.valueOf(
                ContextCompat.getColor(
                    this,
                    if (isSelected) R.color.primary else R.color.status_transit_bg
                )
            )
            binding.layoutCategories.addView(chip)
        }
    }

    private fun bindHeaderFallback() {
        val fallbackName = supplierName
            .ifBlank { intent.getStringExtra("supplier_owner_name").orEmpty() }
            .ifBlank { intent.getStringExtra("supplier_name").orEmpty() }
        if (fallbackName.isNotBlank()) {
            binding.tvSupplierName.text = fallbackName
            binding.toolbar.title = fallbackName
        }
        val fallbackLocation = intent.getStringExtra("location").orEmpty()
        if (fallbackLocation.isNotBlank()) {
            binding.tvLocation.text = fallbackLocation
        }
    }

    private fun loadProducts() {
        val activeSupplierName = currentSupplierName()
        val activeSupplierId = supplier?.id ?: supplierId
        if (activeSupplierName.isBlank()) {
            Log.e(DEBUG_TAG, "loadProducts aborted: blank supplier name supplierId=$activeSupplierId")
            return
        }
        val requestId = ++latestProductsRequestId
        Log.e(
            DEBUG_TAG,
            "loadProducts requestId=$requestId supplierId=$activeSupplierId name='$activeSupplierName' query='$currentQuery' category='$selectedCategoryName'"
        )
        showInlineLoading(R.string.loading_supplier_catalog)
        AppData.loadSupplierProducts(
            supplierName = activeSupplierName,
            supplierId = activeSupplierId,
            query = currentQuery,
            categoryName = selectedCategoryName,
            forceRefresh = supplier == null,
            onSuccess = { products ->
                if (requestId != latestProductsRequestId || isFinishing || isDestroyed) return@loadSupplierProducts
                Log.e(
                    DEBUG_TAG,
                    "loadProducts success requestId=$requestId result=${products.size} supplierId=$activeSupplierId query='$currentQuery' category='$selectedCategoryName'"
                )
                hideInlineLoading()
                productAdapter = SupplierProductAdapter(products, activeSupplierName, ::onProductQuantityChanged)
                binding.rvProducts.adapter = productAdapter
            },
            onError = { message ->
                if (requestId != latestProductsRequestId || isFinishing || isDestroyed) return@loadSupplierProducts
                Log.e(
                    DEBUG_TAG,
                    "loadProducts error requestId=$requestId supplierId=$activeSupplierId message='$message'"
                )
                hideInlineLoading()
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun onProductQuantityChanged(product: Product, delta: Int) {
        val activeSupplierName = currentSupplierName()
        val wasMinimumMet = CartManager.getSupplierCart(activeSupplierName)?.isMinimumMet == true
        if (delta > 0) {
            CartManager.addItem(
                CartItem(
                    id = product.id,
                    name = product.name,
                    supplier = activeSupplierName,
                    price = product.price,
                    quantity = 1,
                    imageUrl = product.imageUrl,
                    offerPrice = product.offerPrice,
                    maximumOfferQuantity = product.maximumOfferQuantity
                )
            )
        } else {
            CartManager.decrementItem(product.id, activeSupplierName)
        }

        updateOrderProgress()
        val selectedCart = CartManager.getSupplierCart(activeSupplierName)
        if (delta > 0 && !wasMinimumMet && selectedCart?.isMinimumMet == true) {
            Toast.makeText(this, R.string.order_minimum_ready_opening_cart, Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, CartActivity::class.java).apply {
                putExtra(CartActivity.EXTRA_SUPPLIER_NAME, activeSupplierName)
            })
        } else if (delta > 0) {
            Toast.makeText(this, R.string.added_to_cart, Toast.LENGTH_SHORT).show()
        }
    }

    private fun currentSupplierName(): String {
        return supplier?.name
            ?.ifBlank { supplier?.ownerName.orEmpty() }
            ?.ifBlank { supplierName }
            ?.trim()
            .orEmpty()
    }

    private fun updateOrderProgress() {
        val supplierCart = CartManager.getSupplierCart(supplierName)
        renderProgress(supplierCart)
    }

    private fun renderProgress(selectedCart: SupplierCart?) {
        if (selectedCart == null) {
            binding.tvOrderProgressSubtitle.text = getString(R.string.complete_minimum_order_requirements)
            binding.tvAmountProgress.text = getString(
                R.string.cart_progress_amount_format,
                CurrencyFormatter.format(0.0),
                CurrencyFormatter.format(0.0)
            )
            binding.tvQuantityProgress.text = getString(R.string.cart_quantity_progress_format, 0, 0)
            binding.tvAmountRemaining.text = getString(R.string.cart_empty_message)
            binding.tvQuantityRemaining.text = getString(R.string.cart_empty_message)
            binding.pbAmount.progress = 0
            binding.pbQuantity.progress = 0
            return
        }

        binding.tvOrderProgressSubtitle.text = selectedCart.supplierName
        binding.tvAmountProgress.text = getString(
            R.string.cart_progress_amount_format,
            CurrencyFormatter.format(selectedCart.subtotal),
            CurrencyFormatter.format(selectedCart.minimumAmount)
        )
        binding.tvQuantityProgress.text = getString(
            R.string.cart_quantity_progress_format,
            selectedCart.totalQuantity,
            selectedCart.minimumQuantity
        )
        binding.tvAmountRemaining.text = if (selectedCart.remainingAmount > 0.0) {
            getString(R.string.cart_add_more_amount_format, CurrencyFormatter.format(selectedCart.remainingAmount))
        } else {
            getString(R.string.minimum_amount_reached)
        }
        binding.tvQuantityRemaining.text = if (selectedCart.remainingQuantity > 0) {
            getString(R.string.cart_add_more_quantity_format, selectedCart.remainingQuantity)
        } else {
            getString(R.string.minimum_quantity_reached)
        }
        binding.pbAmount.progress = calculateProgress(selectedCart.subtotal, selectedCart.minimumAmount)
        binding.pbQuantity.progress = calculateProgress(
            selectedCart.totalQuantity.toDouble(),
            selectedCart.minimumQuantity.toDouble()
        )
    }

    private fun calculateProgress(currentValue: Double, minimumValue: Double): Int {
        if (minimumValue <= 0.0) return if (currentValue > 0.0) 100 else 0
        val progress = (currentValue / minimumValue) * 100
        return if (progress.isFinite()) progress.toInt().coerceIn(0, 100) else 0
    }

    private fun showInlineLoading(@StringRes messageRes: Int) {
        activeLoadingCount += 1
        binding.layoutLoadingOverlay.visibility = android.view.View.VISIBLE
        binding.tvInlineLoadingMessage.setText(messageRes)
        if (binding.layoutLoadingOverlay.animation == null) {
            binding.layoutLoadingOverlay.startAnimation(
                AlphaAnimation(0.45f, 1f).apply {
                    duration = 700L
                    repeatMode = Animation.REVERSE
                    repeatCount = Animation.INFINITE
                }
            )
        }
    }

    private fun hideInlineLoading() {
        activeLoadingCount = (activeLoadingCount - 1).coerceAtLeast(0)
        if (activeLoadingCount == 0) {
            binding.layoutLoadingOverlay.clearAnimation()
            binding.layoutLoadingOverlay.visibility = android.view.View.GONE
        }
    }

    override fun onDestroy() {
        activeLoadingCount = 0
        super.onDestroy()
    }
}
