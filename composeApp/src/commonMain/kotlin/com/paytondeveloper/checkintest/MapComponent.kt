package com.paytondeveloper.checkintest

import androidx.compose.runtime.Composable

@Composable
expect fun MapComponent(
    pinLat: Float,
    pinLong: Float,
    markerTitle: String
)