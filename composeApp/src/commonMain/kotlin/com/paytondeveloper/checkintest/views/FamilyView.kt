package com.paytondeveloper.checkintest.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import co.touchlab.stately.isFrozen
import com.paytondeveloper.checkintest.CIFamily
import com.paytondeveloper.checkintest.MapComponent
import com.paytondeveloper.checkintest.batteryLevel
import com.paytondeveloper.checkintest.controllers.CIManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.DateTimeFormatBuilder
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toLocalDateTime
import org.lighthousegames.logging.logging

@Composable
fun FamilyView(nav: NavController, family: CIFamily) {
    val viewModel by CIManager.shared.uiState.collectAsState()
    var currentPage by remember { mutableStateOf(SelectedPage.main) }
    Scaffold(bottomBar = {
        NavigationBar {
            listOf(SelectedPage.main, SelectedPage.settings).forEach { tab ->
                NavigationBarItem(
                    selected = (currentPage == tab),
                    icon = { Icon(tab.icon, contentDescription = "") },
                    label = {
                        Text(tab.title)
                    },
                    onClick = {
                        currentPage = tab
                    }
                )
            }
        }
    }) {
        Column(verticalArrangement = Arrangement.Bottom) {
            if (family.currentSession == null) {
                MapComponent(
                    pinLat = 0.0f,
                    pinLong = 0.0f,
                    markerTitle = "No Session"
                )
            } else {
                MapComponent(
                    pinLat = family.currentSession.latitude,
                    pinLong = family.currentSession.longitude,
                    markerTitle = "${family.currentSession.host.username}"
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

@Composable
fun FamilyMainPage(family: CIFamily) {
    val viewModel by CIManager.shared.uiState.collectAsState()
    Column(modifier = Modifier.padding(12.dp)) {
        if (family.currentSession == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Nobody is currently hosting a Check In.")
                TextButton(onClick =  {
                    GlobalScope.launch {
                        CIManager.shared.startSession(family, destLat = 0.0, destLong = 0.0)
                        CIManager.shared.refreshData()
                    }
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
        family.users.forEach { user ->
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
                        Text("Battery ${family.currentSession.batteryLevel * 100}% - Last Updated ${family.currentSession.lastUpdate.toLocalDateTime(
                            TimeZone.currentSystemDefault())}")
                    }
                }
            )
        }
    }
}

enum class SelectedPage(val icon: ImageVector, val title: String) {
    main(icon = Icons.Rounded.Home, title = "Home"),
    settings(icon = Icons.Rounded.Settings, title = "Settings")
}