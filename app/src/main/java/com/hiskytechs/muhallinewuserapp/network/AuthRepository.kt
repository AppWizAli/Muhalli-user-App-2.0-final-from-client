package com.hiskytechs.muhallinewuserapp.network

import org.json.JSONObject

data class OtpRequestResult(
    val phone: String,
    val expiresAt: String
)

data class AuthResult(
    val role: String,
    val token: String,
    val userId: Int,
    val displayName: String
)

object AuthRepository {
    fun requestOtp(
        role: String,
        purpose: String,
        payload: Map<String, Any?>
    ): OtpRequestResult {
        val data = ApiClient.postDataObject(
            endpoint = "auth/request-otp",
            bodyParams = payload + mapOf(
                "role" to role,
                "purpose" to purpose
            )
        )

        return OtpRequestResult(
            phone = data.optString("phone"),
            expiresAt = data.optString("expires_at")
        )
    }

    fun verifyOtp(
        role: String,
        purpose: String,
        phone: String,
        code: String
    ): AuthResult {
        val data = ApiClient.postDataObject(
            endpoint = "auth/verify-otp",
            bodyParams = mapOf(
                "role" to role,
                "purpose" to purpose,
                "phone" to phone,
                "code" to code
            )
        )

        return if (role == AppSession.ROLE_BUYER) {
            val buyer = data.optJSONObject("buyer") ?: JSONObject()
            val token = data.optString("token")
            val buyerId = data.optInt("buyer_id", buyer.optInt("id"))
            AppSession.saveBuyerSession(token = token, buyerId = buyerId)
            AuthResult(
                role = role,
                token = token,
                userId = buyerId,
                displayName = buyer.optString("store_name").ifBlank { buyer.optString("buyer_name") }
            )
        } else {
            val supplier = data.optJSONObject("supplier") ?: JSONObject()
            val token = data.optString("token")
            val supplierId = data.optInt("supplier_id", supplier.optInt("id"))
            AppSession.saveSupplierSession(token = token, supplierId = supplierId)
            AuthResult(
                role = role,
                token = token,
                userId = supplierId,
                displayName = supplier.optString("business_name").ifBlank { supplier.optString("owner_name") }
            )
        }
    }
}
