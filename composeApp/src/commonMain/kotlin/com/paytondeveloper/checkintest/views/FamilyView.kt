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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.paytondeveloper.checkintest.CIFamily
import com.paytondeveloper.checkintest.CISession
import com.paytondeveloper.checkintest.MapComponent
import com.paytondeveloper.checkintest.controllers.CIManager
import com.paytondeveloper.checkintest.icons.Battery
import com.paytondeveloper.checkintest.icons.Clock
import com.paytondeveloper.checkintest.icons.Ruler
import dev.jordond.compass.Place
import dev.jordond.compass.autocomplete.Autocomplete
import dev.jordond.compass.autocomplete.mobile
import dev.jordond.compass.geocoder.Geocoder
import dev.jordond.compass.geocoder.mobile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
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
                    pinLat = 0.0f,
                    pinLong = 0.0f,
                    markerTitle = "No Session",
                    destLong = 0.0f,
                    destLat = 0.0f,
                    radius = 0.0
                )
            } else {
                MapComponent(
                    pinLat = family.currentSession.latitude,
                    pinLong = family.currentSession.longitude,
                    destLat = family.currentSession.destinationLat,
                    destLong = family.currentSession.destinationLong,
                    radius = family.currentSession.radius,
                    markerTitle = family.currentSession.host.username
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyMainPage(family: CIFamily) {
    var viewingSession by remember { mutableStateOf(false) }
    var showingDestinationSheet by remember { mutableStateOf(false) }
    val viewModel by CIManager.shared.uiState.collectAsState()
    var address by remember { mutableStateOf("") }
    var locationLat by remember { mutableStateOf(0.0) }
    var locationLong by remember { mutableStateOf(0.0) }
    var autocompletePlaces = remember { mutableStateListOf<Place>() }
    Column(modifier = Modifier.padding(12.dp)) {
        if (family.currentSession == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Nobody is currently hosting a Check In.")
                TextButton(onClick =  {
                    showingDestinationSheet = true
                }) {
                    Text("Start Check In")
                }
            }
        } else if (family.currentSession != null && family.currentSession.host.id == viewModel.user?.id) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("You are currently hosting a Check In.")
                TextButton(onClick =  {
                    GlobalScope.launch {
                        CIManager.shared.endSession(family)
                        CIManager.shared.refreshData()
                    }
                }) {
                    Text("End Check In")
                }
            }
        }
        AnimatedContent(viewingSession) {
            if (!viewingSession) {
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
            } else {
                MoreDetailsPage(family.currentSession!!) {
                    viewingSession = false
                }
            }
        }
    }

    if (showingDestinationSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showingDestinationSheet = false
            }

        ) {
            Column(modifier = Modifier.fillMaxHeight(0.5f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Set a Destination")
                OutlinedTextField(
                    value = address,
                    onValueChange = {
                        address = it
                        autocompletePlaces.removeAll({true})
                        GlobalScope.launch {
                            var autocomplete = Autocomplete.mobile()
                            var places = autocomplete.search(address).getOrNull()
                            places?.let {
                                withContext(Dispatchers.Main) {
                                    autocompletePlaces.addAll(it)
                                }
                            }
                        }
                    },

                )
                Column(modifier = Modifier.scrollable(rememberScrollState(), orientation = Orientation.Vertical)) {
                    autocompletePlaces.forEach {
                        Surface(
                            onClick = {
                                val geocoder = Geocoder.mobile()
                                val log = logging()
                                log.d { it.firstValue }

                                GlobalScope.launch {
                                    val coords = geocoder.forward(it.street!!).getOrNull()
                                    coords?.let {
                                        CIManager.shared.startSession(family, destLat = it[0].latitude, destLong = it[0].longitude, radius = 200.0)
                                        CIManager.shared.refreshData()
                                        withContext(Dispatchers.Main) {
                                            showingDestinationSheet = false
                                        }
                                    }
                                }
                            }
                        ) {
                            ListItem(
                                headlineContent = {
                                    Text("${it.street}")
                                },
                                supportingContent = {
                                    Text("${it.administrativeArea} ${it.postalCode}")
                                },
                                leadingContent = {
                                    Icon(Icons.Rounded.LocationOn, contentDescription = "Location")
                                }
                            )
                        }
                    }
                }

            }
        }
    }
}



@Composable
fun MoreDetailsPage(session: CISession, dismiss: () -> Unit) {


    // Custom equivalent format with a fixed Locale
    Column(modifier = Modifier.scrollable(rememberScrollState(), orientation = Orientation.Vertical)) {
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

//        Text("Battery ${family.currentSession.batteryLevel * 100}% - Last Updated ${family.currentSession.lastUpdate.toLocalDateTime(
//            TimeZone.currentSystemDefault())}\n${family.currentSession.distance}m")

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