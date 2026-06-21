package com.hiskytechs.muhallinewuserapp.Ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hiskytechs.muhallinewuserapp.Data.AppData
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.Utill.AddressManager
import com.hiskytechs.muhallinewuserapp.Utill.CartManager
import com.hiskytechs.muhallinewuserapp.databinding.ActivityCheckoutReviewBinding
import com.hiskytechs.muhallinewuserapp.network.CurrencyFormatter
import java.util.Locale

class CheckoutReviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCheckoutReviewBinding
    private lateinit var loadingDialog: AppLoadingDialog
    private var supplierName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loadingDialog = AppLoadingDialog(this)

        val address = AddressManager.getAddress()
        if (address == null) {
            startActivity(
                Intent(this, CheckoutAddressActivity::class.java).apply {
                    putExtra(
                        CheckoutAddressActivity.EXTRA_SUPPLIER_NAME,
                        intent.getStringExtra(CheckoutAddressActivity.EXTRA_SUPPLIER_NAME)
                    )
                }
            )
            finish()
            return
        }

        supplierName = intent.getStringExtra(CheckoutAddressActivity.EXTRA_SUPPLIER_NAME)
            ?.takeIf { CartManager.getSupplierCart(it) != null }
            ?: CartManager.getSupplierCarts().firstOrNull()?.supplierName.orEmpty()
        val supplierCart = CartManager.getSupplierCart(supplierName)
        if (supplierCart == null) {
            finish()
            return
        }

        binding.ivBack.setOnClickListener { finish() }
        binding.tvEditAddress.setOnClickListener {
            startActivity(
                Intent(this, CheckoutAddressActivity::class.java).apply {
                    putExtra(CheckoutAddressActivity.EXTRA_SUPPLIER_NAME, supplierName)
                }
            )
        }
        binding.btnPlaceOrder.setOnClickListener {
            binding.btnPlaceOrder.isEnabled = false
            loadingDialog.show(R.string.loading_placing_order)
            AppData.createOrder(
                items = supplierCart.items,
                totalAmount = supplierCart.total,
                onSuccess = { createdOrder ->
                    loadingDialog.dismiss()
                    CartManager.clearSupplierCart(supplierName)
                    val intent = Intent(this, OrderSuccessActivity::class.java).apply {
                        putExtra(OrderSuccessActivity.EXTRA_ORDER_ID, createdOrder.orderId)
                        putExtra(OrderSuccessActivity.EXTRA_ORDER_INTERNAL_ID, createdOrder.internalId)
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                    finish()
                },
                onError = { message ->
                    binding.btnPlaceOrder.isEnabled = true
                    loadingDialog.dismiss()
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            )
        }

        binding.tvAddressName.text = address.fullName
        binding.tvAddressPhone.text = address.phoneNumber
        binding.tvAddressLine.text = address.formattedAddress
        binding.tvAddressNote.text = address.note.ifBlank { getString(R.string.no_delivery_note_added) }
        binding.tvSupplierValue.text = supplierCart.supplierName

        binding.tvItemsValue.text = getString(R.string.items_count_format, supplierCart.totalQuantity)
        binding.tvSubtotalValue.text = CurrencyFormatter.format(supplierCart.subtotal)
        binding.tvShippingValue.text = CurrencyFormatter.format(supplierCart.shipping)
        binding.tvTotalValue.text = CurrencyFormatter.format(supplierCart.total)
    }

    override fun onDestroy() {
        loadingDialog.dismiss()
        super.onDestroy()
    }
}
