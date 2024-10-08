
package com.paytondeveloper.checkintest

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

import checkinapp.composeapp.generated.resources.Res
import checkinapp.composeapp.generated.resources.compose_multiplatform
import com.paytondeveloper.checkintest.views.AuthView
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth

@Composable
@Preview
fun App() {

    MaterialTheme {
        if (Firebase.auth.currentUser == null) {
             AuthView()
        } else {
            
        }
    }
}

