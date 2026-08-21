package com.example.nyayalegalai.models

data class SavedAccount(
    val uid: String = "",
    val email: String,
    val name: String,
    val role: String, // "user" or "lawyer"
    val password: String? = null,
    val lastLoginTime: Long,
    val profileImageUrl: String? = null
)
