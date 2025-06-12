package com.example.vdk.model

data class Tracking(
    var id: String? = null,
    val timestamp: Float? = 0f,
    val state: Boolean = false,
)