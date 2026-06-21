package com.hiskytechs.muhallinewuserapp.Models

data class Order(
    val internalId: Int = 0,
    val orderId: String,
    val date: String,
    val status: String,
    val supplier: String,
    val itemsCount: Int,
    val totalAmount: Double,
    val deliveryDate: String? = null,
    val deliveryAddress: String = "",
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val items: List<OrderItem> = emptyList()
)

data class OrderItem(
    val productName: String,
    val unitLabel: String,
    val packaging: String,
    val quantity: Int,
    val unitPrice: Double,
    val lineTotal: Double
)
