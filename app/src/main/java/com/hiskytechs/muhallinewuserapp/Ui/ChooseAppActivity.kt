package com.hiskytechs.muhallinewuserapp.Ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.hiskytechs.muhallinewuserapp.databinding.ActivityChooseAppBinding
import com.hiskytechs.muhallinewuserapp.network.AppSession
import com.hiskytechs.muhallinewuserapp.supplier.Ui.SupplierMainActivity
import com.hiskytechs.muhallinewuserapp.supplier.Ui.ActivitySupplierOnboarding

class ChooseAppActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChooseAppBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChooseAppBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cardBuyer.setOnClickListener { openBuyerApp() }
        binding.btnOpenBuyer.setOnClickListener { openBuyerApp() }
        binding.cardSupplier.setOnClickListener { openSupplierApp() }
        binding.btnOpenSupplier.setOnClickListener { openSupplierApp() }
    }

    private fun openBuyerApp() {
        val destination = when {
            AppSession.hasBuyerSession() -> Intent(this, com.hiskytechs.muhallinewuserapp.MainActivity::class.java)
            else -> Intent(this, ActivityOnboarding::class.java)
        }
        startActivity(destination)
    }

    private fun openSupplierApp() {
        val destination = when {
            AppSession.hasSupplierSession() -> Intent(this, SupplierMainActivity::class.java)
            else -> Intent(this, ActivitySupplierOnboarding::class.java)
        }
        startActivity(destination)
    }
}
