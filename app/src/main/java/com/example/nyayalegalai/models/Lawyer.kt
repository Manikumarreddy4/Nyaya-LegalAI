package com.example.nyayalegalai.models

data class Lawyer(
    val id: Int,
    val name: String,
    val specialization: String,
    val experience: String,
    val city: String,
    val rating: Double,
    val phone: String,
    val email: String
)
