package com.hiskytechs.muhallinewuserapp.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.hiskytechs.muhallinewuserapp.Adapters.OrderAdapter
import com.hiskytechs.muhallinewuserapp.Data.AppData
import com.hiskytechs.muhallinewuserapp.Models.Order
import com.hiskytechs.muhallinewuserapp.Ui.OrderDetailsActivity
import com.hiskytechs.muhallinewuserapp.Utill.ShimmerSkeleton
import com.hiskytechs.muhallinewuserapp.databinding.FragmentOrdersBinding

class OrdersFragment : Fragment() {

    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!
    private lateinit var orderAdapter: OrderAdapter
    private var allOrders = mutableListOf<Order>()
    private var hasLoadedOrders = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFilters()
        if (!hasLoadedOrders) {
            showLoadingState()
        }
        loadOrders()
    }

    private fun setupRecyclerView() {
        orderAdapter = OrderAdapter(allOrders) { order ->
            startActivity(android.content.Intent(requireContext(), OrderDetailsActivity::class.java).apply {
                putExtra(OrderDetailsActivity.EXTRA_ORDER_ID, order.orderId)
                putExtra(OrderDetailsActivity.EXTRA_ORDER_INTERNAL_ID, order.internalId)
            })
        }
        binding.rvOrders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrders.adapter = orderAdapter
    }

    override fun onResume() {
        super.onResume()
        if (!hasLoadedOrders) {
            showLoadingState()
        }
        loadOrders()
    }

    private fun loadOrders() {
        AppData.loadOrders(
            onSuccess = { orders ->
                if (_binding == null) return@loadOrders
                hasLoadedOrders = true
                allOrders = orders.toMutableList()
                orderAdapter.updateOrders(allOrders)
                binding.rvOrders.visibility = View.VISIBLE
                hideLoadingState()
            },
            onError = { message ->
                if (_binding == null) return@loadOrders
                binding.rvOrders.visibility = View.VISIBLE
                hideLoadingState()
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun showLoadingState() {
        if (_binding == null) return
        binding.rvOrders.visibility = View.INVISIBLE
        ShimmerSkeleton.start(binding.layoutOrdersSkeleton)
    }

    private fun hideLoadingState() {
        if (_binding == null) return
        ShimmerSkeleton.stop(binding.layoutOrdersSkeleton)
    }

    private fun setupFilters() {
        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val filteredOrders = when (checkedId) {
                    binding.btnProcessing.id -> allOrders.filter { it.status.lowercase() == "processing" }
                    binding.btnDelivered.id -> allOrders.filter { it.status.lowercase() == "delivered" }
                    else -> allOrders
                }
                orderAdapter.updateOrders(filteredOrders)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (_binding != null) {
            hideLoadingState()
        }
        _binding = null
    }
}
