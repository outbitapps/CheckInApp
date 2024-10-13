package com.paytondeveloper.checkintest

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitViewController
import androidx.compose.ui.window.ComposeUIViewController
import com.paytondeveloper.checkintest.controllers.CIManager
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import platform.UIKit.UIDevice
import platform.UIKit.UIViewController
import platform.UIKit.UIPasteboard

fun MainViewController(
    mapUIViewController: (
        pinLat: Float,
        pinLong: Float,
        destLat: Float,
        destLong: Float,
        radius: Double,
        markerTitle: String
    ) -> UIViewController
) = ComposeUIViewController {
    CIManager.shared
    while (CIManager.shared._uiState.value.loading) {
        //do nothing
    }
    var user = CIManager.shared._uiState.value.user
    CIManager.shared._uiState.value.families.forEach {
        if (it.currentSession != null && it.currentSession.host.id == user?.id) {
            GlobalScope.launch {
                CIManager.shared.updateSession(it)
            }
        }
    }
    GlobalScope.launch {
        CIManager.shared.refreshData()
    }
    mapViewController = mapUIViewController
    App()
}

lateinit var mapViewController: (
    pinLat: Float,
    pinLong: Float,
    destLat: Float,
    destLong: Float,
    radius: Double,
    markerTitle: String
) -> UIViewController

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun MapComponent(
    pinLat: Float,
    pinLong: Float,
    destLat: Float,
    destLong: Float,
    radius: Double,
    markerTitle: String
) {
    UIKitViewController(
        factory = {
            mapViewController(pinLat, pinLong, destLat, destLong, radius, markerTitle)
        },
        modifier = Modifier.fillMaxHeight(0.5f).fillMaxWidth()
    )
}


actual object ClipboardManager {
    actual fun copyToClipboard(text: String) {
        UIPasteboard.generalPasteboard.string = text
    }
}


actual fun batteryLevel(): Double {
    return UIDevice.currentDevice.batteryLevel.toDouble()
}