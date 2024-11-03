package com.paytondeveloper.checkintest

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK
import android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import android.hardware.biometrics.BiometricPrompt
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.WindowCompat
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
import com.google.maps.android.compose.Polyline
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
        makeChannel(context = applicationContext, name = "Check In started", desc = "Get notified whenever someone starts a Check In", id = "cistarted")
        makeChannel(context = applicationContext, name = "Check In manually ended", desc = "Get notified whenever someone manually ends a Check In", id = "ciended")
        makeChannel(context = applicationContext, name = "Check In automatically ended", desc = "Get notified whenever a Check In ends as the host reaches their destination", id = "ciended_dest")
        makeChannel(context = applicationContext, name = "Check In - no progress", desc = "Get notified whenever somebody is not making progress toward their destination", id = "cinoprogress", importance = NotificationManager.IMPORTANCE_HIGH)
        val name = "Background updates"
        val descriptionText = "Notifications that show whenever Check In is updating in the background"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel("BGWORK", name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system.
        val notificationManager: NotificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
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
            .addTag("bgwork")
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

fun makeChannel(context: Context, name: String, desc: String, id: String, importance: Int = NotificationManager.IMPORTANCE_DEFAULT) {
    val name = name
    val descriptionText = desc
    val importance = NotificationManager.IMPORTANCE_DEFAULT
    val mChannel = NotificationChannel(id, name, importance)
    mChannel.description = descriptionText
    // Register the channel with the system. You can't change the importance
    // or other notification behaviors after this.
    val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(mChannel)
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
    history: List<CISessionLocationHistory>,
    dest: CILatLong,
    radius: Double,
) {
    Box(
        modifier = Modifier.fillMaxHeight(0.5f)
    ) {

        var firstCoords = markers.first()
        var lastCoords = markers.last()

        var midLat = (firstCoords.loc.latitude + lastCoords.loc.latitude) / 2
        var midLong = (firstCoords.loc.longitude + lastCoords.loc.longitude) / 2

        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(LatLng(midLat, midLong), 14f)
        }


        GoogleMap(
            cameraPositionState = cameraPositionState,
            onMapLoaded = {
                val bounds: LatLngBounds
                    val builder = LatLngBounds.Builder()
                    for (marker in markers) {
                        builder.include(LatLng(marker.loc.latitude, marker.loc.longitude))
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
                val coordinates = LatLng(it.loc.latitude, it.loc.longitude)
                val markerState = rememberMarkerState(position = coordinates)
                Marker(
                    state = markerState,
                    title = it.title,
                    snippet = it.subtitle,
                    flat = true
                )
            }
            Circle(
                center = LatLng(dest.latitude.toDouble(), dest.longitude.toDouble()),
                radius = radius,
                fillColor = Color(0.35294f, 0.78431f, 0.98039f, 0.5f),
                strokeColor = Color(0.35294f, 0.78431f, 0.98039f, 1.0f)
            )
            var points: MutableList<LatLng> = mutableListOf()
            history.forEach {
                Log.d("history", it.toString())
                points.add(LatLng(it.location.latitude, it.location.longitude))
            }
            Polyline(points)
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
    Log.d("batterylevel", "${batService.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)}")
    return (batService.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).toDouble() / 100)
}

class UploadWorker(appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun doWork(): Result {
        if (!isAppOnForeground(context = applicationContext) && !isWorkScheduled("bgwork")) {

            PrefSingleton.instance.Initialize(applicationContext)
            CIManager.shared
            while (CIManager.shared._uiState.value.loading) {
                //do nothing
            }
            var user = CIManager.shared._uiState.value.user
            if (user != null && CIManager.shared._uiState.value.families.any { it.currentSession != null && it.currentSession!!.host.id == user.id }) {
                notifyUser()
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

        }
        val workRequest = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInitialDelay(Duration.ofMinutes(5))
            .addTag("bgwork")
            .build()
        WorkManager.getInstance(applicationContext).enqueue(workRequest)
        removeNotification()
        return Result.success()
    }
    private fun notifyUser() {
        var builder = NotificationCompat.Builder(applicationContext, "BGWORK")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Check In session")
            .setContentText("Updating location...")
            .setPriority(NotificationCompat.PRIORITY_LOW)

        with(NotificationManagerCompat.from(applicationContext)) {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                // ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                // public fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                //                                        grantResults: IntArray)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.

                return@with
            }
            // notificationId is a unique int for each notification that you must define.
            notify(0, builder.build())
        }
    }
    private fun removeNotification() {
        with(NotificationManagerCompat.from(applicationContext)) {
            cancel(0)
        }
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
//            for (workInfo in workInfoList) {
//                val state: WorkInfo.State = workInfo.state
//                running = (state == WorkInfo.State.RUNNING) or (state == WorkInfo.State.ENQUEUED)
//            }
            if (workInfoList.count() > 1) running = true
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

@Composable
actual fun AppTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if(darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> MaterialTheme.colorScheme
        else -> MaterialTheme.colorScheme
    }

    val view = LocalView.current
    if(!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(
                window,
                view
            ).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )

}