package com.hiskytechs.muhallinewuserapp.Ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.hiskytechs.muhallinewuserapp.Adapters.SupplierAdapter
import com.hiskytechs.muhallinewuserapp.Data.AppData
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.databinding.ActivitySuppliersBinding

class SuppliersActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySuppliersBinding
    private lateinit var selectedCategory: String
    private var currentSort: String = "default"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuppliersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        selectedCategory = intent.getStringExtra(EXTRA_CATEGORY_NAME).orEmpty()
        binding.tvSelectedCategory.text = if (selectedCategory.isBlank()) {
            getString(R.string.showing_all_verified_suppliers)
        } else {
            selectedCategory
        }

        binding.rvSuppliers.layoutManager = LinearLayoutManager(this)

        binding.ivBack.setOnClickListener { finish() }
        binding.layoutFilter.setOnClickListener {
            currentSort = "low_min_order"
            loadSuppliers()
        }
        binding.layoutSort.setOnClickListener {
            currentSort = "cheapest"
            loadSuppliers()
        }

        loadSuppliers()
    }

    override fun onResume() {
        super.onResume()
        loadSuppliers()
    }

    private fun loadSuppliers() {
        AppData.loadSuppliers(
            categoryName = selectedCategory,
            cityFilter = "",
            sort = currentSort,
            forceRefresh = true,
            onSuccess = { suppliers ->
                binding.tvResultsCount.text = getString(R.string.suppliers_found_count, suppliers.size)
                binding.rvSuppliers.adapter = SupplierAdapter(suppliers, selectedCategory)
            },
            onError = { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    companion object {
        const val EXTRA_CATEGORY_NAME = "category_name"
    }
}
