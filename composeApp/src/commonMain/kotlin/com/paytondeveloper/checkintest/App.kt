
package com.paytondeveloper.checkintest

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

import checkinapp.composeapp.generated.resources.Res
import checkinapp.composeapp.generated.resources.compose_multiplatform
import checkinapp.composeapp.generated.resources.diversity_1
import com.paytondeveloper.checkintest.controllers.CIManager
import com.paytondeveloper.checkintest.views.AuthView
import com.paytondeveloper.checkintest.views.FamilyView
import dev.theolm.rinku.compose.ext.DeepLinkListener
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.imageResource
import org.lighthousegames.logging.logging
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Composable
@Preview
fun App() {
    val navController = rememberNavController()
    val viewModel by CIManager.shared.uiState.collectAsState()
    MaterialTheme {
        if (!viewModel.loading) {
            if (viewModel.user == null) {
                AuthView()
            } else {
                MainView(navController)
            }
        } else {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun MainView(navController: NavHostController) {
    DeepLinkListener { link ->
        val log = logging()
        log.d { link.data }
        val token = link.data.replace("checkinapp:///", "")
        GlobalScope.launch {
            CIManager.shared.joinFamily(token)
        }
    }
    val viewModel by CIManager.shared.uiState.collectAsState()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomePage(navController)
        }
        composable("family/{familyID}") { proxy ->
            var groupID = proxy.arguments!!.getString("familyID")!!
            var family = viewModel.families.first { it.id == groupID }
            FamilyView(navController, family)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(nav: NavController) {
    val viewModel by CIManager.shared.uiState.collectAsState()
    var showBottomSheet by remember { mutableStateOf(false) }
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = {
                showBottomSheet = true
            }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Add, contentDescription = "")
                    Text("Make/Join Family")
                }
            }
        }
    ) {
        Column(modifier = Modifier.scrollable(rememberScrollState(), orientation = Orientation.Vertical).fillMaxHeight()) {
            viewModel.families.forEach { family ->
                Surface(onClick = {
                    nav.navigate("family/${family.id}")
                }) {
                    ListItem(
                        headlineContent = {
                            Text(family.name, style = MaterialTheme.typography.titleLarge)
                        },
                        supportingContent = {
                            Text("${family.users.count()} members")
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (family.currentSession != null) {
                                    Box(
                                        modifier = Modifier.width(24.dp).height(24.dp).background(Brush.radialGradient(listOf(Color.Green, Color.Transparent))).border(0.5.dp, Color.Green, shape = CircleShape).clip(
                                            CircleShape)
                                    )
                                }
                                Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = "View", modifier = Modifier.alpha(0.5f))
                            }
                        }
                    )
                }
            }
        }
    }
    if (showBottomSheet) {
        ModalBottomSheet(modifier = Modifier.fillMaxHeight(), onDismissRequest = {
            showBottomSheet = false
        }) {
            CreateFamilyView()
        }
    }
}

@OptIn(ExperimentalUuidApi::class, DelicateCoroutinesApi::class)
@Composable
fun CreateFamilyView() {
    val viewModel by CIManager.shared.uiState.collectAsState()
    var familyName by remember { mutableStateOf("") }
    val focus = LocalFocusManager.current
    Column(modifier = Modifier.padding(12.dp),verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.CenterHorizontally) {
        Column {
            Text("Make a Family", style = MaterialTheme.typography.titleLarge)
            Text("Families are how you connect with others on Check In. When you invite someone to a Family, they can see everybody's Check In status.\nFor now, all we need is a name you'd like for your family. It can be anything!")
            OutlinedTextField(
                value = familyName,
                onValueChange = {
                    familyName = it
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focus.clearFocus(true)
                    }
                )
            )
            Button(modifier = Modifier.padding(8.dp).fillMaxWidth(), onClick = {
                var group = CIFamily(
                    id = Uuid.random().toString(),
                    name = familyName,
                    users = listOf(viewModel.user!!),
                )
                GlobalScope.launch {
                    CIManager.shared.createFamily(group)
                }
            }) {
                Text("Create Group")
            }
        }
    }
}