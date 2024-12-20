package com.paytondeveloper.checkintest.views

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.paytondeveloper.checkintest.CIFamily
import com.paytondeveloper.checkintest.CILatLong
import com.paytondeveloper.checkintest.CIMapMarker
import com.paytondeveloper.checkintest.CISession
import com.paytondeveloper.checkintest.ClipboardManager
import com.paytondeveloper.checkintest.MapComponent
import com.paytondeveloper.checkintest.bioAuthenticate
import com.paytondeveloper.checkintest.controllers.CIManager
import com.paytondeveloper.checkintest.icons.Battery
import com.paytondeveloper.checkintest.icons.Clock
import com.paytondeveloper.checkintest.icons.Ruler
import dev.jordond.compass.Place
import dev.jordond.compass.autocomplete.Autocomplete
import dev.jordond.compass.autocomplete.mobile
import dev.jordond.compass.geocoder.Geocoder
import dev.jordond.compass.geocoder.mobile
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toLocalDateTime
import org.lighthousegames.logging.logging
import kotlin.math.pow
import kotlin.math.roundToInt


@Composable
fun FamilyView(nav: NavController, family: CIFamily) {
    val viewModel by CIManager.shared.uiState.collectAsState()
    var currentPage by remember { mutableStateOf(SelectedPage.main) }
    Scaffold() {
        Column(verticalArrangement = Arrangement.Bottom) {
            if (family.currentSession == null) {
                MapComponent(
                    markers = listOf(CIMapMarker(CILatLong(0.0, 0.0),"No session", "")),
                    dest = CILatLong(0.0,0.0),
                    history = listOf(),
                    radius = 0.0
                )
            } else {
                MapComponent(
                    markers = listOf(
                        CIMapMarker(family.currentSession.location, "${family.currentSession.host.username}'s location", subtitle = "Last Updated ${formatDateTime(family.currentSession.lastUpdate.toLocalDateTime(TimeZone.currentSystemDefault()))}"),
                        CIMapMarker(family.currentSession.destination, title = "${family.currentSession.placeName ?: "No name"}", subtitle = "Destination")
                    ),
                    dest = family.currentSession.destination,
                    radius = family.currentSession.radius,
                    history = family.currentSession.history
                )
            }
            Column {
                when (currentPage) {
                    SelectedPage.main -> {
                        FamilyMainPage(family)
                    }
                    SelectedPage.settings -> {

                    }
                }
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
@Composable
fun FamilyMainPage(family: CIFamily) {
    var viewingSession by remember { mutableStateOf(false) }
    var showingDestinationSheet by remember { mutableStateOf(false) }
    val viewModel by CIManager.shared.uiState.collectAsState()
    var address by remember { mutableStateOf("") }
    var locationLat by remember { mutableStateOf(0.0) }
    var locationLong by remember { mutableStateOf(0.0) }
    var autocompletePlaces = remember { mutableStateListOf<Place>() }
    var loadingInviteCode by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(12.dp).scrollable(rememberScrollState(), orientation = Orientation.Vertical).fillMaxHeight()) {
        if (family.currentSession == null) {
            Column() {
                Text("Nobody is currently hosting a Check In.")
                TextButton(onClick =  {
                    showingDestinationSheet = true
                }) {
                    Text("Start Check In")
                }
            }
        } else if (family.currentSession != null && family.currentSession.host.id == viewModel.user?.id) {
            Column() {
                Text("You are currently hosting a Check In.")
                TextButton(onClick =  {
                    GlobalScope.launch {
                        if (bioAuthenticate()) {

                            CIManager.shared.endSession(family)
                            CIManager.shared.refreshData()

                        }
                    }
                }) {
                    Text("End Check In")
                }
            }
        }
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(viewingSession) {
                if (!viewingSession) {
                    Column {
                        family.users.forEach { user ->
                            Surface(onClick = {
                                if (family.currentSession != null && family.currentSession.host.id == user.id) {
                                    viewingSession = true
                                }
                            }) {
                                ListItem(
                                    headlineContent = {
                                        Text(user.username, style = MaterialTheme.typography.titleLarge)
                                    },
                                    leadingContent = {
                                        Icon(Icons.Rounded.Person, contentDescription = "Member")
                                    },
                                    trailingContent = {
                                        if (family.currentSession != null && family.currentSession.host.id == user.id) {
                                            Box(
                                                modifier = Modifier.width(24.dp).height(24.dp).background(
                                                    Brush.radialGradient(listOf(
                                                        Color.Green, Color.Transparent))).border(0.5.dp, Color.Green, shape = CircleShape).clip(
                                                    CircleShape
                                                )
                                            )
                                        }
                                    },
                                    supportingContent = {
                                        if (family.currentSession != null && family.currentSession.host.id == user.id) {
                                            Text("Hosting a session • Tap for more details")
                                        }
                                    }
                                )
                            }
                        }
                        Surface(onClick = {
                            loadingInviteCode = true
                            GlobalScope.launch {
                                val inviteLink = CIManager.shared.getInviteLink(family)
                                withContext(Dispatchers.Main) {
                                    ClipboardManager.copyToClipboard(inviteLink)
                                    loadingInviteCode = false
                                }
                            }
                        }) {
                            Box(contentAlignment = Alignment.Center) {
                                ListItem(
                                    modifier = Modifier.alpha(if (loadingInviteCode) 0.5f else 1.0f),
                                    headlineContent = {
                                        Text("Invite Member", style = TextStyle(color = MaterialTheme.colorScheme.primary))
                                    },
                                    supportingContent = {
                                        Text("Copy an invite link to your clipboard", style = TextStyle(color = MaterialTheme.colorScheme.primary))
                                    },
                                    leadingContent = {
                                        Icon(Icons.Rounded.Add, contentDescription = "fdsf", tint = MaterialTheme.colorScheme.primary)
                                    }
                                )
                                if (loadingInviteCode) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                } else {
                    MoreDetailsPage(family.currentSession!!) {
                        viewingSession = false
                    }
                }
            }
        }
    }

    if (showingDestinationSheet) {
        var selectedPlace by remember { mutableStateOf<Place?>(null) }
        var selectedRadius by remember { mutableStateOf(200.0) }
        var loading by remember { mutableStateOf(false) }
        ModalBottomSheet(
            onDismissRequest = {
                showingDestinationSheet = false
            }

        ) {
            Column(modifier = Modifier.fillMaxHeight(0.5f).fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Set a Destination")
                val focus = LocalFocusManager.current
                OutlinedTextField(
                    value = address,
                    onValueChange = {
                        address = it

                    },
                    label = { Text("Address or Place Name")},
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            autocompletePlaces.removeAll({true})
                            GlobalScope.launch {
                                var autocomplete = Autocomplete.mobile()
                                var places = autocomplete.search(address).getOrNull()
                                places?.let {
                                    withContext(Dispatchers.Main) {
                                        autocompletePlaces.addAll(it)
                                        focus.clearFocus(true)
                                    }
                                }
                            }
                        }
                    )
                )

                Column(modifier = Modifier.scrollable(rememberScrollState(), orientation = Orientation.Vertical)) {
                    autocompletePlaces.forEach { place ->
                        Surface(
                            onClick = {
                                val log = logging()
                                log.d { place.firstValue }
                                selectedPlace = place

                            }
                        ) {
                            ListItem(
                                headlineContent = {
                                    Text("${place.street}")
                                },
                                supportingContent = {
                                    Text("${place.administrativeArea} ${place.postalCode}")
                                },
                                leadingContent = {
                                    Icon(Icons.Rounded.LocationOn, contentDescription = "Location")
                                }
                            )
                        }
                    }
                    selectedPlace?.let { place ->
                        MapComponent(markers = listOf(CIMapMarker(CILatLong(latitude = place.coordinates.latitude, longitude = place.coordinates.longitude), title = "Selected Destination", subtitle = "")), dest = CILatLong(place.coordinates.latitude, place.coordinates.longitude), radius = selectedRadius, history = listOf())
                        Slider(value = selectedRadius.toFloat(), onValueChange = {
                            selectedRadius = it.toDouble()
                        }, valueRange = 100f..1000f)
                        Box(contentAlignment = Alignment.Center) {
                            Button(onClick = {
                                if (!loading) {
                                    loading = true
                                    GlobalScope.launch {

                                        if (bioAuthenticate()){
                                            val coords = place.coordinates
                                            CIManager.shared.startSession(family, dest = CILatLong(latitude = place.coordinates.latitude, longitude = place.coordinates.longitude), radius = selectedRadius, placeName = place.street ?: place.firstValue)
                                            CIManager.shared.refreshData()
                                            withContext(Dispatchers.Main) {
                                                loading = false
                                                showingDestinationSheet = false
                                            }
                                        }

                                    }
                                }
                            }, modifier = Modifier.alpha(if (loading) 0.5f else 1.0f).fillMaxWidth()) {
                                Text("Start Check In")
                            }
                            if (loading) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }

            }
        }
    }
}



@Composable
fun MoreDetailsPage(session: CISession, dismiss: () -> Unit) {
    Column(modifier = Modifier) {
        TextButton(
            modifier = Modifier.padding(horizontal = 12.dp),
            onClick = {
                dismiss()
            }
        ) {
            Text("Back to Members")
        }
        ListItem(
            headlineContent = {
                Text(session.host.username, style = MaterialTheme.typography.titleLarge)
            },
            leadingContent = {
                Icon(Icons.Rounded.Person, contentDescription = "Member")
            },
            trailingContent = {
                    Box(
                        modifier = Modifier.width(24.dp).height(24.dp).background(
                            Brush.radialGradient(listOf(
                                Color.Green, Color.Transparent))).border(0.5.dp, Color.Green, shape = CircleShape).clip(
                            CircleShape
                        )
                    )
            },
        )
        ListItem(
            headlineContent = {
                Text("${(session.batteryLevel * 100).roundToInt()}%")
            },
            supportingContent = {
                Text("Battery Level")
            },
            leadingContent = {
                Icon(imageVector = Battery, contentDescription = "Battery")
            }
        )
        ListItem(
            headlineContent = {
                Text("${(session.distance / 1609).toString(2)}mi")
            },
            supportingContent = {
                Text("Distance")
            },
            leadingContent = {
                Icon(imageVector = Ruler, contentDescription = "Distance")
            }
        )
        session.placeName?.let {
            ListItem(
                headlineContent = {
                    Text(it)
                },
                supportingContent = {
                    Text("Destination")
                },
                leadingContent = {
                    Icon(imageVector = Icons.Rounded.Home, contentDescription = "Destination")
                }
            )
        }
        ListItem(
            headlineContent = {
                Text(formatDateTime(session.lastUpdate.toLocalDateTime(TimeZone.currentSystemDefault())))
            },
            supportingContent = {
                Text("Last Updated")
            },
            leadingContent = {
                Icon(imageVector = Clock, contentDescription = "Clock")
            }
        )
    }
}

@OptIn(FormatStringsInDatetimeFormats::class)
fun formatDateTime(time: LocalDateTime): String {
    val format = LocalDateTime.Format { byUnicodePattern("HH:mm 'on' MM/dd/yyyy") }

    return format.format(time)
}

fun Double.toString(numOfDec: Int): String {
    val integerDigits = this.toInt()
    val floatDigits = ((this - integerDigits) * 10f.pow(numOfDec)).roundToInt()
    return "${integerDigits}.${floatDigits}"
}

enum class SelectedPage(val icon: ImageVector, val title: String) {
    main(icon = Icons.Rounded.Home, title = "Home"),
    settings(icon = Icons.Rounded.Settings, title = "Settings")
}