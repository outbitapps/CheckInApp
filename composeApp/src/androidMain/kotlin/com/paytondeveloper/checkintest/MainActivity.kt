package com.paytondeveloper.checkintest

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.content.Intent
import android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK
import android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import android.hardware.biometrics.BiometricPrompt
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.mmk.kmpnotifier.extensions.onCreateOrOnNewIntent
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import com.mmk.kmpnotifier.permission.permissionUtil
import com.paytondeveloper.checkintest.controllers.CIManager
import dev.theolm.rinku.compose.ext.Rinku
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.time.Duration
import java.util.concurrent.ExecutionException
import kotlin.coroutines.resume


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PrefSingleton.instance.Initialize(applicationContext)
        NotifierManager.onCreateOrOnNewIntent(intent)
        NotifierManager.initialize(NotificationPlatformConfiguration.Android(
            notificationIconResId = R.drawable.ic_launcher_foreground,
            showPushNotification = true
        ))
        val permissionUtil by permissionUtil()
        permissionUtil.askNotificationPermission()
        CIManager.shared
        while (CIManager.shared._uiState.value.loading) {
            //do nothing
        }
        GlobalScope.launch {
            NotifierManager.getPushNotifier().getToken()
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
        val workRequest = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInitialDelay(Duration.ofMinutes(5))
            .build()
        WorkManager.getInstance(applicationContext).enqueue(workRequest)
        setContent {
            Rinku {
                App()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        NotifierManager.onCreateOrOnNewIntent(intent)
    }
}
actual object ClipboardManager {
    actual fun copyToClipboard(text: String) {
        val clipboard = PrefSingleton.instance.mContext!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
//        val clip = ClipData.newPlainText("Check In join link", text)
        val clip = ClipData.newPlainText("Join", text)
        clipboard.setPrimaryClip(clip)

    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}

@Composable
actual fun MapComponent(
    markers: List<CIMapMarker>,
    destLat: Float,
    destLong: Float,
    radius: Double,
) {
    Box(
        modifier = Modifier.fillMaxHeight(0.5f)
    ) {

        var firstCoords = markers.first()
        var lastCoords = markers.last()

        var midLat = (firstCoords.lat + lastCoords.lat) / 2
        var midLong = (firstCoords.long + lastCoords.long) / 2

        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(LatLng(midLat, midLong), 14f)
        }


        GoogleMap(
            cameraPositionState = cameraPositionState,
            onMapLoaded = {
                val bounds: LatLngBounds
                    val builder = LatLngBounds.Builder()
                    for (marker in markers) {
                        builder.include(LatLng(marker.lat, marker.long))
                    }
                    bounds = builder.build()

                val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 100)
                GlobalScope.launch {
                    withContext(Dispatchers.Main) {
                            if (markers.count() > 1) {
                                cameraPositionState.animate(cameraUpdate, 500)
                            }
                    }
                }
            }
        ) {
            markers.forEach {
                val coordinates = LatLng(it.lat, it.long)
                val markerState = rememberMarkerState(position = coordinates)
                Marker(
                    state = markerState,
                    title = it.title,
                    snippet = it.subtitle,
                    flat = true
                )
            }
            Circle(
                center = LatLng(destLat.toDouble(), destLong.toDouble()),
                radius = radius,
                fillColor = Color(0.35294f, 0.78431f, 0.98039f, 0.5f),
                strokeColor = Color(0.35294f, 0.78431f, 0.98039f, 1.0f)
            )
        }
    }
}

const val r_earth = 6378137

fun latitudeFromCenterAndRadius(center: Double, radius: Double): Double {
//    new_latitude  = latitude  + (dy / r_earth) * (180 / pi);
//    new_longitude = longitude + (dx / r_earth) * (180 / pi) / cos(latitude * pi/180);
    return center + (radius / r_earth) * (180 / Math.PI)
}

fun longitudeFromCenterAndRadius(center: Double, latitude: Double, radius: Double): Double {
    return center + (radius / r_earth) * (180 / Math.PI) / Math.cos(latitude * (Math.PI/180))
}

actual fun batteryLevel(): Double {
    val batService: BatteryManager = PrefSingleton.instance.mContext!!.getSystemService(BATTERY_SERVICE) as BatteryManager
    return (batService.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) / 100).toDouble()
}

class UploadWorker(appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun doWork(): Result {
        if (!isAppOnForeground(context = applicationContext) && !isWorkScheduled("SessionUpdateWorker")) {
            PrefSingleton.instance.Initialize(applicationContext)
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
        }
        val workRequest = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInitialDelay(Duration.ofMinutes(2))
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniqueWork("SessionUpdateWorker", ExistingWorkPolicy.REPLACE, workRequest)
        return Result.success()
    }
    private fun isAppOnForeground(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        val packageName = context.packageName
        for (appProcess in appProcesses) {
            if (appProcess.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName == packageName) {
                return true
            }
        }
        return false
    }
    private fun isWorkScheduled(tag: String): Boolean {
        val instance = WorkManager.getInstance()
        val statuses = instance.getWorkInfosByTag(tag)
        try {
            var running = false
            val workInfoList = statuses.get()
            for (workInfo in workInfoList) {
                val state: WorkInfo.State = workInfo.state
                running = (state == WorkInfo.State.RUNNING) or (state == WorkInfo.State.ENQUEUED)
            }
            return running
        } catch (e: ExecutionException) {
            e.printStackTrace()
            return false
        } catch (e: InterruptedException) {
            e.printStackTrace()
            return false
        }
    }
}


    @OptIn(InternalCoroutinesApi::class)
    actual suspend fun bioAuthenticate(): Boolean = suspendCancellableCoroutine { continuation ->
//        val biometricPrompt = BiometricPrompt(
//            context,
//            { _, result ->
//                continuation.resume(result == BiometricPrompt.AuthenticationResult.AUTHENTICATED)
//            }
//        )
        val promptInfo = BiometricPrompt.Builder(PrefSingleton.instance.mContext!!)
            .setTitle("Verify")
            .setSubtitle("Verify your identity")
            .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            .build()
        promptInfo.authenticate(
            CancellationSignal(), PrefSingleton.instance.mContext!!.mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int,
                                                   errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
//                    continuation.resume(false)
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    continuation.resume(true)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    continuation.resume(false)
                }
            })
    }