package com.example.pleb2.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pleb2.ui.AndroidOnboardingViewModel
import com.example.crypto.NostrKeyInfo

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit
) {
    val onboardingViewModel: AndroidOnboardingViewModel = viewModel()
    var importKey by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showBackupScreen by remember { mutableStateOf(false) }
    var generatedKeyInfo by remember { mutableStateOf<NostrKeyInfo?>(null) }
    val context = LocalContext.current

    if (showBackupScreen && generatedKeyInfo != null) {
        KeyBackupScreen(
            keyInfo = generatedKeyInfo!!,
            onContinue = {
                onboardingViewModel.shared.saveKey(generatedKeyInfo!!.privHex)
                onOnboardingComplete()
            }
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome! Set up your Nostr key:")
        Button(onClick = {
            val keyInfo = onboardingViewModel.shared.generateKeyForUi()
            generatedKeyInfo = keyInfo
            showBackupScreen = true
        }, modifier = Modifier.padding(top = 16.dp)) {
            Text("Create New Key")
        }
        Text("or", modifier = Modifier.padding(vertical = 8.dp))
        OutlinedTextField(
            value = importKey,
            onValueChange = { importKey = it },
            label = { Text("Import Hex Private Key") }
        )
        Button(onClick = {
            val success = onboardingViewModel.shared.importKeyFromNsec(importKey)
            if (success) {
                error = null
                onOnboardingComplete()
            } else {
                error = "Invalid nsec key"
            }
        }, modifier = Modifier.padding(top = 8.dp)) {
            Text("Import nsec Key")
        }
        if (error != null) {
            Text(error!!, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
