package com.hiskytechs.muhallinewuserapp.supplier.Fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.Data.AppData
import com.hiskytechs.muhallinewuserapp.Ui.launchSupportWhatsapp
import com.hiskytechs.muhallinewuserapp.databinding.FragmentSupplierHomeBinding
import com.hiskytechs.muhallinewuserapp.network.BackgroundWork
import com.hiskytechs.muhallinewuserapp.supplier.Adapters.SupplierQuickActionAdapter
import com.hiskytechs.muhallinewuserapp.supplier.Adapters.SupplierRecentOrderAdapter
import com.hiskytechs.muhallinewuserapp.supplier.Data.SupplierData
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierHomeAction
import com.hiskytechs.muhallinewuserapp.supplier.Ui.SupplierAddProductActivity
import com.hiskytechs.muhallinewuserapp.supplier.Ui.SupplierInventoryManagementActivity
import com.hiskytechs.muhallinewuserapp.supplier.Ui.SupplierMainActivity
import com.hiskytechs.muhallinewuserapp.supplier.Ui.SupplierMessagesActivity
import com.hiskytechs.muhallinewuserapp.supplier.Ui.SupplierOrderStatusActivity
import com.hiskytechs.muhallinewuserapp.supplier.Utill.formatPkr

class SupplierHomeFragment : Fragment() {

