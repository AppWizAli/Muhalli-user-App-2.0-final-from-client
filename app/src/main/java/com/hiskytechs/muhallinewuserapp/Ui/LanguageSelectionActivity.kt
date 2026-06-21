package com.hiskytechs.muhallinewuserapp.Ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.hiskytechs.muhallinewuserapp.LocaleManager
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.databinding.ActivityLanguageSelectionBinding

class LanguageSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLanguageSelectionBinding
    private var selectedLanguageTag: String = LocaleManager.LANGUAGE_ENGLISH

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        selectedLanguageTag = LocaleManager.getSavedLanguageTag(this)

        binding.cardEnglish.setOnClickListener {
            selectedLanguageTag = LocaleManager.LANGUAGE_ENGLISH
            renderSelection()
        }

        binding.cardArabicSudan.setOnClickListener {
            selectedLanguageTag = LocaleManager.LANGUAGE_ARABIC_SUDAN
            renderSelection()
        }

        binding.btnContinue.setOnClickListener {
            LocaleManager.updateLanguage(this, selectedLanguageTag)
            startActivity(Intent(this, ChooseAppActivity::class.java))
            finish()
        }

        renderSelection()
    }

    private fun renderSelection() {
        val unselectedBackground = ContextCompat.getColor(this, R.color.white)
        val unselectedStroke = ContextCompat.getColor(this, R.color.divider)
        val englishSelectedBackground = ContextCompat.getColor(this, R.color.status_transit_bg)
        val englishSelectedStroke = ContextCompat.getColor(this, R.color.primary)
        val arabicSelectedBackground = ContextCompat.getColor(this, R.color.status_delivered_bg)
        val arabicSelectedStroke = ContextCompat.getColor(this, R.color.status_delivered_text)

        val englishSelected = selectedLanguageTag == LocaleManager.LANGUAGE_ENGLISH
        binding.cardEnglish.setCardBackgroundColor(
            if (englishSelected) englishSelectedBackground else unselectedBackground
        )
        binding.cardEnglish.strokeColor = if (englishSelected) englishSelectedStroke else unselectedStroke
        binding.ivEnglishCheck.alpha = if (englishSelected) 1f else 0.18f

        val arabicSelected = selectedLanguageTag == LocaleManager.LANGUAGE_ARABIC_SUDAN
        binding.cardArabicSudan.setCardBackgroundColor(
            if (arabicSelected) arabicSelectedBackground else unselectedBackground
        )
        binding.cardArabicSudan.strokeColor = if (arabicSelected) arabicSelectedStroke else unselectedStroke
        binding.ivArabicCheck.alpha = if (arabicSelected) 1f else 0.18f
    }
}
