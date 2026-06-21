package com.hiskytechs.muhallinewuserapp.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.hiskytechs.muhallinewuserapp.Data.AppData
import com.hiskytechs.muhallinewuserapp.Models.Supplier
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.databinding.FragmentSupplierInfoBinding

class SupplierInfoFragment : Fragment() {

    private var _binding: FragmentSupplierInfoBinding? = null
    private val binding get() = _binding!!
    private var supplierName: String = ""

    companion object {
        fun newInstance(supplierName: String): SupplierInfoFragment {
            return SupplierInfoFragment().apply {
                arguments = Bundle().apply {
                    putString("supplier_name", supplierName)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSupplierInfoBinding.inflate(inflater, container, false)
        supplierName = arguments?.getString("supplier_name").orEmpty()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadSupplierInfo()
    }

    private fun loadSupplierInfo() {
        AppData.loadSuppliers(
            cityFilter = "",
            onSuccess = {
                if (_binding == null) return@loadSuppliers
                val supplier = AppData.findSupplierByName(supplierName)
                if (supplier != null) {
                    bindSupplier(supplier)
                }
            },
            onError = { message ->
                if (_binding == null) return@loadSuppliers
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun bindSupplier(supplier: Supplier) {
        binding.tvAboutDescription.text = supplier.description.ifBlank {
            getString(R.string.wholesale_supplier)
        }
        binding.tvPhoneValue.text = supplier.phoneNumber.ifBlank {
            getString(R.string.not_available)
        }
        binding.tvLocationValue.text = listOf(supplier.location, supplier.address)
            .filter { it.isNotBlank() }
            .joinToString(", ")
            .ifBlank { getString(R.string.not_available) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
