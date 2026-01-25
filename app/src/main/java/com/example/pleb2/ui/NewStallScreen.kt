package com.example.pleb2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.uiModels.ShippingZoneUiModel
import com.example.data.uiModels.TagUiModel
import java.util.UUID
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import com.example.data.util.CurrencyList
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuAnchorType
import com.example.pleb2.util.isValidImageUrl
import androidx.compose.foundation.rememberScrollState
import androidx.navigation.NavGraph.Companion.findStartDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewStallScreen(
    // onEventCreated: (String) -> Unit,
    // setBottomBarVisibility: (Boolean) -> Unit,
    navController: androidx.navigation.NavController,
    stallEventId: String? = null
) {
    val navGraphRoute = navController.graph.findStartDestination().route!!
    val parentEntry = remember(navController.currentBackStackEntry) {
        navController.getBackStackEntry(navGraphRoute)
    }
    val viewModel: AndroidNewStallViewModel = hiltViewModel(parentEntry)
    // TODO: State Management in Composable - For screens with many input fields,
    //  it can become cumbersome to manage. A common "best practice" for complex forms
    //  is to encapsulate the form's state within a single data class.
    var stallId by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("") }
    var shippingZones by remember { mutableStateOf<List<ShippingZoneUiModel>>(emptyList()) }
    var newShippingZoneName by remember { mutableStateOf("") }
    var newShippingZoneCost by remember { mutableStateOf("") }
    var newShippingZoneRegions by remember { mutableStateOf("") }
    // Tag management state
    var tags by remember { mutableStateOf<List<TagUiModel>>(emptyList()) }
    var newTagType by remember { mutableStateOf("") }
    var newTagValue by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    // Category management state
    var categories by remember { mutableStateOf<List<TagUiModel>>(emptyList()) }
    var newCategoryValue by remember { mutableStateOf("") }

    // Editing state
    var editingShippingZoneId by remember { mutableStateOf<String?>(null) }
    var editingTagIndex by remember { mutableStateOf<Int?>(null) }
    var editingCategoryIndex by remember { mutableStateOf<Int?>(null) }

    // --- Images State ---
    var images by remember { mutableStateOf<List<String>>(emptyList()) }
    var newImageUrl by remember { mutableStateOf("") }

    val eventState by viewModel.eventState.collectAsState()
    val errorState by viewModel.error.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState(initial = false)
    val loadedStall by viewModel.loadedStall.collectAsState()

    // --- Add a flag to track if the form has been populated ---
    var isFormPopulated by remember { mutableStateOf(false) }
    /*
    DisposableEffect(Unit) {
        setBottomBarVisibility(false)
        onDispose {
            setBottomBarVisibility(true)
        }
    }
    */

    // Remove Scaffold and use a plain Column as the root, like ProductDetailScreen
    // --- Broadcast error snackbar (ProductDetailScreen style) ---
    var broadcastError by remember { mutableStateOf(false) }
    var showBroadcastSnackbar by remember { mutableStateOf(false) }
    var broadcastSnackbarMessage by remember { mutableStateOf("") }
    var broadcastSnackbarIsError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (stallEventId == null) "Create Stall" else "Edit Stall") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier.fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                viewModel.createAndBroadcastStallEvent(
                                    id = stallId,
                                    name = name,
                                    description = description,
                                    currency = currency,
                                    shippingZonesUi = shippingZones,
                                    tags = tags + categories
                                ) { relayResults, eventId ->
                                    val allOk = relayResults.isNotEmpty() && relayResults.values.all { it }
                                    if (allOk) {
                                        navController.popBackStack()
                                    } else {
                                        broadcastSnackbarMessage = errorState ?: "Broadcast failed!"
                                        broadcastSnackbarIsError = true
                                        showBroadcastSnackbar = true
                                    }
                                }
                            },
                            modifier = Modifier.padding(end = 12.dp) // Add right padding to the Save button
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Save Stall")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()

        ) {
            // Reserve static space for the loading indicator
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .fillMaxWidth()
            ) {
                if (isLoading) {
                    LinearProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                if (showBroadcastSnackbar) {
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 16.dp),
                        containerColor = if (broadcastSnackbarIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        contentColor = if (broadcastSnackbarIsError) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
                        action = {
                            Row {
                                if (broadcastSnackbarIsError) {
                                    TextButton(onClick = {
                                        showBroadcastSnackbar = false
                                        viewModel.createAndBroadcastStallEvent(
                                            id = stallId,
                                            name = name,
                                            description = description,
                                            currency = currency,
                                            shippingZonesUi = shippingZones,
                                            tags = tags + categories
                                        ) { relayResults, eventId ->
                                            val allOk = relayResults.isNotEmpty() && relayResults.values.all { it }
                                            if (allOk) {
                                                broadcastSnackbarMessage = "Successfully broadcasted!"
                                                broadcastSnackbarIsError = false
                                            } else {
                                                broadcastSnackbarMessage = errorState ?: "Broadcast failed!"
                                                broadcastSnackbarIsError = true
                                            }
                                            showBroadcastSnackbar = true
                                        }
                                    }) {
                                        Text("Retry", color = MaterialTheme.colorScheme.onError)
                                    }
                                }
                                TextButton(onClick = {
                                    showBroadcastSnackbar = false
                                    broadcastError = false
                                }) {
                                    Text(
                                        "Dismiss",
                                        color = if (broadcastSnackbarIsError) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    ) {
                        Text(
                            broadcastSnackbarMessage,
                            color = if (broadcastSnackbarIsError) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            // Add horizontal padding to all elements below
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                // Stall ID field moved here (scrollable content, above name)
                OutlinedTextField(
                    value = stallId,
                    onValueChange = { /* Do nothing to prevent editing */ },
                    label = { Text("Stall ID (unique, not the same as name)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Stall Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = true, // Force single line so it doesn't grow
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Currency dropdown
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = currency,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Currency") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        CurrencyList.all.forEach { curr ->
                            DropdownMenuItem(
                                text = { Text(curr) },
                                onClick = {
                                    currency = curr
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Shipping Zones", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            val cost = newShippingZoneCost.toFloatOrNull()
                            if (newShippingZoneName.isNotBlank() && cost != null) {
                                shippingZones = shippingZones + ShippingZoneUiModel(
                                    id = UUID.randomUUID().toString(),
                                    name = newShippingZoneName,
                                    cost = cost,
                                    regions = newShippingZoneRegions.split(",").map { it.trim() }
                                        .filter { it.isNotEmpty() }
                                )
                                newShippingZoneName = ""
                                newShippingZoneCost = ""
                                newShippingZoneRegions = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Shipping Zone")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newShippingZoneName,
                        onValueChange = { newShippingZoneName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = newShippingZoneCost,
                        onValueChange = { newShippingZoneCost = it },
                        label = { Text("Cost") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = newShippingZoneRegions,
                        onValueChange = { newShippingZoneRegions = it },
                        label = { Text("Regions") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Show shipping zones as a list
                shippingZones.forEach { zone ->
                    val isEditing = editingShippingZoneId == zone.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (isEditing) {
                            // Editable fields for the shipping zone
                            Column(modifier = Modifier.padding(12.dp)) {
                                OutlinedTextField(
                                    value = newShippingZoneName,
                                    onValueChange = { newShippingZoneName = it },
                                    label = { Text("Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = newShippingZoneCost,
                                    onValueChange = { newShippingZoneCost = it },
                                    label = { Text("Cost") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = newShippingZoneRegions,
                                    onValueChange = { newShippingZoneRegions = it },
                                    label = { Text("Regions") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row {
                                    Button(onClick = {
                                        val cost = newShippingZoneCost.toFloatOrNull()
                                        if (newShippingZoneName.isNotBlank() && cost != null) {
                                            shippingZones = shippingZones.map {
                                                if (it.id == zone.id) it.copy(
                                                    name = newShippingZoneName,
                                                    cost = cost,
                                                    regions = newShippingZoneRegions.split(",")
                                                        .map { r -> r.trim() }
                                                        .filter { r -> r.isNotEmpty() }
                                                ) else it
                                            }
                                            editingShippingZoneId = null
                                            newShippingZoneName = ""
                                            newShippingZoneCost = ""
                                            newShippingZoneRegions = ""
                                        }
                                    }) {
                                        Text("Save")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(onClick = {
                                        editingShippingZoneId = null
                                        newShippingZoneName = ""
                                        newShippingZoneCost = ""
                                        newShippingZoneRegions = ""
                                    }) {
                                        Text("Cancel")
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        zone.name ?: zone.id,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        "Cost: ${zone.cost}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        "Regions: ${zone.regions.joinToString()}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                IconButton(onClick = {
                                    // Edit: set edit mode and populate form fields
                                    editingShippingZoneId = zone.id
                                    newShippingZoneName = zone.name ?: ""
                                    newShippingZoneCost = zone.cost.toString()
                                    newShippingZoneRegions = zone.regions.joinToString(", ")
                                }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit Shipping Zone"
                                    )
                                }
                                IconButton(onClick = {
                                    // Delete: remove only the shipping zone at this index
                                    val idx = shippingZones.indexOf(zone)
                                    if (idx != -1) {
                                        shippingZones =
                                            shippingZones.toMutableList().apply { removeAt(idx) }
                                    }
                                }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Shipping Zone"
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                // --- Images section (copied from NewProductScreen) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Images", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            if (isValidImageUrl(newImageUrl)) {
                                images = images + newImageUrl.trim()
                                newImageUrl = ""
                            }
                        },
                        enabled = isValidImageUrl(newImageUrl)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Image")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newImageUrl,
                        onValueChange = { newImageUrl = it },
                        label = { Text("Image URL") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        isError = newImageUrl.isNotBlank() && !isValidImageUrl(newImageUrl)
                    )
                }
                if (newImageUrl.isNotBlank() && !isValidImageUrl(newImageUrl)) {
                    Text(
                        "Please enter a valid image URL (http/https, ends with .jpg, .png, etc.)",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                // --- Image preview cards with Coil ---
                images.forEachIndexed { idx, url ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            coil.compose.AsyncImage(
                                model = coil.request.ImageRequest.Builder(context)
                                    .data(url)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Image preview",
                                modifier = Modifier.size(48.dp).padding(end = 8.dp)
                            )
                            Text(url, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                images = images.toMutableList().apply { removeAt(idx) }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Image")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                // --- Categories section ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Categories", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = {
                        if (newCategoryValue.isNotBlank() && categories.none { it.value == newCategoryValue }) {
                            categories =
                                categories + TagUiModel(type = "t", value = newCategoryValue.trim())
                            newCategoryValue = ""
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Category")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCategoryValue,
                        onValueChange = { newCategoryValue = it },
                        label = { Text("Category") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                categories.forEachIndexed { idx, category ->
                    val isEditing = editingCategoryIndex == idx
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (isEditing) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                OutlinedTextField(
                                    value = newCategoryValue,
                                    onValueChange = { newCategoryValue = it },
                                    label = { Text("Category") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row {
                                    Button(onClick = {
                                        if (newCategoryValue.isNotBlank()) {
                                            categories = categories.mapIndexed { i, c ->
                                                if (i == idx) c.copy(value = newCategoryValue.trim()) else c
                                            }
                                            editingCategoryIndex = null
                                            newCategoryValue = ""
                                        }
                                    }) {
                                        Text("Save")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(onClick = {
                                        editingCategoryIndex = null
                                        newCategoryValue = ""
                                    }) {
                                        Text("Cancel")
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(category.value, modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    editingCategoryIndex = idx
                                    newCategoryValue = category.value
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Category")
                                }
                                IconButton(onClick = {
                                    categories = categories.toMutableList().apply { removeAt(idx) }
                                }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Category"
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                val showTagsForDevelopment =
                    false // Set to true to see the tags section for development

                if (showTagsForDevelopment) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tags", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.weight(1f))
                        Button(onClick = {
                            if (newTagType.isNotBlank() && newTagValue.isNotBlank() && newTagType.trim() != "t" && tags.none { it.type == newTagType && it.value == newTagValue }) {
                                tags = tags + TagUiModel(
                                    type = newTagType.trim(),
                                    value = newTagValue.trim()
                                )
                                newTagType = ""
                                newTagValue = ""
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Tag")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newTagType,
                            onValueChange = { newTagType = it },
                            label = { Text("Type") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = newTagValue,
                            onValueChange = { newTagValue = it },
                            label = { Text("Value") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Show tags as a list
                    tags.forEachIndexed { idx, tag ->
                        val isEditing = editingTagIndex == idx
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            if (isEditing) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    OutlinedTextField(
                                        value = newTagType,
                                        onValueChange = { newTagType = it },
                                        label = { Text("Type") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = newTagValue,
                                        onValueChange = { newTagValue = it },
                                        label = { Text("Value") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row {
                                        Button(onClick = {
                                            if (newTagType.isNotBlank() && newTagValue.isNotBlank()) {
                                                tags = tags.mapIndexed { i, t ->
                                                    if (i == idx) TagUiModel(
                                                        type = newTagType.trim(),
                                                        value = newTagValue.trim()
                                                    ) else t
                                                }
                                                editingTagIndex = null
                                                newTagType = ""
                                                newTagValue = ""
                                            }
                                        }) {
                                            Text("Save")
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(onClick = {
                                            editingTagIndex = null
                                            newTagType = ""
                                            newTagValue = ""
                                        }) {
                                            Text("Cancel")
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Type: ${tag.type}",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            "Value: ${tag.value}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            " ",
                                            style = MaterialTheme.typography.bodySmall
                                        ) // Use a space for consistent height
                                    }
                                    IconButton(onClick = {
                                        // Edit: set edit mode and populate form fields
                                        editingTagIndex = idx
                                        newTagType = tag.type
                                        newTagValue = tag.value
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Tag")
                                    }
                                    IconButton(onClick = {
                                        // Delete: remove only the tag at this index
                                        tags = tags.toMutableList().apply { removeAt(idx) }
                                    }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete Tag"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                errorState?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }


        // Log and fetch when stallEventId changes
        LaunchedEffect(stallEventId) {
            if (stallEventId != null && !isFormPopulated) {
                println("[NewStallScreen] Calling fetchStallByEventId with stallEventId=$stallEventId")
                android.util.Log.i(
                    "NewStallScreen",
                    "Calling fetchStallByEventId with stallEventId=$stallEventId"
                )
                viewModel.fetchStallByEventId(stallEventId)
            }
        }

        // Log when loadedStall changes
        LaunchedEffect(loadedStall) {
            // --- Only populate the form if it hasn't been populated yet ---
            if (loadedStall != null && !isFormPopulated) {
                println("[NewStallScreen] Populating form with loadedStall: $loadedStall")
                android.util.Log.i(
                    "NewStallScreen",
                    "Populating form with loadedStall: $loadedStall"
                )
                loadedStall?.let { stall ->
                    stallId = stall.stall_id
                    name = stall.name
                    description = stall.description ?: ""
                    currency = stall.currency
                    shippingZones = stall.shipping
                    // Load tags from envelope if present
                    val tagsFromEnvelope = stall.envelope?.tags?.mapNotNull {
                        if (it.size >= 2) {
                            TagUiModel(type = it[0], value = it[1])
                        } else null
                    } ?: emptyList()
                    categories = tagsFromEnvelope.filter { it.type == "t" }
                    //tags = tagsFromEnvelope.filter { it.type != "t" && it.type != "d" && it.type != "name" }
                    tags = tagsFromEnvelope.filter { it.type != "t" }
                }
                isFormPopulated = true // Mark the form as populated
            }
        }
    }
}