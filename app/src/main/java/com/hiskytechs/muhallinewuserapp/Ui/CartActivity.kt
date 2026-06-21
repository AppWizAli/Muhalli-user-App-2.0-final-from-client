package com.hiskytechs.muhallinewuserapp.Ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.hiskytechs.muhallinewuserapp.Fragments.CartFragment
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.databinding.ActivityCartBinding

class CartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.cart_fragment_container,
                    CartFragment.newInstance(
                        showHeader = false,
                        initialSupplierName = intent.getStringExtra(EXTRA_SUPPLIER_NAME)
                    )
                )
                .commit()
        }
    }

    companion object {
        const val EXTRA_SUPPLIER_NAME = "supplier_name"
    }
}
