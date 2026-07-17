package com.hiskytechs.muhallinewuserapp.supplier.Ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.hiskytechs.muhallinewuserapp.databinding.ActivitySupplierNotificationsBinding
import com.hiskytechs.muhallinewuserapp.supplier.Adapters.SupplierNotificationAdapter
import com.hiskytechs.muhallinewuserapp.supplier.Data.SupplierData

class SupplierNotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySupplierNotificationsBinding
    private lateinit var adapter: SupplierNotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySupplierNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivBack.setOnClickListener { finish() }
        adapter = SupplierNotificationAdapter(emptyList())
        binding.rvNotifications.layoutManager = LinearLayoutManager(this)
        binding.rvNotifications.adapter = adapter
        loadNotifications()
    }

    private fun loadNotifications() {
        SupplierData.loadNotifications(
            onSuccess = { items ->
                if (isFinishing || isDestroyed) return@loadNotifications
                adapter.updateItems(items)
                binding.rvNotifications.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
                binding.tvEmptyNotifications.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            },
            onError = { message ->
                if (isFinishing || isDestroyed) return@loadNotifications
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        )
    }
}
