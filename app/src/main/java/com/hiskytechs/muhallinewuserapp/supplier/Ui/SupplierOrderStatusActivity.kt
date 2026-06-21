package com.hiskytechs.muhallinewuserapp.supplier.Ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.Ui.AppLoadingDialog
import com.hiskytechs.muhallinewuserapp.databinding.ActivitySupplierOrderStatusBinding
import com.hiskytechs.muhallinewuserapp.supplier.Data.SupplierData
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierOrder
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierOrderStatus
import com.hiskytechs.muhallinewuserapp.supplier.Utill.formatPkr
import com.hiskytechs.muhallinewuserapp.supplier.Utill.orderStatusBackground
import com.hiskytechs.muhallinewuserapp.supplier.Utill.orderStatusTextColor

class SupplierOrderStatusActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySupplierOrderStatusBinding
    private lateinit var loadingDialog: AppLoadingDialog
    private var order: SupplierOrder? = null
    private lateinit var statusOptions: List<StatusOptionView>
    private var selectedStatus: SupplierOrderStatus = SupplierOrderStatus.PENDING
    private var requestedOrderId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySupplierOrderStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loadingDialog = AppLoadingDialog(this)

        requestedOrderId = intent.getStringExtra(EXTRA_ORDER_ID).orEmpty()
        statusOptions = listOf(
            StatusOptionView(
                SupplierOrderStatus.PENDING,
                binding.layoutStatusPending,
                binding.tvStatusPendingTitle,
                binding.tvStatusPendingSubtitle
            ),
            StatusOptionView(
                SupplierOrderStatus.CONFIRMED,
                binding.layoutStatusConfirmed,
                binding.tvStatusConfirmedTitle,
                binding.tvStatusConfirmedSubtitle
            ),
            StatusOptionView(
                SupplierOrderStatus.SHIPPED,
                binding.layoutStatusShipped,
                binding.tvStatusShippedTitle,
                binding.tvStatusShippedSubtitle
            ),
            StatusOptionView(
                SupplierOrderStatus.DELIVERED,
                binding.layoutStatusDelivered,
                binding.tvStatusDeliveredTitle,
                binding.tvStatusDeliveredSubtitle
            )
        )

        binding.ivBack.setOnClickListener { finish() }
        binding.btnSaveStatus.setOnClickListener { saveStatus() }
        binding.btnChatBuyer.setOnClickListener { openBuyerChat() }
        binding.layoutBuyerPhoneRow.setOnClickListener { dialBuyerPhone() }
        binding.tvCallBuyer.setOnClickListener { dialBuyerPhone() }
        binding.layoutBuyerAddressRow.setOnClickListener { openBuyerAddressInMaps() }
        binding.tvOpenMap.setOnClickListener { openBuyerAddressInMaps() }

        statusOptions.forEach { option ->
            option.container.setOnClickListener {
                selectedStatus = option.status
                renderStatusSelection()
            }
        }

        loadOrder()
    }

    private fun bindOrder() {
        val order = order ?: return
        binding.tvOrderId.text = "#${order.id}"
        binding.tvRetailerName.text = order.retailerName.ifBlank { order.buyerName }
        val hasAddress = order.deliveryAddress.isNotBlank()
        binding.tvDeliveryAddress.text = order.deliveryAddress.ifBlank { getString(R.string.not_available) }
        binding.tvDeliveryAddress.setTextColor(
            ContextCompat.getColor(
                this,
                if (hasAddress) R.color.primary else R.color.supplier_text_secondary
            )
        )
        binding.tvDeliveryAddress.setOnClickListener(if (hasAddress) View.OnClickListener { openBuyerAddressInMaps() } else null)
        binding.tvBuyerPhoneValue.text = order.buyerPhone.ifBlank { getString(R.string.not_available) }
        binding.tvBuyerAddressValue.text = order.deliveryAddress.ifBlank { getString(R.string.not_available) }
        binding.layoutBuyerPhoneRow.visibility = if (order.buyerPhone.isBlank()) View.GONE else View.VISIBLE
        binding.layoutBuyerAddressRow.visibility = if (hasAddress) View.VISIBLE else View.GONE
        binding.tvOrderNotes.visibility = if (order.notes.isBlank()) View.GONE else View.VISIBLE
        binding.tvOrderNotes.text = order.notes
        binding.tvOrderDateValue.text = order.orderDateTime.ifBlank { order.orderDate }
        binding.tvExpectedDeliveryValue.text = order.expectedDeliveryDate
            .takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }
            ?: getString(R.string.not_available)
        binding.tvItemsCountValue.text = getString(R.string.supplier_items_count_format, order.itemsCount)
        binding.tvOrderAmountValue.text = formatPkr(order.amountPkr)
        binding.tvCurrentStatusValue.text = order.status.label
        binding.tvCurrentStatusValue.setBackgroundResource(orderStatusBackground(order.status))
        binding.tvCurrentStatusValue.setTextColor(
            ContextCompat.getColor(this, orderStatusTextColor(order.status))
        )
        bindOrderItems(order)
    }

    private fun bindOrderItems(order: SupplierOrder) {
        binding.cardOrderItems.visibility = View.VISIBLE
        binding.layoutOrderItems.removeAllViews()
        if (order.items.isEmpty()) {
            val emptyMessage = TextView(this).apply {
                text = getString(R.string.order_items_loading_or_missing)
                setTextColor(ContextCompat.getColor(this@SupplierOrderStatusActivity, R.color.supplier_text_secondary))
                textSize = 13f
            }
            binding.layoutOrderItems.addView(emptyMessage)
            return
        }
        order.items.forEach { item ->
            val row = LayoutInflater.from(this).inflate(R.layout.item_order_invoice, binding.layoutOrderItems, false)
            row.findViewById<TextView>(R.id.tvInvoiceProductName).text = item.productName
            row.findViewById<TextView>(R.id.tvInvoiceMeta).text = getString(
                R.string.invoice_item_meta_format,
                item.packaging.ifBlank { getString(R.string.not_available) },
                item.quantity,
                item.unitLabel.ifBlank { getString(R.string.not_available) },
                formatPkr(item.unitPricePkr)
            )
            row.findViewById<TextView>(R.id.tvInvoiceLineTotal).text = formatPkr(item.lineTotalPkr)
            row.findViewById<TextView>(R.id.tvInvoiceQuantity).text = "x${item.quantity}"
            binding.layoutOrderItems.addView(row)
        }
    }

    private fun openBuyerChat() {
        val currentOrder = order ?: return
        val cachedConversation = SupplierData.findConversationForBuyer(
            currentOrder.buyerName,
            currentOrder.retailerName
        )
        if (cachedConversation != null) {
            openConversation(cachedConversation.id)
            return
        }

        binding.btnChatBuyer.isEnabled = false
        loadingDialog.show(R.string.loading_message)
        SupplierData.refreshMessages(
            onSuccess = {
                binding.btnChatBuyer.isEnabled = true
                loadingDialog.dismiss()
                val conversation = SupplierData.findConversationForBuyer(
                    currentOrder.buyerName,
                    currentOrder.retailerName
                )
                if (conversation == null) {
                    Toast.makeText(this, R.string.supplier_chat_not_available, Toast.LENGTH_SHORT).show()
                } else {
                    openConversation(conversation.id)
                }
            },
            onError = { message ->
                binding.btnChatBuyer.isEnabled = true
                loadingDialog.dismiss()
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun openConversation(conversationId: String) {
        startActivity(
            Intent(this, SupplierChatConversationActivity::class.java)
                .putExtra(SupplierChatConversationActivity.EXTRA_CONVERSATION_ID, conversationId)
        )
    }

    private fun dialBuyerPhone() {
        val phone = order?.buyerPhone.orEmpty().trim()
        if (phone.isBlank()) return
        startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null)))
    }

    private fun openBuyerAddressInMaps() {
        val address = order?.deliveryAddress.orEmpty().trim()
        if (address.isBlank()) return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(address)}"))
        startActivity(intent)
    }

    private fun renderStatusSelection() {
        statusOptions.forEach { option ->
            val isSelected = option.status == selectedStatus
            option.container.setBackgroundResource(
                if (isSelected) R.drawable.bg_supplier_card_selected else R.drawable.bg_supplier_card
            )
            option.title.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (isSelected) R.color.primary else R.color.text_dark
                )
            )
            option.subtitle.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (isSelected) R.color.primary else R.color.supplier_text_secondary
                )
            )
        }
    }

    private fun saveStatus() {
        val currentOrder = order ?: return
        binding.btnSaveStatus.isEnabled = false
        loadingDialog.show(R.string.loading_updating_status)
        SupplierData.updateOrderStatus(
            orderId = currentOrder.id,
            status = selectedStatus,
            onSuccess = {
                loadingDialog.dismiss()
                Toast.makeText(this, getString(R.string.supplier_order_status_updated), Toast.LENGTH_SHORT).show()
                finish()
            },
            onError = { message ->
                binding.btnSaveStatus.isEnabled = true
                loadingDialog.dismiss()
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun loadOrder() {
        val cachedOrder = SupplierData.findOrder(requestedOrderId)
        if (cachedOrder != null) {
            order = cachedOrder
            selectedStatus = cachedOrder.status
            bindOrder()
            renderStatusSelection()
            if (cachedOrder.items.isNotEmpty() && cachedOrder.deliveryAddress.isNotBlank()) {
                return
            }
        }

        loadingDialog.show(R.string.loading_orders)
        SupplierData.refreshOrderDetail(
            orderId = requestedOrderId,
            onSuccess = { detailedOrder ->
                loadingDialog.dismiss()
                val loadedOrder = detailedOrder ?: SupplierData.findOrder(requestedOrderId)
                if (loadedOrder == null) {
                    finish()
                } else {
                    order = loadedOrder
                    selectedStatus = loadedOrder.status
                    bindOrder()
                    renderStatusSelection()
                }
            },
            onError = {
                SupplierData.refreshOrders(
                    onSuccess = {
                        loadingDialog.dismiss()
                        val loadedOrder = SupplierData.findOrder(requestedOrderId)
                        if (loadedOrder == null) {
                            finish()
                        } else {
                            order = loadedOrder
                            selectedStatus = loadedOrder.status
                            bindOrder()
                            renderStatusSelection()
                        }
                    },
                    onError = { message ->
                loadingDialog.dismiss()
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                finish()
                    }
                )
            }
        )
    }

    private data class StatusOptionView(
        val status: SupplierOrderStatus,
        val container: LinearLayout,
        val title: TextView,
        val subtitle: TextView
    )

    companion object {
        private const val EXTRA_ORDER_ID = "extra_order_id"

        fun open(context: Context, orderId: String) {
            context.startActivity(
                Intent(context, SupplierOrderStatusActivity::class.java)
                    .putExtra(EXTRA_ORDER_ID, orderId)
            )
        }
    }

    override fun onDestroy() {
        loadingDialog.dismiss()
        super.onDestroy()
    }
}
