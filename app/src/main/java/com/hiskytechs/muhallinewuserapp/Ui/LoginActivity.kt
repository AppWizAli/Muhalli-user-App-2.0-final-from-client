package com.hiskytechs.muhallinewuserapp.Ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.databinding.ActivityLoginBinding
import com.hiskytechs.muhallinewuserapp.network.AuthRepository
import com.hiskytechs.muhallinewuserapp.network.BackgroundWork
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var loadingDialog: AppLoadingDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loadingDialog = AppLoadingDialog(this)

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.btnLogin.setOnClickListener { requestOtp() }
        binding.tvForgotPassword.setOnClickListener { requestOtp() }
    }

    private fun requestOtp() {
        val phone = binding.etPhone.text?.toString()?.trim().orEmpty()
        if (phone.length < 8) {
            Toast.makeText(this, getString(R.string.otp_phone_required), Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnLogin.isEnabled = false
        loadingDialog.show()
        BackgroundWork.run(
            task = {
                AuthRepository.requestOtp(
                    role = "buyer",
                    purpose = "login",
                    payload = mapOf("phone" to phone)
                )
            },
            onSuccess = { result ->
                binding.btnLogin.isEnabled = true
                loadingDialog.dismiss()
                Toast.makeText(this, getString(R.string.otp_sent_success), Toast.LENGTH_SHORT).show()
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
            role = "buyer",
            purpose = "login",
            phone = phone,
            payloadJson = JSONObject(mapOf("phone" to phone)).toString()
        )
    }
}
