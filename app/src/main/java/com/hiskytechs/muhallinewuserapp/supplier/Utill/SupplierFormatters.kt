package com.hiskytechs.muhallinewuserapp.supplier.Utill

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.network.CurrencyFormatter
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierOrderStatus
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierStockState
import java.util.Locale

fun formatPkr(amount: Int): String {
    return CurrencyFormatter.format(amount)
}

fun formatPositivePkr(amount: Int): String {
    return CurrencyFormatter.formatPositive(amount)
}

fun initials(value: String): String {
    return value.trim().take(1).uppercase(Locale.getDefault())
}

@DrawableRes
fun orderStatusBackground(status: SupplierOrderStatus): Int {
    return when (status) {
        SupplierOrderStatus.PENDING -> R.drawable.bg_supplier_status_pending
        SupplierOrderStatus.CONFIRMED -> R.drawable.bg_supplier_status_confirmed
        SupplierOrderStatus.SHIPPED -> R.drawable.bg_supplier_status_shipped
        SupplierOrderStatus.DELIVERED -> R.drawable.bg_supplier_status_delivered
    }
}

@ColorRes
fun orderStatusTextColor(status: SupplierOrderStatus): Int {
    return when (status) {
        SupplierOrderStatus.PENDING -> R.color.supplier_warning_text
        SupplierOrderStatus.CONFIRMED -> R.color.supplier_confirmed_text
        SupplierOrderStatus.SHIPPED -> R.color.supplier_shipped_text
        SupplierOrderStatus.DELIVERED -> R.color.supplier_success_text
    }
}

@DrawableRes
fun stockBackground(state: SupplierStockState): Int {
    return when (state) {
        SupplierStockState.IN_STOCK -> R.drawable.bg_supplier_stock_in
        SupplierStockState.LOW_STOCK -> R.drawable.bg_supplier_stock_low
        SupplierStockState.OUT_OF_STOCK -> R.drawable.bg_supplier_stock_out
    }
}

@ColorRes
fun stockTextColor(state: SupplierStockState): Int {
    return when (state) {
        SupplierStockState.IN_STOCK -> R.color.supplier_success_text
        SupplierStockState.LOW_STOCK -> R.color.supplier_warning_text
        SupplierStockState.OUT_OF_STOCK -> R.color.supplier_error_text
    }
}

@DrawableRes
fun inventoryBackground(state: SupplierStockState): Int {
    return when (state) {
        SupplierStockState.IN_STOCK -> R.drawable.bg_supplier_inventory_in
        SupplierStockState.LOW_STOCK -> R.drawable.bg_supplier_inventory_low
        SupplierStockState.OUT_OF_STOCK -> R.drawable.bg_supplier_inventory_out
    }
}
