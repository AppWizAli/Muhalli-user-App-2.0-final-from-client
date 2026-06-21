package com.hiskytechs.muhallinewuserapp.supplier.Ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.firebase.messaging.FirebaseMessaging
import com.hiskytechs.muhallinewuserapp.Data.AppData
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.databinding.ActivitySupplierMainBinding
import com.hiskytechs.muhallinewuserapp.notifications.AppNotificationHelper
import com.hiskytechs.muhallinewuserapp.supplier.Fragments.SupplierEarningsFragment
import com.hiskytechs.muhallinewuserapp.supplier.Fragments.SupplierHomeFragment
import com.hiskytechs.muhallinewuserapp.supplier.Fragments.SupplierOrdersFragment
import com.hiskytechs.muhallinewuserapp.supplier.Fragments.SupplierProductsFragment
import com.hiskytechs.muhallinewuserapp.supplier.Fragments.SupplierProfileFragment

class SupplierMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySupplierMainBinding
    private val tabFragments = linkedMapOf<Int, Fragment>()
    private var activeTabId: Int = 0
    private var isChangingSelectedTab = false
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            syncFirebaseNotifications()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySupplierMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsets()
        AppNotificationHelper.ensureChannel(this)
        AppData.loadPublicSettings(onSuccess = {}, onError = {})

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (!isChangingSelectedTab) {
                showTab(item.itemId)
            }
            true
        }

        if (savedInstanceState == null) {
            val destination = intent.getIntExtra(EXTRA_TAB_ID, R.id.nav_supplier_home)
            openTab(destination)
        }
        maybeRequestNotificationPermission()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val destination = intent.getIntExtra(EXTRA_TAB_ID, R.id.nav_supplier_home)
        openTab(destination)
    }

    fun openTab(itemId: Int) {
        if (binding.bottomNavigation.selectedItemId != itemId) {
            isChangingSelectedTab = true
            binding.bottomNavigation.selectedItemId = itemId
            isChangingSelectedTab = false
        }
        showTab(itemId)
    }

    private fun showTab(itemId: Int) {
        if (activeTabId == itemId && tabFragments[itemId]?.isVisible == true) return

        val transaction = supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)

        tabFragments[activeTabId]?.let { transaction.hide(it) }

        val fragment = tabFragments.getOrPut(itemId) {
            when (itemId) {
                R.id.nav_supplier_products -> SupplierProductsFragment()
                R.id.nav_supplier_orders -> SupplierOrdersFragment()
                R.id.nav_supplier_earnings -> SupplierEarningsFragment()
                R.id.nav_supplier_profile -> SupplierProfileFragment()
                else -> SupplierHomeFragment()
            }
        }

        if (fragment.isAdded) {
            transaction.show(fragment)
        } else {
            transaction.add(R.id.fragmentContainer, fragment, itemId.toString())
        }

        activeTabId = itemId
        transaction.commit()
    }

    private fun applyWindowInsets() {
        val initialBottomPadding = binding.bottomNavigation.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.bottomNavigation.setPadding(
                binding.bottomNavigation.paddingLeft,
                binding.bottomNavigation.paddingTop,
                binding.bottomNavigation.paddingRight,
                initialBottomPadding + systemBars.bottom
            )
            insets
        }
    }

    companion object {
        const val EXTRA_TAB_ID = "extra_tab_id"

        fun open(context: Context, tabId: Int) {
            context.startActivity(
                Intent(context, SupplierMainActivity::class.java).putExtra(EXTRA_TAB_ID, tabId)
            )
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            syncFirebaseNotifications()
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            syncFirebaseNotifications()
            return
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun syncFirebaseNotifications() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            AppData.registerNotificationToken(token)
        }
    }
}
