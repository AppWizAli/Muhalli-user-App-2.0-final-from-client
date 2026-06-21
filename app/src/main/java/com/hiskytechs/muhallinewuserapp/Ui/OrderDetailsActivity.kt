package com.hiskytechs.muhallinewuserapp.Ui

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.hiskytechs.muhallinewuserapp.Data.AppData
import com.hiskytechs.muhallinewuserapp.MainActivity
import com.hiskytechs.muhallinewuserapp.Models.Order
import com.hiskytechs.muhallinewuserapp.Models.OrderItem
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.Utill.AddressManager
import com.hiskytechs.muhallinewuserapp.databinding.ActivityOrderDetailsBinding
import com.hiskytechs.muhallinewuserapp.network.CurrencyFormatter

class OrderDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderDetailsBinding
    private var requestedOrderId: String = ""
    private var requestedBackendOrderId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        requestedOrderId = intent.getStringExtra(EXTRA_ORDER_ID).orEmpty()
        requestedBackendOrderId = intent.getIntExtra(EXTRA_ORDER_INTERNAL_ID, 0)
        loadOrder()
    }

    private fun loadOrder() {
        val cachedOrder = AppData.findOrder(requestedOrderId)
        if (cachedOrder != null) {
            bindOrder(cachedOrder)
            if (cachedOrder.items.isNotEmpty()) {
                return
            }
        }

        AppData.loadOrderDetail(
            orderId = requestedOrderId,
            backendOrderId = requestedBackendOrderId.takeIf { it > 0 },
            onSuccess = { detailedOrder ->
                val loadedOrder = detailedOrder ?: AppData.findOrder(requestedOrderId)
                if (loadedOrder == null) {
                    loadOrdersFallback()
                } else {
                    bindOrder(loadedOrder)
                }
            },
            onError = { loadOrdersFallback() }
        )
    }

    private fun loadOrdersFallback() {
        AppData.loadOrders(
            onSuccess = {
                val loadedOrder = AppData.findOrder(requestedOrderId)
                if (loadedOrder == null) finish() else bindOrder(loadedOrder)
            },
            onError = { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                finish()
            }
        )
    }

    private fun bindOrder(order: Order) {
        val statusUi = buildStatusUi(order)
        bindSummary(order, statusUi)
        bindInvoice(order)
        bindTracking(order, statusUi)
        bindAddress()

        binding.btnContactSupplier.setOnClickListener {
            val supplier = AppData.findSupplierByName(order.supplier)
            val thread = AppData.findThreadBySupplierName(order.supplier)
            val targetIntent = if (supplier != null) {
                Intent(this, ChatConversationActivity::class.java).apply {
                    putExtra(ChatConversationActivity.EXTRA_THREAD_ID, thread?.threadId ?: 0)
                    putExtra(ChatConversationActivity.EXTRA_SUPPLIER_ID, supplier.id)
                    putExtra(ChatConversationActivity.EXTRA_SUPPLIER_NAME, supplier.name)
                    putExtra(ChatConversationActivity.EXTRA_SUPPLIER_LOCATION, supplier.location)
                }
            } else {
                Intent(this, ChatsActivity::class.java)
            }
            startActivity(targetIntent)
        }

        binding.btnViewOrders.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).apply {
                putExtra("navigate_to", "orders")
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
            finish()
        }
    }

    private fun bindSummary(order: Order, statusUi: OrderStatusUi) {
        val supplier = AppData.findSupplierByName(order.supplier)
        val amounts = calculateInvoiceAmounts(order)
        binding.tvSupplierName.text = order.supplier
        binding.tvSupplierLocation.text = supplier?.location ?: getString(R.string.wholesale_supplier)
        binding.tvOrderId.text = order.orderId
        binding.tvOrderDate.text = order.date
        binding.tvTotal.text = CurrencyFormatter.format(amounts.total)
        binding.tvItems.text = getString(R.string.items_count_format, order.itemsCount)
        binding.tvEstimatedDate.text = statusUi.estimatedDeliveryText
        binding.tvOrderStatusBadge.text = statusUi.badgeText
        binding.tvOrderStatusBadge.setBackgroundResource(statusUi.badgeBackgroundRes)
        binding.tvOrderStatusBadge.setTextColor(
            ContextCompat.getColor(this, statusUi.badgeTextColorRes)
        )
        binding.tvTrackingSummary.text = statusUi.trackingSummary
    }

    private fun bindInvoice(order: Order) {
        val amounts = calculateInvoiceAmounts(order)
        binding.layoutInvoiceItems.removeAllViews()
        if (order.items.isEmpty()) {
            binding.layoutInvoiceItems.addView(TextView(this).apply {
                text = getString(R.string.invoice_items_not_loaded)
                setTextColor(ContextCompat.getColor(this@OrderDetailsActivity, R.color.text_grey))
                textSize = 13f
            })
        } else {
            order.items.forEach { item ->
                binding.layoutInvoiceItems.addView(createInvoiceRow(item))
            }
        }

        binding.tvInvoiceSubtotal.text = getString(R.string.invoice_subtotal_format, CurrencyFormatter.format(amounts.subtotal))
        binding.tvInvoiceDelivery.text = getString(R.string.invoice_delivery_format, CurrencyFormatter.format(amounts.deliveryFee))
        binding.tvInvoiceTotal.text = getString(R.string.invoice_total_format, CurrencyFormatter.format(amounts.total))
    }

    private fun createInvoiceRow(item: OrderItem): View {
        val row = LayoutInflater.from(this).inflate(R.layout.item_order_invoice, binding.layoutInvoiceItems, false)
        val title = row.findViewById<TextView>(R.id.tvInvoiceProductName)
        val meta = row.findViewById<TextView>(R.id.tvInvoiceMeta)
        val amount = row.findViewById<TextView>(R.id.tvInvoiceLineTotal)
        val quantity = row.findViewById<TextView>(R.id.tvInvoiceQuantity)
        val packagingText = item.packaging.ifBlank { getString(R.string.not_available) }
        title.text = item.productName
        meta.text = getString(
            R.string.invoice_item_meta_format,
            packagingText,
            item.quantity,
            item.unitLabel.ifBlank { getString(R.string.not_available) },
            CurrencyFormatter.format(item.unitPrice)
        )
        amount.text = CurrencyFormatter.format(item.lineTotal)
        quantity.text = "x${item.quantity}"
        return row
    }

    private fun calculateInvoiceAmounts(order: Order): InvoiceAmounts {
        val subtotal = order.subtotal.takeIf { it > 0.0 } ?: order.items.sumOf { it.lineTotal }
        val deliveryFee = when {
            order.deliveryFee > 0.0 -> order.deliveryFee
            order.totalAmount > 0.0 -> (order.totalAmount - subtotal).coerceAtLeast(0.0)
            else -> 0.0
        }
        val total = when {
            subtotal > 0.0 || deliveryFee > 0.0 -> subtotal + deliveryFee
            order.totalAmount > 0.0 -> order.totalAmount
            else -> 0.0
        }
        return InvoiceAmounts(subtotal = subtotal, deliveryFee = deliveryFee, total = total)
    }

    private data class InvoiceAmounts(
        val subtotal: Double,
        val deliveryFee: Double,
        val total: Double
    )

    private fun bindAddress() {
        val address = AddressManager.getAddress()
        val orderAddress = AppData.findOrder(requestedOrderId)?.deliveryAddress.orEmpty()
        binding.tvDeliveryAddress.text = orderAddress.ifBlank { address?.formattedAddress.orEmpty() }
            .ifBlank { getString(R.string.not_available) }
    }

    private fun bindTracking(order: Order, statusUi: OrderStatusUi) {
        val isCancelled = order.status.equals("cancelled", ignoreCase = true)
        updateStep(
            stepViews = StepViews(
                binding.ivStepOne,
                binding.tvStepOneTitle,
                binding.tvStepOneSubtitle,
                binding.tvStepOneMeta,
                binding.viewLineOne
            ),
            stepNumber = 1,
            currentStep = statusUi.currentStep,
            title = getString(R.string.order_placed),
            subtitle = if (order.status.equals("pending", ignoreCase = true)) {
                getString(R.string.waiting_for_supplier_confirmation)
            } else {
                getString(R.string.your_order_has_been_confirmed)
            },
            meta = getString(R.string.order_meta_format, order.date, getString(R.string.order_time_placed)),
            currentColorRes = statusUi.currentColorRes,
            currentBackgroundRes = statusUi.currentBackgroundRes,
            currentIconRes = R.drawable.ic_check_circle_24
        )
        updateStep(
            stepViews = StepViews(
                binding.ivStepTwo,
                binding.tvStepTwoTitle,
                binding.tvStepTwoSubtitle,
                binding.tvStepTwoMeta,
                binding.viewLineTwo
            ),
            stepNumber = 2,
            currentStep = statusUi.currentStep,
            title = getString(R.string.order_confirmed),
            subtitle = getString(R.string.supplier_confirmed_your_order),
            meta = getString(
                R.string.order_meta_format,
                order.date,
                getString(R.string.order_time_confirmed)
            ),
            currentColorRes = statusUi.currentColorRes,
            currentBackgroundRes = statusUi.currentBackgroundRes,
            currentIconRes = R.drawable.ic_check_circle_24
        )
        updateStep(
            stepViews = StepViews(
                binding.ivStepThree,
                binding.tvStepThreeTitle,
                binding.tvStepThreeSubtitle,
                binding.tvStepThreeMeta,
                binding.viewLineThree
            ),
            stepNumber = 3,
            currentStep = statusUi.currentStep,
            title = if (isCancelled) {
                getString(R.string.order_cancelled)
            } else {
                getString(R.string.preparing_order)
            },
            subtitle = if (isCancelled) {
                getString(R.string.order_cancelled_before_dispatch)
            } else {
                getString(R.string.your_items_are_being_prepared)
            },
            meta = if (isCancelled) {
                getString(R.string.order_meta_format, order.date, getString(R.string.order_time_cancelled))
            } else {
                getString(R.string.order_meta_format, order.date, getString(R.string.order_time_preparing))
            },
            currentColorRes = statusUi.currentColorRes,
            currentBackgroundRes = statusUi.currentBackgroundRes,
            currentIconRes = if (isCancelled) {
                R.drawable.ic_delete_24
            } else {
                R.drawable.ic_sync_24
            }
        )
        updateStep(
            stepViews = StepViews(
                binding.ivStepFour,
                binding.tvStepFourTitle,
                binding.tvStepFourSubtitle,
                binding.tvStepFourMeta,
                binding.viewLineFour
            ),
            stepNumber = 4,
            currentStep = statusUi.currentStep,
            title = if (isCancelled) getString(R.string.dispatch_skipped) else getString(R.string.out_for_delivery),
            subtitle = if (isCancelled) {
                getString(R.string.delivery_did_not_start)
            } else {
                getString(R.string.order_is_on_the_way)
            },
            meta = if (isCancelled) {
                getString(R.string.cancelled_before_dispatch)
            } else {
                order.deliveryDate ?: getString(R.string.estimated_next_business_day)
            },
            currentColorRes = statusUi.currentColorRes,
            currentBackgroundRes = statusUi.currentBackgroundRes,
            currentIconRes = R.drawable.ic_local_shipping_24,
            pendingIconRes = R.drawable.ic_local_shipping_24
        )
        updateStep(
            stepViews = StepViews(
                binding.ivStepFive,
                binding.tvStepFiveTitle,
                binding.tvStepFiveSubtitle,
                binding.tvStepFiveMeta,
                null
            ),
            stepNumber = 5,
            currentStep = statusUi.currentStep,
            title = if (isCancelled) getString(R.string.tracking_closed) else getString(R.string.delivered),
            subtitle = if (isCancelled) {
                getString(R.string.order_not_delivered)
            } else if (statusUi.currentStep >= 5) {
                getString(R.string.order_delivered_successfully)
            } else {
                getString(R.string.waiting_for_final_delivery)
            },
            meta = if (isCancelled) {
                getString(R.string.no_further_delivery_updates)
            } else {
                order.deliveryDate ?: getString(R.string.estimated_after_dispatch)
            },
            currentColorRes = statusUi.currentColorRes,
            currentBackgroundRes = statusUi.currentBackgroundRes,
            currentIconRes = R.drawable.ic_check_circle_24,
            pendingIconRes = R.drawable.ic_location_on_24
        )
    }

    private fun updateStep(
        stepViews: StepViews,
        stepNumber: Int,
        currentStep: Int,
        title: String,
        subtitle: String,
        meta: String,
        currentColorRes: Int = R.color.status_transit_text,
        currentBackgroundRes: Int = R.drawable.bg_tracking_current,
        currentIconRes: Int = R.drawable.ic_sync_24,
        pendingIconRes: Int = R.drawable.ic_sync_24
    ) {
        val doneColor = ContextCompat.getColor(this, R.color.status_delivered_text)
        val currentColor = ContextCompat.getColor(this, currentColorRes)
        val pendingColor = ContextCompat.getColor(this, R.color.text_grey)
        val dividerColor = ContextCompat.getColor(this, R.color.divider)

        stepViews.title.text = title
        stepViews.subtitle.text = subtitle
        stepViews.meta.text = meta

        when {
            stepNumber < currentStep -> {
                stepViews.icon.setBackgroundResource(R.drawable.bg_tracking_done)
                stepViews.icon.setImageResource(R.drawable.ic_check_circle_24)
                stepViews.icon.imageTintList = ColorStateList.valueOf(doneColor)
                stepViews.title.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
                stepViews.subtitle.setTextColor(ContextCompat.getColor(this, R.color.text_grey))
                stepViews.meta.setTextColor(doneColor)
                stepViews.connector?.setBackgroundColor(doneColor)
            }
            stepNumber == currentStep -> {
                stepViews.icon.setBackgroundResource(currentBackgroundRes)
                stepViews.icon.setImageResource(currentIconRes)
                stepViews.icon.imageTintList = ColorStateList.valueOf(currentColor)
                stepViews.title.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
                stepViews.subtitle.setTextColor(ContextCompat.getColor(this, R.color.text_grey))
                stepViews.meta.setTextColor(currentColor)
                stepViews.connector?.setBackgroundColor(dividerColor)
            }
            else -> {
                stepViews.icon.setBackgroundResource(R.drawable.bg_tracking_pending)
                stepViews.icon.setImageResource(pendingIconRes)
                stepViews.icon.imageTintList = ColorStateList.valueOf(pendingColor)
                stepViews.title.setTextColor(pendingColor)
                stepViews.subtitle.setTextColor(pendingColor)
                stepViews.meta.setTextColor(pendingColor)
                stepViews.connector?.setBackgroundColor(dividerColor)
            }
        }
    }

    private fun buildStatusUi(order: Order): OrderStatusUi {
        return when (order.status.lowercase()) {
            "delivered" -> OrderStatusUi(
                badgeText = getString(R.string.delivered),
                badgeBackgroundRes = R.drawable.bg_status_delivered,
                badgeTextColorRes = R.color.status_delivered_text,
                estimatedDeliveryText = order.deliveryDate ?: getString(R.string.delivered),
                trackingSummary = getString(R.string.delivered_to_saved_address),
                currentStep = 5,
                currentColorRes = R.color.status_delivered_text,
                currentBackgroundRes = R.drawable.bg_tracking_done
            )
            "in transit", "shipped" -> OrderStatusUi(
                badgeText = getString(R.string.status_in_transit),
                badgeBackgroundRes = R.drawable.bg_status_transit,
                badgeTextColorRes = R.color.status_transit_text,
                estimatedDeliveryText = order.deliveryDate ?: getString(R.string.arriving_soon),
                trackingSummary = getString(R.string.supplier_dispatched_order),
                currentStep = 4,
                currentColorRes = R.color.status_transit_text,
                currentBackgroundRes = R.drawable.bg_tracking_current
            )
            "pending" -> OrderStatusUi(
                badgeText = getString(R.string.pending),
                badgeBackgroundRes = R.drawable.bg_status_processing,
                badgeTextColorRes = R.color.status_processing_text,
                estimatedDeliveryText = order.deliveryDate ?: getString(R.string.waiting_for_supplier_confirmation),
                trackingSummary = getString(R.string.waiting_for_supplier_confirmation),
                currentStep = 1,
                currentColorRes = R.color.status_processing_text,
                currentBackgroundRes = R.drawable.bg_tracking_processing
            )
            "cancelled" -> OrderStatusUi(
                badgeText = getString(R.string.status_cancelled),
                badgeBackgroundRes = R.drawable.bg_status_cancelled,
                badgeTextColorRes = R.color.status_cancelled_text,
                estimatedDeliveryText = getString(R.string.status_cancelled),
                trackingSummary = getString(R.string.order_cancelled_summary),
                currentStep = 3,
                currentColorRes = R.color.status_cancelled_text,
                currentBackgroundRes = R.drawable.bg_tracking_cancelled
            )
            else -> OrderStatusUi(
                badgeText = getString(R.string.processing),
                badgeBackgroundRes = R.drawable.bg_status_processing,
                badgeTextColorRes = R.color.status_processing_text,
                estimatedDeliveryText = order.deliveryDate ?: getString(R.string.preparing_for_dispatch),
                trackingSummary = getString(R.string.supplier_preparing_items),
                currentStep = 3,
                currentColorRes = R.color.status_processing_text,
                currentBackgroundRes = R.drawable.bg_tracking_processing
            )
        }
    }

    private data class StepViews(
        val icon: ImageView,
        val title: TextView,
        val subtitle: TextView,
        val meta: TextView,
        val connector: View?
    )

    private data class OrderStatusUi(
        val badgeText: String,
        val badgeBackgroundRes: Int,
        val badgeTextColorRes: Int,
        val estimatedDeliveryText: String,
        val trackingSummary: String,
        val currentStep: Int,
        val currentColorRes: Int,
        val currentBackgroundRes: Int
    )

    companion object {
        const val EXTRA_ORDER_ID = "order_id"
        const val EXTRA_ORDER_INTERNAL_ID = "order_internal_id"
    }
}
