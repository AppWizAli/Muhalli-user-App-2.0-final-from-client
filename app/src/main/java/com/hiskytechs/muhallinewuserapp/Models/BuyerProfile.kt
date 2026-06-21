package com.hiskytechs.muhallinewuserapp.Models

data class BuyerProfile(
    var storeName: String,
    var buyerName: String,
    var email: String,
    var phoneNumber: String,
    var city: String,
    var memberSince: String,
    var address: String = "",
    var latitude: Double? = null,
    var longitude: Double? = null
)
