package com.hiskytechs.muhallinewuserapp.supplier.Data

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import com.hiskytechs.muhallinewuserapp.MuhalliApplication
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.network.ApiClient
import com.hiskytechs.muhallinewuserapp.network.ApiException
import com.hiskytechs.muhallinewuserapp.network.ApiFormatting
import com.hiskytechs.muhallinewuserapp.network.ApiConfig
import com.hiskytechs.muhallinewuserapp.network.AppSession
import com.hiskytechs.muhallinewuserapp.network.BackgroundWork
import com.hiskytechs.muhallinewuserapp.network.CurrencyFormatter
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierCatalogProduct
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierCategory
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierChatMessage
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierConversation
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierDashboardStats
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierEarningsPeriod
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierHomeAction
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierIntroPage
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierNotificationItem
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierOrder
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierOrderItem
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierOrderStatus
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierProduct
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierProductFilter
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierProfile
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierProfileAction
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierProfileOption
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierQuickAction
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierStockState
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierTransaction
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object SupplierData {

    private const val TAG = "SupplierData"
    const val INVENTORY_PAGE_SIZE = 50
    private const val SEARCH_MAX_PAGES = 5

    private const val CACHE_PREFS = "supplier_data_cache"
    private const val CACHE_CATALOG = "catalog"
    private const val CACHE_CATEGORIES = "categories"
    private const val CACHE_PRODUCTS = "products"
    private const val CACHE_PROFILE = "profile"
    private const val CACHE_ORDERS = "orders"
    private const val CACHE_EARNINGS = "earnings"
    private const val CACHE_CONVERSATIONS = "conversations"
    private const val CACHE_NOTIFICATIONS = "supplier_notifications"

    private val introPages = listOf(
        SupplierIntroPage(
            R.drawable.ic_storefront_24,
            string(R.string.supplier_intro_title_products),
            string(R.string.supplier_intro_body_products)
        ),
        SupplierIntroPage(
            R.drawable.ic_grid_view_24,
            string(R.string.supplier_intro_title_orders),
            string(R.string.supplier_intro_body_orders)
        ),
        SupplierIntroPage(
            R.drawable.ic_attach_money_24,
            string(R.string.supplier_intro_title_earnings),
            string(R.string.supplier_intro_body_earnings)
        )
    )

    private val quickActions = listOf(
        SupplierQuickAction(
            string(R.string.supplier_quick_action_add_product),
            string(R.string.supplier_quick_action_add_product_desc),
            R.drawable.ic_add_24,
            SupplierHomeAction.ADD_PRODUCT
        ),
        SupplierQuickAction(
            string(R.string.supplier_quick_action_orders),
            string(R.string.supplier_quick_action_orders_desc),
            R.drawable.ic_shopping_cart_24,
            SupplierHomeAction.VIEW_ORDERS
        ),
        SupplierQuickAction(
            string(R.string.supplier_quick_action_messages),
            string(R.string.supplier_quick_action_messages_desc),
            R.drawable.ic_chat_bubble_24,
            SupplierHomeAction.OPEN_MESSAGES
        ),
        SupplierQuickAction(
            string(R.string.supplier_quick_action_inventory),
            string(R.string.supplier_quick_action_inventory_desc),
            R.drawable.ic_sync_24,
            SupplierHomeAction.OPEN_INVENTORY
        )
    )

    private val profileOptions = listOf(
        SupplierProfileOption(string(R.string.supplier_profile_option_products), R.drawable.ic_grid_view_24, SupplierProfileAction.PRODUCTS),
        SupplierProfileOption(string(R.string.supplier_profile_option_orders), R.drawable.ic_shopping_cart_24, SupplierProfileAction.ORDERS),
        SupplierProfileOption(string(R.string.supplier_profile_option_earnings), R.drawable.ic_attach_money_24, SupplierProfileAction.EARNINGS),
        SupplierProfileOption(string(R.string.supplier_profile_option_address), R.drawable.ic_location_on_24, SupplierProfileAction.BUSINESS_ADDRESS),
        SupplierProfileOption(string(R.string.supplier_profile_option_notifications), R.drawable.ic_notifications_24, SupplierProfileAction.NOTIFICATIONS),
        SupplierProfileOption(string(R.string.supplier_profile_option_password), R.drawable.ic_lock_24, SupplierProfileAction.CHANGE_PASSWORD),
        SupplierProfileOption(string(R.string.supplier_profile_option_support), R.drawable.ic_info_24, SupplierProfileAction.HELP_SUPPORT),
        SupplierProfileOption(string(R.string.supplier_profile_option_about), R.drawable.ic_info_24, SupplierProfileAction.ABOUT),
        SupplierProfileOption(string(R.string.supplier_profile_option_logout), R.drawable.ic_arrow_back_24, SupplierProfileAction.LOGOUT, true)
    )

    private var categoriesCache: List<SupplierCategory> = emptyList()
    private var catalogProductsCache: List<SupplierCatalogProduct> = emptyList()
    private var fullCatalogProductsCache: List<SupplierCatalogProduct> = emptyList()
    private val catalogCacheLock = Any()
    private var supplierProductsCache: MutableList<SupplierProduct> = mutableListOf()
    private var ordersCache: MutableList<SupplierOrder> = mutableListOf()
    private var transactionsCache: List<SupplierTransaction> = emptyList()
    private var conversationsCache: MutableList<SupplierConversation> = mutableListOf()
    private val messagesCache = linkedMapOf<String, MutableList<SupplierChatMessage>>()

    private var dashboardStats = SupplierDashboardStats(0, 0, 0, 0)
    private var profile = SupplierProfile("", "", "", "", "", "", 0, 0)

    fun getIntroPages(): List<SupplierIntroPage> = introPages
    fun getQuickActions(): List<SupplierQuickAction> = quickActions
    fun getProfileOptions(): List<SupplierProfileOption> = profileOptions
    fun getCategories(): List<SupplierCategory> = categoriesCache
    fun getRecentOrders(): List<SupplierOrder> = ordersCache.take(3)
    fun getProfile(): SupplierProfile = profile.copy()
    fun getDashboardStats(): SupplierDashboardStats = dashboardStats
    fun hasCachedProducts(): Boolean = supplierProductsCache.isNotEmpty()
    fun hasCachedProfile(): Boolean = profile.businessName.isNotBlank() || profile.phoneNumber.isNotBlank()
    fun hasCachedDashboard(): Boolean = hasCachedProfile() || hasCachedProducts() || ordersCache.isNotEmpty()

    fun restoreCachedProducts(): Boolean {
        val catalogRestored = restoreCachedCatalog()
        val categoriesRestored = restoreCachedCategories()
        val productsRaw = cachedString(CACHE_PRODUCTS)
        val productsRestored = if (productsRaw.isNullOrBlank()) {
            false
        } else {
            runCatching {
                supplierProductsCache = parseSupplierProducts(JSONArray(productsRaw)).toMutableList()
                true
            }.getOrDefault(false)
        }
        return catalogRestored || categoriesRestored || productsRestored
    }

    fun restoreCachedProfile(): Boolean {
        val profileRaw = cachedString(CACHE_PROFILE)
        return if (profileRaw.isNullOrBlank()) {
            false
        } else {
            runCatching {
                profile = parseProfile(JSONObject(profileRaw))
                true
            }.getOrDefault(false)
        }
    }

    fun restoreCachedOrders(): Boolean {
        return restoreOrdersFromCache()
    }

    fun restoreCachedEarnings(): Boolean {
        val earningsRaw = cachedString(CACHE_EARNINGS)
        return if (earningsRaw.isNullOrBlank()) {
            false
        } else {
            runCatching {
                transactionsCache = parseTransactions(JSONObject(earningsRaw).optJSONArray("transactions"))
                true
            }.getOrDefault(false)
        }
    }

    fun restoreCachedDashboard(): Boolean {
        val restoredProfile = restoreCachedProfile()
        val restoredOrders = restoreOrdersFromCache()
        val restoredConversations = restoreCachedConversations()
        if (restoredProfile || restoredOrders || restoredConversations) {
            updateDashboardStats()
            return true
        }
        return false
    }

    fun restoreCachedHomeSummary(): Boolean {
        val restoredProfile = restoreCachedProfile()
        val restoredOrders = restoreOrdersFromCache()
        if (restoredProfile || restoredOrders) {
            updateDashboardStats()
            return true
        }
        return false
    }

    fun getLowStockAlert(): String {
        val lowStockCount = supplierProductsCache.count { it.stockState == SupplierStockState.LOW_STOCK }
        return if (lowStockCount > 0) {
            string(R.string.supplier_low_stock_alert_count, lowStockCount)
        } else {
            string(R.string.supplier_low_stock_all_healthy)
        }
    }

    fun refreshDashboard(onSuccess: () -> Unit, onError: (String) -> Unit) {
        BackgroundWork.run(
            task = {
                refreshProfileSync()
                refreshProductsSync()
                refreshOrdersSync()
                refreshMessagesSync()
                updateDashboardStats()
            },
            onSuccess = { onSuccess() },
            onError = onError
        )
    }

    fun refreshHomeSummary(onSuccess: () -> Unit, onError: (String) -> Unit) {
        BackgroundWork.run(
            task = {
                refreshProfileSync()
                refreshOrdersSync()
                updateDashboardStats()
            },
            onSuccess = { onSuccess() },
            onError = onError
        )
    }

    fun refreshHomeExtras(onSuccess: () -> Unit, onError: (String) -> Unit) {
        BackgroundWork.run(
            task = {
                refreshProductsSync()
                runCatching { refreshMessagesSync() }
                updateDashboardStats()
            },
            onSuccess = { onSuccess() },
            onError = onError
        )
    }

    fun refreshProducts(onSuccess: () -> Unit, onError: (String) -> Unit) {
        BackgroundWork.run(
            task = { refreshProductsSync() },
            onSuccess = { onSuccess() },
            onError = onError
        )
    }

    fun refreshCategories(onSuccess: () -> Unit, onError: (String) -> Unit) {
        BackgroundWork.run(
            task = { refreshCategoriesSync() },
            onSuccess = { onSuccess() },
            onError = onError
        )
    }

    fun refreshCatalogForCategory(categoryId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        BackgroundWork.run(
            task = { refreshCatalogForCategorySync(categoryId) },
            onSuccess = { onSuccess() },
            onError = onError
        )
    }

    fun refreshCatalogForCategories(
        categoryIds: Collection<String>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        BackgroundWork.run(
            task = { refreshCatalogForCategoriesSync(categoryIds) },
            onSuccess = { onSuccess() },
            onError = onError
        )
    }

    fun refreshCatalogForSearch(query: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        BackgroundWork.run(
            task = { refreshCatalogForSearchSync(query) },
            onSuccess = { onSuccess() },
            onError = onError
        )
    }

    fun refreshInventoryProducts(onSuccess: () -> Unit, onError: (String) -> Unit) {
        BackgroundWork.run(
            task = {
                supplierProductsCache = parseSupplierProducts(
                    refreshSupplierProductsArray(page = 1, limit = INVENTORY_PAGE_SIZE)
                ).toMutableList()
            },
            onSuccess = { onSuccess() },
            onError = onError
        )
    }

    fun refreshInventoryProductsPage(
        page: Int,
        onSuccess: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        BackgroundWork.run(
            task = {
                val products = parseSupplierProducts(
                    refreshSupplierProductsArray(page = page, limit = INVENTORY_PAGE_SIZE)
                )
                if (page <= 1) {
                    supplierProductsCache = products.toMutableList()
                } else {
                    val existingIds = supplierProductsCache.map { it.id }.toHashSet()
                    supplierProductsCache.addAll(products.filterNot { it.id in existingIds })
                }
                products.size >= INVENTORY_PAGE_SIZE
            },
            onSuccess = onSuccess,
            onError = onError
        )
    }

    fun refreshCatalog(onSuccess: () -> Unit, onError: (String) -> Unit) {
        BackgroundWork.run(
            task = { refreshCatalogSync(fetchAllPages = true) },
            onSuccess = { onSuccess() },
            onError = onError
        )
    }

    fun refreshOrders(onSuccess: () -> Unit, onError: (String) -> Unit) {
        BackgroundWork.run(
            task = { refreshOrdersSync() },
            onSuccess = { onSuccess() },
            onError = onError
        )
    }

    fun refreshOrderDetail(orderId: String, onSuccess: (SupplierOrder?) -> Unit, onError: (String) -> Unit) {
        BackgroundWork.run(
            task = { refreshOrderDetailSync(orderId) },
            onSuccess = onSuccess,
            onError = onError
        )
    }

    fun loadNotifications(
        onSuccess: (List<SupplierNotificationItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        BackgroundWork.run(
            task = {
                runCatching {
                    ApiClient.getDataArray(
                        endpoint = "supplier/notifications",
                        queryParams = mapOf(
                            "supplier_id" to AppSession.supplierId,
                            "page" to 1,
                            "limit" to 30
                        )
                    ).also { cacheString(CACHE_NOTIFICATIONS, it.toString()) }
                }.getOrElse { error ->
                    cachedNotificationsArray() ?: throw error
                }.let(::parseNotifications)
            },
            onSuccess = onSuccess,
            onError = onError
        )
    }

    private fun cachedNotificationsArray(): JSONArray? {
        val raw = cachedString(CACHE_NOTIFICATIONS)
        return if (raw.isNullOrBlank()) null else runCatching { JSONArray(raw) }.getOrNull()
    }

    private fun parseNotifications(array: JSONArray): List<SupplierNotificationItem> {
        return buildList {
            repeat(array.length()) { index ->
                val item = array.optJSONObject(index) ?: return@repeat
                add(
                    SupplierNotificationItem(
                        id = item.optInt("id"),
                        title = item.optString("title"),
                        message = item.optString("message"),
                        createdAtLabel = ApiFormatting.displayDateTime(item.optString("created_at")),
                        linkType = item.optString("link_type"),
                        linkValue = item.optString("link_value")
                    )
                )
            }
        }
    }

    fun refreshEarnings(onSuccess: () -> Unit, onError: (String) -> Unit) {
        BackgroundWork.run(
            task = { refreshEarningsSync() },
            onSuccess = { onSuccess() },
            onError = onError
        )
    }

    fun refreshMessages(onSuccess: () -> Unit, onError: (String) -> Unit) {
        BackgroundWork.run(
            task = { refreshMessagesSync() },
            onSuccess = { onSuccess() },
            onError = onError
        )
    }

    fun refreshProfile(onSuccess: () -> Unit, onError: (String) -> Unit) {
        BackgroundWork.run(
            task = { refreshProfileSync() },
            onSuccess = { onSuccess() },
            onError = onError
        )
    }

    fun getOrders(status: SupplierOrderStatus? = null): List<SupplierOrder> {
        return if (status == null) ordersCache.toList() else ordersCache.filter { it.status == status }
    }

    fun findOrder(orderId: String): SupplierOrder? {
        return ordersCache.find { order ->
            order.id.equals(orderId, ignoreCase = true) ||
                order.backendId.toString() == orderId
        }?.copy()
    }

    fun updateOrderStatus(
        orderId: String,
        status: SupplierOrderStatus,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        BackgroundWork.run(
            task = {
                val order = findOrder(orderId) ?: throw ApiException("Order not found.")
                ApiClient.postDataObject(
                    endpoint = "supplier/orders/status",
                    bodyParams = mapOf(
                        "supplier_id" to AppSession.supplierId,
                        "order_id" to order.backendId,
                        "status" to status.toApiValue()
                    )
                )
                refreshOrdersSync()
            },
            onSuccess = { onSuccess() },
            onError = onError
        )
    }

    fun getProducts(
        filter: SupplierProductFilter = SupplierProductFilter.ALL,
        query: String = ""
    ): List<SupplierProduct> {
        val normalizedQuery = query.trim().lowercase(Locale.getDefault())
        return supplierProductsCache.filter { product ->
            val matchesFilter = when (filter) {
                SupplierProductFilter.ALL -> true
                SupplierProductFilter.ACTIVE -> product.isActive
                SupplierProductFilter.INACTIVE -> !product.isActive
                SupplierProductFilter.LOW_STOCK -> product.stockState == SupplierStockState.LOW_STOCK
                SupplierProductFilter.OFFERS -> product.isOnOffer
            }
            val matchesQuery = normalizedQuery.isBlank() ||
                product.name.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
                product.categoryName.lowercase(Locale.getDefault()).contains(normalizedQuery)
            matchesFilter && matchesQuery
        }
    }

    fun getCatalogProducts(categoryId: String, query: String = ""): List<SupplierCatalogProduct> {
        val normalizedQuery = query.trim().lowercase(Locale.getDefault())
        val categoryName = normalizeLookupText(findCategory(categoryId)?.name.orEmpty())
        val source = synchronized(catalogCacheLock) { catalogProductsCache }
        val filtered = source.filter { product ->
            val normalizedProductCategory = normalizeLookupText(product.categoryName)
            val matchesCategory = product.categoryId == categoryId ||
                (categoryName.isNotBlank() &&
                    (
                        normalizedProductCategory == categoryName ||
                            normalizedProductCategory.contains(categoryName) ||
                            categoryName.contains(normalizedProductCategory)
                        )
                    )
            matchesCategory &&
                (
                    normalizedQuery.isBlank() ||
                        product.name.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
                        product.packaging.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
                        product.unitLabel.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
                        product.categoryName.lowercase(Locale.getDefault()).contains(normalizedQuery)
                    )
        }
        Log.d(
            TAG,
            "getCatalogProducts(categoryId=$categoryId, query='$query') -> ${filtered.size} items (categoryName=${findCategory(categoryId)?.name.orEmpty()})"
        )
        return filtered
    }

    fun hasFullCatalogProducts(): Boolean = synchronized(catalogCacheLock) { fullCatalogProductsCache.isNotEmpty() }

    fun searchCatalogProducts(query: String = ""): List<SupplierCatalogProduct> {
        val normalizedQuery = query.trim().lowercase(Locale.getDefault())
        if (normalizedQuery.isBlank()) return emptyList()
        val source = synchronized(catalogCacheLock) {
            fullCatalogProductsCache.ifEmpty { catalogProductsCache }
        }
        // A category-name search should return every product in that category,
        // even when a product's stored category label differs slightly.
        val matchingCategoryIds = getCategories(query)
            .map { it.id }
            .toSet()
        val filtered = source.filter { product ->
            product.name.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
                product.packaging.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
                product.unitLabel.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
                product.categoryName.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
                product.categoryId in matchingCategoryIds
        }
        Log.d(
            TAG,
            "searchCatalogProducts(query='$query') -> ${filtered.size} items from ${source.size}; matchingCategories=${matchingCategoryIds.size}"
        )
        return filtered
    }

    private fun normalizeLookupText(value: String): String {
        return value.trim().lowercase(Locale.getDefault()).replace(Regex("\\s+"), " ")
    }

    fun getCategories(query: String = ""): List<SupplierCategory> {
        val normalizedQuery = query.trim().lowercase(Locale.getDefault())
        return categoriesCache.filter { category ->
            normalizedQuery.isBlank() || category.name.lowercase(Locale.getDefault()).contains(normalizedQuery)
        }
    }

    fun findCategory(categoryId: String): SupplierCategory? = categoriesCache.find { it.id == categoryId }
    fun findCatalogProduct(productId: String): SupplierCatalogProduct? =
        synchronized(catalogCacheLock) { catalogProductsCache.find { it.id == productId } }
    fun findProduct(productId: String): SupplierProduct? = supplierProductsCache.find { it.id == productId }
    fun findConversation(conversationId: String): SupplierConversation? = conversationsCache.find { it.id == conversationId }
    fun findConversationForBuyer(vararg buyerNames: String): SupplierConversation? {
        val names = buyerNames
            .map { it.trim().lowercase(Locale.getDefault()) }
            .filter { it.isNotBlank() }
            .distinct()
        if (names.isEmpty()) return null
        return conversationsCache.firstOrNull { conversation ->
            val retailerName = conversation.retailerName.trim().lowercase(Locale.getDefault())
            names.any { name -> retailerName == name || retailerName.contains(name) || name.contains(retailerName) }
        }
    }
    fun getMessages(conversationId: String): List<SupplierChatMessage> = messagesCache[conversationId]?.toList().orEmpty()

    fun clearCachedState() {
        synchronized(catalogCacheLock) {
            categoriesCache = emptyList()
            catalogProductsCache = emptyList()
            fullCatalogProductsCache = emptyList()
        }
        supplierProductsCache = mutableListOf()
        ordersCache = mutableListOf()
        transactionsCache = emptyList()
        conversationsCache = mutableListOf()
        messagesCache.clear()
        dashboardStats = SupplierDashboardStats(0, 0, 0, 0)
        profile = SupplierProfile("", "", "", "", "", "", 0, 0)
        cachePrefs().edit().clear().apply()
    }

    fun getConversations(query: String = ""): List<SupplierConversation> {
        val normalizedQuery = query.trim().lowercase(Locale.getDefault())
        return conversationsCache.filter { conversation ->
            normalizedQuery.isBlank() ||
                conversation.retailerName.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
                conversation.lastMessage.lowercase(Locale.getDefault()).contains(normalizedQuery)
        }
    }

    fun loadConversation(
        conversationId: String,
        onSuccess: (List<SupplierChatMessage>) -> Unit,
        onError: (String) -> Unit
    ) {
        BackgroundWork.run(
            task = {
                val payload = ApiClient.getDataObject(
                    endpoint = "supplier/messages/thread",
                    queryParams = mapOf(
                        "supplier_id" to AppSession.supplierId,
                        "thread_id" to conversationId
                    )
                )
                val messages = parseMessages(payload.optJSONArray("messages"), conversationId)
                messagesCache[conversationId] = messages.toMutableList()
                messages
            },
            onSuccess = onSuccess,
            onError = onError
        )
    }

    fun sendMessage(
        conversationId: String,
        message: String,
        messageType: String = "text",
        voiceDataUrl: String? = null,
        voiceDuration: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        BackgroundWork.run(
            task = {
                val body = linkedMapOf<String, Any?>(
                    "supplier_id" to AppSession.supplierId,
                    "thread_id" to conversationId.toIntOrNull(),
                    "message_type" to messageType
                ).apply {
                    if (messageType == "voice") {
                        put("voice_data_url", voiceDataUrl)
                        put("voice_duration", voiceDuration)
                    } else {
                        put("message_body", message)
                    }
                }
                val payload = ApiClient.postDataObject(
                    endpoint = "supplier/messages/send",
                    bodyParams = body
                )
                val messages = parseMessages(payload.optJSONArray("messages"), conversationId)
                messagesCache[conversationId] = messages.toMutableList()
                conversationsCache = conversationsCache.map { conversation ->
                    if (conversation.id == conversationId) {
                        conversation.copy(
                            lastMessage = if (messageType == "voice") string(R.string.voice_message) else message,
                            timeLabel = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
                            unreadCount = 0
                        )
                    } else {
                        conversation
                    }
                }.toMutableList()
            },
            onSuccess = { onSuccess() },
            onError = onError
        )
    }

    fun getTransactions(period: SupplierEarningsPeriod): List<SupplierTransaction> {
        return when (period) {
            SupplierEarningsPeriod.ALL -> transactionsCache
            SupplierEarningsPeriod.THIS_MONTH -> transactionsCache.filter { it.period == SupplierEarningsPeriod.THIS_MONTH }
            SupplierEarningsPeriod.LAST_MONTH -> transactionsCache.filter { it.period == SupplierEarningsPeriod.LAST_MONTH }
        }
    }

    fun updateProfile(
        updatedProfile: SupplierProfile,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        BackgroundWork.run(
            task = {
                ApiClient.postDataObject(
                    endpoint = "supplier/profile/update",
                    bodyParams = mapOf(
                        "supplier_id" to AppSession.supplierId,
                        "business_name" to updatedProfile.businessName,
                        "owner_name" to updatedProfile.ownerName,
                        "phone" to updatedProfile.phoneNumber,
                        "city" to updatedProfile.city,
                        "address" to updatedProfile.businessAddress,
                        "minimum_order_quantity" to updatedProfile.minimumOrderQuantity,
                        "minimum_order_amount" to updatedProfile.minimumOrderAmountPkr,
                        "delivery_time" to updatedProfile.deliveryTime,
                        "payment_terms" to updatedProfile.paymentTerms,
                        "description" to updatedProfile.description,
                        "latitude" to updatedProfile.latitude,
                        "longitude" to updatedProfile.longitude
                    )
                )
                profile = updatedProfile
                cacheString(CACHE_PROFILE, profileToJson(updatedProfile).toString())
            },
            onSuccess = { onSuccess() },
            onError = onError
        )
    }

    fun setProductAvailability(
        productId: String,
        isActive: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        updateProduct(productId, onSuccess, onError) { product ->
            mapOf("status" to if (isActive) "active" else "draft")
        }
    }

    fun adjustStock(
        productId: String,
        delta: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        BackgroundWork.run(
            task = {
                val product = findProduct(productId) ?: throw ApiException("Product not found.")
                val updatedStock = (product.stock + delta).coerceAtLeast(0)
                val payload = ApiClient.postDataObject(
                    endpoint = "supplier/products/update",
                    bodyParams = linkedMapOf<String, Any?>(
                        "supplier_id" to AppSession.supplierId,
                        "listing_id" to product.id.toIntOrNull(),
                        "catalog_product_id" to product.catalogProductId.toIntOrNull(),
                        "price" to product.pricePkr,
                        "stock_quantity" to updatedStock,
                        "delivery_time" to product.deliveryDays,
                        "status" to if (product.isActive) "active" else "draft",
                        "min_order_qty" to profile.minimumOrderQuantity,
                        "min_order_amount" to profile.minimumOrderAmountPkr
                    )
                )
                val updatedProduct = parseSupplierProducts(JSONArray().put(payload)).firstOrNull()
                if (updatedProduct != null) {
                    supplierProductsCache.indexOfFirst { it.id == productId }
                        .takeIf { it >= 0 }
                        ?.let { index -> supplierProductsCache[index] = updatedProduct }
                } else {
                    product.stock = updatedStock
                }
                updateDashboardStats()
            },
            onSuccess = { onSuccess() },
            onError = onError
        )
    }

    fun applyLocalStockDelta(productId: String, delta: Int): Boolean {
        val product = findProduct(productId) ?: return false
        product.stock = (product.stock + delta).coerceAtLeast(0)
        updateDashboardStats()
        return true
    }

    fun addProduct(
        catalogProductId: String,
        pricePkr: Int,
        stock: Int?,
        deliveryDays: String?,
        imageDataUrl: String?,
        offerPricePkr: Int?,
        maximumOfferQuantity: Int?,
        isActive: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        BackgroundWork.run(
            task = {
                ApiClient.postDataObject(
                    endpoint = "supplier/products/create",
                    bodyParams = linkedMapOf<String, Any?>(
                        "supplier_id" to AppSession.supplierId,
                        "catalog_product_id" to catalogProductId.toIntOrNull(),
                        "price" to pricePkr,
                        "status" to if (isActive) "active" else "draft",
                        "min_order_qty" to profile.minimumOrderQuantity,
                        "min_order_amount" to profile.minimumOrderAmountPkr
                    ).apply {
                        stock?.let { put("stock_quantity", it) }
                        deliveryDays?.takeIf { it.isNotBlank() }?.let { put("delivery_time", it) }
                        imageDataUrl?.takeIf { it.isNotBlank() }?.let { put("image_data_url", it) }
                        offerPricePkr?.takeIf { it > 0 }?.let { put("offer_price", it) }
                        maximumOfferQuantity?.takeIf { it > 0 }?.let { put("maximum_quantity", it) }
                    }
                )
                refreshProductsSync()
            },
            onSuccess = { onSuccess() },
            onError = onError
        )
    }

    fun updateProductDetails(
        productId: String,
        catalogProductId: String,
        pricePkr: Int,
        stock: Int?,
        deliveryDays: String?,
        imageDataUrl: String?,
        offerPricePkr: Int?,
        maximumOfferQuantity: Int?,
        isActive: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        BackgroundWork.run(
            task = {
                val product = findProduct(productId) ?: throw ApiException("Product not found.")
                val body = linkedMapOf<String, Any?>(
                    "supplier_id" to AppSession.supplierId,
                    "listing_id" to product.id.toIntOrNull(),
                    "catalog_product_id" to catalogProductId.toIntOrNull(),
                    "price" to pricePkr,
                    "status" to if (isActive) "active" else "draft",
                    "min_order_qty" to profile.minimumOrderQuantity,
                    "min_order_amount" to profile.minimumOrderAmountPkr
                )
                stock?.let { body["stock_quantity"] = it }
                deliveryDays?.takeIf { it.isNotBlank() }?.let { body["delivery_time"] = it }
                imageDataUrl?.takeIf { it.isNotBlank() }?.let { body["image_data_url"] = it }

                ApiClient.postDataObject(
                    endpoint = "supplier/products/update",
                    bodyParams = body
                )

                if (offerPricePkr != null && offerPricePkr > 0) {
                    ApiClient.postDataObject(
                        endpoint = "supplier/offers/create",
                        bodyParams = mapOf(
                            "supplier_id" to AppSession.supplierId,
                            "listing_id" to product.id.toIntOrNull(),
                            "catalog_product_id" to catalogProductId.toIntOrNull(),
                            "title" to product.name,
                            "description" to string(R.string.supplier_special_offer_description),
                            "badge_label" to string(R.string.supplier_special_offer_badge),
                            "discount_label" to CurrencyFormatter.format(offerPricePkr),
                            "offer_price" to offerPricePkr,
                            "maximum_quantity" to maximumOfferQuantity?.takeIf { it > 0 }
                        )
                    )
                }

                refreshProductsSync()
            },
            onSuccess = { onSuccess() },
            onError = onError
        )
    }

    fun bulkUploadProducts(
        fileName: String,
        fileDataBase64: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        BackgroundWork.run(
            task = {
                val result = ApiClient.postDataObject(
                    endpoint = "supplier/products/bulk-upload",
                    bodyParams = mapOf(
                        "supplier_id" to AppSession.supplierId,
                        "file_name" to fileName,
                        "file_data_base64" to fileDataBase64
                    )
                )
                refreshProductsSync()
                val imported = result.optInt("imported_count")
                val created = result.optInt("created_count")
                val updated = result.optInt("updated_count")
                val errorCount = result.optInt("error_count")
                string(
                    R.string.supplier_bulk_upload_summary,
                    imported,
                    created,
                    updated,
                    errorCount
                )
            },
            onSuccess = onSuccess,
            onError = onError
        )
    }

    fun bulkUploadCatalogProducts(
        fileName: String,
        fileDataBase64: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        BackgroundWork.run(
            task = {
                val result = ApiClient.postDataObject(
                    endpoint = "supplier/catalog/bulk-upload",
                    bodyParams = mapOf(
                        "supplier_id" to AppSession.supplierId,
                        "file_name" to fileName,
                        "file_data_base64" to fileDataBase64
                    )
                )
                refreshProductsSync()
                val imported = result.optInt("imported_count")
                val created = result.optInt("created_count")
                val updated = result.optInt("updated_count")
                val errorCount = result.optInt("error_count")
                string(
                    R.string.supplier_bulk_catalog_upload_summary,
                    imported,
                    created,
                    updated,
                    errorCount
                )
            },
            onSuccess = onSuccess,
            onError = onError
        )
    }

    fun addProductOffer(
        productId: String,
        offerPricePkr: Int,
        maximumQuantity: Int?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        BackgroundWork.run(
            task = {
                val product = findProduct(productId) ?: throw ApiException("Product not found.")
                ApiClient.postDataObject(
                    endpoint = "supplier/offers/create",
                    bodyParams = mapOf(
                        "supplier_id" to AppSession.supplierId,
                        "listing_id" to product.id.toIntOrNull(),
                        "catalog_product_id" to product.catalogProductId.toIntOrNull(),
                        "title" to product.name,
                        "description" to string(R.string.supplier_special_offer_description),
                        "badge_label" to string(R.string.supplier_special_offer_badge),
                        "discount_label" to CurrencyFormatter.format(offerPricePkr),
                        "offer_price" to offerPricePkr,
                        "maximum_quantity" to maximumQuantity
                    )
                )
                product.isOnOffer = true
                product.offerPricePkr = offerPricePkr
                product.maximumOfferQuantity = maximumQuantity ?: 0
            },
            onSuccess = { onSuccess() },
            onError = onError
        )
    }

    private fun updateProduct(
        productId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        extras: (SupplierProduct) -> Map<String, Any>
    ) {
        BackgroundWork.run(
            task = {
                val product = findProduct(productId) ?: throw ApiException("Product not found.")
                val body = linkedMapOf<String, Any?>(
                    "supplier_id" to AppSession.supplierId,
                    "listing_id" to product.id.toIntOrNull(),
                    "catalog_product_id" to product.catalogProductId.toIntOrNull(),
                    "price" to product.pricePkr,
                    "stock_quantity" to product.stock,
                    "delivery_time" to product.deliveryDays,
                    "status" to if (product.isActive) "active" else "draft",
                    "min_order_qty" to profile.minimumOrderQuantity,
                    "min_order_amount" to profile.minimumOrderAmountPkr
                )
                extras(product).forEach { (key, value) -> body[key] = value }

                ApiClient.postDataObject(
                    endpoint = "supplier/products/update",
                    bodyParams = body
                )
                refreshProductsSync()
            },
            onSuccess = { onSuccess() },
            onError = onError
        )
    }

    private fun refreshProfileSync() {
        val payload = ApiClient.getDataObject(
            endpoint = "supplier/profile",
            queryParams = mapOf("supplier_id" to AppSession.supplierId)
        )
        val rawProfile = payload.toString()
        if (rawProfile != cachedString(CACHE_PROFILE) || !hasCachedProfile()) {
            profile = parseProfile(payload)
            cacheString(CACHE_PROFILE, rawProfile)
        }
    }

    private fun parseProfile(payload: JSONObject): SupplierProfile {
        return SupplierProfile(
            businessName = payload.optString("business_name"),
            ownerName = payload.optString("owner_name"),
            phoneNumber = payload.optString("phone"),
            emailAddress = payload.optString("email"),
            city = payload.optString("city"),
            businessAddress = payload.optString("address"),
            minimumOrderQuantity = payload.optInt("minimum_order_quantity"),
            minimumOrderAmountPkr = safeMoneyInt(payload.optDouble("minimum_order_amount")),
            deliveryTime = payload.optString("delivery_time"),
            paymentTerms = payload.optString("payment_terms"),
            description = payload.optString("description"),
            businessLicenseNumber = payload.optString("business_license_number"),
            status = payload.optString("status"),
            latitude = if (payload.isNull("latitude")) null else payload.optDouble("latitude"),
            longitude = if (payload.isNull("longitude")) null else payload.optDouble("longitude")
        )
    }

    private fun profileToJson(value: SupplierProfile): JSONObject {
        return JSONObject().apply {
            put("business_name", value.businessName)
            put("owner_name", value.ownerName)
            put("phone", value.phoneNumber)
            put("city", value.city)
            put("address", value.businessAddress)
            put("minimum_order_quantity", value.minimumOrderQuantity)
            put("minimum_order_amount", value.minimumOrderAmountPkr)
            put("delivery_time", value.deliveryTime)
            put("payment_terms", value.paymentTerms)
            put("description", value.description)
            put("business_license_number", value.businessLicenseNumber)
            put("status", value.status)
            put("latitude", value.latitude)
            put("longitude", value.longitude)
        }
    }

    private fun refreshProductsSync() {
        refreshCatalogSync()

        supplierProductsCache = parseSupplierProducts(
            refreshSupplierProductsArray()
        ).toMutableList()
    }

    private fun refreshSupplierProductsArray(page: Int? = null, limit: Int? = null): JSONArray {
        val queryParams = mutableMapOf<String, Any?>("supplier_id" to AppSession.supplierId)
        page?.let { queryParams["page"] = it.coerceAtLeast(1) }
        limit?.let { queryParams["limit"] = it.coerceAtLeast(1) }
        val productsArray = ApiClient.getDataArray(
                endpoint = "supplier/products",
                queryParams = queryParams
            )
        if (page == null || page <= 1) {
            val rawProducts = productsArray.toString()
            if (rawProducts != cachedString(CACHE_PRODUCTS)) {
                cacheString(CACHE_PRODUCTS, rawProducts)
            }
        }
        return productsArray
    }

    private fun refreshCatalogSync(fetchAllPages: Boolean = false) {
        val catalogArray = if (fetchAllPages) {
            fetchAllCatalogPages()
        } else {
            ApiClient.getDataArray("supplier/catalog")
        }
        Log.d(TAG, "refreshCatalogSync(fetchAllPages=$fetchAllPages) received ${catalogArray.length()} rows")
        val categoriesArray = runCatching {
            ApiClient.getDataArray("buyer/categories")
        }.getOrNull()
        val rawCatalog = catalogArray.toString()
        if (rawCatalog != cachedString(CACHE_CATALOG) || catalogProductsCache.isEmpty() || fullCatalogProductsCache.isEmpty()) {
            val parsedCatalog = parseCatalogProducts(catalogArray)
            synchronized(catalogCacheLock) {
                catalogProductsCache = parsedCatalog
                if (fetchAllPages) {
                    fullCatalogProductsCache = parsedCatalog
                }
            }
            cacheString(CACHE_CATALOG, rawCatalog)
            Log.d(TAG, "Catalog cache updated: ${catalogProductsCache.size} rows")
        }

        val parsedCategories = categoriesArray?.let(::parseCategories) ?: parseCatalogCategories(catalogArray)
        val rawCategories = categoriesArray?.toString() ?: parseCategoriesRaw(parsedCategories)
        if (rawCategories != cachedString(CACHE_CATEGORIES) || categoriesCache.isEmpty()) {
            categoriesCache = parsedCategories
            cacheString(CACHE_CATEGORIES, rawCategories)
            Log.d(TAG, "Category cache updated: ${categoriesCache.size} rows")
        }
    }

    private fun refreshCategoriesSync() {
        val categoriesArray = ApiClient.getDataArray("buyer/categories")
        val parsedCategories = parseCategories(categoriesArray)
        val rawCategories = parseCategoriesRaw(parsedCategories)
        if (rawCategories != cachedString(CACHE_CATEGORIES) || categoriesCache.isEmpty()) {
            categoriesCache = parsedCategories
            cacheString(CACHE_CATEGORIES, rawCategories)
            Log.d(TAG, "Category cache refreshed only: ${categoriesCache.size} rows")
        }
    }

    private fun refreshCatalogForCategorySync(categoryId: String) {
        refreshCatalogForCategoriesSync(listOf(categoryId))
    }

    private fun refreshCatalogForCategoriesSync(categoryIds: Collection<String>) {
        val normalizedCategoryIds = categoryIds
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        if (normalizedCategoryIds.isEmpty()) {
            throw ApiException("At least one category id is required.")
        }

        val catalogArray = JSONArray()
        val seenProductIds = linkedSetOf<String>()
        normalizedCategoryIds.forEach { categoryId ->
            val categoryProducts = fetchAllCatalogPages(categoryId)
            repeat(categoryProducts.length()) { index ->
                val product = categoryProducts.optJSONObject(index) ?: return@repeat
                val productId = product.optInt("id").toString()
                if (seenProductIds.add(productId)) {
                    catalogArray.put(product)
                }
            }
        }
        val rawCatalog = catalogArray.toString()
        synchronized(catalogCacheLock) {
            catalogProductsCache = parseCatalogProducts(catalogArray)
        }
        cacheString(CACHE_CATALOG, rawCatalog)
        Log.d(
            TAG,
            "Category catalogs refreshed: categoryIds=$normalizedCategoryIds rows=${catalogProductsCache.size}"
        )
    }

    private fun refreshCatalogForSearchSync(query: String) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) {
            throw ApiException("A search query is required.")
        }
        // The backend now filters by the search term itself (FULLTEXT index),
        // so a real search rarely needs more than a page. This cap just
        // guards against a very broad term still fanning out into hundreds of
        // sequential requests against a catalog with lacs/millions of rows.
        val catalogArray = fetchAllCatalogPages(searchQuery = normalizedQuery, maxPages = SEARCH_MAX_PAGES)
        val rawCatalog = catalogArray.toString()
        synchronized(catalogCacheLock) {
            catalogProductsCache = parseCatalogProducts(catalogArray)
        }
        cacheString(CACHE_CATALOG, rawCatalog)
        Log.d(TAG, "Catalog search refreshed: query='$normalizedQuery' rows=${catalogProductsCache.size}")
    }

    private fun fetchAllCatalogPages(
        categoryId: String? = null,
        searchQuery: String? = null,
        maxPages: Int = Int.MAX_VALUE
    ): JSONArray {
        val merged = JSONArray()
        val seenIds = linkedSetOf<String>()
        var page = 1
        val limit = 100
        while (page <= maxPages) {
            val queryParams = mutableMapOf<String, Any?>(
                "page" to page,
                "limit" to limit
            )
            if (!categoryId.isNullOrBlank()) {
                queryParams["category_id"] = categoryId.toIntOrNull() ?: 0
            }
            if (!searchQuery.isNullOrBlank()) {
                queryParams["search"] = searchQuery
            }
            val pageArray = ApiClient.getDataArray(
                endpoint = "supplier/catalog",
                queryParams = queryParams
            )
            var addedCount = 0
            repeat(pageArray.length()) { index ->
                val item = pageArray.optJSONObject(index) ?: return@repeat
                val id = item.optInt("id").toString()
                if (seenIds.add(id)) {
                    merged.put(item)
                    addedCount += 1
                }
            }
            if (pageArray.length() < limit || addedCount == 0) {
                break
            }
            page += 1
        }
        return merged
    }

    private fun restoreCachedCatalog(): Boolean {
        val catalogRaw = cachedString(CACHE_CATALOG)
        return if (catalogRaw.isNullOrBlank()) {
            false
        } else {
            runCatching {
                val catalogArray = JSONArray(catalogRaw)
                synchronized(catalogCacheLock) {
                    catalogProductsCache = parseCatalogProducts(catalogArray)
                    if (categoriesCache.isEmpty()) {
                        categoriesCache = parseCatalogCategories(catalogArray)
                    }
                }
                true
            }.getOrDefault(false)
        }
    }

    fun restoreCachedCategories(): Boolean {
        val categoriesRaw = cachedString(CACHE_CATEGORIES)
        return if (categoriesRaw.isNullOrBlank()) {
            false
        } else {
            runCatching {
                categoriesCache = parseCategories(JSONArray(categoriesRaw))
                true
            }.getOrDefault(false)
        }
    }

    private fun refreshOrdersSync() {
        val ordersArray = ApiClient.getDataArray(
            endpoint = "supplier/orders",
            queryParams = mapOf("supplier_id" to AppSession.supplierId)
        )
        val rawOrders = ordersArray.toString()
        if (rawOrders != cachedString(CACHE_ORDERS) || ordersCache.isEmpty()) {
            ordersCache = parseOrders(ordersArray).toMutableList()
            cacheString(CACHE_ORDERS, rawOrders)
        }
    }

    private fun refreshOrderDetailSync(orderId: String): SupplierOrder? {
        val cachedOrder = findOrder(orderId)
        val backendId = cachedOrder?.backendId ?: orderId.toIntOrNull() ?: return cachedOrder
        val payload = ApiClient.getDataObject(
            endpoint = "supplier/orders/detail",
            queryParams = mapOf(
                "supplier_id" to AppSession.supplierId,
                "order_id" to backendId
            )
        )
        if (payload.length() == 0) return cachedOrder
        val detailedOrder = parseOrders(JSONArray().put(payload)).firstOrNull() ?: return cachedOrder
        val existingIndex = ordersCache.indexOfFirst { order ->
            order.backendId == detailedOrder.backendId || order.id.equals(detailedOrder.id, ignoreCase = true)
        }
        if (existingIndex >= 0) {
            ordersCache[existingIndex] = detailedOrder
        } else {
            ordersCache.add(0, detailedOrder)
        }
        updateDashboardStats()
        return detailedOrder
    }

    private fun refreshEarningsSync() {
        val payload = ApiClient.getDataObject(
            endpoint = "supplier/earnings",
            queryParams = mapOf("supplier_id" to AppSession.supplierId)
        )
        val rawEarnings = payload.toString()
        if (rawEarnings != cachedString(CACHE_EARNINGS) || transactionsCache.isEmpty()) {
            transactionsCache = parseTransactions(payload.optJSONArray("transactions"))
            cacheString(CACHE_EARNINGS, rawEarnings)
        }
    }

    private fun refreshMessagesSync() {
        val conversationsArray = ApiClient.getDataArray(
            endpoint = "supplier/messages",
            queryParams = mapOf("supplier_id" to AppSession.supplierId)
        )
        val rawConversations = conversationsArray.toString()
        if (rawConversations != cachedString(CACHE_CONVERSATIONS) || conversationsCache.isEmpty()) {
            conversationsCache = parseConversations(conversationsArray).toMutableList()
            cacheString(CACHE_CONVERSATIONS, rawConversations)
        }
    }

    private fun restoreOrdersFromCache(): Boolean {
        val ordersRaw = cachedString(CACHE_ORDERS)
        return if (ordersRaw.isNullOrBlank()) {
            false
        } else {
            runCatching {
                ordersCache = parseOrders(JSONArray(ordersRaw)).toMutableList()
                true
            }.getOrDefault(false)
        }
    }

    private fun restoreCachedConversations(): Boolean {
        val conversationsRaw = cachedString(CACHE_CONVERSATIONS)
        return if (conversationsRaw.isNullOrBlank()) {
            false
        } else {
            runCatching {
                conversationsCache = parseConversations(JSONArray(conversationsRaw)).toMutableList()
                true
            }.getOrDefault(false)
        }
    }

    private fun updateDashboardStats() {
        dashboardStats = SupplierDashboardStats(
            todayOrders = ordersCache.count { isToday(it.orderDate) },
            pendingOrders = ordersCache.count { it.status == SupplierOrderStatus.PENDING },
            thisMonthRevenuePkr = ordersCache
                .filter { isCurrentMonth(it.orderDate) && it.status == SupplierOrderStatus.DELIVERED }
                .sumOf { it.amountPkr },
            totalProducts = supplierProductsCache.size
        )
    }

    private fun parseCatalogCategories(catalogArray: JSONArray): List<SupplierCategory> {
        val grouped = linkedMapOf<String, MutableList<JSONObject>>()
        repeat(catalogArray.length()) { index ->
            val item = catalogArray.optJSONObject(index) ?: return@repeat
            val categoryName = item.optString("category_name")
            grouped.getOrPut(categoryName) { mutableListOf() }.add(item)
        }

        return grouped.entries.mapIndexed { index, entry ->
            val count = entry.value.size
            SupplierCategory(
                id = entry.value.firstOrNull()?.optInt("category_id").toString(),
                name = entry.key,
                productCount = count,
                catalogCount = count,
                listingCount = 0,
                accentColorRes = ApiFormatting.accentColorRes("${entry.key}-$index")
            )
        }
    }

    private fun parseCategories(array: JSONArray): List<SupplierCategory> {
        return buildList {
            repeat(array.length()) { index ->
                val item = array.optJSONObject(index) ?: return@repeat
                val name = item.optString("name")
                    .ifBlank { item.optString("category_name") }
                if (name.isBlank()) return@repeat
                val catalogCount = item.optInt("catalog_count")
                val listingCount = item.optInt("listing_count")
                add(
                    SupplierCategory(
                        id = item.optInt("id").toString(),
                        name = name,
                        productCount = catalogCount,
                        catalogCount = catalogCount,
                        listingCount = listingCount,
                        accentColorRes = ApiFormatting.accentColorRes("$name-$index")
                    )
                )
            }
        }
    }

    private fun parseCategoriesRaw(categories: List<SupplierCategory>): String {
        return JSONArray().apply {
            categories.forEach { category ->
                put(
                    JSONObject().apply {
                        put("id", category.id)
                        put("name", category.name)
                        put("catalog_count", category.catalogCount)
                        put("listing_count", category.listingCount)
                    }
                )
            }
        }.toString()
    }

    private fun parseCatalogProducts(array: JSONArray): List<SupplierCatalogProduct> {
        return buildList {
            repeat(array.length()) { index ->
                val item = array.optJSONObject(index) ?: return@repeat
                add(
                    SupplierCatalogProduct(
                        id = item.optInt("id").toString(),
                        categoryId = item.optInt("category_id").toString(),
                        categoryName = item.optString("category_name"),
                        name = item.optString("name"),
                        unitLabel = item.optString("unit_type"),
                        packaging = item.optString("packaging"),
                        imageUrl = ApiConfig.resolveMediaUrl(item.optString("image_url")),
                        accentColorRes = ApiFormatting.accentColorRes("${item.optString("name")}-$index")
                    )
                )
            }
        }
    }

    private fun parseSupplierProducts(array: JSONArray): List<SupplierProduct> {
        return buildList {
            repeat(array.length()) { index ->
                val item = array.optJSONObject(index) ?: return@repeat
                add(
                    SupplierProduct(
                        id = item.optInt("id").toString(),
                        catalogProductId = item.optInt("catalog_product_id").toString(),
                        name = item.optString("name"),
                        categoryName = item.optString("category_name"),
                        unitLabel = item.optString("unit_type"),
                        imageUrl = ApiConfig.resolveMediaUrl(item.optString("image_url")),
                        pricePkr = safeMoneyInt(item.optDouble("price")),
                        stock = item.optInt("stock_quantity"),
                        deliveryDays = item.optString("delivery_time"),
                        isActive = item.optString("status").equals("active", true),
                        isOnOffer = item.optBoolean("is_on_offer", item.optInt("is_on_offer", 0) == 1),
                        offerPricePkr = safeMoneyInt(item.optDouble("offer_price")),
                        maximumOfferQuantity = item.optInt("maximum_quantity"),
                        accentColorRes = ApiFormatting.accentColorRes("${item.optString("name")}-$index")
                    )
                )
            }
        }
    }

    private fun parseOrders(array: JSONArray): List<SupplierOrder> {
        return buildList {
            repeat(array.length()) { index ->
                val item = array.optJSONObject(index) ?: return@repeat
                val orderItems = parseOrderItems(item.optJSONArray("items"))
                val notes = item.optString("notes")
                val buyerName = item.cleanString("buyer_name").ifBlank { notes.valueAfterLabel("Name") }
                val storeName = item.cleanString("store_name")
                val buyerCity = item.cleanString("buyer_city").ifBlank { item.cleanString("city") }
                val buyerAddress = item.cleanString("delivery_address").ifBlank {
                    item.cleanString("buyer_address").ifBlank { item.cleanString("address") }
                }.ifBlank { notes.valueAfterLabel("Address") }
                add(
                    SupplierOrder(
                        backendId = item.optInt("id"),
                        id = item.optString("order_number"),
                        retailerName = storeName.ifBlank { buyerName },
                        buyerName = buyerName.ifBlank { storeName },
                        buyerPhone = item.cleanString("buyer_phone").ifBlank { item.cleanString("phone") }
                            .ifBlank { notes.valueAfterLabel("Phone") },
                        deliveryAddress = listOf(buyerAddress, buyerCity)
                            .filter { it.isNotBlank() }
                            .distinct()
                            .joinToString(", "),
                        notes = notes.valueAfterLabel("Note"),
                        orderDate = ApiFormatting.displayDate(item.optString("order_date")),
                        orderDateTime = ApiFormatting.displayDateTime(
                            item.optString("created_at").ifBlank { item.optString("order_date") }
                        ),
                        expectedDeliveryDate = ApiFormatting.displayDate(item.optString("delivery_date")),
                        itemsCount = item.optInt("item_count", orderItems.size),
                        amountPkr = safeMoneyInt(item.optDouble("total_amount")),
                        status = item.optString("status").toSupplierStatus(),
                        items = orderItems
                    )
                )
            }
        }
    }

    private fun parseOrderItems(array: JSONArray?): List<SupplierOrderItem> {
        if (array == null) return emptyList()
        return buildList {
            repeat(array.length()) { index ->
                val item = array.optJSONObject(index) ?: return@repeat
                val supplierProductId = item.optString("supplier_product_id", item.optString("product_id"))
                val productName = item.optString("product_name")
                    .ifBlank { item.optString("catalog_name") }
                    .ifBlank { item.optString("supplier_product_name") }
                    .ifBlank { item.optString("item_name") }
                    .ifBlank { item.optString("name") }
                    .ifBlank { supplierProductId.takeIf { it.isNotBlank() }?.let { findProduct(it)?.name }.orEmpty() }
                    .ifBlank { string(R.string.order_item_unknown_product) }
                val quantity = item.optInt("quantity", item.optInt("qty"))
                val unitPrice = safeMoneyInt(item.optDouble("unit_price", item.optDouble("price")))
                val lineTotal = safeMoneyInt(item.optDouble("line_total", item.optDouble("total")))
                add(
                    SupplierOrderItem(
                        productName = productName,
                        unitLabel = item.optString("unit_label", item.optString("unit_type")),
                        packaging = item.optString("packaging").ifBlank {
                            item.optString("carton_packing")
                        },
                        quantity = quantity,
                        unitPricePkr = unitPrice,
                        lineTotalPkr = if (lineTotal > 0) lineTotal else unitPrice * quantity
                    )
                )
            }
        }
    }

    private fun String.valueAfterLabel(label: String): String {
        return lineSequence()
            .firstOrNull { it.startsWith("$label:", ignoreCase = true) }
            ?.substringAfter(':')
            ?.trim()
            .orEmpty()
    }

    private fun org.json.JSONObject.cleanString(name: String): String {
        return optString(name).takeUnless { it.equals("null", ignoreCase = true) }.orEmpty().trim()
    }

    private fun parseTransactions(array: JSONArray?): List<SupplierTransaction> {
        if (array == null) return emptyList()
        return buildList {
            repeat(array.length()) { index ->
                val item = array.optJSONObject(index) ?: return@repeat
                val displayDate = ApiFormatting.displayDate(item.optString("order_date"))
                add(
                    SupplierTransaction(
                        retailerName = item.optString("order_number"),
                        orderId = item.optString("order_number"),
                        date = displayDate,
                        amountPkr = safeMoneyInt(item.optDouble("total_amount")),
                        period = when {
                            isCurrentMonth(displayDate) -> SupplierEarningsPeriod.THIS_MONTH
                            isLastMonth(displayDate) -> SupplierEarningsPeriod.LAST_MONTH
                            else -> SupplierEarningsPeriod.ALL
                        }
                    )
                )
            }
        }
    }

    private fun parseConversations(array: JSONArray): List<SupplierConversation> {
        return buildList {
            repeat(array.length()) { index ->
                val item = array.optJSONObject(index) ?: return@repeat
                add(
                    SupplierConversation(
                        id = item.optInt("id").toString(),
                        retailerName = item.optString("store_name"),
                        lastMessage = item.optString("last_message"),
                        timeLabel = ApiFormatting.displayDateTime(item.optString("last_message_at")),
                        unreadCount = item.optInt("supplier_unread_count"),
                        accentColorRes = ApiFormatting.accentColorRes("${item.optString("store_name")}-$index")
                    )
                )
            }
        }
    }

    private fun parseMessages(array: JSONArray?, conversationId: String): List<SupplierChatMessage> {
        if (array == null) return emptyList()
        return buildList {
            repeat(array.length()) { index ->
                val item = array.optJSONObject(index) ?: return@repeat
                add(
                    SupplierChatMessage(
                        id = item.optInt("id").toString(),
                        conversationId = conversationId,
                        message = item.optString("message_body"),
                        timeLabel = ApiFormatting.displayTime(item.optString("created_at")),
                        isMine = item.optString("sender_type").equals("supplier", true),
                        type = when (item.optString("message_type").lowercase(Locale.getDefault())) {
                            "voice" -> com.hiskytechs.muhallinewuserapp.Models.ChatMessageType.VOICE
                            "media" -> com.hiskytechs.muhallinewuserapp.Models.ChatMessageType.MEDIA
                            else -> com.hiskytechs.muhallinewuserapp.Models.ChatMessageType.TEXT
                        },
                        voiceDuration = item.optString("voice_duration")
                    )
                )
            }
        }
    }

    private fun String.toSupplierStatus(): SupplierOrderStatus {
        return when (lowercase(Locale.getDefault())) {
            "pending" -> SupplierOrderStatus.PENDING
            "shipped" -> SupplierOrderStatus.SHIPPED
            "delivered" -> SupplierOrderStatus.DELIVERED
            else -> SupplierOrderStatus.CONFIRMED
        }
    }

    private fun SupplierOrderStatus.toApiValue(): String {
        return when (this) {
            SupplierOrderStatus.PENDING -> "pending"
            SupplierOrderStatus.CONFIRMED -> "processing"
            SupplierOrderStatus.SHIPPED -> "shipped"
            SupplierOrderStatus.DELIVERED -> "delivered"
        }
    }

    private fun isToday(displayDate: String): Boolean {
        return displayDate == ApiFormatting.displayDate(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
    }

    private fun isCurrentMonth(displayDate: String): Boolean {
        return monthMatch(displayDate, 0)
    }

    private fun isLastMonth(displayDate: String): Boolean {
        return monthMatch(displayDate, -1)
    }

    private fun monthMatch(displayDate: String, offset: Int): Boolean {
        return runCatching {
            val parsed = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).parse(displayDate) ?: return false
            val target = Calendar.getInstance().apply { add(Calendar.MONTH, offset) }
            val calendar = Calendar.getInstance().apply { time = parsed }
            calendar.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == target.get(Calendar.MONTH)
        }.getOrDefault(false)
    }

    private fun safeMoneyInt(value: Double): Int {
        return if (value.isFinite()) value.toInt() else 0
    }

    private fun cachePrefs() = MuhalliApplication.instance.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE)

    private fun cacheKey(name: String): String {
        return "supplier_${AppSession.supplierId}_$name"
    }

    private fun cachedString(name: String): String? {
        return cachePrefs().getString(cacheKey(name), null)
    }

    private fun cacheString(name: String, value: String) {
        val key = cacheKey(name)
        val prefs = cachePrefs()
        if (prefs.getString(key, null) != value) {
            prefs.edit().putString(key, value).apply()
        }
    }

    private fun string(@StringRes resId: Int, vararg args: Any): String {
        return MuhalliApplication.instance.getString(resId, *args)
    }
}
