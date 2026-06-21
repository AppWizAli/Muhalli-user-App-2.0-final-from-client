package com.hiskytechs.muhallinewuserapp.Ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.hiskytechs.muhallinewuserapp.MainActivity
import com.hiskytechs.muhallinewuserapp.databinding.ActivityOrderSuccessBinding

class OrderSuccessActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderSuccessBinding
    private var orderId: String = ""
    private var internalOrderId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderSuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        orderId = intent.getStringExtra(EXTRA_ORDER_ID).orEmpty()
        internalOrderId = intent.getIntExtra(EXTRA_ORDER_INTERNAL_ID, 0)
        if (orderId.isNotBlank()) {
            binding.tvOrderId.text = orderId
        }

        binding.ivBack.setOnClickListener { goHome() }
        binding.btnViewInvoice.setOnClickListener { openInvoice() }
        binding.btnGoHome.setOnClickListener { goHome() }
    }

    private fun openInvoice() {
        if (orderId.isBlank()) {
            goHome()
            return
        }
        startActivity(Intent(this, OrderDetailsActivity::class.java).apply {
            putExtra(OrderDetailsActivity.EXTRA_ORDER_ID, orderId)
            putExtra(OrderDetailsActivity.EXTRA_ORDER_INTERNAL_ID, internalOrderId)
        })
        finish()
    }

    private fun goHome() {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("navigate_to", "home")
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finish()
    }

    companion object {
        const val EXTRA_ORDER_ID = "order_id"
        const val EXTRA_ORDER_INTERNAL_ID = "order_internal_id"
    }
}
