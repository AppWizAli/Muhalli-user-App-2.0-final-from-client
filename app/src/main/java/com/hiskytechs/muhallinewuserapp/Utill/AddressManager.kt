package com.hiskytechs.muhallinewuserapp.Utill

import android.content.Context
import com.hiskytechs.muhallinewuserapp.MuhalliApplication
import com.hiskytechs.muhallinewuserapp.Models.Address

object AddressManager {
    private const val PREFS_NAME = "buyer_saved_address"
    private const val KEY_FULL_NAME = "full_name"
    private const val KEY_PHONE = "phone"
    private const val KEY_STREET = "street"
    private const val KEY_CITY = "city"
    private const val KEY_NOTE = "note"

    private var savedAddress: Address? = null

    fun getAddress(): Address? {
        if (savedAddress == null) {
            savedAddress = loadAddress()
        }
        return savedAddress
    }

    fun saveAddress(address: Address) {
        savedAddress = address
        prefs().edit()
            .putString(KEY_FULL_NAME, address.fullName)
            .putString(KEY_PHONE, address.phoneNumber)
            .putString(KEY_STREET, address.streetAddress)
            .putString(KEY_CITY, address.city)
            .putString(KEY_NOTE, address.note)
            .apply()
    }

    fun hasAddress(): Boolean = getAddress() != null

    private fun loadAddress(): Address? {
        val prefs = prefs()
        val fullName = prefs.getString(KEY_FULL_NAME, "").orEmpty()
        val phone = prefs.getString(KEY_PHONE, "").orEmpty()
        val street = prefs.getString(KEY_STREET, "").orEmpty()
        val city = prefs.getString(KEY_CITY, "").orEmpty()
        if (fullName.isBlank() || phone.isBlank() || street.isBlank() || city.isBlank()) return null
        return Address(
            fullName = fullName,
            phoneNumber = phone,
            streetAddress = street,
            city = city,
            note = prefs.getString(KEY_NOTE, "").orEmpty()
        )
    }

    private fun prefs() = MuhalliApplication.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
