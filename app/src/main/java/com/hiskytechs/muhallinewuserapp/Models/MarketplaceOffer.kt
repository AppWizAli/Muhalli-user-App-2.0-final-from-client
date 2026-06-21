package com.hiskytechs.muhallinewuserapp.Models

data class MarketplaceOffer(
    val id: Int,
    val supplierId: Int,
    val catalogProductId: Int,
    val title: String,
    val description: String,
    val badgeLabel: String,
    val discountLabel: String,
    val city: String,
    val imageUrl: String,
    val supplierName: String,
    val productName: String,
    val offerPrice: Double = 0.0,
    val maximumQuantity: Int = 0,
    val originalPrice: Double = 0.0,
    val stockQuantity: Int = 0
)
