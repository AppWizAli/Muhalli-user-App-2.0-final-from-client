package com.hiskytechs.muhallinewuserapp.supplier.Ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.Ui.LocationSupport
import com.hiskytechs.muhallinewuserapp.Ui.MapActivity
import com.hiskytechs.muhallinewuserapp.databinding.ActivitySupplierEditProfileBinding
import com.hiskytechs.muhallinewuserapp.supplier.Data.SupplierData
import com.hiskytechs.muhallinewuserapp.supplier.Utill.initials

class SupplierEditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySupplierEditProfileBinding
    private var currentProfile = SupplierData.getProfile()
    private var selectedCity: String = ""
    private var selectedAddress: String = ""
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null
    private var suppressCityWatcher = false
    private val mapPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult
        selectedCity = data.getStringExtra(MapActivity.EXTRA_SELECTED_CITY).orEmpty()
        selectedAddress = data.getStringExtra(MapActivity.EXTRA_SELECTED_ADDRESS).orEmpty()
        selectedLatitude = data.takeIf { it.hasExtra(MapActivity.EXTRA_SELECTED_LATITUDE) }
            ?.getDoubleExtra(MapActivity.EXTRA_SELECTED_LATITUDE, 0.0)
        selectedLongitude = data.takeIf { it.hasExtra(MapActivity.EXTRA_SELECTED_LONGITUDE) }
            ?.getDoubleExtra(MapActivity.EXTRA_SELECTED_LONGITUDE, 0.0)
        if (selectedCity.isNotBlank()) {
            suppressCityWatcher = true
            binding.etCity.setText(selectedCity)
            suppressCityWatcher = false
        }
        if (selectedAddress.isNotBlank()) {
            binding.etAddress.setText(selectedAddress)
        }
        currentProfile = currentProfile.copy(
            city = selectedCity.ifBlank { currentProfile.city },
            businessAddress = selectedAddress.ifBlank { currentProfile.businessAddress },
            latitude = selectedLatitude ?: currentProfile.latitude,
            longitude = selectedLongitude ?: currentProfile.longitude
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySupplierEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivBack.setOnClickListener { finish() }
        LocationSupport.bindSuggestions(this, binding.etCity)
        binding.etCity.addTextChangedListener { editable ->
            if (suppressCityWatcher) return@addTextChangedListener
            val city = editable?.toString()?.trim().orEmpty()
            if (selectedAddress.isNotBlank() && !city.equals(selectedCity, ignoreCase = true)) {
                clearSelectedLocation()
            }
        }
        loadProfile()
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
        binding.btnSave.setOnClickListener {
            val city = binding.etCity.text?.toString()?.trim().orEmpty()
            if (city.isBlank()) {
                Toast.makeText(this, getString(R.string.choose_city_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (binding.etAddress.text?.toString()?.trim().isNullOrBlank()) {
                Toast.makeText(this, getString(R.string.location_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            SupplierData.updateProfile(
                updatedProfile = currentProfile.copy(
                    businessName = binding.etBusinessName.text?.toString().orEmpty(),
                    ownerName = binding.etOwnerName.text?.toString().orEmpty(),
                    phoneNumber = binding.etPhone.text?.toString().orEmpty(),
                    city = city,
                    businessAddress = binding.etAddress.text?.toString().orEmpty(),
                    minimumOrderQuantity = binding.etMinimumOrderQuantity.text?.toString()?.toIntOrNull() ?: currentProfile.minimumOrderQuantity,
                    minimumOrderAmountPkr = binding.etMinimumOrderAmount.text?.toString()?.toIntOrNull() ?: currentProfile.minimumOrderAmountPkr,
                    latitude = selectedLatitude ?: currentProfile.latitude,
                    longitude = selectedLongitude ?: currentProfile.longitude
                ),
                onSuccess = {
                    Toast.makeText(this, getString(R.string.supplier_profile_updated), Toast.LENGTH_SHORT).show()
                    finish()
                },
                onError = { message ->
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun loadProfile() {
        SupplierData.refreshProfile(
            onSuccess = {
                currentProfile = SupplierData.getProfile()
                selectedCity = currentProfile.city
                selectedAddress = currentProfile.businessAddress
                selectedLatitude = currentProfile.latitude
                selectedLongitude = currentProfile.longitude
                binding.tvAvatar.text = initials(currentProfile.businessName)
                suppressCityWatcher = true
                binding.etBusinessName.setText(currentProfile.businessName)
                binding.etOwnerName.setText(currentProfile.ownerName)
                binding.etPhone.setText(currentProfile.phoneNumber)
                binding.etCity.setText(currentProfile.city)
                suppressCityWatcher = false
                binding.etAddress.setText(currentProfile.businessAddress)
                binding.etMinimumOrderQuantity.setText(currentProfile.minimumOrderQuantity.toString())
                binding.etMinimumOrderAmount.setText(currentProfile.minimumOrderAmountPkr.toString())
            },
            onError = { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun clearSelectedLocation() {
        selectedCity = ""
        selectedAddress = ""
        selectedLatitude = null
        selectedLongitude = null
        binding.etAddress.setText("")
    }
}
