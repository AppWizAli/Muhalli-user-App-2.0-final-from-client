package com.hiskytechs.muhallinewuserapp.Fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.hiskytechs.muhallinewuserapp.Adapters.OfferAdapter
import com.hiskytechs.muhallinewuserapp.Adapters.ProductSearchAdapter
import com.hiskytechs.muhallinewuserapp.Adapters.SupplierAdapter
import com.hiskytechs.muhallinewuserapp.Data.AppData
import com.hiskytechs.muhallinewuserapp.Models.CartItem
import com.hiskytechs.muhallinewuserapp.Models.MarketplaceOffer
import com.hiskytechs.muhallinewuserapp.Models.Product
import com.hiskytechs.muhallinewuserapp.Models.Supplier
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.Ui.CartActivity
import com.hiskytechs.muhallinewuserapp.Ui.MapActivity
import com.hiskytechs.muhallinewuserapp.Ui.NotificationsActivity
import com.hiskytechs.muhallinewuserapp.Ui.ReferralActivity
import com.hiskytechs.muhallinewuserapp.Ui.SupplierDetailsActivity
import com.hiskytechs.muhallinewuserapp.Utill.CartManager
import com.hiskytechs.muhallinewuserapp.Utill.ShimmerSkeleton
import com.hiskytechs.muhallinewuserapp.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var supplierAdapter: SupplierAdapter
    private lateinit var offerAdapter: OfferAdapter
    private lateinit var productSearchAdapter: ProductSearchAdapter
    private var currentSort = SORT_DEFAULT
    private var currentSearch = ""
    private var nextSupplierLoadForceRefresh = false
    private val searchHandler = Handler(Looper.getMainLooper())
    private var pendingSearchLoad: Runnable? = null
    private var skippedInitialResume = false
    private var isShowingHomeSkeleton = false
    private var hasLoadedInitialHomeContent = false
    private val pendingInitialSections = linkedSetOf<String>()

    private val mapPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            val selectedCity = result.data?.getStringExtra(MapActivity.EXTRA_SELECTED_CITY).orEmpty()
            if (selectedCity.isBlank()) return@registerForActivityResult
            val selectedAddress = result.data?.getStringExtra(MapActivity.EXTRA_SELECTED_ADDRESS).orEmpty()
            val selectedLatitude = if (result.data?.hasExtra(MapActivity.EXTRA_SELECTED_LATITUDE) == true) {
                result.data?.getDoubleExtra(MapActivity.EXTRA_SELECTED_LATITUDE, 0.0)
            } else {
                null
            }
            val selectedLongitude = if (result.data?.hasExtra(MapActivity.EXTRA_SELECTED_LONGITUDE) == true) {
                result.data?.getDoubleExtra(MapActivity.EXTRA_SELECTED_LONGITUDE, 0.0)
            } else {
                null
            }
            binding.tvActiveCity.text = selectedCity
            val updatedProfile = AppData.buyerProfile.copy(
                city = selectedCity,
                address = selectedAddress.ifBlank { selectedCity },
                latitude = selectedLatitude,
                longitude = selectedLongitude
            )
            binding.swipeRefresh.isRefreshing = true
            AppData.updateBuyerProfile(
                updatedProfile = updatedProfile,
                onSuccess = {
                    nextSupplierLoadForceRefresh = true
                    loadOffers()
                    loadSuppliers()
                },
                onError = { message ->
                    binding.swipeRefresh.isRefreshing = false
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            )
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        setupInteractions()
        prepareInitialHomeLoad()
        loadProfileAndContent()
    }

    override fun onResume() {
        super.onResume()
        if (!skippedInitialResume) {
            skippedInitialResume = true
            return
        }
        if (_binding != null) {
            loadOffers()
            loadSuppliers()
            loadReferralSummary()
        }
    }

    private fun setupRecyclerViews() {
        supplierAdapter = SupplierAdapter(emptyList())
        binding.rvSuppliers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSuppliers.setHasFixedSize(true)
        binding.rvSuppliers.adapter = supplierAdapter

        android.util.Log.e(
            "SUPPLIER_DEBUG",
            "layoutManager = ${binding.rvSuppliers.layoutManager}"
        )
        productSearchAdapter = ProductSearchAdapter(
            emptyList(),
            onAddClick = ::addProductToCart,
            onOpenSupplier = { product ->
                Log.e(
                    "MH_SUPPLIER_DEBUG",
                    "homeProductOpen supplierId=${product.supplierId} supplierName='${product.supplierName}' productId=${product.id} productName='${product.name}'"
                )
                startActivity(Intent(requireContext(), SupplierDetailsActivity::class.java).apply {
                    putExtra("supplier_id", product.supplierId)
                    putExtra("supplier_name", product.supplierName)
                    putExtra("location", binding.tvActiveCity.text?.toString().orEmpty())
                })
            }
        )
        binding.rvSearchProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSearchProducts.setHasFixedSize(true)
        binding.rvSearchProducts.adapter = productSearchAdapter

        offerAdapter = OfferAdapter(emptyList(), ::onOfferClicked)
        binding.rvOffers.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.rvOffers.setHasFixedSize(false)
        binding.rvOffers.adapter = offerAdapter
    }

    private fun setupInteractions() {
        binding.etSearch.addTextChangedListener {
            currentSearch = it?.toString().orEmpty()
            scheduleSupplierLoad()
        }
        binding.swipeRefresh.setOnRefreshListener {
            nextSupplierLoadForceRefresh = true
            loadProfileAndContent()
        }
        binding.ivHomeNotifications.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationsActivity::class.java))
        }
        binding.btnOpenReferral.setOnClickListener {
            startActivity(Intent(requireContext(), ReferralActivity::class.java))
        }
        binding.cardReferral.setOnClickListener {
            startActivity(Intent(requireContext(), ReferralActivity::class.java))
        }
        binding.btnPickCity.setOnClickListener {
            mapPickerLauncher.launch(
                Intent(requireContext(), MapActivity::class.java).apply {
                    putExtra(MapActivity.EXTRA_MODE, MapActivity.MODE_PICK)
                    putExtra(MapActivity.EXTRA_CITY, binding.tvActiveCity.text?.toString().orEmpty())
                }
            )
        }
        binding.btnDefaultSort.setOnClickListener {
            currentSort = SORT_DEFAULT
            updateSortButtons()
            loadSuppliers()
        }
        binding.btnCheapest.setOnClickListener {
            currentSort = SORT_CHEAPEST
            updateSortButtons()
            loadSuppliers()
        }
        binding.btnMinOrder.setOnClickListener {
            currentSort = SORT_MIN_ORDER
            updateSortButtons()
            loadSuppliers()
        }
        updateSortButtons()
    }

    private fun loadProfileAndContent() {
        prepareInitialHomeLoad()
        AppData.loadBuyerProfile(
            onSuccess = { profile ->
                if (_binding == null) return@loadBuyerProfile
                binding.tvActiveCity.text = profile.city.ifBlank {
                    getString(R.string.default_city_name)
                }
                markInitialSectionLoaded(INITIAL_SECTION_PROFILE)
                nextSupplierLoadForceRefresh = true
                loadOffers()
                loadSuppliers()
                loadReferralSummary()
            },
            onError = {
                if (_binding == null) return@loadBuyerProfile
                binding.tvActiveCity.text = getString(R.string.default_city_name)
                markInitialSectionLoaded(INITIAL_SECTION_PROFILE)
                nextSupplierLoadForceRefresh = true
                loadOffers()
                loadSuppliers()
                loadReferralSummary()
            }
        )
    }

    private fun loadOffers() {
        AppData.loadOffers(
            cityFilter = binding.tvActiveCity.text?.toString().orEmpty(),
            onSuccess = { offers ->
                if (_binding == null) return@loadOffers
                markInitialSectionLoaded(INITIAL_SECTION_OFFERS)
                binding.swipeRefresh.isRefreshing = false
                offerAdapter.updateItems(offers)
                binding.rvOffers.requestLayout()
            },
            onError = { message ->
                if (_binding == null) return@loadOffers
                markInitialSectionLoaded(INITIAL_SECTION_OFFERS)
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun loadSuppliers() {
        if (currentSearch.isNotBlank()) {
            loadProductResults()
            return
        }

        binding.tvSearchResultsTitle.visibility = View.GONE
        binding.rvSearchProducts.visibility = View.GONE
        binding.rvSuppliers.visibility = View.VISIBLE
        val requestSearch = currentSearch
        val forceRefresh = nextSupplierLoadForceRefresh
        nextSupplierLoadForceRefresh = false
        AppData.loadHomeSuppliers(
            searchQuery = requestSearch,
            cityFilter = "",
            sort = currentSort,
            forceRefresh = forceRefresh,
            onSuccess = { suppliers ->
                if (_binding == null) return@loadHomeSuppliers
                if (requestSearch != currentSearch) {
                    return@loadHomeSuppliers
                }
                markInitialSectionLoaded(INITIAL_SECTION_SUPPLIERS)
                binding.swipeRefresh.isRefreshing = false
                renderSuppliers(suppliers)
            },
            onError = { message ->
                if (_binding == null) return@loadHomeSuppliers
                markInitialSectionLoaded(INITIAL_SECTION_SUPPLIERS)
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun loadProductResults() {
        val requestSearch = currentSearch
        AppData.loadProductSearchResults(
            searchQuery = requestSearch,
            cityFilter = "",
            forceRefresh = nextSupplierLoadForceRefresh.also { nextSupplierLoadForceRefresh = false },
            onSuccess = { products ->
                if (_binding == null) return@loadProductSearchResults
                if (requestSearch != currentSearch) {
                    return@loadProductSearchResults
                }
                markInitialSectionLoaded(INITIAL_SECTION_SUPPLIERS)
                binding.swipeRefresh.isRefreshing = false
                binding.tvSearchResultsTitle.visibility = View.VISIBLE
                binding.rvSearchProducts.visibility = View.VISIBLE
                binding.rvSuppliers.visibility = View.GONE
                binding.tvSearchResultsTitle.text = getString(
                    R.string.search_results_title_format,
                    requestSearch,
                    products.size
                )
                productSearchAdapter.updateItems(products)
                binding.tvSupplierCount.text = getString(
                    R.string.search_sellers_count_format,
                    products.size
                )
            },
            onError = { message ->
                if (_binding == null) return@loadProductSearchResults
                markInitialSectionLoaded(INITIAL_SECTION_SUPPLIERS)
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun onOfferClicked(offer: MarketplaceOffer) {
        AppData.resolveOfferProduct(
            offer = offer,
            onSuccess = { product ->
                if (_binding == null) return@resolveOfferProduct
                val wasMinimumMet = CartManager.getSupplierCart(product.supplierName)?.isMinimumMet == true
                CartManager.addItem(
                    CartItem(
                        id = product.id,
                        name = product.name,
                        supplier = product.supplierName,
                        price = product.price,
                        quantity = 1,
                        imageUrl = product.imageUrl,
                        offerPrice = offer.offerPrice.takeIf { it > 0.0 } ?: product.offerPrice,
                        maximumOfferQuantity = offer.maximumQuantity.takeIf { it > 0 }
                            ?: product.maximumOfferQuantity
                    )
                )
                Toast.makeText(requireContext(), R.string.offer_added_to_cart, Toast.LENGTH_SHORT).show()

                val supplierCart = CartManager.getSupplierCart(product.supplierName)
                if (!wasMinimumMet && supplierCart?.isMinimumMet == true) {
                    startActivity(Intent(requireContext(), CartActivity::class.java).apply {
                        putExtra(CartActivity.EXTRA_SUPPLIER_NAME, product.supplierName)
                    })
                }
            },
            onError = { message ->
                if (_binding == null) return@resolveOfferProduct
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                if (offer.supplierName.isNotBlank()) {
                    startActivity(Intent(requireContext(), SupplierDetailsActivity::class.java).apply {
                        putExtra("supplier_name", offer.supplierName)
                        putExtra("location", offer.city)
                    })
                }
            }
        )
    }

    private fun addProductToCart(product: Product) {
        CartManager.addItem(
            CartItem(
                id = product.id,
                name = product.name,
                supplier = product.supplierName,
                price = product.price,
                quantity = 1,
                imageUrl = product.imageUrl,
                offerPrice = product.offerPrice,
                maximumOfferQuantity = product.maximumOfferQuantity
            )
        )
        Toast.makeText(requireContext(), R.string.added_to_cart, Toast.LENGTH_SHORT).show()
    }

    private fun renderSuppliers(suppliers: List<Supplier>) {

        android.util.Log.e("SUPPLIER_DEBUG", "renderSuppliers size = ${suppliers.size}")

        binding.rvSuppliers.visibility = View.VISIBLE

        supplierAdapter.updateItems(suppliers)

        android.util.Log.e(
            "SUPPLIER_DEBUG",
            "adapter count after update = ${supplierAdapter.itemCount}"
        )

        binding.rvSuppliers.post {
            android.util.Log.e(
                "SUPPLIER_DEBUG",
                "rv childCount = ${binding.rvSuppliers.childCount}"
            )
        }
    }

    private fun loadReferralSummary() {
        AppData.loadReferralSummary(
            onSuccess = { summary ->
                if (_binding == null) return@loadReferralSummary
                binding.tvReferralHeadline.text = if (summary.referralCode.isBlank()) {
                    getString(R.string.referral_home_title)
                } else {
                    getString(R.string.referral_code_home_format, summary.referralCode)
                }
                binding.tvReferralSubline.text = getString(
                    R.string.referral_home_reward_format,
                    summary.totalClaims,
                    summary.rewardAmount
                )
            },
            onError = {
                if (_binding == null) return@loadReferralSummary
                binding.tvReferralHeadline.text = getString(R.string.referral_home_title)
                binding.tvReferralSubline.text = getString(R.string.referral_home_subtitle)
            }
        )
    }

    private fun prepareInitialHomeLoad() {
        if (hasLoadedInitialHomeContent) return
        pendingInitialSections.clear()
        pendingInitialSections.add(INITIAL_SECTION_PROFILE)
        pendingInitialSections.add(INITIAL_SECTION_OFFERS)
        pendingInitialSections.add(INITIAL_SECTION_SUPPLIERS)
        showInlineLoading()
    }

    private fun markInitialSectionLoaded(section: String) {
        pendingInitialSections.remove(section)
        if (!hasLoadedInitialHomeContent && pendingInitialSections.isEmpty()) {
            hasLoadedInitialHomeContent = true
            hideInlineLoading()
        }
    }

    private fun showInlineLoading() {
        if (_binding == null || isShowingHomeSkeleton || hasLoadedInitialHomeContent) return
        isShowingHomeSkeleton = true
        ShimmerSkeleton.start(binding.layoutLoadingOverlay)
    }

    private fun hideInlineLoading() {
        if (_binding == null || !isShowingHomeSkeleton) return
        isShowingHomeSkeleton = false
        ShimmerSkeleton.stop(binding.layoutLoadingOverlay)
    }

    private fun updateSortButtons() {
        setSortState(binding.btnDefaultSort, currentSort == SORT_DEFAULT)
        setSortState(binding.btnCheapest, currentSort == SORT_CHEAPEST)
        setSortState(binding.btnMinOrder, currentSort == SORT_MIN_ORDER)
    }

    private fun scheduleSupplierLoad() {
        pendingSearchLoad?.let(searchHandler::removeCallbacks)
        pendingSearchLoad = Runnable {
            loadSuppliers()
        }.also { searchHandler.postDelayed(it, SEARCH_DEBOUNCE_MS) }
    }

    private fun setSortState(button: MaterialButton, isActive: Boolean) {
        button.alpha = if (isActive) 1f else 0.7f
        button.strokeWidth = if (isActive) 0 else 2
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pendingSearchLoad?.let(searchHandler::removeCallbacks)
        pendingSearchLoad = null
        if (_binding != null) {
            hideInlineLoading()
        }
        _binding = null
    }

    companion object {
        private const val SORT_DEFAULT = "default"
        private const val SORT_CHEAPEST = "cheapest"
        private const val SORT_MIN_ORDER = "low_min_order"
        private const val SEARCH_DEBOUNCE_MS = 350L
        private const val INITIAL_SECTION_PROFILE = "profile"
        private const val INITIAL_SECTION_OFFERS = "offers"
        private const val INITIAL_SECTION_SUPPLIERS = "suppliers"
    }
}
