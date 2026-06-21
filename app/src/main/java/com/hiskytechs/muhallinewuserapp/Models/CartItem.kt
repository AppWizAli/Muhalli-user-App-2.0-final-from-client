package com.hiskytechs.muhallinewuserapp.Models

data class CartItem(
    val id: String,
    val name: String,
    val supplier: String,
    val price: Double,
    var quantity: Int,
    val imageUrl: String? = null,
    val offerPrice: Double = 0.0,
    val maximumOfferQuantity: Int = 0
) {
    val hasOffer: Boolean
        get() = offerPrice > 0.0 && offerPrice < price

    val offerQuantity: Int
        get() = if (!hasOffer) {
            0
        } else if (maximumOfferQuantity > 0) {
            quantity.coerceAtMost(maximumOfferQuantity)
        } else {
            quantity
        }

    val regularQuantity: Int
        get() = (quantity - offerQuantity).coerceAtLeast(0)

    val subtotal: Double
        get() = if (hasOffer) {
            (offerQuantity * offerPrice) + (regularQuantity * price)
        } else {
            price * quantity
        }

    val displayUnitPrice: Double
        get() = if (hasOffer) offerPrice else price
}
