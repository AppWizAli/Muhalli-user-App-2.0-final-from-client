package com.hiskytechs.muhallinewuserapp.Ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.hiskytechs.muhallinewuserapp.Models.Address
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.Utill.AddressManager
import com.hiskytechs.muhallinewuserapp.Utill.KeyboardInsets
import com.hiskytechs.muhallinewuserapp.databinding.ActivityCheckoutAddressBinding

class CheckoutAddressActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCheckoutAddressBinding
    private var continueToReview: Boolean = true
    private var selectedCity: String = ""
    private var selectedAddress: String = ""
    private var suppressCityWatcher = false
    private val mapPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult
        selectedCity = data.getStringExtra(MapActivity.EXTRA_SELECTED_CITY).orEmpty()
        selectedAddress = data.getStringExtra(MapActivity.EXTRA_SELECTED_ADDRESS).orEmpty()
        if (selectedCity.isNotBlank()) {
            suppressCityWatcher = true
            binding.etCity.setText(selectedCity)
            suppressCityWatcher = false
        }
        binding.etStreetAddress.setText(selectedAddress)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutAddressBinding.inflate(layoutInflater)
        setContentView(binding.root)
        KeyboardInsets.applyBottomPadding(binding.root)
        continueToReview = intent.getBooleanExtra(EXTRA_CONTINUE_TO_REVIEW, true)

        binding.ivBack.setOnClickListener { finish() }
        binding.btnUseSavedAddress.setOnClickListener {
            if (continueToReview) openReviewScreen() else finish()
        }
        binding.btnEditSavedAddress.setOnClickListener {
            showForm(AddressManager.getAddress())
        }
        LocationSupport.bindSuggestions(this, binding.etCity)
        binding.etCity.addTextChangedListener { editable ->
            if (suppressCityWatcher) return@addTextChangedListener
            val city = editable?.toString()?.trim().orEmpty()
            if (selectedAddress.isNotBlank() && !city.equals(selectedCity, ignoreCase = true)) {
                clearSelectedLocation()
            }
        }
        binding.btnChooseLocation.setOnClickListener {
            val city = binding.etCity.text?.toString()?.trim().orEmpty()
            if (city.isBlank()) {
                Toast.makeText(this, getString(R.string.choose_city_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            mapPicker.launch(
                Intent(this, MapActivity::class.java).apply {
                    putExtra(MapActivity.EXTRA_MODE, MapActivity.MODE_PICK_LOCATION)
                    putExtra(MapActivity.EXTRA_CITY, city)
                    putExtra(MapActivity.EXTRA_TITLE, getString(R.string.select_location))
                }
            )
        }
        binding.btnSaveAddress.setOnClickListener {
            saveAddressAndContinue()
        }

        if (!continueToReview) {
            binding.btnUseSavedAddress.text = getString(R.string.done)
            binding.btnSaveAddress.text = getString(R.string.save_address)
        }

        renderSavedAddress()
    }

    private fun renderSavedAddress() {
        val savedAddress = AddressManager.getAddress()
        if (savedAddress == null) {
            binding.cardSavedAddress.visibility = View.GONE
            showForm()
            return
        }

        binding.cardSavedAddress.visibility = View.VISIBLE
        binding.cardAddressForm.visibility = View.GONE
        binding.tvSavedName.text = savedAddress.fullName
        binding.tvSavedPhone.text = savedAddress.phoneNumber
        binding.tvSavedAddress.text = savedAddress.formattedAddress
        binding.tvSavedNote.visibility = if (savedAddress.note.isBlank()) View.GONE else View.VISIBLE
        binding.tvSavedNote.text = savedAddress.note
    }

    private fun showForm(address: Address? = null) {
        binding.cardAddressForm.visibility = View.VISIBLE
        if (address != null) {
            binding.etFullName.setText(address.fullName)
            binding.etPhoneNumber.setText(address.phoneNumber)
            binding.etStreetAddress.setText(address.streetAddress)
            selectedCity = address.city
            selectedAddress = address.streetAddress
            suppressCityWatcher = true
            binding.etCity.setText(address.city)
            suppressCityWatcher = false
            binding.etAddressNote.setText(address.note)
        }
    }

    private fun saveAddressAndContinue() {
        val fullName = binding.etFullName.text?.toString()?.trim().orEmpty()
        val phoneNumber = binding.etPhoneNumber.text?.toString()?.trim().orEmpty()
        val streetAddress = binding.etStreetAddress.text?.toString()?.trim().orEmpty()
        val city = binding.etCity.text?.toString()?.trim().orEmpty()
        val note = binding.etAddressNote.text?.toString()?.trim().orEmpty()

        val requiredField = getString(R.string.required_field)
        binding.etFullName.error = if (fullName.isBlank()) requiredField else null
        binding.etPhoneNumber.error = if (phoneNumber.isBlank()) requiredField else null
        binding.etStreetAddress.error = if (streetAddress.isBlank()) requiredField else null
        binding.etCity.error = if (city.isBlank()) requiredField else null

        if (fullName.isBlank() || phoneNumber.isBlank() || streetAddress.isBlank() || city.isBlank()) {
            return
        }

        AddressManager.saveAddress(
            Address(
                fullName = fullName,
                phoneNumber = phoneNumber,
                streetAddress = streetAddress,
                city = city,
                note = note
            )
        )
        if (continueToReview) openReviewScreen() else finish()
    }

    private fun clearSelectedLocation() {
        selectedCity = ""
        selectedAddress = ""
        binding.etStreetAddress.setText("")
    }

    private fun openReviewScreen() {
        startActivity(
            Intent(this, CheckoutReviewActivity::class.java).apply {
                putExtra(EXTRA_SUPPLIER_NAME, intent.getStringExtra(EXTRA_SUPPLIER_NAME))
            }
        )
    }

    companion object {
        const val EXTRA_CONTINUE_TO_REVIEW = "continue_to_review"
        const val EXTRA_SUPPLIER_NAME = "supplier_name"
    }
}
