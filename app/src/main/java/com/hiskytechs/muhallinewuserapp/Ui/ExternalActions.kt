package com.hiskytechs.muhallinewuserapp.Ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.hiskytechs.muhallinewuserapp.R

fun launchSupportWhatsapp(
    context: Context,
    phoneNumber: String,
    prefilledMessage: String
) {
    val normalizedPhone = phoneNumber.filter { it.isDigit() }
    if (normalizedPhone.isBlank()) {
        Toast.makeText(context, R.string.support_whatsapp_missing, Toast.LENGTH_SHORT).show()
        return
    }

    val deepLink = Uri.parse(
        "https://wa.me/$normalizedPhone?text=${Uri.encode(prefilledMessage)}"
    )
    val intent = Intent(Intent.ACTION_VIEW, deepLink)
    val chooser = Intent.createChooser(intent, context.getString(R.string.contact_support))
    runCatching { context.startActivity(chooser) }
        .onFailure {
            Toast.makeText(context, R.string.whatsapp_not_available, Toast.LENGTH_SHORT).show()
        }
}
