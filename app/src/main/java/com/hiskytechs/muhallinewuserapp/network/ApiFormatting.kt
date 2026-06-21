package com.hiskytechs.muhallinewuserapp.network

import com.hiskytechs.muhallinewuserapp.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.absoluteValue

object ApiFormatting {
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val apiDateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val displayDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private val displayTimeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    private val supplierHeaderPalette = listOf(
        "#EEF4FF",
        "#EFFBF3",
        "#FFF6EC",
        "#F5F2FF",
        "#FFF1F6",
        "#EAF9F8"
    )

    fun displayDate(value: String?): String {
        if (value.isNullOrBlank()) return ""
        return runCatching { displayDateFormat.format(apiDateFormat.parse(value) ?: return value) }
            .getOrElse { value }
    }

    fun displayDateTime(value: String?): String {
        if (value.isNullOrBlank()) return ""
        return runCatching {
            val date = apiDateTimeFormat.parse(value) ?: return value
            val now = Calendar.getInstance()
            val then = Calendar.getInstance().apply { time = date }
            if (
                now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
            ) {
                displayTimeFormat.format(date)
            } else {
                displayDateFormat.format(date)
            }
        }.getOrElse { value }
    }

    fun displayTime(value: String?): String {
        if (value.isNullOrBlank()) return ""
        return runCatching { displayTimeFormat.format(apiDateTimeFormat.parse(value) ?: return value) }
            .getOrElse { value }
    }

    fun supplierHeaderColor(index: Int): String {
        return supplierHeaderPalette[index.mod(supplierHeaderPalette.size)]
    }

    fun accentColorRes(seed: String): Int {
        return when (seed.hashCode().absoluteValue % 6) {
            0 -> R.color.primary
            1 -> R.color.supplier_blue
            2 -> R.color.supplier_orange
            3 -> R.color.supplier_teal
            4 -> R.color.supplier_blush
            else -> R.color.supplier_soft_blue
        }
    }
}
