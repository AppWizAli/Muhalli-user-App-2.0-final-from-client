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
