package com.example.pleb2.ui

import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.pleb2.R
import com.example.pleb2.ui.components.AppDrawerContent
import com.example.pleb2.ui.components.DrawerNavItem
import com.example.pleb2.ui.components.ProductCard
import com.example.pleb2.ui.theme.LocalThemeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyStallScreen(
    navController: NavController,
    nestedNavController: NavController,
    /* stallTapEvents: SharedFlow<Unit> */
) {
    val navGraphRoute = nestedNavController.graph.findStartDestination().route!!
    val parentEntry = remember(nestedNavController.currentBackStackEntry) {
        nestedNavController.getBackStackEntry(navGraphRoute)
    }
    val viewModel: AndroidMyStallViewModel = hiltViewModel(parentEntry)
    val error by viewModel.error.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val productUiModels by viewModel.productUiModels.collectAsState()
    //var searchFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    val stallCardUiModels by viewModel.stallCardUiModels.collectAsState()
    // Observe the empty state from the viewmodel (do not remove any code, just comment out if needed)
    val showEmptyStallState by viewModel.showEmptyStallState.collectAsState()
    //val showEmptyProductState by viewModel.showEmptyProductState.collectAsState()

    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var deleteAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val drawerState = rememberDrawerState(androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()

//    LaunchedEffect(Unit) {
//        viewModel.reloadAll()
//    }

    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete this item?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteAction?.invoke()
                        showDeleteConfirmationDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmationDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Show empty state card if no stalls exist (do not remove any code, just comment out if needed)
    if (showEmptyStallState) {
        Box(
            modifier = Modifier
                .fillMaxSize(), // Fill the whole screen
            contentAlignment = Alignment.Center // Center content vertically and horizontally
        ) {
            androidx.compose.material3.Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f) // Make card a bit narrower for better look
                    .clickable {
                        // TODO: Replace with your navigation to create stall screen
                        navController.navigate("createStall")
                    },
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
                elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No stalls found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Create your first stall to start selling!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.material3.Button(onClick = {
                        // Best practice: Let navigation host (MainActivity/AppNavHost) handle DI and dependency passing.
                        navController.navigate("createStall")
                    }) {
                        Text("Create New Stall")
                    }
                }
            }
        }
        return
    }
    val sortedStalls = stallCardUiModels.sortedByDescending { it.envelope?.created_at ?: 0L }
    var expanded by remember { mutableStateOf(false) }
    val selectedStallEventId by viewModel.selectedStallEventId.collectAsState()
    val selectedStallBelow = sortedStalls.find { it.envelope?.id == selectedStallEventId }
    //val rotation by animateFloatAsState(if (expanded) 180f else 0f)

    val selectedStallStallId by viewModel.selectedStallStallId.collectAsState()
    val filteredProducts = if (selectedStallStallId != null) {
        productUiModels.filter { it.stall_id == selectedStallStallId }
    } else {
        emptyList()
    }

    val infiniteTransition = rememberInfiniteTransition()
    val shake by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

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
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(74.dp)
                        .padding(top = 20.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🌶️", fontSize = 32.sp,
                        modifier = Modifier.clickable { scope.launch { drawerState.open() } }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = selectedStallBelow?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .height(52.dp)
                                .fillMaxWidth(),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            placeholder = {
                                Text(
                                    stringResource(id = com.example.pleb2.R.string.stall_drop_down_menu),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp)
                            )
                            },
                            trailingIcon = {
                                val rotation by androidx.compose.animation.core.animateFloatAsState(if (expanded) 180f else 0f)
                                IconButton(onClick = { expanded = !expanded }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Dropdown",
                                        modifier = Modifier.rotate(rotation)
                                    )
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        ) {
                            sortedStalls.forEach { stall ->
                                androidx.compose.material3.Card(
                                    modifier = Modifier
                                        .padding(vertical = 6.dp, horizontal = 8.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
                                    elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    viewModel.selectStall(stall.envelope?.id ?: "")
                                                    expanded = false
                                                },
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            // Stall Name
                                            Text(
                                                text = stall.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            // Stall ID with Store Icon
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Store, contentDescription = "Stall ID", tint = MaterialTheme.colorScheme.outline)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = stall.stall_id,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            // Event ID with Info Icon
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Info, contentDescription = "Event ID", tint = MaterialTheme.colorScheme.outline)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = stall.event_id,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                            }

                                            // Formatted Date with Calendar Icon
                                            stall.formattedCreatedAt?.let { formattedDate ->
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Event, contentDescription = "Last Update", tint = MaterialTheme.colorScheme.outline)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "last: $formattedDate",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    IconButton(onClick = {
                                                        stall.envelope?.id?.let { stallEventId ->
                                                            navController.navigate("myStallDetail/$stallEventId")
                                                            expanded = false
                                                        }
                                                    }) {
                                                        Icon(Icons.Default.Visibility, contentDescription = "View Stall")
                                                    }
                                                    IconButton(onClick = {
                                                        stall.envelope?.id?.let { stallEventId ->
                                                            navController.navigate("editStall/$stallEventId")
                                                            expanded = false
                                                        }
                                                    }) {
                                                        Icon(Icons.Default.Edit, contentDescription = "Edit Stall")
                                                    }
                                                    IconButton(onClick = {
                                                        deleteAction = {
                                                            stall.envelope?.id?.let { eventId ->
                                                                // viewModel.createDeleteEvent(
                                                                //     eventIdsToDelete = listOf(eventId),
                                                                //     reason = "Deleting stall"
                                                                // ) { deleteEvent ->
                                                                //     viewModel.broadcastEvent(deleteEvent) {
                                                                //         viewModel.shared.reloadAll()
                                                                //     }
                                                                // }
                                                                viewModel.deleteEventsAndReload(
                                                                    eventIdsToDelete = listOf(eventId),
                                                                    reason = "Deleting stall"
                                                                )
                                                            }
                                                        }
                                                        showDeleteConfirmationDialog = true
                                                    }) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete Stall")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            // Add button to create a new stall at the end of the dropdown
                            DropdownMenuItem(
                                onClick = {
                                    expanded = false
                                    navController.navigate("createStall")
                            },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Add, contentDescription = "Add Stall", tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Create New Stall", color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Reserve static space for the loading indicator
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
                // Products LazyColumn (filtered by selected stall)
                Log.i("MyStallScreen", "selectedStallStallId: $selectedStallStallId")
                productUiModels.forEach { Log.i("MyStallScreen", "product: ${it.name}, stall_id: ${it.stall_id}") }
                Log.i("MyStallScreen", "filteredProducts: ${filteredProducts.map { it.name + ", " + it.stall_id }}")

                //if (filteredProducts.isEmpty() && selectedStallStallId != null && !isLoading) {
                if (filteredProducts.isEmpty() && selectedStallStallId != null) {

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddShoppingCart,
                                contentDescription = "No products found",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            TextButton(onClick = {
                                val stallEventId = selectedStallBelow?.event_id
                                if (stallEventId != null) {
                                    navController.navigate("createProduct/$stallEventId")
                                }
                            }) {
                                Text("Create new product")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        items(filteredProducts) { product ->
                            ProductCard(
                                model = product,
                                onClick = { navController.navigate("productDetail/${product.event_id}") },
                                onEditClick = {
                                    val stallEventId = selectedStallBelow?.envelope?.id
                                    val productEventId = product.envelope?.id
                                    if (stallEventId != null && productEventId != null) {
                                        navController.navigate("editProduct/$stallEventId/$productEventId")
                                    }
                                },
                                onDeleteClick = {
                                    deleteAction = {
                                        product.envelope?.id?.let { eventId ->
                                            // viewModel.createDeleteEvent(
                                            //     eventIdsToDelete = listOf(eventId),
                                            //     reason = "Deleting product"
                                            // ) { deleteEvent ->
                                            //     viewModel.broadcastEvent(deleteEvent) {
                                            //         viewModel.shared.reloadAll()
                                            //     }
                                            // }
                                            viewModel.deleteEventsAndReload(
                                                eventIdsToDelete = listOf(eventId),
                                                reason = "Deleting product"
                                            )
                                        }
                                    }
                                    showDeleteConfirmationDialog = true
                                },
                                onVerificationResult = { isVerified ->
                                    if (!isVerified) {
                                        Log.w("MyStallScreen", "Verification failed for event: ${product.event_id}")
                                        viewModel.reportVerificationError(product.event_id)
                                    } else {
                                        Log.i("MyStallScreen", "Verification successful for event: ${product.event_id}")
                                    }
                                }
                            )
                        }
                    }
                }
            }
            // FloatingActionButton to add new product
            val fabModifier = if (filteredProducts.isEmpty() && selectedStallStallId != null) {
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .graphicsLayer {
                        translationX = shake
                    }
            } else {
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            }

            androidx.compose.material3.FloatingActionButton(
                onClick = {
                    Log.i("MyStallScreen", "selectedStallEventId: $selectedStallEventId")
                    Log.i("MyStallScreen", "selectedStallBelow: $selectedStallBelow")
                    Log.i("MyStallScreen", "selectedStallBelow?.envelope: ${selectedStallBelow?.envelope}")
                    sortedStalls.forEach { Log.i("MyStallScreen", "sortedStall event_id: ${it.event_id}, stall.stall_id: ${it.stall_id}") }
                    Log.i("MyStallScreen", "Selected envelope: ${selectedStallBelow?.envelope}")
                    val stallEventId = selectedStallBelow?.event_id
                    Log.i("MyStallScreen", "Selected stallEventId for createProduct: $stallEventId, envelope: ${selectedStallBelow?.envelope}")
                    if (stallEventId != null) {
                        navController.navigate("createProduct/$stallEventId")
                    } else {
                        // Optionally show a message or fallback
                    }
                },
                modifier = fabModifier
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Product")
            }
        }
    }

    // --- Error Snack bar (Material3, theme error colors, Retry) ---
    if (error != null) {
        val clipboardManager = LocalClipboardManager.current
        androidx.compose.material3.Snackbar(
            modifier = Modifier.padding(16.dp),
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { viewModel.reloadAll() }) {
                        Text("Retry", color = MaterialTheme.colorScheme.onError)
                    }
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss", color = MaterialTheme.colorScheme.onError)
                    }
                    TextButton(onClick = {
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(error ?: ""))
                    }) {
                        Text("Copy", color = MaterialTheme.colorScheme.onError)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Show error message exactly as ProductListScreen: single Text with embedded \n
                androidx.compose.foundation.text.selection.SelectionContainer {
                    Text(
                        error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.onError,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
