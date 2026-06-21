package com.hiskytechs.muhallinewuserapp.supplier.Fragments

import android.content.Intent
import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.util.Base64
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.Ui.AppLoadingDialog
import com.hiskytechs.muhallinewuserapp.databinding.FragmentSupplierStoreProductsBinding
import com.hiskytechs.muhallinewuserapp.network.BackgroundWork
import com.hiskytechs.muhallinewuserapp.supplier.Adapters.SupplierStoreProductAdapter
import com.hiskytechs.muhallinewuserapp.supplier.Data.SupplierData
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierProductFilter
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierProduct
import com.hiskytechs.muhallinewuserapp.supplier.Ui.SupplierAddProductActivity

class SupplierProductsFragment : Fragment() {

    private var _binding: FragmentSupplierStoreProductsBinding? = null
    private val binding get() = _binding!!
    private var loadingDialog: AppLoadingDialog? = null
    private lateinit var productAdapter: SupplierStoreProductAdapter
    private var currentFilter = SupplierProductFilter.ALL
    private var filteredProducts: List<SupplierProduct> = emptyList()
    private var visibleProductCount = PRODUCT_PAGE_SIZE
    private var didInitialRefresh = false
    private var isAppendingProductPage = false
    private val searchHandler = Handler(Looper.getMainLooper())
    private var pendingProductSearch: Runnable? = null
    private var skippedInitialResume = false
    private val bulkUploadPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            uploadBulkFile(uri)
        }
    }
    private val bulkCatalogUploadPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            uploadBulkCatalogFile(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSupplierStoreProductsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingDialog = (activity as? AppCompatActivity)?.let(::AppLoadingDialog)
        productAdapter = SupplierStoreProductAdapter(
            items = emptyList(),
            onEdit = {
                SupplierAddProductActivity.openEdit(requireContext(), it.id)
            },
            onPriceEdit = ::showPriceDialog,
            onOffer = ::showOfferDialog,
            onToggle = { product, checked ->
                loadingDialog?.show(R.string.loading_saving_product)
                SupplierData.setProductAvailability(
                    productId = product.id,
                    isActive = checked,
                    onSuccess = {
                        if (_binding == null) return@setProductAvailability
                        loadingDialog?.dismiss()
                        loadProducts()
                    },
                    onError = { message ->
                        if (_binding == null) return@setProductAvailability
                        loadingDialog?.dismiss()
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
        binding.rvProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProducts.setHasFixedSize(true)
        binding.rvProducts.adapter = productAdapter
        showProductsSkeleton()
        binding.rvProducts.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy <= 0 || visibleProductCount >= filteredProducts.size) return

                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                if (lastVisible >= productAdapter.itemCount - 4) {
                    loadNextProductPage()
                }
            }
        })

        binding.etSearchProducts.addTextChangedListener { scheduleProductsLoad() }

        val chips = listOf(binding.chipAll, binding.chipActive, binding.chipInactive, binding.chipLowStock, binding.chipOffers)
        binding.chipAll.setOnClickListener {
            currentFilter = SupplierProductFilter.ALL
            updateChipState(chips, binding.chipAll)
            loadProducts()
        }
        binding.chipActive.setOnClickListener {
            currentFilter = SupplierProductFilter.ACTIVE
            updateChipState(chips, binding.chipActive)
            loadProducts()
        }
        binding.chipInactive.setOnClickListener {
            currentFilter = SupplierProductFilter.INACTIVE
            updateChipState(chips, binding.chipInactive)
            loadProducts()
        }
        binding.chipLowStock.setOnClickListener {
            currentFilter = SupplierProductFilter.LOW_STOCK
            updateChipState(chips, binding.chipLowStock)
            loadProducts()
        }
        binding.chipOffers.setOnClickListener {
            currentFilter = SupplierProductFilter.OFFERS
            updateChipState(chips, binding.chipOffers)
            loadProducts()
        }

        binding.fabAddProduct.setOnClickListener {
            startActivity(Intent(requireContext(), SupplierAddProductActivity::class.java))
        }
        binding.btnBulkUpload.setOnClickListener {
            bulkUploadPicker.launch(
                arrayOf(
                    "text/*",
                    "application/csv",
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                )
            )
        }
        binding.btnBulkCatalogUpload.setOnClickListener {
            bulkCatalogUploadPicker.launch(
                arrayOf(
                    "text/*",
                    "application/csv",
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                )
            )
        }

        updateChipState(chips, binding.chipAll)
        binding.root.post { restoreProductsCacheInBackground() }
    }

    override fun onResume() {
        super.onResume()
        if (!skippedInitialResume) {
            skippedInitialResume = true
            return
        }
        if (_binding != null && didInitialRefresh) {
            refreshProducts(showBlockingLoader = false)
        }
    }

    private fun refreshProducts(showBlockingLoader: Boolean = true) {
        if (showBlockingLoader) showProductsSkeleton()
        SupplierData.refreshProducts(
            onSuccess = {
                if (_binding == null) return@refreshProducts
                hideProductsSkeleton()
                didInitialRefresh = true
                loadProducts()
            },
            onError = { message ->
                if (_binding == null) return@refreshProducts
                hideProductsSkeleton()
                if (!didInitialRefresh) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun restoreProductsCacheInBackground() {
        BackgroundWork.run(
            task = { SupplierData.restoreCachedProducts() },
            onSuccess = { restoredFromCache ->
                if (_binding == null) return@run
                if (restoredFromCache) {
                    didInitialRefresh = true
                    loadProducts()
                    hideProductsSkeleton()
                }
                refreshProducts(showBlockingLoader = !restoredFromCache)
            },
            onError = {
                if (_binding == null) return@run
                refreshProducts(showBlockingLoader = true)
            }
        )
    }

    private fun loadProducts() {
        filteredProducts = SupplierData.getProducts(
            filter = currentFilter,
            query = binding.etSearchProducts.text?.toString().orEmpty()
        )
        visibleProductCount = PRODUCT_PAGE_SIZE
        renderProductPage()
    }

    private fun scheduleProductsLoad() {
        pendingProductSearch?.let(searchHandler::removeCallbacks)
        pendingProductSearch = Runnable {
            loadProducts()
        }.also { searchHandler.postDelayed(it, SEARCH_DEBOUNCE_MS) }
    }

    private fun loadNextProductPage() {
        if (isAppendingProductPage || visibleProductCount >= filteredProducts.size) return
        isAppendingProductPage = true
        binding.progressProductsPage.visibility = View.VISIBLE
        binding.rvProducts.postDelayed({
            if (_binding == null) return@postDelayed
            visibleProductCount = (visibleProductCount + PRODUCT_PAGE_SIZE).coerceAtMost(filteredProducts.size)
            renderProductPage()
        }, 180)
    }

    private fun renderProductPage() {
        productAdapter.updateItems(filteredProducts.take(visibleProductCount))
        binding.progressProductsPage.visibility = View.GONE
        isAppendingProductPage = false
        hideProductsSkeleton()
    }

    private fun showProductsSkeleton() {
        if (_binding == null) return
        binding.layoutProductsSkeleton.visibility = View.VISIBLE
        binding.rvProducts.visibility = View.GONE
        if (binding.layoutProductsSkeleton.animation == null) {
            binding.layoutProductsSkeleton.startAnimation(
                AlphaAnimation(0.45f, 1f).apply {
                    duration = 700L
                    repeatMode = Animation.REVERSE
                    repeatCount = Animation.INFINITE
                }
            )
        }
    }

    private fun hideProductsSkeleton() {
        if (_binding == null) return
        binding.layoutProductsSkeleton.clearAnimation()
        binding.layoutProductsSkeleton.visibility = View.GONE
        binding.rvProducts.visibility = View.VISIBLE
    }

    companion object {
        private const val PRODUCT_PAGE_SIZE = 20
        private const val SEARCH_DEBOUNCE_MS = 300L
    }

    private fun uploadBulkFile(uri: Uri) {
        val fileName = uri.displayName()
        val resolver = requireContext().contentResolver
        val readError = getString(R.string.supplier_bulk_file_read_failed)
        binding.btnBulkUpload.isEnabled = false
        loadingDialog?.show(R.string.loading_uploading_sheet)

        BackgroundWork.run(
            task = { encodeUploadFile(resolver, uri, readError) },
            onSuccess = { encodedFile ->
                SupplierData.bulkUploadProducts(
                    fileName = fileName,
                    fileDataBase64 = encodedFile,
                    onSuccess = { summary ->
                        if (_binding == null) return@bulkUploadProducts
                        binding.btnBulkUpload.isEnabled = true
                        loadingDialog?.dismiss()
                        Toast.makeText(requireContext(), summary, Toast.LENGTH_LONG).show()
                        refreshProducts()
                    },
                    onError = { message ->
                        if (_binding == null) return@bulkUploadProducts
                        binding.btnBulkUpload.isEnabled = true
                        loadingDialog?.dismiss()
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    }
                )
            },
            onError = { message ->
                if (_binding == null) return@run
                binding.btnBulkUpload.isEnabled = true
                loadingDialog?.dismiss()
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun uploadBulkCatalogFile(uri: Uri) {
        val fileName = uri.displayName()
        val resolver = requireContext().contentResolver
        val readError = getString(R.string.supplier_bulk_file_read_failed)
        binding.btnBulkCatalogUpload.isEnabled = false
        loadingDialog?.show(R.string.loading_uploading_sheet)

        BackgroundWork.run(
            task = { encodeUploadFile(resolver, uri, readError) },
            onSuccess = { encodedFile ->
                SupplierData.bulkUploadCatalogProducts(
                    fileName = fileName,
                    fileDataBase64 = encodedFile,
                    onSuccess = { summary ->
                        if (_binding == null) return@bulkUploadCatalogProducts
                        binding.btnBulkCatalogUpload.isEnabled = true
                        loadingDialog?.dismiss()
                        Toast.makeText(requireContext(), summary, Toast.LENGTH_LONG).show()
                        refreshProducts()
                    },
                    onError = { message ->
                        if (_binding == null) return@bulkUploadCatalogProducts
                        binding.btnBulkCatalogUpload.isEnabled = true
                        loadingDialog?.dismiss()
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    }
                )
            },
            onError = { message ->
                if (_binding == null) return@run
                binding.btnBulkCatalogUpload.isEnabled = true
                loadingDialog?.dismiss()
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun encodeUploadFile(resolver: ContentResolver, uri: Uri, readError: String): String {
        return resolver.openInputStream(uri)?.use { stream ->
            Base64.encodeToString(stream.readBytes(), Base64.NO_WRAP)
        }.orEmpty().ifBlank {
            throw IllegalStateException(readError)
        }
    }

    private fun Uri.displayName(): String {
        val resolver = requireContext().contentResolver
        return resolver.query(this, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else {
                    null
                }
            }
            ?.takeIf { it.isNotBlank() }
            ?: lastPathSegment.orEmpty().ifBlank { "products.csv" }
    }

    private fun showOfferDialog(product: SupplierProduct) {
        val priceInput = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = getString(R.string.supplier_offer_price_hint)
            setText(product.pricePkr.toString())
        }
        val quantityInput = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = getString(R.string.supplier_offer_max_quantity_hint)
        }
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 12, 40, 0)
            addView(priceInput)
            addView(quantityInput)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.supplier_add_to_offers))
            .setMessage(product.name)
            .setView(container)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.supplier_add_to_offers) { _, _ ->
                val offerPrice = priceInput.text?.toString()?.toIntOrNull() ?: product.pricePkr
                val maximumQuantity = quantityInput.text?.toString()?.toIntOrNull()
                loadingDialog?.show(R.string.loading_saving_product)
                SupplierData.addProductOffer(
                    productId = product.id,
                    offerPricePkr = offerPrice,
                    maximumQuantity = maximumQuantity,
                    onSuccess = {
                        if (_binding == null) return@addProductOffer
                        loadingDialog?.dismiss()
                        Toast.makeText(requireContext(), R.string.supplier_offer_saved, Toast.LENGTH_SHORT).show()
                        loadProducts()
                    },
                    onError = { message ->
                        if (_binding == null) return@addProductOffer
                        loadingDialog?.dismiss()
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .show()
    }

    private fun showPriceDialog(product: SupplierProduct) {
        val priceInput = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = getString(R.string.supplier_enter_price)
            setText(product.pricePkr.toString())
            setSelection(text?.length ?: 0)
        }
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 12, 40, 0)
            addView(priceInput)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.supplier_price_per_unit))
            .setMessage(product.name)
            .setView(container)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.supplier_update, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newPrice = priceInput.text?.toString()?.trim()?.toIntOrNull()
                if (newPrice == null || newPrice <= 0) {
                    priceInput.error = getString(R.string.supplier_price_required)
                    return@setOnClickListener
                }
                if (newPrice == product.pricePkr) {
                    dialog.dismiss()
                    return@setOnClickListener
                }

                dialog.dismiss()
                loadingDialog?.show(R.string.loading_saving_product)
                SupplierData.updateProductDetails(
                    productId = product.id,
                    catalogProductId = product.catalogProductId,
                    pricePkr = newPrice,
                    stock = product.stock,
                    deliveryDays = product.deliveryDays,
                    imageDataUrl = null,
                    offerPricePkr = null,
                    maximumOfferQuantity = null,
                    isActive = product.isActive,
                    onSuccess = {
                        if (_binding == null) return@updateProductDetails
                        loadingDialog?.dismiss()
                        Toast.makeText(requireContext(), R.string.supplier_product_updated, Toast.LENGTH_SHORT).show()
                        loadProducts()
                    },
                    onError = { message ->
                        if (_binding == null) return@updateProductDetails
                        loadingDialog?.dismiss()
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
        dialog.show()
    }

    private fun updateChipState(chips: List<TextView>, selectedChip: TextView) {
        chips.forEach { chip ->
            val isSelected = chip == selectedChip
            chip.setBackgroundResource(
                if (isSelected) R.drawable.bg_supplier_pill_selected else R.drawable.bg_supplier_pill_default
            )
            chip.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (isSelected) R.color.white else R.color.supplier_text_secondary
                )
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pendingProductSearch?.let(searchHandler::removeCallbacks)
        pendingProductSearch = null
        loadingDialog?.dismiss()
        loadingDialog = null
        _binding = null
    }
}
