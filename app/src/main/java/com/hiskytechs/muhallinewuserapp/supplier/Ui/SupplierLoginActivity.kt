package com.hiskytechs.muhallinewuserapp.supplier.Ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hiskytechs.muhallinewuserapp.Ui.AppLoadingDialog
import com.hiskytechs.muhallinewuserapp.databinding.ActivitySupplierLoginBinding
import com.hiskytechs.muhallinewuserapp.network.AuthRepository
import com.hiskytechs.muhallinewuserapp.network.BackgroundWork
import com.hiskytechs.muhallinewuserapp.Ui.OtpVerificationActivity
import org.json.JSONObject

class SupplierLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySupplierLoginBinding
    private lateinit var loadingDialog: AppLoadingDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySupplierLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loadingDialog = AppLoadingDialog(this)

        binding.btnLogin.setOnClickListener { requestOtp() }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, SupplierRegisterActivity::class.java))
        }

        binding.tvForgotPassword.setOnClickListener { requestOtp() }
    }

    private fun requestOtp() {
        val phone = binding.etPhone.text?.toString()?.trim().orEmpty()
        if (phone.length < 8) {
            Toast.makeText(this, getString(com.hiskytechs.muhallinewuserapp.R.string.supplier_phone_required), Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnLogin.isEnabled = false
        loadingDialog.show()
        BackgroundWork.run(
            task = {
                AuthRepository.requestOtp(
                    role = "supplier",
                    purpose = "login",
                    payload = mapOf("phone" to phone)
                )
            },
            onSuccess = { result ->
                binding.btnLogin.isEnabled = true
                loadingDialog.dismiss()
                openVerification(result.phone.ifBlank { phone })
            },
            onError = { message ->
                binding.btnLogin.isEnabled = true
                loadingDialog.dismiss()
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onDestroy() {
        loadingDialog.dismiss()
        super.onDestroy()
    }

    private fun openVerification(phone: String) {
        OtpVerificationActivity.open(
            context = this,
            role = "supplier",
            purpose = "login",
            phone = phone,
            payloadJson = JSONObject(mapOf("phone" to phone)).toString()
        )
    }
}
