package com.hiskytechs.muhallinewuserapp.network

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

object ApiConfig {
    // Laravel-compatible legacy API endpoint. Keep this path when Laravel replaces the PHP panel at /muhali.
    const val BASE_URL = "https://mhally.com/api/index.php"


    fun resolveMediaUrl(value: String?): String {
        val raw = value?.trim().orEmpty()
        if (raw.isBlank()) return ""
        if (raw.startsWith("http://") || raw.startsWith("https://")) return raw

        val apiUri = Uri.parse(BASE_URL)
        val origin = buildString {
            append(apiUri.scheme ?: "http") 
            append("://")
            append(apiUri.host ?: "10.0.2.2")
            apiUri.port.takeIf { it != -1 }?.let {
                append(":")
                append(it)
            }
        }
        return if (raw.startsWith("/")) "$origin$raw" else "$origin/$raw"
    }
}

object AppSession {
    private const val PREFS_NAME = "muhalli_session"
    private const val KEY_BUYER_ID = "buyer_id"
    private const val KEY_BUYER_TOKEN = "buyer_token"
    private const val KEY_SUPPLIER_ID = "supplier_id"
    private const val KEY_SUPPLIER_TOKEN = "supplier_token"
    private const val KEY_ACTIVE_ROLE = "active_role"

    const val ROLE_BUYER = "buyer"
    const val ROLE_SUPPLIER = "supplier"

    private var initialized = false
    private var appContext: Context? = null

    var buyerId: Int = 0
        private set
    var buyerToken: String = ""
        private set
    var supplierId: Int = 0
        private set
    var supplierToken: String = ""
        private set
    var activeRole: String = ""
        private set

    fun initialize(context: Context) {
        if (initialized) return
        appContext = context.applicationContext
        val prefs = preferences()
        buyerId = prefs.getInt(KEY_BUYER_ID, 0)
        buyerToken = prefs.getString(KEY_BUYER_TOKEN, "").orEmpty()
        supplierId = prefs.getInt(KEY_SUPPLIER_ID, 0)
        supplierToken = prefs.getString(KEY_SUPPLIER_TOKEN, "").orEmpty()
        activeRole = prefs.getString(KEY_ACTIVE_ROLE, "").orEmpty()
        initialized = true
    }

    fun saveBuyerSession(token: String, buyerId: Int) {
        this.buyerId = buyerId
        buyerToken = token
        activeRole = ROLE_BUYER
        persist()
    }

    fun saveSupplierSession(token: String, supplierId: Int) {
        this.supplierId = supplierId
        supplierToken = token
        activeRole = ROLE_SUPPLIER
        persist()
    }

    fun clearAll() {
        buyerId = 0
        buyerToken = ""
        supplierId = 0
        supplierToken = ""
        activeRole = ""
        persist()
    }

    fun hasBuyerSession(): Boolean = buyerId > 0 && buyerToken.isNotBlank()

    fun hasSupplierSession(): Boolean = supplierId > 0 && supplierToken.isNotBlank()

    fun authTokenForEndpoint(endpoint: String): String {
        return when {
            endpoint.startsWith("buyer/orders") ||
                endpoint.startsWith("buyer/notifications") ||
                endpoint.startsWith("buyer/referrals") ||
                endpoint.startsWith("buyer/chats") ||
                endpoint.startsWith("buyer/profile") ->
                buyerToken
            endpoint.startsWith("supplier/orders") ||
                endpoint.startsWith("supplier/notifications") ||
                endpoint.startsWith("supplier/chats") ||
                endpoint.startsWith("supplier/profile") ->
                supplierToken
            else -> ""
        }
    }

    private fun persist() {
        preferences().edit()
            .putInt(KEY_BUYER_ID, buyerId)
            .putString(KEY_BUYER_TOKEN, buyerToken)
            .putInt(KEY_SUPPLIER_ID, supplierId)
            .putString(KEY_SUPPLIER_TOKEN, supplierToken)
            .putString(KEY_ACTIVE_ROLE, activeRole)
            .apply()
    }

    private fun preferences() = requireNotNull(appContext) {
        "AppSession.initialize must be called before use."
    }.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

class ApiException(message: String) : Exception(message)

object BackgroundWork {
    private val executor = Executors.newFixedThreadPool(4)
    private val mainHandler = Handler(Looper.getMainLooper())

    fun <T> run(
        task: () -> T,
        onSuccess: (T) -> Unit,
        onError: (String) -> Unit
    ) {
        executor.execute {
            try {
                val result = task()
                mainHandler.post { onSuccess(result) }
            } catch (throwable: Throwable) {
                val message = throwable.message?.takeIf { it.isNotBlank() } ?: "Something went wrong."
                mainHandler.post { onError(message) }
            }
        }
    }
}

object ApiClient {
    private const val DEBUG_TAG = "MH_API_DEBUG"

