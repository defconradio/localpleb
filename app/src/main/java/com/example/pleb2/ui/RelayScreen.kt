package com.example.pleb2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.example.pleb2.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelayScreen(
    navController: androidx.navigation.NavController,
    viewModel: AndroidRelayViewModel = hiltViewModel()
) {
    val relays by viewModel.relays.collectAsState()
    val newRelay by viewModel.newRelay.collectAsState()
    val relayInputError by viewModel.relayInputError.collectAsState()
    val relayTimeoutMs by viewModel.relayTimeoutMs.collectAsState()
    var timeoutInput: String by remember(relayTimeoutMs) { mutableStateOf(relayTimeoutMs.toString()) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface, // Match other screens
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.nav_relays)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                //.padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = CardDefaults.shape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Nostr Relays", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    relays.forEach { relay ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(relay, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.removeRelay(relay) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove relay")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newRelay,
                        onValueChange = { value -> viewModel.onNewRelayChanged(value) },
                        label = { Text(stringResource(R.string.add_relay)) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = relayInputError != null,
                        supportingText = {
                            val error = relayInputError
                            if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(onClick = { viewModel.addRelay() }) {
                            Text(stringResource(R.string.add))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = timeoutInput,
                        onValueChange = { value ->
                            timeoutInput = value
                        },
                        label = { Text("Relay Timeout (ms)") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = timeoutInput.toLongOrNull() == null,
                        supportingText = {
                            if (timeoutInput.toLongOrNull() == null) Text("Enter a valid number", color = MaterialTheme.colorScheme.error)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = {
                                timeoutInput.toLongOrNull()?.let { viewModel.setRelayTimeoutMs(it) }
                            },
                            enabled = timeoutInput.toLongOrNull() != null
                        ) {
                            Text("Save Timeout")
                        }
                    }
                }
            }
        }
    }
}