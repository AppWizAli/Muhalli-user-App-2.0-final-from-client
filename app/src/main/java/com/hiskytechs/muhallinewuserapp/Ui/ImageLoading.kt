package com.hiskytechs.muhallinewuserapp.Ui

import android.widget.ImageView
import coil.load
import coil.size.Scale
import com.hiskytechs.muhallinewuserapp.R

fun ImageView.loadMarketplaceImage(imageUrl: String?) {
    val source = imageUrl?.takeIf { it.isNotBlank() } ?: R.drawable.ic_image_24
    load(source) {
        size(MAX_MARKETPLACE_IMAGE_SIZE_PX, MAX_MARKETPLACE_IMAGE_SIZE_PX)
        scale(Scale.FILL)
        crossfade(false)
        placeholder(R.drawable.ic_image_24)
        error(R.drawable.ic_image_24)
    }
}

private const val MAX_MARKETPLACE_IMAGE_SIZE_PX = 480
