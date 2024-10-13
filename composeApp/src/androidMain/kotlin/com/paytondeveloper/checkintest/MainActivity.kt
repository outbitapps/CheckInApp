package com.paytondeveloper.checkintest

import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.mmk.kmpnotifier.extensions.onCreateOrOnNewIntent
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import com.mmk.kmpnotifier.permission.permissionUtil
import com.paytondeveloper.checkintest.controllers.CIManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.Duration
import java.util.concurrent.TimeUnit


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
            .setInitialDelay(Duration.ofSeconds(30))
            .build()
        WorkManager.getInstance(applicationContext).enqueue(workRequest)
        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        NotifierManager.onCreateOrOnNewIntent(intent)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}

@Composable
actual fun MapComponent(
    pinLat: Float,
    pinLong: Float,
    markerTitle: String
) {
    Box(
        modifier = Modifier.fillMaxHeight(0.5f)
    ) {
        val coordinates = LatLng(pinLat.toDouble(), pinLong.toDouble())
        val markerState = rememberMarkerState(position = coordinates)

        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(coordinates, 17f)
        }
        GoogleMap(
            cameraPositionState = cameraPositionState
        ) {
            Marker(
                state = markerState,
                title = markerTitle,
                snippet = "Last Location",
                flat = true
            )
        }
    }
}

actual fun batteryLevel(): Double {
    val batService: BatteryManager = PrefSingleton.instance.mContext!!.getSystemService(BATTERY_SERVICE) as BatteryManager
    return (batService.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) / 100).toDouble()
}

class UploadWorker(appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun doWork(): Result {
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
        val workRequest = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInitialDelay(Duration.ofMinutes(2))
            .build()
        WorkManager.getInstance(applicationContext).enqueue(workRequest)
        return Result.success()
    }
}