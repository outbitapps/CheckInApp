package com.paytondeveloper.checkintest

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
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
import com.mmk.kmpnotifier.notification.NotifierManager
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun MainViewController(
    mapUIViewController: (
        markers: List<CIMapMarker>,
        history: List<CISessionLocationHistory>,
        dest: CILatLong,
        radius: Double,
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
    markers: List<CIMapMarker>,
    history: List<CISessionLocationHistory>,
    dest: CILatLong,
    radius: Double,
) -> UIViewController

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun MapComponent(
    markers: List<CIMapMarker>,
    history: List<CISessionLocationHistory>,
    dest: CILatLong,
    radius: Double,
) {
    UIKitViewController(
        factory = {
            mapViewController(markers, history, dest, radius)
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

@OptIn(ExperimentalForeignApi::class)
actual suspend fun bioAuthenticate(): Boolean = suspendCoroutine { continuation ->
    val context = LAContext()
    if (context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, null)) {
        context.evaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, "Verify your identity") { success, _ ->
            continuation.resume(success)
        }
    } else {
        // Biometric authentication not available
        continuation.resume(false)
    }
}
@Composable
actual fun AppTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        content = content
    )

}