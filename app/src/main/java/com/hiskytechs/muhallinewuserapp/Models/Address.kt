package com.hiskytechs.muhallinewuserapp.Models

data class Address(
    val fullName: String,
    val phoneNumber: String,
    val streetAddress: String,
    val city: String,
    val note: String = ""
) {
    val formattedAddress: String
        get() = "$streetAddress, $city"
}
