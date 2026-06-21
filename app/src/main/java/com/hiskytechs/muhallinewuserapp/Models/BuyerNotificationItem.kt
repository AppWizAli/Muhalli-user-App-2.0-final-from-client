package com.hiskytechs.muhallinewuserapp.Models

data class BuyerNotificationItem(
    val id: Int,
    val title: String,
    val message: String,
    val createdAtLabel: String,
    val linkType: String,
    val linkValue: String
)