    private var _binding: FragmentSupplierHomeBinding? = null
    private val binding get() = _binding!!
    private var skippedInitialResume = false
    private var didLoadExtras = false
    private var isShowingHomeSkeleton = false
    private lateinit var recentOrderAdapter: SupplierRecentOrderAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSupplierHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindStaticHomeActions()
        if (SupplierData.hasCachedDashboard()) {
            bindHome()
        } else {
            showHomeSkeleton()
        }
        restoreHomeCacheInBackground()
    }

    override fun onResume() {
        super.onResume()
        if (!skippedInitialResume) {
            skippedInitialResume = true
            return
        }
        refreshHome(showErrors = false, showBlockingLoader = false)
    }

    private fun refreshHome(showErrors: Boolean = true, showBlockingLoader: Boolean = false) {
        SupplierData.refreshHomeSummary(
            onSuccess = {
                if (_binding == null) return@refreshHomeSummary
                bindHome()
            },
            onError = { message ->
                if (_binding == null) return@refreshHomeSummary
                if (showErrors && !SupplierData.hasCachedDashboard()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun restoreHomeCacheInBackground() {
        BackgroundWork.run(
            task = { SupplierData.restoreCachedHomeSummary() },
            onSuccess = { restoredFromCache ->
                if (_binding == null) return@run
                if (restoredFromCache) {
                    bindHome()
                } else {
                    showHomeSkeleton()
                }
                refreshHome(showBlockingLoader = !restoredFromCache)
                scheduleHomeExtrasRefresh()
            },
            onError = {
                if (_binding == null) return@run
                refreshHome(showBlockingLoader = true)
                scheduleHomeExtrasRefresh()
            }
        )
    }

    private fun scheduleHomeExtrasRefresh() {
        if (didLoadExtras) return
        didLoadExtras = true
        binding.root.postDelayed({
            if (_binding == null) return@postDelayed
            SupplierData.refreshHomeExtras(
                onSuccess = {
                    if (_binding == null) return@refreshHomeExtras
                    bindHome()
                },
                onError = {}
            )
        }, HOME_EXTRAS_DELAY_MS)
    }

    private fun bindHome() {
        hideHomeSkeleton()
        val profile = SupplierData.getProfile()
        val stats = SupplierData.getDashboardStats()
        binding.tvBusinessName.text = profile.businessName.ifBlank { getString(R.string.supplier_fallback) }
        binding.tvBusinessSubtitle.text = getString(R.string.supplier_verified_chip)
        binding.tvTodayOrdersValue.text = stats.todayOrders.toString()
        binding.tvPendingOrdersValue.text = stats.pendingOrders.toString()
        binding.tvRevenueValue.text = formatPkr(stats.thisMonthRevenuePkr)
        binding.tvProductsValue.text = stats.totalProducts.toString()
        binding.tvLowStockMessage.text = SupplierData.getLowStockAlert()
        val unreadCount = SupplierData.getConversations().sumOf { it.unreadCount }
        binding.tvNotificationBadge.text = unreadCount.toString()
        binding.tvNotificationBadge.visibility = if (unreadCount > 0) View.VISIBLE else View.GONE
        recentOrderAdapter.updateItems(SupplierData.getRecentOrders())
        binding.rvRecentOrders.visibility = View.VISIBLE
    }

    private fun showHomeSkeleton() {
        if (_binding == null || isShowingHomeSkeleton) return
        isShowingHomeSkeleton = true
        homeSkeletonViews().forEach { view ->
            if (view is android.widget.TextView) {
                view.text = ""
            }
            view.setBackgroundResource(R.drawable.bg_shimmer_placeholder)
            view.startAnimation(shimmerAnimation())
        }
        binding.tvNotificationBadge.visibility = View.GONE
        binding.rvRecentOrders.visibility = View.GONE
        binding.layoutRecentOrdersSkeleton.visibility = View.VISIBLE
        binding.layoutRecentOrdersSkeleton.startAnimation(shimmerAnimation())
    }

    private fun hideHomeSkeleton() {
        if (_binding == null || !isShowingHomeSkeleton) return
        isShowingHomeSkeleton = false
        homeSkeletonViews().forEach { view ->
            view.clearAnimation()
            view.background = null
        }
        binding.layoutRecentOrdersSkeleton.clearAnimation()
        binding.layoutRecentOrdersSkeleton.visibility = View.GONE
    }

    private fun homeSkeletonViews(): List<View> {
        return listOf(
            binding.tvBusinessName,
            binding.tvBusinessSubtitle,
            binding.tvTodayOrdersValue,
            binding.tvPendingOrdersValue,
            binding.tvRevenueValue,
            binding.tvProductsValue,
            binding.tvLowStockMessage
        )
    }

    private fun shimmerAnimation(): Animation {
        return AlphaAnimation(0.45f, 1f).apply {
            duration = 700L
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
    }

    private fun bindStaticHomeActions() {
        binding.btnHomeAddProduct.setOnClickListener {
            startActivity(Intent(requireContext(), SupplierAddProductActivity::class.java))
        }

        binding.rvQuickActions.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.rvQuickActions.setHasFixedSize(true)
        binding.rvQuickActions.adapter = SupplierQuickActionAdapter(SupplierData.getQuickActions()) { action ->
            when (action.action) {
                SupplierHomeAction.ADD_PRODUCT -> startActivity(Intent(requireContext(), SupplierAddProductActivity::class.java))
                SupplierHomeAction.VIEW_ORDERS -> (activity as? SupplierMainActivity)?.openTab(R.id.nav_supplier_orders)
                SupplierHomeAction.OPEN_MESSAGES -> startActivity(Intent(requireContext(), SupplierMessagesActivity::class.java))
                SupplierHomeAction.OPEN_INVENTORY -> startActivity(Intent(requireContext(), SupplierInventoryManagementActivity::class.java))
            }
        }

        binding.rvRecentOrders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecentOrders.setHasFixedSize(true)
        recentOrderAdapter = SupplierRecentOrderAdapter(emptyList()) {
            SupplierOrderStatusActivity.open(requireContext(), it.id)
        }
        binding.rvRecentOrders.adapter = recentOrderAdapter

        binding.tvViewAllOrders.setOnClickListener {
            (activity as? SupplierMainActivity)?.openTab(R.id.nav_supplier_orders)
        }

        binding.layoutNotifications.setOnClickListener {
            startActivity(Intent(requireContext(), SupplierMessagesActivity::class.java))
        }

        binding.layoutSupport.setOnClickListener {
            openSupportWhatsApp()
        }

        binding.layoutLowStock.setOnClickListener {
            startActivity(Intent(requireContext(), SupplierInventoryManagementActivity::class.java))
        }
    }

    private fun openSupportWhatsApp() {
        AppData.loadPublicSettings(
            onSuccess = { settings ->
                if (_binding == null) return@loadPublicSettings
                launchSupportWhatsapp(
                    context = requireContext(),
                    phoneNumber = settings.supportWhatsapp,
                    prefilledMessage = settings.supportWhatsappMessage
                )
            },
            onError = { message ->
                if (_binding == null) return@loadPublicSettings
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (_binding != null) {
            hideHomeSkeleton()
        }
        _binding = null
    }

    companion object {
        private const val HOME_EXTRAS_DELAY_MS = 1200L
    }
}
