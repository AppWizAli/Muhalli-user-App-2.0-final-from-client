package com.hiskytechs.muhallinewuserapp.supplier.Models

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.hiskytechs.muhallinewuserapp.Models.ChatMessageType

data class SupplierIntroPage(
    @DrawableRes val iconRes: Int,
    val title: String,
    val description: String
)

data class SupplierCategory(
    val id: String,
    val name: String,
    val productCount: Int,
    val catalogCount: Int = productCount,
    val listingCount: Int = productCount,
    @ColorRes val accentColorRes: Int
)

data class SupplierCatalogProduct(
    val id: String,
    val categoryId: String,
    val categoryName: String,
    val name: String,
    val unitLabel: String,
    val packaging: String,
    val imageUrl: String,
    @ColorRes val accentColorRes: Int
)

data class SupplierProduct(
    val id: String,
    val catalogProductId: String,
    val name: String,
    val categoryName: String,
    val unitLabel: String,
    val imageUrl: String,
    var pricePkr: Int,
    var stock: Int,
    var deliveryDays: String,
    var isActive: Boolean,
    var isOnOffer: Boolean = false,
    var offerPricePkr: Int = 0,
    var maximumOfferQuantity: Int = 0,
    @ColorRes val accentColorRes: Int
) {
    val hasActiveOffer: Boolean
        get() = isOnOffer && offerPricePkr > 0 && offerPricePkr < pricePkr

    val displayPricePkr: Int
        get() = if (hasActiveOffer) offerPricePkr else pricePkr

    val stockState: SupplierStockState
        get() = when {
            stock <= 0 -> SupplierStockState.OUT_OF_STOCK
            stock <= 10 -> SupplierStockState.LOW_STOCK
            else -> SupplierStockState.IN_STOCK
        }
}

enum class SupplierStockState {
    IN_STOCK,
    LOW_STOCK,
    OUT_OF_STOCK
}

enum class SupplierProductFilter {
    ALL,
    ACTIVE,
    INACTIVE,
    LOW_STOCK,
    OFFERS
}

enum class SupplierOrderStatus(val label: String) {
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
    SHIPPED("Shipped"),
    DELIVERED("Delivered")
}

data class SupplierOrder(
    val backendId: Int = 0,
    val id: String,
    val retailerName: String,
    val buyerName: String = "",
    val buyerPhone: String = "",
    val deliveryAddress: String = "",
    val notes: String = "",
    val orderDate: String,
    val orderDateTime: String = "",
    val expectedDeliveryDate: String,
    val itemsCount: Int,
    val amountPkr: Int,
    val status: SupplierOrderStatus,
    val items: List<SupplierOrderItem> = emptyList()
)

data class SupplierOrderItem(
    val productName: String,
    val unitLabel: String,
    val packaging: String = "",
    val quantity: Int,
    val unitPricePkr: Int,
    val lineTotalPkr: Int
)

data class SupplierDashboardStats(
    val todayOrders: Int,
    val pendingOrders: Int,
    val thisMonthRevenuePkr: Int,
    val totalProducts: Int
)

enum class SupplierHomeAction {
    ADD_PRODUCT,
    VIEW_ORDERS,
    OPEN_MESSAGES,
    OPEN_INVENTORY
}

data class SupplierQuickAction(
    val title: String,
    val subtitle: String,
    @DrawableRes val iconRes: Int,
    val action: SupplierHomeAction
)

enum class SupplierEarningsPeriod {
    ALL,
    THIS_MONTH,
    LAST_MONTH
}

data class SupplierTransaction(
    val retailerName: String,
    val orderId: String,
    val date: String,
    val amountPkr: Int,
    val period: SupplierEarningsPeriod
)

data class SupplierProfile(
    var businessName: String,
    var ownerName: String,
    var phoneNumber: String,
    var emailAddress: String,
    var city: String,
    var businessAddress: String,
    var minimumOrderQuantity: Int,
    var minimumOrderAmountPkr: Int,
    var deliveryTime: String = "",
    var paymentTerms: String = "",
    var description: String = "",
    var businessLicenseNumber: String = "",
    var status: String = "",
    var latitude: Double? = null,
    var longitude: Double? = null
)

enum class SupplierProfileAction {
    PRODUCTS,
    ORDERS,
    EARNINGS,
    BUSINESS_ADDRESS,
    NOTIFICATIONS,
    CHANGE_PASSWORD,
    HELP_SUPPORT,
    ABOUT,
    LOGOUT
}

data class SupplierProfileOption(
    val title: String,
    @DrawableRes val iconRes: Int,
    val action: SupplierProfileAction,
    val isDanger: Boolean = false
)

data class SupplierConversation(
    val id: String,
    val retailerName: String,
    val lastMessage: String,
    val timeLabel: String,
    val unreadCount: Int,
    @ColorRes val accentColorRes: Int
)

data class SupplierChatMessage(
    val id: String,
    val conversationId: String,
    val message: String,
    val timeLabel: String,
    val isMine: Boolean,
    val type: ChatMessageType = ChatMessageType.TEXT,
    val voiceDuration: String = ""
)

data class SupplierNotificationItem(
    val id: Int,
    val title: String,
    val message: String,
    val createdAtLabel: String,
    val linkType: String,
    val linkValue: String
)
