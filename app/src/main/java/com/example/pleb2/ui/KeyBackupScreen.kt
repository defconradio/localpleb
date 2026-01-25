package com.example.pleb2.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.crypto.NostrKeyInfo

@Composable
fun KeyBackupScreen(
    keyInfo: NostrKeyInfo,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Backup your keys!", modifier = Modifier.padding(bottom = 16.dp))
        Text("You must save your private key in a secure place. If you lose it, you will lose access to your account and funds.", modifier = Modifier.padding(bottom = 16.dp))
        Text("\u26a0\ufe0f WARNING: If you lose this key, you will lose access to your account and ALL your data forever!", modifier = Modifier.padding(bottom = 16.dp), color = Color.Red)
        Text("There is NO WAY to recover your key if lost. Write it down and store it in a VERY safe place. If someone else gets this key, they can steal your identity and funds.", modifier = Modifier.padding(bottom = 16.dp), color = Color.Red)
        Text("Private Key (nsec, bech32):", modifier = Modifier.padding(bottom = 4.dp))
        SelectionContainer {
            OutlinedTextField(
                value = keyInfo.nsec,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        Text("Public Key (npub, bech32):", modifier = Modifier.padding(bottom = 4.dp))
        SelectionContainer {
            OutlinedTextField(
                value = keyInfo.npub,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        Button(onClick = onContinue, modifier = Modifier.padding(top = 16.dp)) {
            Text("I have backed up my key")
        }
    }
}
