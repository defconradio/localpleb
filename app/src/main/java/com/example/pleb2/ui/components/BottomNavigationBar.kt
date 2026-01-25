package com.example.pleb2.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.pleb2.R

@Composable
fun BottomNavigationBar(
    selectedRoute: String? = null,
    onHomeClick: () -> Unit,
    onStallClick: () -> Unit,
    onBookClick: () -> Unit
) {
    val homeClickActionLabel = stringResource(R.string.nav_home_click_action)
    val stallClickActionLabel = stringResource(R.string.nav_stall_click_action)
    val bookClickActionLabel = "Navigate to Book"


    Box(
        modifier = Modifier
            .fillMaxWidth(), // Remove all padding so the bar fills the bottom
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            tonalElevation = 0.dp, // Remove tonal elevation to match screen background
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface
        ) {
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding(),
                    //.height(56.dp), // Reduce height by half (was 64.dp)
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Home, contentDescription = stringResource(R.string.nav_home)) },
                    selected = selectedRoute == "productList",
                    onClick = onHomeClick,
                    modifier = Modifier.semantics {
                        onClick(label = homeClickActionLabel, action = null)
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Store, contentDescription = stringResource(R.string.nav_stall)) },
                    selected = selectedRoute == "myStall",
                    onClick = onStallClick,
                    modifier = Modifier.semantics {
                        onClick(label = stallClickActionLabel, action = null)
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Book, contentDescription = "Book") },
                    selected = selectedRoute == "book",
                    onClick = onBookClick,
                    modifier = Modifier.semantics {
                        onClick(label = bookClickActionLabel, action = null)
                    }
                )
            }
        }
    }
}
