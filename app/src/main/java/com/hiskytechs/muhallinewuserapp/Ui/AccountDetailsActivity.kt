package com.hiskytechs.muhallinewuserapp.Ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.hiskytechs.muhallinewuserapp.Data.AppData
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.Utill.KeyboardInsets
import com.hiskytechs.muhallinewuserapp.databinding.ActivityAccountDetailsBinding

class AccountDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountDetailsBinding
    private var currentProfile = AppData.buyerProfile
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
        binding.etAddress.setText(selectedAddress)
        currentProfile = currentProfile.copy(
            city = selectedCity.ifBlank { currentProfile.city },
            address = selectedAddress.ifBlank { currentProfile.address },
            latitude = selectedLatitude ?: currentProfile.latitude,
            longitude = selectedLongitude ?: currentProfile.longitude
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        KeyboardInsets.applyBottomPadding(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
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
        loadProfile()

        binding.btnSaveProfile.setOnClickListener {
            val buyerName = binding.etBuyerName.text?.toString()?.trim().orEmpty()
            val city = binding.etCity.text?.toString()?.trim().orEmpty()
            if (city.isBlank()) {
                Toast.makeText(this, getString(R.string.choose_city_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (binding.etAddress.text?.toString()?.trim().isNullOrBlank()) {
                Toast.makeText(this, getString(R.string.location_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val updatedProfile = currentProfile.copy(
                storeName = buyerName,
                buyerName = buyerName,
                phoneNumber = binding.etPhone.text?.toString()?.trim().orEmpty(),
                city = city,
                address = binding.etAddress.text?.toString()?.trim().orEmpty(),
                latitude = selectedLatitude ?: currentProfile.latitude,
                longitude = selectedLongitude ?: currentProfile.longitude
            )
            AppData.updateBuyerProfile(
                updatedProfile = updatedProfile,
                onSuccess = {
                    Toast.makeText(this, getString(R.string.account_details_updated), Toast.LENGTH_SHORT)
                        .show()
                    finish()
                },
                onError = { message ->
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun loadProfile() {
        AppData.loadBuyerProfile(
            onSuccess = { profile ->
                currentProfile = profile
                selectedCity = profile.city
                selectedAddress = profile.address
                selectedLatitude = profile.latitude
                selectedLongitude = profile.longitude
                suppressCityWatcher = true
                binding.etBuyerName.setText(profile.buyerName)
                binding.etPhone.setText(profile.phoneNumber)
                binding.etCity.setText(profile.city)
                suppressCityWatcher = false
                binding.etAddress.setText(profile.address)
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
