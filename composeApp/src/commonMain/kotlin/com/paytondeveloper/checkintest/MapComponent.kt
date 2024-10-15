package com.paytondeveloper.checkintest

import androidx.compose.runtime.Composable

@Composable
expect fun MapComponent(
    markers: List<CIMapMarker>,
    destLat: Float,
    destLong: Float,
    radius: Double,
)

data class CIMapMarker(
    val lat: Double,
    val long: Double,
    val title: String,
    val subtitle: String
)