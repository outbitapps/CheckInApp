package com.paytondeveloper.checkintest.controllers

import androidx.lifecycle.ViewModel
import com.mmk.kmpnotifier.notification.NotifierManager
import com.paytondeveloper.checkintest.AppInfo
import com.paytondeveloper.checkintest.CIFamily
import com.paytondeveloper.checkintest.CILatLong
import com.paytondeveloper.checkintest.CISession
import com.paytondeveloper.checkintest.OBUser
import com.paytondeveloper.checkintest.batteryLevel
import dev.jordond.compass.Priority
import dev.jordond.compass.geolocation.Geolocator
import dev.jordond.compass.geolocation.GeolocatorResult
import dev.jordond.compass.geolocation.mobile
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.lighthousegames.logging.logging
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(DelicateCoroutinesApi::class)
public class CIManager: ViewModel() {
    val log = logging("CIManager")
    var baseURL = "http://check.paytondev.cloud"
    val _uiState = MutableStateFlow(AuthControllerState(user = null))
    val uiState: StateFlow<AuthControllerState> = _uiState.asStateFlow()
    var token: String
        get() {
            return AppInfo.settings.getString("token", defaultValue = "")
        }
        set(value) {
            AppInfo.settings.putString("token", value)
        }
    var geolocator = Geolocator.mobile()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    init {

        GlobalScope.launch {
            try {
                refreshData()
            } catch (e: Exception) {
                log.e(msg = {
                    "err refreshing: $e"
                })
            }
        }
    }
    suspend fun endSession(family: CIFamily) {
        var request = HttpRequestBuilder()
        request.url("$baseURL/family/endsession/${family.id}")
        request.withToken(token)
        AppInfo.httpClient.post(request)
    }

    suspend fun joinFamily(joinToken: String) {
        var request = HttpRequestBuilder()
        request.url("$baseURL/family/join/$joinToken")
        request.withToken(token)
        AppInfo.httpClient.post(request)
        refreshData()
    }

    suspend fun refreshData() {
        _uiState.update {
            _uiState.value.copy(
                loading = true
            )
        }
//        var request = Request.Builder().url("${_uiState.value.baseURL}/users/fetch-user")
//            .method("GET", body = null)
//            .addHeader("Authorization", "Bearer ${_uiState.value.authToken}")
//            .build()
        var request = HttpRequestBuilder()
        request.url("$baseURL/users/get")
        request.withToken(token)

        try {
            var response = AppInfo.httpClient.get(request)
            val user = response.body<OBUser>()
            var families: MutableList<CIFamily> = mutableListOf()
            user.familyIDs.forEach {
                val family = fetchFamily(it)
                families.add(family)
                log.d(tag = "sessionid") { family.currentSession?.id }
            }
            _uiState.update {
                _uiState.value.copy(
                    user = user,
                    loading = false,
                    families = families
                )
            }
            log.d { AppInfo.settings.getStringOrNull("pushtoken") }
            val pushToken = NotifierManager.getPushNotifier().getToken()
            log.d { "pushtoken $pushToken" }
            pushToken?.let {
                updatePNSToken(pushToken)
            }
        } catch (e:Exception) {
            log.e(msg = {"error refresing: ${e}"})
            _uiState.update {
                _uiState.value.copy(
                    loading = false
                )
            }
        }
    }

