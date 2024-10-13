package com.paytondeveloper.checkintest

import androidx.compose.runtime.Composable

@Composable
expect fun MapComponent(
    pinLat: Float,
    pinLong: Float,
    destLat: Float,
    destLong: Float,
    radius: Double,
    markerTitle: String
)