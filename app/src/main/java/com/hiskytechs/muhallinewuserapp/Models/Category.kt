package com.hiskytechs.muhallinewuserapp.Models

data class Category(
    val id: Int = 0,
    val name: String,
    val productCount: String,
    val iconResId: Int,
    val backgroundColor: String,
    val description: String = "",
    val imageUrl: String = ""
)
