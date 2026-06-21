package com.hiskytechs.muhallinewuserapp.supplier.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.Ui.AppLoadingDialog
import com.hiskytechs.muhallinewuserapp.databinding.FragmentSupplierOrdersBinding
import com.hiskytechs.muhallinewuserapp.supplier.Adapters.SupplierOrderAdapter
import com.hiskytechs.muhallinewuserapp.supplier.Data.SupplierData
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierOrderStatus
import com.hiskytechs.muhallinewuserapp.supplier.Ui.SupplierOrderStatusActivity

class SupplierOrdersFragment : Fragment() {

    private var _binding: FragmentSupplierOrdersBinding? = null
    private val binding get() = _binding!!
    private var loadingDialog: AppLoadingDialog? = null
    private lateinit var orderAdapter: SupplierOrderAdapter
    private var currentFilter: SupplierOrderStatus? = null
    private var didInitialRefresh = false
    private var skippedInitialResume = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSupplierOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingDialog = (activity as? AppCompatActivity)?.let(::AppLoadingDialog)
        orderAdapter = SupplierOrderAdapter(emptyList()) { order ->
            SupplierOrderStatusActivity.open(requireContext(), order.id)
        }
        binding.rvOrders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrders.setHasFixedSize(true)
        binding.rvOrders.adapter = orderAdapter

        val chips = listOf(
            binding.chipOrdersAll,
            binding.chipOrdersPending,
            binding.chipOrdersConfirmed,
            binding.chipOrdersShipped,
            binding.chipOrdersDelivered
        )

        binding.chipOrdersAll.setOnClickListener {
            updateChipState(chips, binding.chipOrdersAll)
            currentFilter = null
            loadOrders()
        }
        binding.chipOrdersPending.setOnClickListener {
            updateChipState(chips, binding.chipOrdersPending)
            currentFilter = SupplierOrderStatus.PENDING
            loadOrders()
        }
        binding.chipOrdersConfirmed.setOnClickListener {
            updateChipState(chips, binding.chipOrdersConfirmed)
            currentFilter = SupplierOrderStatus.CONFIRMED
            loadOrders()
        }
        binding.chipOrdersShipped.setOnClickListener {
            updateChipState(chips, binding.chipOrdersShipped)
            currentFilter = SupplierOrderStatus.SHIPPED
            loadOrders()
        }
        binding.chipOrdersDelivered.setOnClickListener {
            updateChipState(chips, binding.chipOrdersDelivered)
            currentFilter = SupplierOrderStatus.DELIVERED
            loadOrders()
        }

        updateChipState(chips, binding.chipOrdersAll)
        if (SupplierData.restoreCachedOrders()) {
            didInitialRefresh = true
            loadOrders()
        }
        refreshOrders(showBlockingLoader = !didInitialRefresh)
    }

    override fun onResume() {
        super.onResume()
        if (!skippedInitialResume) {
            skippedInitialResume = true
            return
        }
        if (_binding != null && didInitialRefresh) {
            refreshOrders(showBlockingLoader = false)
        }
    }

    private fun refreshOrders(showBlockingLoader: Boolean = true) {
        if (showBlockingLoader) showOrdersSkeleton()
        SupplierData.refreshOrders(
            onSuccess = {
                if (_binding == null) return@refreshOrders
                hideOrdersSkeleton()
                didInitialRefresh = true
                loadOrders()
            },
            onError = { message ->
                if (_binding == null) return@refreshOrders
                hideOrdersSkeleton()
                if (!didInitialRefresh) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun loadOrders() {
        orderAdapter.updateItems(SupplierData.getOrders(currentFilter))
        hideOrdersSkeleton()
    }

    private fun showOrdersSkeleton() {
        if (_binding == null) return
        binding.layoutOrdersSkeleton.visibility = View.VISIBLE
        binding.rvOrders.visibility = View.GONE
        if (binding.layoutOrdersSkeleton.animation == null) {
            binding.layoutOrdersSkeleton.startAnimation(
                AlphaAnimation(0.45f, 1f).apply {
                    duration = 700L
                    repeatMode = Animation.REVERSE
                    repeatCount = Animation.INFINITE
                }
            )
        }
    }

    private fun hideOrdersSkeleton() {
        if (_binding == null) return
        binding.layoutOrdersSkeleton.clearAnimation()
        binding.layoutOrdersSkeleton.visibility = View.GONE
        binding.rvOrders.visibility = View.VISIBLE
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
        loadingDialog?.dismiss()
        loadingDialog = null
        _binding = null
    }
}
