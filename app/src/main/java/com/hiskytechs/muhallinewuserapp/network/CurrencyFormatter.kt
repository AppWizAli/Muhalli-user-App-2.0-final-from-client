package com.hiskytechs.muhallinewuserapp.network

import android.content.Context
import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    private const val PREFS_NAME = "muhalli_currency"
    private const val KEY_SYMBOL = "symbol"
    private const val FALLBACK_SYMBOL = "PKR"

    @Volatile
    private var symbol: String = FALLBACK_SYMBOL

    fun initialize(context: Context) {
        symbol = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SYMBOL, FALLBACK_SYMBOL)
            .orEmpty()
            .ifBlank { FALLBACK_SYMBOL }
    }

    fun update(context: Context, value: String?) {
        val nextSymbol = value?.trim().orEmpty().ifBlank { FALLBACK_SYMBOL }
        symbol = nextSymbol
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SYMBOL, nextSymbol)
            .apply()
    }

    fun currentSymbol(): String = symbol

    fun format(amount: Double): String {
        return "${number(amount)} $symbol"
    }

    fun format(amount: Int): String {
        return "${NumberFormat.getNumberInstance(Locale.getDefault()).format(amount)} $symbol"
    }

    fun formatPositive(amount: Int): String {
        return "+ ${format(amount)}"
    }

    private fun number(amount: Double): String {
        val safeAmount = if (amount.isFinite()) amount else 0.0
        return NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = if (safeAmount % 1.0 == 0.0) 0 else 2
            maximumFractionDigits = 2
        }.format(safeAmount)
    }
}
