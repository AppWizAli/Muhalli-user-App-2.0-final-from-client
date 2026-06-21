package com.hiskytechs.muhallinewuserapp.Fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.hiskytechs.muhallinewuserapp.Adapters.CartAdapter
import com.hiskytechs.muhallinewuserapp.Adapters.CartSupplierAdapter
import com.hiskytechs.muhallinewuserapp.Data.AppData
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.Ui.CheckoutAddressActivity
import com.hiskytechs.muhallinewuserapp.Ui.CheckoutReviewActivity
import com.hiskytechs.muhallinewuserapp.Ui.SupplierDetailsActivity
import com.hiskytechs.muhallinewuserapp.Utill.AddressManager
import com.hiskytechs.muhallinewuserapp.Utill.CartManager
import com.hiskytechs.muhallinewuserapp.Utill.SupplierCart
import com.hiskytechs.muhallinewuserapp.databinding.FragmentCartBinding
import com.hiskytechs.muhallinewuserapp.network.CurrencyFormatter
import java.util.Locale
import kotlin.math.roundToInt

class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!
    private lateinit var cartAdapter: CartAdapter
    private lateinit var supplierAdapter: CartSupplierAdapter
    private var showHeader: Boolean = true
    private var selectedSupplierName: String? = null
    private var initialSupplierName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        showHeader = arguments?.getBoolean(ARG_SHOW_HEADER) ?: true
        initialSupplierName = arguments?.getString(ARG_INITIAL_SUPPLIER)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSupplierRecyclerView()
        setupCartRecyclerView()
        refreshCartUi(initialSupplierName)

        binding.btnCheckout.setOnClickListener {
            val activeSupplierName = selectedSupplierName
            val selectedCart = activeSupplierName?.let { CartManager.getSupplierCart(it) }

            when {
                selectedCart == null -> {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.cart_empty_message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                !selectedCart.isMinimumMet -> {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.cart_checkout_requirements_message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> {
                    val checkoutClass = if (AddressManager.hasAddress()) {
                        CheckoutReviewActivity::class.java
                    } else {
                        CheckoutAddressActivity::class.java
                    }
                    startActivity(
                        Intent(requireContext(), checkoutClass).apply {
                            putExtra(CheckoutAddressActivity.EXTRA_SUPPLIER_NAME, activeSupplierName)
                        }
                    )
                }
            }
        }

        if (!showHeader) {
            binding.tvTitle.visibility = View.GONE
            binding.tvItemCountBadge.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null && this::cartAdapter.isInitialized && this::supplierAdapter.isInitialized) {
            refreshCartUi(selectedSupplierName)
        }
    }

    private fun setupSupplierRecyclerView() {
        supplierAdapter = CartSupplierAdapter(
            supplierCarts = emptyList(),
            selectedSupplierName = null,
            onSupplierSelected = { supplierName ->
                refreshCartUi(supplierName)
            },
            onSupplierDoubleTapped = { supplierName ->
                openSupplierProducts(supplierName)
            }
        )
        binding.rvSuppliers.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.rvSuppliers.adapter = supplierAdapter
    }

    private fun setupCartRecyclerView() {
        cartAdapter = CartAdapter(
            emptyList(),
            onQuantityChanged = {
                refreshCartUi(selectedSupplierName)
            },
            onDeleteItem = { item ->
                CartManager.removeItem(item)
                refreshCartUi(selectedSupplierName)
            }
        )
        binding.rvCartItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCartItems.adapter = cartAdapter
    }

    private fun openSupplierProducts(supplierName: String) {
        val supplier = AppData.findSupplierByName(supplierName)
        startActivity(
            Intent(requireContext(), SupplierDetailsActivity::class.java).apply {
                putExtra("supplier_id", supplier?.id ?: 0)
                putExtra("supplier_name", supplier?.name ?: supplierName)
                putExtra("supplier_owner_name", supplier?.ownerName.orEmpty())
                putExtra("location", supplier?.location.orEmpty())
            }
        )
    }

    private fun refreshCartUi(preferredSupplierName: String? = selectedSupplierName) {
        val supplierCarts = CartManager.getSupplierCarts()
        selectedSupplierName = supplierCarts
            .firstOrNull { it.supplierName.equals(preferredSupplierName, ignoreCase = true) }
            ?.supplierName
            ?: supplierCarts.firstOrNull()?.supplierName
        initialSupplierName = null

        val selectedCart = selectedSupplierName?.let { CartManager.getSupplierCart(it) }
        supplierAdapter.updateItems(supplierCarts, selectedSupplierName)
        cartAdapter.updateItems(selectedCart?.items.orEmpty())
        updateSummary(selectedCart, supplierCarts.isNotEmpty())
    }

    private fun updateSummary(selectedCart: SupplierCart?, hasSuppliers: Boolean) {
        binding.rvSuppliers.visibility = if (hasSuppliers) View.VISIBLE else View.GONE

        if (selectedCart == null) {
            binding.tvItemCountBadge.text = getString(R.string.items_count_format, 0)
            binding.tvSubtotal.text = formatCurrency(0.0)
            binding.tvShipping.text = formatCurrency(0.0)
            binding.tvTotal.text = formatCurrency(0.0)
            binding.minimumProgressGroup.visibility = View.GONE
            binding.pbMinimumAmount.progress = 0
            binding.btnCheckout.isEnabled = false
            binding.btnCheckout.alpha = 0.6f
            return
        }

        updateMinimumProgress(selectedCart)
        binding.tvItemCountBadge.text = getString(
            R.string.items_count_format,
            selectedCart.totalQuantity
        )
        binding.tvSubtotal.text = formatCurrency(selectedCart.subtotal)
        binding.tvShipping.text = formatCurrency(selectedCart.shipping)
        binding.tvTotal.text = formatCurrency(selectedCart.total)
        binding.btnCheckout.isEnabled = selectedCart.isMinimumMet
        binding.btnCheckout.alpha = if (selectedCart.isMinimumMet) 1f else 0.6f
    }

    private fun updateMinimumProgress(selectedCart: SupplierCart) {
        if (selectedCart.minimumAmount <= 0.0) {
            binding.minimumProgressGroup.visibility = View.GONE
            binding.pbMinimumAmount.progress = 100
            return
        }

        binding.minimumProgressGroup.visibility = View.VISIBLE
        val rawProgress = ((selectedCart.subtotal / selectedCart.minimumAmount) * 100)
        val progress = if (rawProgress.isFinite()) {
            rawProgress.coerceIn(0.0, 100.0).roundToInt()
        } else {
            0
        }
        binding.pbMinimumAmount.progress = progress
        binding.tvMinimumProgress.text = getString(
            R.string.cart_minimum_progress_format,
            formatCurrency(selectedCart.subtotal),
            formatCurrency(selectedCart.minimumAmount)
        )
    }

    private fun formatCurrency(amount: Double): String {
        return CurrencyFormatter.format(amount)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_SHOW_HEADER = "show_header"
        private const val ARG_INITIAL_SUPPLIER = "initial_supplier"

        fun newInstance(showHeader: Boolean, initialSupplierName: String? = null): CartFragment {
            return CartFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_SHOW_HEADER, showHeader)
                    putString(ARG_INITIAL_SUPPLIER, initialSupplierName)
                }
            }
        }
    }
}
