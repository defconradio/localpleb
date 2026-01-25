package com.example.pleb2.ui

import android.app.Activity
import android.content.Context
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.navigation.NavController
import com.example.pleb2.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import com.example.crypto.NostrKeyInfo
import com.example.crypto.PublicAccountInfo

fun showBiometricPrompt(
    context: Context,
    onSuccess: () -> Unit,
    onError: (() -> Unit)? = null
) {
    val activity = context as? ComponentActivity ?: return
    val executor = ContextCompat.getMainExecutor(context)
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Authenticate to Show Private Key")
        .setSubtitle("Use your device security to view your private key")
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        .build()

    // Fix: Use FragmentActivity for BiometricPrompt constructor
    val biometricPrompt = BiometricPrompt(activity as androidx.fragment.app.FragmentActivity, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, "Authentication succeeded", android.widget.Toast.LENGTH_SHORT).show()
                    onSuccess()
                }
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, "Authentication error: $errString", android.widget.Toast.LENGTH_SHORT).show()
                    onError?.invoke()
                }
            }
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, "Authentication failed", android.widget.Toast.LENGTH_SHORT).show()
                    onError?.invoke()
                }
            }
        })

    biometricPrompt.authenticate(promptInfo)
}

@Composable
fun AccountSwitcherDialog(
    onDismissRequest: () -> Unit,
    onboardingViewModel: AndroidOnboardingViewModel,
    onAddAccount: () -> Unit,
    onImportAccount: () -> Unit
) {
    val accounts by onboardingViewModel.shared.accounts.collectAsState()
    val currentAccount by onboardingViewModel.shared.publicAccountInfo.collectAsState()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Switch Account") },
        text = {
            Column {
                Text("Select an account to make it active or add a new one.")
                Spacer(modifier = Modifier.padding(8.dp))
                LazyColumn {
                    items(accounts) { account ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onboardingViewModel.shared.switchAccount(account.pubKeyHex)
                                    onDismissRequest()
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = account.npub.take(12) + "...",
                                style = if (account.pubKeyHex == currentAccount?.pubKeyHex) {
                                    MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary)
                                } else {
                                    MaterialTheme.typography.bodyLarge
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onAddAccount) {
                    Text("New")
                }
                TextButton(onClick = onImportAccount) {
                    Text("Import")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddAccountDialog(
    onDismissRequest: () -> Unit,
    onboardingViewModel: AndroidOnboardingViewModel
) {
    var newKeyNsec by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Import Account") },
        text = {
            Column {
                Text("Enter your nsec private key to import an existing account.")
                OutlinedTextField(
                    value = newKeyNsec,
                    onValueChange = { newKeyNsec = it },
                    label = { Text("nsec private key") },
                    isError = showError
                )
                if (showError) {
                    Text("Invalid nsec key", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (onboardingViewModel.shared.importKeyFromNsec(newKeyNsec)) {
                    onDismissRequest()
                } else {
                    showError = true
                }
            }) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    navController: NavController,
    onboardingViewModel: AndroidOnboardingViewModel = viewModel(),
    onLogout: (() -> Unit)? = null
) {
    val accountInfo by onboardingViewModel.shared.accountInfo.collectAsState()
    val publicAccountInfo by onboardingViewModel.shared.publicAccountInfo.collectAsState()

    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAccountSwitcher by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showNewAccountBackup by remember { mutableStateOf(false) }
    var newKeyInfoForBackup by remember { mutableStateOf<NostrKeyInfo?>(null) }
    val isPrivateKeyVisible by onboardingViewModel.shared.isPrivateKeyVisible.collectAsState()

    LaunchedEffect(Unit) {
        onboardingViewModel.shared.loadPublicAccountInfo()
        onboardingViewModel.shared.loadAccounts()
    }

    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            onboardingViewModel.shared.hidePrivateKey()
        }
    }

    if (showNewAccountBackup) {
        newKeyInfoForBackup?.let { keyInfo ->
            KeyBackupScreen(
                keyInfo = keyInfo,
                onContinue = {
                    onboardingViewModel.shared.saveKey(keyInfo.privHex)
                    showNewAccountBackup = false
                    newKeyInfoForBackup = null
                }
            )
        }
        return
    }

    if (showAccountSwitcher) {
        AccountSwitcherDialog(
            onDismissRequest = { showAccountSwitcher = false },
            onboardingViewModel = onboardingViewModel,
            onAddAccount = {
                newKeyInfoForBackup = onboardingViewModel.shared.generateKeyForUi()
                showAccountSwitcher = false
                showNewAccountBackup = true
            },
            onImportAccount = {
                showAccountSwitcher = false
                showAddAccountDialog = true
            }
        )
    }

    if (showAddAccountDialog) {
        AddAccountDialog(
            onDismissRequest = { showAddAccountDialog = false },
            onboardingViewModel = onboardingViewModel
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Account") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
            //horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Account Info Card
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Account Information", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
                    Text(stringResource(id = R.string.public_key_npub), style = MaterialTheme.typography.labelMedium)
                    SelectionContainer {
                        OutlinedTextField(
                            value = publicAccountInfo?.npub ?: "",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            if (isPrivateKeyVisible) {
                                onboardingViewModel.shared.hidePrivateKey()
                            } else {
                                showBiometricPrompt(
                                    context = context,
                                    onSuccess = { onboardingViewModel.shared.revealPrivateKey() }
                                )
                            }
                        }) {
                            Icon(
                                imageVector = if (isPrivateKeyVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (isPrivateKeyVisible) "Hide" else "Show",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text("Private Key (nsec):", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedTextField(
                        value = if (isPrivateKeyVisible) accountInfo?.nsec ?: "" else "••••••••••••••••••••••••••••••••",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }
            // Warning Box
            Text(
                "Never share your private key. Backup securely.",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(top = 24.dp, bottom = 24.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    .padding(16.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            // Switch/Logout actions at the bottom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                Button(
                    onClick = { showAccountSwitcher = true },
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text("Switch User")
                }
                if (onLogout != null) {
                    Button(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text("Logout")
                    }
                }
            }
            if (showLogoutDialog) {
                AlertDialog(
                    onDismissRequest = { showLogoutDialog = false },
                    title = { Text("Confirm Logout") },
                    text = {
                        Text("If you logout without backing up your private key, you will lose access to this account. Are you sure you want to logout?")
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showLogoutDialog = false
                            onboardingViewModel.shared.deleteKey()
                            // After logout, if no keys are left, navigate to onboarding
                            if (onboardingViewModel.shared.hasKey.value == false) {
                                onLogout?.invoke()
                            }
                        }) {
                            Text("Logout Anyway")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLogoutDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
            // TODO: Add QR code for npub (optional, for easy sharing)
        }
    }
}
