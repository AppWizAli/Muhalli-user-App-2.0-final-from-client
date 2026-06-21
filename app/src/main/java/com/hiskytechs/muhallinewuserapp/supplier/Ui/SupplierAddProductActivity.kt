package com.hiskytechs.muhallinewuserapp.supplier.Ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.Ui.AppLoadingDialog
import com.hiskytechs.muhallinewuserapp.Ui.loadMarketplaceImage
import com.hiskytechs.muhallinewuserapp.databinding.ActivitySupplierAddProductBinding
import com.hiskytechs.muhallinewuserapp.network.BackgroundWork
import com.hiskytechs.muhallinewuserapp.Utill.ShimmerSkeleton
import com.hiskytechs.muhallinewuserapp.supplier.Adapters.SupplierCatalogProductAdapter
import com.hiskytechs.muhallinewuserapp.supplier.Adapters.SupplierCategoryAdapter
import com.hiskytechs.muhallinewuserapp.supplier.Data.SupplierData
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierCatalogProduct
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierCategory
import com.hiskytechs.muhallinewuserapp.supplier.Utill.initials

class SupplierAddProductActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SupplierAddProduct"
        private const val EXTRA_EDIT_PRODUCT_ID = "extra_edit_product_id"

        fun openEdit(context: Context, productId: String) {
            context.startActivity(
                Intent(context, SupplierAddProductActivity::class.java)
                    .putExtra(EXTRA_EDIT_PRODUCT_ID, productId)
            )
        }
    }

    private lateinit var binding: ActivitySupplierAddProductBinding
    private lateinit var loadingDialog: AppLoadingDialog
    private lateinit var categoryAdapter: SupplierCategoryAdapter
    private lateinit var catalogProductAdapter: SupplierCatalogProductAdapter
    private lateinit var productSearchAdapter: SupplierCatalogProductAdapter
    private var selectedCategory: SupplierCategory? = null
    private var selectedProduct: SupplierCatalogProduct? = null
    private var currentStep = 1
    private var selectedImageDataUrl: String? = null
    private var isCategoryLoading = false
    private var isProductSearchLoading = false
    private var loadedSearchCategoryIds: Set<String> = emptySet()
    private var loadedCatalogProductSearchQuery = ""

    private val editProductId: String?
        get() = intent.getStringExtra(EXTRA_EDIT_PRODUCT_ID)
    private val isEditMode: Boolean
        get() = !editProductId.isNullOrBlank()

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult
        binding.ivSelectedProductImage.loadMarketplaceImage(uri.toString())
        binding.tvSelectedProductThumb.alpha = 0f
        binding.tvSelectedImageStatus.text = getString(R.string.supplier_upload_product_image)
        binding.btnSaveProduct.isEnabled = false
        val resolver = contentResolver
        val mimeType = resolver.getType(uri).orEmpty().ifBlank { "image/jpeg" }
        BackgroundWork.run(
            task = { uri.toDataUrl(mimeType) },
            onSuccess = { dataUrl ->
                selectedImageDataUrl = dataUrl
                binding.btnSaveProduct.isEnabled = true
            },
            onError = {
                selectedImageDataUrl = null
                binding.btnSaveProduct.isEnabled = true
                Toast.makeText(this, R.string.supplier_bulk_file_read_failed, Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySupplierAddProductBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loadingDialog = AppLoadingDialog(this)
        if (isEditMode) {
            binding.tvAddProductTitle.text = getString(R.string.supplier_edit_product)
            binding.btnSaveProduct.text = getString(R.string.supplier_update)
        }

        setupCategoryStep()
        setupProductStep()
        setupPricingStep()
        loadCatalogData()
        binding.btnStickyNext.setOnClickListener {
            when (currentStep) {
                1 -> goToProductStep()
                2 -> goToPricingStep()
            }
        }

        binding.ivBack.setOnClickListener {
            when (currentStep) {
                3 -> {
                    if (isEditMode) {
                        finish()
                        return@setOnClickListener
                    }
                    currentStep = 2
                    renderStep()
                }
                2 -> {
                    currentStep = 1
                    renderStep()
                }
                else -> finish()
            }
        }
    }

    private fun setupCategoryStep() {
        categoryAdapter = SupplierCategoryAdapter(SupplierData.getCategories()) { category ->
            Log.d(TAG, "Category clicked: id=${category.id}, name=${category.name}, catalogCount=${category.catalogCount}, listingCount=${category.listingCount}")
            selectedCategory = category
            binding.tvSelectedCategory.text = category.name
            selectedProduct = null
            catalogProductAdapter.selectedProductId = null
            loadingDialog.show(R.string.loading_supplier_catalog)
            Log.d(TAG, "Fetching catalog for categoryId=${category.id} before opening product step")
            SupplierData.refreshCatalogForCategory(
                categoryId = category.id,
                onSuccess = {
                    loadingDialog.dismiss()
                    if (!isFinishing && !isDestroyed) {
                        val hasProducts = loadCatalogProducts(showReason = true)
                        if (hasProducts) {
                            Log.d(TAG, "Catalog found for category=${category.name}; moving to product step")
                            goToProductStep()
                        } else {
                            Log.d(TAG, "No catalog items shown for category=${category.name}; staying on category step")
                        }
                    }
                },
                onError = { message ->
                    loadingDialog.dismiss()
                    if (!isFinishing && !isDestroyed) {
                        Log.w(TAG, "Category catalog fetch failed for category=${category.name}: $message")
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
        binding.rvCategories.layoutManager = GridLayoutManager(this, 2)
        binding.rvCategories.adapter = categoryAdapter
        productSearchAdapter = SupplierCatalogProductAdapter(emptyList()) { product ->
            selectCatalogProductFromSearch(product)
        }
        binding.rvCatalogProductSearchResults.layoutManager = LinearLayoutManager(this)
        binding.rvCatalogProductSearchResults.adapter = productSearchAdapter
        binding.etSearchCategories.addTextChangedListener { loadCombinedCategorySearch() }
        binding.btnNextCategory.setOnClickListener {
            goToProductStep()
        }
    }

    private fun setupProductStep() {
        catalogProductAdapter = SupplierCatalogProductAdapter(emptyList()) { product ->
            selectedProduct = product
            bindSelectedProduct(product)
            selectedImageDataUrl = null
            binding.tvSelectedImageStatus.text = getString(R.string.supplier_product_image_helper)
        }
        binding.rvCatalogProducts.layoutManager = LinearLayoutManager(this)
        binding.rvCatalogProducts.adapter = catalogProductAdapter
        binding.etSearchCatalog.addTextChangedListener { loadCatalogProducts(showReason = false) }
        binding.tvChangeCategory.setOnClickListener {
            currentStep = 1
            renderStep()
        }
        binding.btnNextProduct.setOnClickListener {
            goToPricingStep()
        }
    }

    private fun setupPricingStep() {
        binding.btnChooseProductImage.setOnClickListener {
            imagePicker.launch("image/*")
        }

        binding.btnSaveProduct.setOnClickListener {
            val product = selectedProduct ?: run {
                Toast.makeText(this, getString(R.string.supplier_select_product_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val price = binding.etPrice.text?.toString()?.trim().orEmpty()
            val stock = binding.etStock.text?.toString()?.trim().orEmpty()
            val deliveryDays = binding.etDeliveryDays.text?.toString()?.trim().orEmpty()
            val offerPrice = binding.etOfferPrice.text?.toString()?.trim().orEmpty()
            val offerMaxQuantity = binding.etOfferMaxQuantity.text?.toString()?.trim().orEmpty()
            clearPricingErrors()

            val parsedPrice = price.toIntOrNull()
            val parsedStock = if (stock.isBlank()) null else stock.toIntOrNull()
            val parsedOfferPrice = if (offerPrice.isBlank()) null else offerPrice.toIntOrNull()
            val parsedOfferMaxQuantity = if (offerMaxQuantity.isBlank()) null else offerMaxQuantity.toIntOrNull()
            var hasError = false
            if (parsedPrice == null || parsedPrice <= 0) {
                binding.etPrice.error = getString(R.string.supplier_price_required)
                hasError = true
            }
            if (stock.isNotBlank() && (parsedStock == null || parsedStock < 0)) {
                binding.etStock.error = getString(R.string.supplier_stock_required)
                hasError = true
            }
            if (offerPrice.isNotBlank() && (parsedOfferPrice == null || parsedOfferPrice <= 0)) {
                binding.etOfferPrice.error = getString(R.string.supplier_offer_price_error)
                hasError = true
            }
            if (parsedOfferPrice != null && parsedPrice != null && parsedOfferPrice >= parsedPrice) {
                binding.etOfferPrice.error = getString(R.string.supplier_offer_price_error)
                hasError = true
            }
            if (hasError) {
                return@setOnClickListener
            }

            binding.btnSaveProduct.isEnabled = false
            loadingDialog.show(R.string.loading_saving_product)
            val onSuccess = {
                loadingDialog.dismiss()
                Toast.makeText(
                    this,
                    getString(if (isEditMode) R.string.supplier_product_updated else R.string.supplier_product_saved),
                    Toast.LENGTH_SHORT
                ).show()
                SupplierMainActivity.open(this, R.id.nav_supplier_products)
                finish()
            }
            val onError = { message: String ->
                binding.btnSaveProduct.isEnabled = true
                loadingDialog.dismiss()
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }

            val productId = editProductId
            val stockValue = if (stock.isBlank()) null else requireNotNull(parsedStock)
            val deliveryDaysValue = deliveryDays.takeIf { it.isNotBlank() }
            val offerPriceValue = parsedOfferPrice?.takeIf { it > 0 && parsedPrice != null && it < parsedPrice }
            val offerQuantityValue = if (offerPriceValue != null) parsedOfferMaxQuantity?.takeIf { it > 0 } else null
            if (isEditMode && !productId.isNullOrBlank()) {
                SupplierData.updateProductDetails(
                    productId = productId,
                    catalogProductId = product.id,
                    pricePkr = requireNotNull(parsedPrice),
                    stock = stockValue,
                    deliveryDays = deliveryDaysValue,
                    imageDataUrl = selectedImageDataUrl,
                    offerPricePkr = offerPriceValue,
                    maximumOfferQuantity = offerQuantityValue,
                    isActive = binding.switchAvailable.isChecked,
                    onSuccess = onSuccess,
                    onError = onError
                )
            } else {
                SupplierData.addProduct(
                    catalogProductId = product.id,
                    pricePkr = requireNotNull(parsedPrice),
                    stock = stockValue,
                    deliveryDays = deliveryDaysValue,
                    imageDataUrl = selectedImageDataUrl,
                    offerPricePkr = offerPriceValue,
                    maximumOfferQuantity = offerQuantityValue,
                    isActive = binding.switchAvailable.isChecked,
                    onSuccess = onSuccess,
                    onError = onError
                )
            }
        }
    }

    private fun clearPricingErrors() {
        binding.etPrice.error = null
        binding.etStock.error = null
        binding.etDeliveryDays.error = null
        binding.etOfferPrice.error = null
        binding.etOfferMaxQuantity.error = null
    }

    private fun loadCatalogProducts(showReason: Boolean): Boolean {
        val category = selectedCategory ?: return false
        val categoryId = category.id
        val searchQuery = binding.etSearchCatalog.text?.toString().orEmpty()
        val allProducts = SupplierData.getCatalogProducts(categoryId, "")
        val filteredProducts = SupplierData.getCatalogProducts(categoryId, searchQuery)
        Log.d(
            TAG,
            "Catalog filter: categoryId=$categoryId, categoryName=${category.name}, search='$searchQuery', total=${allProducts.size}, filtered=${filteredProducts.size}"
        )
        catalogProductAdapter.updateItems(filteredProducts)
        binding.rvCatalogProducts.scrollToPosition(0)
        if (filteredProducts.isNotEmpty()) return true
        if (!showReason) return false

        val messageRes = when {
            categoryId.isBlank() -> R.string.supplier_category_invalid
            searchQuery.isNotBlank() && allProducts.isNotEmpty() -> R.string.supplier_catalog_search_no_results
            category.catalogCount <= 0 && category.listingCount > 0 -> R.string.supplier_category_has_listings_no_catalog
            category.catalogCount <= 0 && category.listingCount <= 0 -> R.string.supplier_category_empty
            allProducts.isEmpty() -> R.string.supplier_category_no_catalog_products
            else -> R.string.supplier_category_empty
        }
        Log.d(TAG, "Catalog empty reason=${resources.getResourceEntryName(messageRes)} for category=${category.name}")
        Toast.makeText(this, getString(messageRes), Toast.LENGTH_SHORT).show()
        return false
    }

    private fun loadCategories() {
        val query = binding.etSearchCategories.text?.toString().orEmpty()
        if (query.isNotBlank()) {
            loadCombinedCategorySearch()
            return
        }
        val items = SupplierData.getCategories()
        Log.d(TAG, "Category search='$query' -> ${items.size} categories")
        categoryAdapter.updateItems(items)
        binding.rvCategories.scrollToPosition(0)
    }

    private fun loadCombinedCategorySearch() {
        val query = binding.etSearchCategories.text?.toString().orEmpty()
        val isSearchingProducts = query.isNotBlank()

        if (!isSearchingProducts) {
            val categoryItems = SupplierData.getCategories()
            Log.d(TAG, "Category browse restored -> ${categoryItems.size} categories")
            categoryAdapter.updateItems(categoryItems)
            productSearchAdapter.updateItems(emptyList())
            binding.rvCatalogProductSearchResults.visibility = View.GONE
            binding.layoutCatalogProductSearchLoading.visibility = View.GONE
            binding.rvCategories.visibility = if (isCategoryLoading) View.GONE else View.VISIBLE
            binding.layoutCategorySkeleton.visibility = if (isCategoryLoading) View.VISIBLE else View.GONE
            binding.rvCategories.scrollToPosition(0)
            return
        }

        // A typed query always uses the catalog-product result area, including
        // while a background catalog prefetch is still running.
        binding.rvCategories.visibility = View.GONE
        binding.layoutCategorySkeleton.visibility = View.GONE

        val matchingCategoryIds = SupplierData.getCategories(query)
            .map { it.id }
            .toSet()
        val needsCategoryCatalogLoad = matchingCategoryIds.isNotEmpty() &&
            !SupplierData.hasFullCatalogProducts() &&
            !loadedSearchCategoryIds.containsAll(matchingCategoryIds)

        if (needsCategoryCatalogLoad) {
            productSearchAdapter.updateItems(emptyList())
            binding.rvCatalogProductSearchResults.visibility = View.GONE
            binding.layoutCatalogProductSearchLoading.visibility = View.VISIBLE
            if (isProductSearchLoading) return

            isProductSearchLoading = true
            Log.d(TAG, "Category search='$query' loading catalogs for $matchingCategoryIds")
            SupplierData.refreshCatalogForCategories(
                categoryIds = matchingCategoryIds,
                onSuccess = {
                    isProductSearchLoading = false
                    loadedSearchCategoryIds = matchingCategoryIds
                    loadedCatalogProductSearchQuery = ""
                    if (!isFinishing && !isDestroyed) {
                        loadCombinedCategorySearch()
                    }
                },
                onError = { message ->
                    isProductSearchLoading = false
                    if (!isFinishing && !isDestroyed) {
                        binding.layoutCatalogProductSearchLoading.visibility = View.GONE
                        Log.w(TAG, "Category search catalog refresh failed: $message")
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                }
            )
            return
        }

        val normalizedQuery = query.trim()
        val needsProductSearchLoad = !SupplierData.hasFullCatalogProducts() &&
            matchingCategoryIds.isEmpty() &&
            !loadedCatalogProductSearchQuery.equals(normalizedQuery, ignoreCase = true)

        if (needsProductSearchLoad) {
            productSearchAdapter.updateItems(emptyList())
            binding.rvCatalogProductSearchResults.visibility = View.GONE
            binding.layoutCatalogProductSearchLoading.visibility = View.VISIBLE
            if (isProductSearchLoading) return
            isProductSearchLoading = true
            Log.d(TAG, "Product search='$query' loading filtered catalog results")
            SupplierData.refreshCatalogForSearch(
                query = query,
                onSuccess = {
                    isProductSearchLoading = false
                    if (!isFinishing && !isDestroyed) {
                        loadedCatalogProductSearchQuery = normalizedQuery
                        loadedSearchCategoryIds = emptySet()
                        loadCombinedCategorySearch()
                    }
                },
                onError = { message ->
                    isProductSearchLoading = false
                    if (!isFinishing && !isDestroyed) {
                        binding.layoutCatalogProductSearchLoading.visibility = View.GONE
                        Log.w(TAG, "Filtered product search failed: $message")
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                }
            )
            return
        }

        val productItems = SupplierData.searchCatalogProducts(query)
        Log.d(TAG, "Combined search='$query' -> ${productItems.size} catalog products")
        productSearchAdapter.updateItems(productItems)
        val showProducts = productItems.isNotEmpty()
        binding.layoutCatalogProductSearchLoading.visibility = View.GONE
        binding.rvCatalogProductSearchResults.visibility = if (showProducts) View.VISIBLE else View.GONE
        binding.rvCategories.visibility = View.GONE
        binding.layoutCategorySkeleton.visibility = View.GONE
        if (showProducts) {
            binding.rvCatalogProductSearchResults.scrollToPosition(0)
        }
    }

    private fun selectCatalogProductFromSearch(product: SupplierCatalogProduct) {
        Log.d(
            TAG,
            "Product selected from category step search: productId=${product.id}, categoryId=${product.categoryId}, name=${product.name}"
        )
        selectedProduct = product
        selectedCategory = SupplierData.findCategory(product.categoryId) ?: SupplierCategory(
            id = product.categoryId,
            name = product.categoryName,
            productCount = 0,
            catalogCount = 0,
            listingCount = 0,
            accentColorRes = product.accentColorRes
        )
        selectedCategory?.let { category ->
            categoryAdapter.selectedCategoryId = category.id
            binding.tvSelectedCategory.text = category.name
        }
        catalogProductAdapter.selectedProductId = product.id
        productSearchAdapter.selectedProductId = product.id
        bindSelectedProduct(product)
        selectedImageDataUrl = null
        binding.tvSelectedImageStatus.text = getString(R.string.supplier_product_image_helper)
        currentStep = 3
        renderStep()
    }

    private fun renderStep() {
        setEditContentLoading(false)
        binding.tvStepLabel.text = getString(R.string.supplier_step_format, currentStep)
        binding.layoutStepCategory.visibility = if (currentStep == 1) View.VISIBLE else View.GONE
        binding.layoutStepProduct.visibility = if (currentStep == 2) View.VISIBLE else View.GONE
        binding.layoutStepPricing.visibility = if (currentStep == 3) View.VISIBLE else View.GONE
        binding.btnStickyNext.visibility = if (currentStep == 3) View.GONE else View.VISIBLE
        binding.btnStickyNext.text = getString(
            if (currentStep == 1) {
                R.string.supplier_next_choose_product
            } else {
                R.string.supplier_next_set_pricing
            }
        )
        binding.scrollAddProduct.post { binding.scrollAddProduct.smoothScrollTo(0, 0) }
    }

    private fun setEditContentLoading(isLoading: Boolean) {
        if (!isEditMode) return
        binding.scrollAddProduct.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
        binding.btnStickyNext.visibility = if (isLoading || currentStep == 3) View.GONE else View.VISIBLE
    }

    private fun loadCatalogData() {
        Log.d(TAG, "loadCatalogData() started. editMode=$isEditMode")
        showCategoryLoading(true)
        if (isEditMode) {
            setEditContentLoading(true)
        }
        BackgroundWork.run(
            task = {
                if (isEditMode) {
                    SupplierData.restoreCachedProducts()
                } else {
                    SupplierData.restoreCachedCategories()
                }
            },
            onSuccess = { restoredFromCache ->
                if (isFinishing || isDestroyed) return@run
                Log.d(TAG, "cache restore complete restoredFromCache=$restoredFromCache")
                if (restoredFromCache && isEditMode) {
                    prefillEditProduct(finishIfMissing = false)
                }
                val onSuccess: () -> Unit = {
                    Log.d(TAG, "Category refresh completed. categories=${SupplierData.getCategories().size}")
                    showCategoryLoading(false)
                    loadCategories()
                    if (isEditMode && selectedProduct == null) {
                        prefillEditProduct(finishIfMissing = true)
                    } else if (isEditMode) {
                        setEditContentLoading(false)
                    } else {
                        renderStep()
                    }
                    Unit
                }
                val onError = { message: String ->
                    Log.w(TAG, "Category refresh failed: $message")
                    showCategoryLoading(false)
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    if (!isEditMode) {
                        renderStep()
                    } else if (selectedProduct == null) {
                        renderStep()
                    }
                }
                SupplierData.refreshCategories(onSuccess = onSuccess, onError = onError)
            },
            onError = {
                if (isFinishing || isDestroyed) return@run
                Log.w(TAG, "cache restore failed; falling back to category refresh")
                val onSuccess: () -> Unit = {
                    Log.d(TAG, "Category refresh completed after restore fallback. categories=${SupplierData.getCategories().size}")
                    showCategoryLoading(false)
                    loadCategories()
                    if (isEditMode) {
                        prefillEditProduct(finishIfMissing = true)
                    } else {
                        renderStep()
                    }
                    Unit
                }
                val onError = { message: String ->
                    Log.w(TAG, "Catalog refresh failed after restore fallback: $message")
                    showCategoryLoading(false)
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    renderStep()
                }
                SupplierData.refreshCategories(onSuccess = onSuccess, onError = onError)
            }
        )
    }

    private fun showCategoryLoading(show: Boolean) {
        isCategoryLoading = show
        val isSearchingProducts = binding.etSearchCategories.text?.toString().orEmpty().isNotBlank()
        val isShowingProductResults = binding.rvCatalogProductSearchResults.visibility == View.VISIBLE
        binding.layoutCategorySkeleton.visibility =
            if (show && !isSearchingProducts && !isShowingProductResults) View.VISIBLE else View.GONE
        binding.rvCategories.visibility =
            if (!show && !isSearchingProducts && !isShowingProductResults) View.VISIBLE else View.GONE
        if (show) {
            ShimmerSkeleton.start(binding.layoutCategorySkeleton)
        } else {
            ShimmerSkeleton.stop(binding.layoutCategorySkeleton)
        }
    }

    private fun goToProductStep() {
        if (selectedCategory == null) {
            Toast.makeText(this, getString(R.string.supplier_select_category_first), Toast.LENGTH_SHORT).show()
            return
        }
        currentStep = 2
        renderStep()
    }

    private fun goToPricingStep() {
        if (selectedProduct == null) {
            Toast.makeText(this, getString(R.string.supplier_select_product_first), Toast.LENGTH_SHORT).show()
            return
        }
        currentStep = 3
        renderStep()
    }

    private fun prefillEditProduct(finishIfMissing: Boolean = true): Boolean {
        val product = editProductId?.let(SupplierData::findProduct)
        if (product == null) {
            if (finishIfMissing) {
                Toast.makeText(this, getString(R.string.supplier_product_not_found), Toast.LENGTH_SHORT).show()
                finish()
            }
            return false
        }

        selectedProduct = SupplierData.findCatalogProduct(product.catalogProductId)
        selectedCategory = selectedProduct?.let { SupplierData.findCategory(it.categoryId) }
            ?: SupplierData.getCategories().firstOrNull { it.name == product.categoryName }

        selectedCategory?.let { category ->
            categoryAdapter.selectedCategoryId = category.id
            loadCategories()
            binding.tvSelectedCategory.text = category.name
        }

        selectedProduct?.let { catalogProduct ->
            catalogProductAdapter.selectedProductId = catalogProduct.id
            bindSelectedProduct(catalogProduct, product.imageUrl)
        } ?: run {
            selectedProduct = SupplierCatalogProduct(
                id = product.catalogProductId,
                categoryId = selectedCategory?.id.orEmpty(),
                categoryName = selectedCategory?.name.orEmpty(),
                name = product.name,
                unitLabel = product.unitLabel,
                packaging = "",
                imageUrl = product.imageUrl,
                accentColorRes = product.accentColorRes
            )
            bindSelectedProduct(requireNotNull(selectedProduct), product.imageUrl)
        }

        binding.etPrice.setText(product.pricePkr.toString())
        binding.etStock.setText(product.stock.toString())
        binding.etDeliveryDays.setText(product.deliveryDays)
        binding.etOfferPrice.setText(if (product.offerPricePkr > 0) product.offerPricePkr.toString() else "")
        binding.etOfferMaxQuantity.setText(
            if (product.maximumOfferQuantity > 0) product.maximumOfferQuantity.toString() else ""
        )
        binding.switchAvailable.isChecked = product.isActive
        binding.tvSelectedImageStatus.text = getString(R.string.supplier_product_image_helper)
        currentStep = 3
        renderStep()
        return true
    }

    private fun bindSelectedProduct(product: SupplierCatalogProduct, fallbackImageUrl: String = product.imageUrl) {
        val imageUrl = fallbackImageUrl.ifBlank { product.imageUrl }
        binding.tvSelectedProductThumb.text = initials(product.name)
        binding.tvSelectedProductName.text = product.name
        binding.tvSelectedProductMeta.text = listOf(
            selectedCategory?.name.orEmpty(),
            getString(R.string.supplier_catalog_product_label),
            product.unitLabel
        ).filter { it.isNotBlank() }.joinToString(" • ")
        binding.ivSelectedProductImage.loadMarketplaceImage(imageUrl)
        binding.tvSelectedProductThumb.alpha = if (imageUrl.isBlank()) 1f else 0f
    }

    private fun Uri.toDataUrl(mimeType: String): String {
        val bytes = contentResolver.openInputStream(this)?.use { it.readBytes() } ?: ByteArray(0)
        if (bytes.isEmpty()) throw IllegalStateException("Image file is empty.")
        return "data:$mimeType;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    override fun onDestroy() {
        loadingDialog.dismiss()
        ShimmerSkeleton.stop(binding.layoutCategorySkeleton)
        super.onDestroy()
    }

}
