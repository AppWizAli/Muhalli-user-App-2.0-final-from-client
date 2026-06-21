package com.hiskytechs.muhallinewuserapp.Utill

import com.hiskytechs.muhallinewuserapp.Data.AppData
import com.hiskytechs.muhallinewuserapp.Models.CartItem

data class SupplierCart(
    val supplierName: String,
    val items: List<CartItem>,
    val minimumAmount: Double,
    val minimumQuantity: Int,
    val shippingCost: Double
) {
    val subtotal: Double
        get() = items.sumOf { it.subtotal }

    val shipping: Double
        get() = if (items.isEmpty()) 0.0 else shippingCost

    val total: Double
        get() = subtotal + shipping

    val lineItemCount: Int
        get() = items.size

    val totalQuantity: Int
        get() = items.sumOf { it.quantity }

    val remainingAmount: Double
        get() = (minimumAmount - subtotal).coerceAtLeast(0.0)

    val remainingQuantity: Int
        get() = (minimumQuantity - totalQuantity).coerceAtLeast(0)

    val isMinimumMet: Boolean
        get() = remainingAmount <= 0.0 && remainingQuantity <= 0
}

object CartManager {
    private val cartItems = mutableListOf<CartItem>()
    private const val SHIPPING_COST = 25.0
    private const val DEFAULT_MINIMUM_AMOUNT = 0.0
    private const val DEFAULT_MINIMUM_QUANTITY = 0

    fun addItem(item: CartItem) {
        val existingItem = cartItems.find {
            it.id == item.id && it.supplier.equals(item.supplier, ignoreCase = true)
        }
        if (existingItem != null) {
            existingItem.quantity += item.quantity
        } else {
            cartItems.add(item)
        }
    }

    fun decrementItem(productId: String, supplierName: String) {
        val existingItem = cartItems.find {
            it.id == productId && it.supplier.equals(supplierName, ignoreCase = true)
        } ?: return

        if (existingItem.quantity > 1) {
            existingItem.quantity--
        } else {
            cartItems.remove(existingItem)
        }
    }

    fun getItems(): List<CartItem> = cartItems.toList()

    fun getItems(supplierName: String): List<CartItem> {
        return cartItems.filter { it.supplier.equals(supplierName, ignoreCase = true) }
    }

    fun getSupplierCarts(): List<SupplierCart> {
        return cartItems
            .groupBy { it.supplier }
            .map { (supplierName, items) ->
                val supplier = AppData.findSupplierByName(supplierName)
                SupplierCart(
                    supplierName = supplierName,
                    items = items,
                    minimumAmount = supplier?.minimumAmount ?: DEFAULT_MINIMUM_AMOUNT,
                    minimumQuantity = supplier?.minimumQuantity ?: DEFAULT_MINIMUM_QUANTITY,
                    shippingCost = SHIPPING_COST
                )
            }
    }

    fun getSupplierCart(supplierName: String): SupplierCart? {
        return getSupplierCarts().firstOrNull { it.supplierName.equals(supplierName, ignoreCase = true) }
    }

    fun removeItem(item: CartItem) {
        cartItems.remove(item)
    }

    fun clearSupplierCart(supplierName: String) {
        cartItems.removeAll { it.supplier.equals(supplierName, ignoreCase = true) }
    }

    fun clearCart() {
        cartItems.clear()
    }

    fun getSubtotal(): Double = cartItems.sumOf { it.subtotal }

    fun getSubtotal(supplierName: String): Double = getSupplierCart(supplierName)?.subtotal ?: 0.0

    fun getShipping(): Double = getSupplierCarts().sumOf { it.shipping }

    fun getShipping(supplierName: String): Double = getSupplierCart(supplierName)?.shipping ?: 0.0

    fun getTotal(): Double = getSubtotal() + getShipping()

    fun getTotal(supplierName: String): Double = getSupplierCart(supplierName)?.total ?: 0.0

    fun getCartCount(): Int = cartItems.size

    fun getTotalQuantity(): Int = cartItems.sumOf { it.quantity }

    fun getTotalQuantity(supplierName: String): Int = getSupplierCart(supplierName)?.totalQuantity ?: 0

    fun getProductQuantity(productId: String, supplierName: String): Int {
        return cartItems
            .firstOrNull { it.id == productId && it.supplier.equals(supplierName, ignoreCase = true) }
            ?.quantity
            ?: 0
    }
}
