package com.lanxin.prophet.model

data class LocationSnapshot(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val provider: String
)
