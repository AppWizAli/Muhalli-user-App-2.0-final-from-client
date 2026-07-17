package com.hiskytechs.muhallinewuserapp.Ui

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import com.hiskytechs.muhallinewuserapp.R

object LocationSupport {
    fun supportedCities(context: Context): List<String> = listOf(
        context.getString(R.string.city_khartoum),
        context.getString(R.string.city_bahry),
        context.getString(R.string.city_omdurman)
    )

    fun bindSuggestions(context: Context, view: AutoCompleteTextView) {
        val suggestions = supportedCities(context)
        view.setAdapter(
            ArrayAdapter(
                context,
                android.R.layout.simple_dropdown_item_1line,
                suggestions
            )
        )
        view.threshold = 0
        // A fixed height (rather than "wrap_content"/auto) keeps the dropdown
        // from trying to measure against the full screen height, which on
        // edge-to-edge API levels doesn't shrink for the keyboard and left the
        // list rendered behind it.
        view.dropDownHeight = (168 * context.resources.displayMetrics.density).toInt()
        view.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                view.showDropDown()
            }
        }
        view.setOnClickListener {
            view.showDropDown()
        }
        view.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_location_on_24, 0, 0, 0)
    }
}