    fun getDataObject(endpoint: String, queryParams: Map<String, Any?> = emptyMap()): JSONObject {
        val response = requestJson(
            method = "GET",
            endpoint = endpoint,
            queryParams = queryParams
        )
        val data = response.opt("data")
        return data as? JSONObject ?: JSONObject()
    }

    fun getDataArray(endpoint: String, queryParams: Map<String, Any?> = emptyMap()): JSONArray {
        val response = requestJson(
            method = "GET",
            endpoint = endpoint,
            queryParams = queryParams
        )
        val data = response.opt("data")
        return data as? JSONArray ?: JSONArray()
    }

    fun postDataObject(endpoint: String, bodyParams: Map<String, Any?> = emptyMap()): JSONObject {
        val response = requestJson(
            method = "POST",
            endpoint = endpoint,
            bodyParams = bodyParams
        )
        val data = response.opt("data")
        return data as? JSONObject ?: JSONObject()
    }

    private fun requestJson(
        method: String,
        endpoint: String,
        queryParams: Map<String, Any?> = emptyMap(),
        bodyParams: Map<String, Any?> = emptyMap()
    ): JSONObject {
        val url = URL(buildUrl(endpoint, queryParams))
        val authToken = AppSession.authTokenForEndpoint(endpoint)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 8000
            readTimeout = 10000
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Cache-Control", "no-cache")
            setRequestProperty("Pragma", "no-cache")
            authToken.takeIf { it.isNotBlank() }?.let {
                setRequestProperty("Authorization", "Bearer $it")
            }
            doInput = true
        }

        if (method == "POST") {
            connection.doOutput = true
            connection.outputStream.use { output ->
                output.write(mapToJsonObject(bodyParams).toString().toByteArray(StandardCharsets.UTF_8))
            }
        }

        return try {
            val statusCode = connection.responseCode
            val body = readBody(
                if (statusCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream ?: connection.inputStream
                }
            )

            val json = if (body.isBlank()) JSONObject() else JSONObject(body)
            if (endpoint == "buyer/products" || endpoint == "buyer/suppliers" || endpoint == "buyer/home") {
                val dataValue = json.opt("data")
                val dataSize = when (dataValue) {
                    is JSONArray -> dataValue.length()
                    is JSONObject -> dataValue.length()
                    else -> -1
                }
                Log.e(
                    DEBUG_TAG,
                    "endpoint=$endpoint status=$statusCode authUsed=${authToken.isNotBlank()} url=${url} bodyLen=${body.length} dataType=${dataValue?.javaClass?.simpleName ?: "null"} dataSize=$dataSize bodyHead=${body.take(180)}"
                )
            }
            if (!json.optBoolean("success", statusCode in 200..299)) {
                throw ApiException(json.optString("message", "Request failed."))
            }
            json
        } finally {
            connection.disconnect()
        }
    }

    private fun buildUrl(endpoint: String, queryParams: Map<String, Any?>): String {
        val uriBuilder = Uri.parse(ApiConfig.BASE_URL).buildUpon()
            .appendQueryParameter("endpoint", endpoint)

        queryParams.forEach { (key, value) ->
            val stringValue = value?.toString()
            if (!stringValue.isNullOrBlank()) {
                uriBuilder.appendQueryParameter(key, stringValue)
            }
        }

        return uriBuilder.build().toString()
    }

    private fun readBody(stream: InputStream?): String {
        if (stream == null) return ""
        return BufferedReader(InputStreamReader(stream)).use { reader ->
            buildString {
                var line = reader.readLine()
                while (line != null) {
                    append(line)
                    line = reader.readLine()
                }
            }
        }
    }

    private fun mapToJsonObject(values: Map<String, Any?>): JSONObject {
        val json = JSONObject()
        values.forEach { (key, value) ->
            json.put(key, toJsonValue(value))
        }
        return json
    }

    private fun toJsonValue(value: Any?): Any {
        return when (value) {
            null -> JSONObject.NULL
            is JSONObject -> value
            is JSONArray -> value
            is Map<*, *> -> {
                val nested = JSONObject()
                value.forEach { (nestedKey, nestedValue) ->
                    if (nestedKey != null) {
                        nested.put(nestedKey.toString(), toJsonValue(nestedValue))
                    }
                }
                nested
            }
            is Iterable<*> -> {
                val array = JSONArray()
                value.forEach { item -> array.put(toJsonValue(item)) }
                array
            }
            is Array<*> -> {
                val array = JSONArray()
                value.forEach { item -> array.put(toJsonValue(item)) }
                array
            }
            else -> value
        }
    }
}
