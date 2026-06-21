package com.hiskytechs.muhallinewuserapp.Models

data class Product(
    val id: String,
    val catalogProductId: Int = 0,
    val name: String,
    val price: Double,
    val unit: String,
    val imageResId: Int,
    val imageUrl: String = "",
    val supplierName: String,
    val supplierId: Int = 0,
    val packaging: String = "",
    val stockQuantity: Int = 0,
    val deliveryTime: String = "",
    val categoryName: String = "",
    val offerPrice: Double = 0.0,
    val maximumOfferQuantity: Int = 0,
    val effectivePrice: Double = 0.0
) {
    val hasOffer: Boolean
        get() = offerPrice > 0.0 && offerPrice < price

    val displayPrice: Double
        get() = if (hasOffer) offerPrice else price
}
