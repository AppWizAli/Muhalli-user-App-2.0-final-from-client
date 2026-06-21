package com.hiskytechs.muhallinewuserapp.supplier.Ui

import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.databinding.ActivitySupplierInventoryManagementBinding
import com.hiskytechs.muhallinewuserapp.supplier.Adapters.SupplierInventoryAdapter
import com.hiskytechs.muhallinewuserapp.supplier.Data.SupplierData
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierProductFilter
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierStockState

class SupplierInventoryManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySupplierInventoryManagementBinding
    private lateinit var inventoryAdapter: SupplierInventoryAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private var isRefreshing = false
    private var isLoadingMore = false
    private var inventoryPage = 1
    private var hasMoreInventory = true
    private var firstResumeHandled = false
    private val pendingStockUpdates = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySupplierInventoryManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        inventoryAdapter = SupplierInventoryAdapter(
            items = emptyList(),
            onAdjustStock = { product, delta ->
                if (!pendingStockUpdates.add(product.id)) return@SupplierInventoryAdapter
                SupplierData.applyLocalStockDelta(product.id, delta)
                loadInventory()
                SupplierData.adjustStock(
                    productId = product.id,
                    delta = delta,
                    onSuccess = {
                        pendingStockUpdates.remove(product.id)
                        loadInventory()
                    },
                    onError = { message ->
                        pendingStockUpdates.remove(product.id)
                        SupplierData.applyLocalStockDelta(product.id, -delta)
                        loadInventory()
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onUpdate = {
                Toast.makeText(this, getString(R.string.supplier_stock_updated), Toast.LENGTH_SHORT).show()
            }
        )
        layoutManager = LinearLayoutManager(this)
        binding.rvInventory.layoutManager = layoutManager
        binding.rvInventory.adapter = inventoryAdapter
        binding.rvInventory.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy <= 0 || isRefreshing || isLoadingMore || !hasMoreInventory) return
                val visibleCount = layoutManager.childCount
                val firstVisible = layoutManager.findFirstVisibleItemPosition()
                val totalCount = inventoryAdapter.itemCount
                if (visibleCount + firstVisible >= totalCount - 6) {
                    loadMoreInventory()
                }
            }
        })
        binding.ivBack.setOnClickListener { finish() }

        val restoredFromCache = SupplierData.restoreCachedProducts()
        if (restoredFromCache) {
            loadInventory()
        } else {
            showLoadingState(true)
        }
        refreshInventory(showLoading = !restoredFromCache)
    }

    override fun onResume() {
        super.onResume()
        if (!firstResumeHandled) {
            firstResumeHandled = true
            return
        }
        refreshInventory(showLoading = false)
    }

    private fun refreshInventory(showLoading: Boolean) {
        if (isRefreshing) return
        isRefreshing = true
        inventoryPage = 1
        hasMoreInventory = true
        if (showLoading) {
            showLoadingState(true)
        } else if (inventoryAdapter.itemCount > 0) {
            showRefreshingState()
        }
        SupplierData.refreshInventoryProductsPage(
            page = inventoryPage,
            onSuccess = { hasMore ->
                isRefreshing = false
                hasMoreInventory = hasMore
                loadInventory()
            },
            onError = { message ->
                isRefreshing = false
                updateStateVisibility()
                Toast.makeText(
                    this,
                    getString(R.string.supplier_inventory_refresh_failed, message),
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    private fun loadMoreInventory() {
        isLoadingMore = true
        showLoadingMoreState()
        SupplierData.refreshInventoryProductsPage(
            page = inventoryPage + 1,
            onSuccess = { hasMore ->
                inventoryPage += 1
                hasMoreInventory = hasMore
                isLoadingMore = false
                loadInventory()
            },
            onError = { message ->
                isLoadingMore = false
                updateStateVisibility()
                Toast.makeText(
                    this,
                    getString(R.string.supplier_inventory_refresh_failed, message),
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    private fun loadInventory() {
        val products = SupplierData.getProducts(SupplierProductFilter.ALL)
        binding.tvInventoryTotalProducts.text = products.size.toString()
        binding.tvInventoryLowStock.text = products.count { it.stockState == SupplierStockState.LOW_STOCK }.toString()
        binding.tvInventoryOutOfStock.text = products.count { it.stockState == SupplierStockState.OUT_OF_STOCK }.toString()
        inventoryAdapter.updateItems(products)
        updateStateVisibility()
    }

    private fun showLoadingState(show: Boolean) {
        binding.progressInventory.visibility = if (show) View.VISIBLE else View.GONE
        binding.tvInventoryState.visibility = if (show) View.VISIBLE else View.GONE
        binding.tvInventoryState.text = getString(R.string.supplier_inventory_refreshing)
        binding.layoutInventorySkeleton.visibility = if (show) View.VISIBLE else View.GONE
        if (show) startSkeletonShimmer() else stopSkeletonShimmer()
    }

    private fun showRefreshingState() {
        binding.progressInventory.visibility = View.VISIBLE
        binding.tvInventoryState.visibility = View.VISIBLE
        binding.tvInventoryState.text = getString(R.string.supplier_inventory_refreshing)
    }

    private fun showLoadingMoreState() {
        binding.progressInventory.visibility = View.VISIBLE
        binding.tvInventoryState.visibility = View.VISIBLE
        binding.tvInventoryState.text = getString(R.string.supplier_inventory_loading_more)
    }

    private fun updateStateVisibility() {
        val hasProducts = inventoryAdapter.itemCount > 0
        binding.progressInventory.visibility = View.GONE
        binding.layoutInventorySkeleton.visibility = View.GONE
        stopSkeletonShimmer()
        binding.tvInventoryState.visibility = if (hasProducts) View.GONE else View.VISIBLE
        if (!hasProducts) {
            binding.tvInventoryState.text = getString(R.string.supplier_inventory_empty)
        }
    }

    private fun startSkeletonShimmer() {
        if (binding.layoutInventorySkeleton.animation != null) return
        binding.layoutInventorySkeleton.startAnimation(
            AlphaAnimation(0.45f, 1f).apply {
                duration = 700L
                repeatMode = Animation.REVERSE
                repeatCount = Animation.INFINITE
            }
        )
    }

    private fun stopSkeletonShimmer() {
        binding.layoutInventorySkeleton.clearAnimation()
    }
}