    suspend fun fetchFamily(id: String): CIFamily {
        var request = HttpRequestBuilder()
        request.url("$baseURL/family/get/$id")
        request.withToken(token)
        var response = AppInfo.httpClient.get(request)
        return response.body()
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun getSessionData(dest: CILatLong, radius: Double, placeName: String?): CISession {
        val currentBatteryLevel = batteryLevel()
        log.d("batterylevel", {currentBatteryLevel})
        var currentLat = 0.0
        var currentLong = 0.0
        var result = geolocator.current(priority = Priority.HighAccuracy)
        when (result) {
            is GeolocatorResult.Success -> {
                // Do something with result.location
                currentLat = result.data.coordinates.latitude
                currentLong = result.data.coordinates.longitude
            }
            is GeolocatorResult.Error -> {
//                return@startSession
            }
        }
        var session = CISession(
            id = Uuid.random().toString(),
            host = _uiState.value.user!!,
            batteryLevel = currentBatteryLevel,
            destination = dest,
            started = Clock.System.now(),
            lastUpdate = Clock.System.now(),
            location = CILatLong(longitude = currentLong, latitude = currentLat),
            radius = radius,
            distance = 0.0,
            placeName = placeName,
            history = listOf()
        )
        return session
    }

    suspend fun startSession(family: CIFamily, dest: CILatLong, radius: Double, placeName: String?) {
        val session = getSessionData(dest, radius, placeName = placeName)
        var request = HttpRequestBuilder()
        request.url("$baseURL/family/startsession/${family.id}")
        request.contentType(ContentType.Application.Json)
        request.setBody(session)
        request.withToken(token)
        log.d { "starting session" }
        try {
            var res = AppInfo.httpClient.post(request)
            log.d { res.status }
        } catch (e: Exception){
            log.e { "error starting session: $e" }
        }
    }

    suspend fun getInviteLink(family: CIFamily): String {
        var request = HttpRequestBuilder()
        request.url("$baseURL/family/invite/${family.id}")
        request.withToken(token)
        var res = AppInfo.httpClient.post(request)
        log.d { res.body<String>() }
        return res.body<String>()
    }

    suspend fun updateSession(family: CIFamily) {
        val oldSession = family.currentSession!!
        var newSession = getSessionData(oldSession.destination, radius = oldSession.radius, placeName = oldSession.placeName)
        newSession.id = oldSession.id
        newSession.started = oldSession.started
        newSession.distance = oldSession.distance
        newSession.history = oldSession.history
        var request = HttpRequestBuilder()
        request.url("$baseURL/family/updatesession/${family.id}")
        request.contentType(ContentType.Application.Json)
        request.setBody(newSession)
        request.withToken(token)
        try {
            AppInfo.httpClient.post(request)
        } catch (e: Exception) {
            log.e { "error updating session: $e" }
        }
    }

    suspend fun signUp(username: String, email: String, password: String): SQSignUpResponse {
//        var requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), content = data)

//        var request = Request.Builder()
//            .url("${_uiState.value.baseURL}/users/signup")
//            .post(requestBody)
//            .build()
        var request = HttpRequestBuilder()
        request.url("$baseURL/users/signup")
        request.contentType(ContentType.Application.Json)
        request.setBody(UserSignup(email, username, password))

        log.d("signup", { "${request.body}" })

        try {
            var response = AppInfo.httpClient.post(request)
            if (response.status == HttpStatusCode.Conflict) {
                return SQSignUpResponse.ALREADY_EXISTS
            }
            response.body<NewSession>().let { session ->
                token = session.token
                _uiState.update {
                    _uiState.value.copy(
                        user = session.user
                    )
                }
                log.d("signup", { "username: ${session.user.username}" })
                return SQSignUpResponse.SUCCESS

            }

        } catch (e: Exception) {

            log.e(msg = {"error signing up: ${e}"})
        }
        return SQSignUpResponse.NO_CONNECTION
    }
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun signIn(email: String, password: String): SQSignUpResponse {
        log.d("signin"
        ) {
            Base64.encode(
                "${email.lowercase()}:${password}".encodeToByteArray(),
            )
        }
//        var request = Request.Builder()
//            .url("${_uiState.value.baseURL}/users/login")
//            .method("PUT", RequestBody.create("application/json".toMediaTypeOrNull(), ""))
//
//            .addHeader("Authorization", "Basic ${Base64.encodeToString("${email.lowercase()}:${password}".encodeToByteArray(), Base64.NO_WRAP)}")
//            .build()
        var request = HttpRequestBuilder()
        request.url("$baseURL/users/login")
        request.setBody("")
        request.headers["Authorization"] = "Basic ${Base64.encode("${email.lowercase()}:${password}".encodeToByteArray())}"

        try {
            val response = AppInfo.httpClient.put(request)
            if (response.status == HttpStatusCode.NotFound) {
                return SQSignUpResponse.INCORRECT_PASSWORD
            }
            if (response.status == HttpStatusCode.Unauthorized) {
                return SQSignUpResponse.INCORRECT_PASSWORD
            }
            response.body<NewSession>().let { tokenResponse ->
//                storeToken(tokenResponse.token)
                token = tokenResponse.token
                _uiState.update {
                    _uiState.value.copy(
                        user = tokenResponse.user
                    )
                }
                log.d("signin") { "username ${tokenResponse.user.username}" }
                return SQSignUpResponse.SUCCESS
            }

        } catch (e: Exception) {

            log.e(msg = { "err signup $e"})
        }
        return SQSignUpResponse.NO_CONNECTION
    }
    suspend fun sendPasswordResetEmail(email: String) {
//        var request = Request.Builder()
//            .url("${_uiState.value.baseURL}/users/pwresetemail/${email}")
//            .post(RequestBody.create("application/json".toMediaTypeOrNull(), ""))
//            .build()
        var request = HttpRequestBuilder()
        request.url("$baseURL/users/pwresetemail/$email")
        request.setBody("")

        try {
            var res = AppInfo.httpClient.post(request)
            log.d("pwreset") { "${res.status}" }
        } catch (e: Exception) {
            log.e { "error w password reset $e" }
        }
    }
    suspend fun updatePNSToken(pushToken: String) {
            var builder = HttpRequestBuilder()
            builder.url("$baseURL/users/pushtoken/$pushToken")
            builder.withToken(token)
            try {
                var res = AppInfo.httpClient.post(builder)
            } catch (e: Exception) {
                log.e { "error updating pnstoken $e" }
            }
    }

    suspend fun createFamily(family: CIFamily) {
        val request = HttpRequestBuilder()
        request.url("$baseURL/family/create")
        request.withToken(token)
        request.contentType(ContentType.Application.Json)
        request.setBody(family)
        try {
            var res = AppInfo.httpClient.post(request)
            log.d(tag = "createfam") { res.status.value }
            refreshData()
        } catch (e: Exception) {
            log.e { "error creating family: $e" }
        }
    }

    companion object {
        public  var shared = CIManager()
    }

}

fun HttpRequestBuilder.withToken(token: String) {
    this.headers.set("Authorization", "Bearer $token")
}

data class AuthControllerState(
    val user: OBUser?,
    val loading: Boolean = true,
    val families: MutableList<CIFamily> = mutableListOf()
)
enum class SQSignUpResponse(val message: String) {
    SUCCESS("Success!"),
    ALREADY_EXISTS("That account already exists! Try logging in instead."),
    NO_CONNECTION("Unable to reach SharedQ. Maybe check your network connection?"),
    INCORRECT_PASSWORD("That password wasn't correct. Try again!")
}
@Serializable
data class NewSession(
    val token: String,
    val user: OBUser
)
@Serializable
data class UserSignup(
    val email: String,
    val username: String,
    val password: String
)