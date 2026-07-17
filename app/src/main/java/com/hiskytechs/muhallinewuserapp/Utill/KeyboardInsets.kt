package com.hiskytechs.muhallinewuserapp.Utill

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

object KeyboardInsets {

    /**
     * Pads [root]'s bottom edge by the keyboard/system-bar inset so content
     * anchored at the bottom (chat composer, autocomplete dropdowns, etc.)
     * stays above the keyboard instead of being drawn behind it. Needed
     * because targeting edge-to-edge API levels stops the window from
     * resizing on its own when the keyboard opens.
     */
    fun applyBottomPadding(root: View) {
        val initialPadding = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                initialPadding + maxOf(keyboard.bottom, systemBars.bottom)
            )
            insets
        }
    }
}
