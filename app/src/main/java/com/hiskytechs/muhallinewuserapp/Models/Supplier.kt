package com.hiskytechs.muhallinewuserapp.Models

data class Supplier(
    val id: Int = 0,
    val name: String,
    val location: String,
    val productCount: String,
    val deliveryTime: String,
    val minimumAmount: Double,
    val minimumQuantity: Int,
    val categories: List<String>,
    val lowestPrice: Double = 0.0,
    val isVerified: Boolean = true,
    val headerColor: String = "#EAF2FF",
    val email: String = "",
    val phoneNumber: String = "",
    val address: String = "",
    val description: String = "",
    val paymentTerms: String = "",
    val ownerName: String = "",
    val status: String = ""
)
