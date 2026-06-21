package com.hiskytechs.muhallinewuserapp.Ui

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.databinding.DialogLoadingBinding

class AppLoadingDialog(private val activity: AppCompatActivity) {

    private var dialog: Dialog? = null

    fun show(@StringRes messageRes: Int = R.string.loading_message) {
        if (activity.isFinishing || activity.isDestroyed) {
            return
        }

        val existingDialog = dialog
        if (existingDialog?.isShowing == true) {
            existingDialog.findViewById<android.widget.TextView>(R.id.tvLoadingMessage)?.setText(messageRes)
            return
        }

        val binding = DialogLoadingBinding.inflate(activity.layoutInflater).apply {
            tvLoadingMessage.setText(messageRes)
        }

        dialog = Dialog(activity).apply {
            setContentView(binding.root)
            setCancelable(false)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window?.setDimAmount(0.42f)
            show()
        }
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }
}
