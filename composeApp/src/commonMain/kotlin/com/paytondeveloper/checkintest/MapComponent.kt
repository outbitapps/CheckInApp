package com.paytondeveloper.checkintest

import androidx.compose.runtime.Composable

@Composable
expect fun MapComponent(
    markers: List<CIMapMarker>,
    history: List<CISessionLocationHistory>,
    dest: CILatLong,
    radius: Double,
)

data class CIMapMarker(
    val loc: CILatLong,
    val title: String,
    val subtitle: String
)