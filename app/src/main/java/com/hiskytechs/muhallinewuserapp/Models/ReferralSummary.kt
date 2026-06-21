package com.hiskytechs.muhallinewuserapp.Models

data class ReferralClaim(
    val referredStoreName: String,
    val referredCity: String,
    val rewardAmount: Double,
    val createdAtLabel: String
)

data class ReferralSummary(
    val enabled: Boolean,
    val referralCode: String,
    val rewardAmount: Double,
    val refereeRewardAmount: Double,
    val totalClaims: Int,
    val earnedAmount: Double,
    val recentClaims: List<ReferralClaim>
)
