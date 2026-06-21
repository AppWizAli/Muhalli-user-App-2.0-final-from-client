package com.hiskytechs.muhallinewuserapp.Adapters

import android.content.res.ColorStateList
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.hiskytechs.muhallinewuserapp.Models.Order
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.databinding.ItemOrderBinding
import com.hiskytechs.muhallinewuserapp.network.CurrencyFormatter

class OrderAdapter(
    private var orders: List<Order>,
    private val onViewDetails: (Order) -> Unit
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(val binding: ItemOrderBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding =
            ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {

        val order = orders[position]

        holder.binding.apply {
           Log.e("stateeeee", order.status)
            val context = root.context

            val normalizedStatus = order.status
                .trim()
                .lowercase()
                .removePrefix("wc-")
                .replace("_", "-")
                .replace(" ", "-")

            tvOrderId.text = order.orderId
            tvDate.text = order.date
            tvSupplierName.text = order.supplier
            tvItemsCount.text =
                context.getString(R.string.products_count_format, order.itemsCount)

            tvTotalAmount.text =
                CurrencyFormatter.format(order.totalAmount)

            /*
             * Localized status
             */
            tvStatus.text = localizedStatus(normalizedStatus)

            /*
             * Status styling
             */
            when (normalizedStatus) {

                "completed",
                "delivered" -> {

                    tvStatus.backgroundTintList =
                        ColorStateList.valueOf(
                            ContextCompat.getColor(
                                context,
                                R.color.status_delivered_bg
                            )
                        )

                    tvStatus.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.status_delivered_text
                        )
                    )

                    ivStatusIcon.setImageResource(R.drawable.ic_check_circle_24)

                    ivStatusIcon.imageTintList =
                        ColorStateList.valueOf(
                            ContextCompat.getColor(
                                context,
                                R.color.status_delivered_text
                            )
                        )
                }

                "in-transit",
                "shipped" -> {

                    tvStatus.backgroundTintList =
                        ColorStateList.valueOf(
                            ContextCompat.getColor(
                                context,
                                R.color.status_transit_bg
                            )
                        )

                    tvStatus.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.status_transit_text
                        )
                    )

                    ivStatusIcon.setImageResource(R.drawable.ic_local_shipping_24)

                    ivStatusIcon.imageTintList =
                        ColorStateList.valueOf(
                            ContextCompat.getColor(
                                context,
                                R.color.status_transit_text
                            )
                        )
                }

                "processing",
                "pending",
                "on-hold" -> {

                    tvStatus.backgroundTintList =
                        ColorStateList.valueOf(
                            ContextCompat.getColor(
                                context,
                                R.color.status_processing_bg
                            )
                        )

                    tvStatus.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.status_processing_text
                        )
                    )

                    ivStatusIcon.setImageResource(R.drawable.ic_schedule_24)

                    ivStatusIcon.imageTintList =
                        ColorStateList.valueOf(
                            ContextCompat.getColor(
                                context,
                                R.color.status_processing_text
                            )
                        )
                }

                "cancelled",
                "canceled",
                "failed" -> {

                    tvStatus.backgroundTintList =
                        ColorStateList.valueOf(
                            ContextCompat.getColor(
                                context,
                                R.color.status_cancelled_bg
                            )
                        )

                    tvStatus.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.status_cancelled_text
                        )
                    )

                    ivStatusIcon.setImageResource(R.drawable.ic_delete_24)

                    ivStatusIcon.imageTintList =
                        ColorStateList.valueOf(
                            ContextCompat.getColor(
                                context,
                                R.color.status_cancelled_text
                            )
                        )
                }
            }

            btnViewDetails.setOnClickListener {
                onViewDetails(order)
            }
        }
    }

    override fun getItemCount(): Int = orders.size

    fun updateOrders(newOrders: List<Order>) {
        orders = newOrders
        notifyDataSetChanged()
    }

    private fun localizedStatus(status: String): String {

        return when (status) {

            "completed",
            "delivered" -> "تم التسليم"

            "processing" -> "قيد المعالجة"

            "pending" -> "قيد الانتظار"

            "on-hold" -> "قيد المراجعة"

            "shipped",
            "in-transit" -> "قيد الشحن"

            "cancelled",
            "canceled" -> "ملغي"

            "failed" -> "فشل الطلب"

            "refunded" -> "تم الاسترجاع"

            else -> status
        }
    }
}