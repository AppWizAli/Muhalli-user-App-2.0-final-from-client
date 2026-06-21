package com.hiskytechs.muhallinewuserapp

import android.app.Activity
import android.content.Context
import android.text.TextUtils
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleManager {

    const val LANGUAGE_ENGLISH = "en"
    const val LANGUAGE_ARABIC_SUDAN = "ar-SD"

    private const val PREFS_NAME = "locale_preferences"
    private const val KEY_LANGUAGE_TAG = "language_tag"

    fun getSavedLanguageTag(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE_TAG, LANGUAGE_ENGLISH) ?: LANGUAGE_ENGLISH
    }

    fun applySavedLocale(context: Context) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(getSavedLanguageTag(context))
        )
    }

    fun updateLanguage(context: Context, languageTag: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE_TAG, languageTag)
            .apply()

        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(languageTag)
        )
    }

    fun applyLayoutDirection(activity: Activity) {
        val layoutDirection = if (isRtl(activity)) {
            View.LAYOUT_DIRECTION_RTL
        } else {
            View.LAYOUT_DIRECTION_LTR
        }
        val textDirection = if (layoutDirection == View.LAYOUT_DIRECTION_RTL) {
            View.TEXT_DIRECTION_RTL
        } else {
            View.TEXT_DIRECTION_LTR
        }

        activity.window?.decorView?.let { root ->
            root.layoutDirection = layoutDirection
            root.textDirection = textDirection
        }

        activity.findViewById<View?>(android.R.id.content)?.let { content ->
            content.layoutDirection = layoutDirection
            content.textDirection = textDirection
        }
    }

    private fun isRtl(context: Context): Boolean {
        return TextUtils.getLayoutDirectionFromLocale(currentLocale(context)) == View.LAYOUT_DIRECTION_RTL
    }

    private fun currentLocale(context: Context): Locale {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        return appLocales[0]
            ?: context.resources.configuration.locales[0]
            ?: Locale.forLanguageTag(getSavedLanguageTag(context))
    }
}
