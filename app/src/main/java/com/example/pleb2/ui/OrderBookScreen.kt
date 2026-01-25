package com.example.pleb2.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.pleb2.R
import com.example.pleb2.ui.components.AppDrawerContent
import com.example.pleb2.ui.components.DrawerNavItem
import com.example.pleb2.ui.theme.LocalThemeState
import com.example.data.uiModels.OrderUiModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun OrderBookScreen(
    navController: NavController,
    nestedNavController: NavController
) {
    val navGraphRoute = nestedNavController.graph.findStartDestination().route!!
    val parentEntry = remember(nestedNavController.currentBackStackEntry) {
        nestedNavController.getBackStackEntry(navGraphRoute)
    }
    val viewModel: AndroidOrderBookViewModel = hiltViewModel(parentEntry)
    val orderList by viewModel.filteredOrderList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchText by viewModel.searchText.collectAsState()
    var searchFieldValue by remember { mutableStateOf(TextFieldValue(searchText)) }
    val drawerState = rememberDrawerState(androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collectLatest { event ->
            when (event) {
                is OrderBookNavigationEvent.NavigateToOrder -> {
                    navController.navigate("order/${event.conversationId}/${event.partnerPubkeys}")
                }
            }
        }
    }

    LaunchedEffect(searchText) {
        if (searchText != searchFieldValue.text) {
            searchFieldValue = TextFieldValue(searchText, TextFieldValue(searchText).selection)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                navController = navController,
                drawerState = drawerState,
                scope = scope
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
                    .padding(top = 20.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🌶️",
                    fontSize = 32.sp,
                    modifier = Modifier.clickable { scope.launch { drawerState.open() } }
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = searchFieldValue,
                    onValueChange = { value ->
                        searchFieldValue = value
                        if (value.text != searchText) {
                            viewModel.updateSearchText(value.text)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    placeholder = {
                        Text(
                            "Search messages...",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp)
                        )
                    }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.height(4.dp).fillMaxWidth()) {
                if (isLoading) {
                    LinearProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            if (!isLoading) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(orderList) { order ->
                        // Debug: print order.* to logcat
                        android.util.Log.d("OrderBookScreen", "OrderUiModel: " + order.toString())
                        OrderItem(order = order) {
                            viewModel.onOrderClicked(order)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderItem(order: OrderUiModel, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        ListItem(
            headlineContent = {
                Text(
                    // Display subject if available, otherwise generate a title from participants
                    //text = order.tags.find { it.size > 1 && it[0] == "subject" }?.getOrNull(1) ?: "Chat without subject ${order.pubkey}",
                    text = "order pubkey: ${order.pubkey}",

                    style = MaterialTheme.typography.titleMedium
                )
            },
            supportingContent = {
                Text(
                    text = order.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingContent = {
                Icon(
                    Icons.Default.Forum,
                    contentDescription = "Chat Icon"
                )
            },
            overlineContent = {
                // As requested, not showing the full participant list.
                // Showing a participant count instead for better UI.
                // val participantCount = (order.tags.filter { it.size > 1 && it[0] == "p" }.map { it[1] } + order.pubkey).distinct().size
                // Text(
                //     text = if (order.tags.any { it.size > 1 && it[0] == "subject" }) "$participantCount Participants" else "Direct Message",
                //     style = MaterialTheme.typography.labelSmall
                // )
                // Show conversation id (subject) instead of participant count
                Text(
                    //TODO remove all this nonsense when finish just keep the data
                    text = "product event id: " + (order.tags.find { it.size > 1 && it[0] == "subject" }?.getOrNull(1) ?: "message without subject, no product event id"),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        )
    }
}
