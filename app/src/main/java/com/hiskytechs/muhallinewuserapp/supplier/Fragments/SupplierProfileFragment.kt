package com.hiskytechs.muhallinewuserapp.supplier.Fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.databinding.FragmentSupplierProfileBinding
import com.hiskytechs.muhallinewuserapp.supplier.Adapters.SupplierProfileOptionAdapter
import com.hiskytechs.muhallinewuserapp.supplier.Data.SupplierData
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierProfileAction
import com.hiskytechs.muhallinewuserapp.supplier.Ui.SupplierEditProfileActivity
import com.hiskytechs.muhallinewuserapp.supplier.Ui.SupplierLoginActivity
import com.hiskytechs.muhallinewuserapp.supplier.Ui.SupplierMainActivity
import com.hiskytechs.muhallinewuserapp.supplier.Utill.initials
import com.hiskytechs.muhallinewuserapp.supplier.Utill.formatPkr
import com.hiskytechs.muhallinewuserapp.Utill.LogoutManager
import com.hiskytechs.muhallinewuserapp.Utill.ThemeManager

class SupplierProfileFragment : Fragment() {

    private var _binding: FragmentSupplierProfileBinding? = null
    private val binding get() = _binding!!
    private var didInitialRefresh = false
    private var skippedInitialResume = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSupplierProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvProfileOptions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProfileOptions.setHasFixedSize(true)
        binding.rvProfileOptions.adapter = SupplierProfileOptionAdapter(SupplierData.getProfileOptions()) { option ->
            when (option.action) {
                SupplierProfileAction.PRODUCTS -> (activity as? SupplierMainActivity)?.openTab(R.id.nav_supplier_products)
                SupplierProfileAction.ORDERS -> (activity as? SupplierMainActivity)?.openTab(R.id.nav_supplier_orders)
                SupplierProfileAction.EARNINGS -> (activity as? SupplierMainActivity)?.openTab(R.id.nav_supplier_earnings)
                SupplierProfileAction.BUSINESS_ADDRESS -> startActivity(Intent(requireContext(), SupplierEditProfileActivity::class.java))
                SupplierProfileAction.LOGOUT -> {
                    LogoutManager.clearAll(requireContext())
                    startActivity(
                        Intent(requireContext(), SupplierLoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                    activity?.finishAffinity()
                }
                else -> Toast.makeText(requireContext(), getString(R.string.supplier_coming_soon), Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), SupplierEditProfileActivity::class.java))
        }

        binding.swThemeMode.isChecked = ThemeManager.isDarkModeEnabled(requireContext())
        binding.swThemeMode.setOnCheckedChangeListener { _, isChecked ->
            ThemeManager.setDarkModeEnabled(requireContext(), isChecked)
        }

        if (SupplierData.restoreCachedProfile()) {
            didInitialRefresh = true
            bindProfile()
        }
        refreshProfile(showBlockingLoader = !didInitialRefresh)
    }

    override fun onResume() {
        super.onResume()
        if (!skippedInitialResume) {
            skippedInitialResume = true
            return
        }
        if (_binding != null && didInitialRefresh) {
            refreshProfile(showBlockingLoader = false)
        }
    }

    private fun refreshProfile(showBlockingLoader: Boolean = true) {
        SupplierData.refreshProfile(
            onSuccess = {
                if (_binding == null) return@refreshProfile
                didInitialRefresh = true
                bindProfile()
            },
            onError = { message ->
                if (_binding == null) return@refreshProfile
                if (!didInitialRefresh && !SupplierData.hasCachedProfile()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun bindProfile() {
        val profile = SupplierData.getProfile()
        binding.tvAvatarInitial.text = initials(profile.businessName)
        binding.tvProfileBusinessName.text = profile.businessName.ifBlank { getString(R.string.supplier_fallback) }
        binding.tvProfileOwnerName.text = profile.ownerName.ifBlank { getString(R.string.not_available) }
        binding.tvProfilePhone.text = profile.phoneNumber.ifBlank { getString(R.string.not_available) }
        binding.tvProfileCity.text = profile.city.ifBlank { getString(R.string.not_available) }
        binding.tvProfileMinAmount.text = formatPkr(profile.minimumOrderAmountPkr)
        binding.tvProfileMinQuantity.text = getString(R.string.supplier_min_order_qty) + " " + profile.minimumOrderQuantity
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
