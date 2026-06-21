package com.hiskytechs.muhallinewuserapp.Ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.network.AppSession
import com.hiskytechs.muhallinewuserapp.supplier.Ui.SupplierMainActivity

class ActivitySplash : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        window.decorView.post {
            val destination = when {
                AppSession.activeRole == AppSession.ROLE_BUYER && AppSession.hasBuyerSession() -> {
                    Intent(this, com.hiskytechs.muhallinewuserapp.MainActivity::class.java)
                }
                AppSession.activeRole == AppSession.ROLE_SUPPLIER && AppSession.hasSupplierSession() -> {
                    Intent(this, SupplierMainActivity::class.java)
                }
                else -> Intent(this, LanguageSelectionActivity::class.java)
            }
            startActivity(destination)
            overridePendingTransition(0, 0)
            finish()
        }
    }
}
