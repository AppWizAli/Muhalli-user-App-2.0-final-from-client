package com.hiskytechs.muhallinewuserapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.firebase.messaging.FirebaseMessaging
import com.hiskytechs.muhallinewuserapp.Fragments.CategoriesFragment
import com.hiskytechs.muhallinewuserapp.Fragments.ChatsFragment
import com.hiskytechs.muhallinewuserapp.Fragments.HomeFragment
import com.hiskytechs.muhallinewuserapp.Fragments.OrdersFragment
import com.hiskytechs.muhallinewuserapp.Fragments.ProfileFragment
import com.hiskytechs.muhallinewuserapp.Data.AppData
import com.hiskytechs.muhallinewuserapp.Ui.CartActivity
import com.hiskytechs.muhallinewuserapp.Ui.NotificationsActivity
import com.hiskytechs.muhallinewuserapp.Ui.launchSupportWhatsapp
import com.hiskytechs.muhallinewuserapp.Utill.CartManager
import com.hiskytechs.muhallinewuserapp.databinding.ActivityMainBinding
import com.hiskytechs.muhallinewuserapp.notifications.AppNotificationHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val notificationPollHandler = Handler(Looper.getMainLooper())
    private val notificationPollRunnable = object : Runnable {
        override fun run() {
            AppData.loadNotifications(onSuccess = {}, onError = {})
            notificationPollHandler.postDelayed(this, NOTIFICATION_POLL_INTERVAL_MS)
        }
    }
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            syncFirebaseNotifications()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsets()
        AppNotificationHelper.ensureChannel(this)
        AppData.loadPublicSettings(onSuccess = {}, onError = {})

        // Load HomeFragment by default
        if (savedInstanceState == null) {
            handleNavigation(intent)
        }

        binding.layoutCartEntry.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }
        binding.ivNotificationsEntry.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        binding.ivSupportEntry.setOnClickListener {
            openSupportWhatsApp()
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_categories -> {
                     loadFragment(CategoriesFragment())
                    true
                }
                R.id.nav_chats -> {
                     loadFragment(ChatsFragment())
                    true
                }
                R.id.nav_orders -> {
                     loadFragment(OrdersFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
        maybeRequestNotificationPermission()
    }

    override fun onResume() {
        super.onResume()
        updateCartBadge()
        syncFirebaseNotifications()
        AppData.loadNotifications(onSuccess = {}, onError = {})
        notificationPollHandler.removeCallbacks(notificationPollRunnable)
        notificationPollHandler.postDelayed(notificationPollRunnable, NOTIFICATION_POLL_INTERVAL_MS)
    }

    override fun onPause() {
        notificationPollHandler.removeCallbacks(notificationPollRunnable)
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNavigation(intent)
    }

    private fun handleNavigation(intent: Intent?) {
        val navigateTo = intent?.getStringExtra("navigate_to")
        when (navigateTo) {
            "home" -> navigateToTab(R.id.nav_home)
            "cart" -> {
                startActivity(Intent(this, CartActivity::class.java))
                navigateToTab(R.id.nav_home)
            }
            "chats" -> navigateToTab(R.id.nav_chats)
            "orders" -> navigateToTab(R.id.nav_orders)
            "profile" -> navigateToTab(R.id.nav_profile)
            "categories" -> navigateToTab(R.id.nav_categories)
            else -> navigateToTab(R.id.nav_home)
        }
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

    fun navigateToTab(itemId: Int) {
        binding.bottomNavigation.selectedItemId = itemId
        val fragment = when (itemId) {
            R.id.nav_categories -> CategoriesFragment()
            R.id.nav_chats -> ChatsFragment()
            R.id.nav_orders -> OrdersFragment()
            R.id.nav_profile -> ProfileFragment()
            else -> HomeFragment()
        }
        loadFragment(fragment)
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun updateCartBadge() {
        val count = CartManager.getTotalQuantity()
        binding.tvCartBadge.visibility = if (count > 0) View.VISIBLE else View.GONE
        binding.tvCartBadge.text = count.toString()
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

    private fun openSupportWhatsApp() {
        AppData.loadPublicSettings(
            onSuccess = { settings ->
                launchSupportWhatsapp(
                    context = this,
                    phoneNumber = settings.supportWhatsapp,
                    prefilledMessage = settings.supportWhatsappMessage
                )
            },
            onError = { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    companion object {
        private const val NOTIFICATION_POLL_INTERVAL_MS = 30000L
    }
}
