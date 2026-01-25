// Test comment: Verifying AI file write access - 2026-01-24
package com.example.pleb2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.uiModels.ProductShippingZoneUiModel
import com.example.data.uiModels.TagUiModel
import java.util.UUID
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.example.data.util.CurrencyList
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuAnchorType
import com.example.data.uiModels.StallUiModel
import com.example.pleb2.util.isValidImageUrl
import androidx.navigation.NavGraph.Companion.findStartDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProductScreen(
    navController: androidx.navigation.NavController,
    stallEventId: String? = null,
    productEventId: String? = null, // Renamed for navigation
    // setBottomBarVisibility: (Boolean) -> Unit
) {
    val navGraphRoute = navController.graph.findStartDestination().route!!
    val parentEntry = remember(navController.currentBackStackEntry) {
        navController.getBackStackEntry(navGraphRoute)
    }
    val viewModel: AndroidNewProductViewModel = hiltViewModel(parentEntry)

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.clearState()
        }
    }

    // TODO: State Management in Composables - For screens with many input fields,
    //  it can become cumbersome to manage. A common "best practice" for complex forms
    //  is to encapsulate the form's state within a single data class.
    // --- Product State Variables ---
    var productId by remember { mutableStateOf(productEventId ?: UUID.randomUUID().toString()) }
    var stallId by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var images by remember { mutableStateOf<List<String>>(emptyList()) }
    var currency by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var specs by remember { mutableStateOf<List<List<String>>>(emptyList()) }
    var shipping by remember { mutableStateOf<List<ProductShippingZoneUiModel>>(emptyList()) }
    var tags by remember { mutableStateOf<List<TagUiModel>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    // Category management state
    var categories by remember { mutableStateOf<List<TagUiModel>>(emptyList()) }
    var newCategoryValue by remember { mutableStateOf("") }

    // UI state for adding new shipping zone, spec, image, etc.
    var newSpecKey by remember { mutableStateOf("") }
    var newSpecValue by remember { mutableStateOf("") }
    var newImageUrl by remember { mutableStateOf("") }
    var newTagType by remember { mutableStateOf("") }
    var newTagValue by remember { mutableStateOf("") }

    // Editing state
    var editingTagIndex by remember { mutableStateOf<Int?>(null) }
    var editingCategoryIndex by remember { mutableStateOf<Int?>(null) }

    // ViewModel state
    val eventState by viewModel.eventState.collectAsState()
    val errorState by viewModel.error.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState(initial = false)
    val loadedProduct by viewModel.loadedProduct.collectAsState()
    val loadedStall: StallUiModel? by viewModel.loadedStall.collectAsState()

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

    // Populate only the stallId and shipping from loadedStall for now
    LaunchedEffect(loadedStall) {
        //println("[NewProductScreen] loadedStall updated: $loadedStall")
        android.util.Log.i("NewProductScreen.kt", "loadedStall updated: $loadedStall")
        loadedStall?.let { stall ->
            stallId = stall.stall_id
            shipping = stall.shipping.map { ProductShippingZoneUiModel(it.id, it.cost) }
            // No tags assignment here, tags are handled only in loadedProduct
        }
    }

    // Populate all fields from loadedProduct when editing
    LaunchedEffect(loadedProduct) {
        // --- Only populate the form if it hasn't been populated yet ---
        if (loadedProduct != null && !isFormPopulated) {
            android.util.Log.i(
                "NewProductScreen.kt",
                "Populating form with loadedProduct: $loadedProduct"
            )
            loadedProduct?.let { product ->
                productId = product.product_id
                stallId = product.stall_id
                name = product.name
                description = product.description ?: ""
                images = product.images ?: emptyList()
                currency = product.currency
                price = product.price.toString()
                quantity = product.quantity?.toString() ?: ""
                specs = product.specs
                shipping = product.shipping.map { ProductShippingZoneUiModel(it.id, it.cost) }
                // Load tags from envelope if present (like NewStallScreen)
                val tagsFromEnvelope = product.envelope?.tags?.mapNotNull {
                    if (it.size >= 2) TagUiModel(type = it[0], value = it[1]) else null
                } ?: emptyList()
                categories = tagsFromEnvelope.filter { it.type == "t" }
                tags = tagsFromEnvelope.filter { it.type != "t" }
            }
            isFormPopulated = true // Mark the form as populated
        }
    }

    // Log when the screen is entered for navigation
    LaunchedEffect(Unit) {
        //println("[NewProductScreen] Composable entered, stallEventId=$stallEventId, productId=$productId")
        android.util.Log.i(
            "NewProductScreen.kt",
            "Composable entered, stallEventId=$stallEventId, productId=$productId"
        )
    }

    // Log and fetch when eventId changes
    LaunchedEffect(stallEventId) {
        if (stallEventId != null && !isFormPopulated) {
            //println("[NewProductScreen] Calling fetchStallByEventId with stallEventId=$stallEventId")
            android.util.Log.i(
                "NewProductScreen.kt",
                "Calling fetchStallByEventId with stallEventId=$stallEventId"
            )
            viewModel.fetchStallByEventId(stallEventId)
        }
    }

    // Fetch product if editing
    LaunchedEffect(productEventId) {
        if (productEventId != null && !isFormPopulated) {
            //println("[NewProductScreen] Calling fetchProductByEventId with productEventId=$productEventId")
            android.util.Log.i(
                "NewProductScreen.kt",
                "Calling fetchProductByEventId with productEventId=$productEventId"
            )
            viewModel.fetchProductByEventId(productEventId)
        }
    }

    // --- Broadcast error snackbar (ProductDetailScreen style) ---
    var broadcastError by remember { mutableStateOf(false) }
    var showBroadcastSnackbar by remember { mutableStateOf(false) }
    var broadcastSnackbarMessage by remember { mutableStateOf("") }
    var broadcastSnackbarIsError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (productEventId == null) "Create Product" else "Edit Product") },
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
                                viewModel.createAndBroadcastProductEvent(
                                    id = productId,
                                    stallId = stallId,
                                    name = name,
                                    description = description,
                                    images = images,
                                    currency = currency,
                                    price = price.toFloatOrNull(),
                                    quantity = quantity.toIntOrNull(),
                                    specs = specs,
                                    shipping = shipping,
                                    tags = tags + categories
                                ) { relayResults, eventId ->
                                    val allOk =
                                        relayResults.isNotEmpty() && relayResults.values.all { it }
                                    if (allOk) {
                                        navController.popBackStack()
                                    } else {
                                        broadcastSnackbarMessage = errorState ?: "Broadcast failed!"
                                        broadcastSnackbarIsError = true
                                        showBroadcastSnackbar = true
                                    }
                                }
                            },
                            modifier = Modifier.padding(end = 8.dp) // Add right padding to the Save button
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Save Product")
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
        // Remove Scaffold and use a plain Column as the root, like ProductDetailScreen
        /*
        var broadcastError by remember { mutableStateOf(false) }
        var showBroadcastSnackbar by remember { mutableStateOf(false) }
        var broadcastSnackbarMessage by remember { mutableStateOf("") }
        var broadcastSnackbarIsError by remember { mutableStateOf(false) }
        */
        /*eventState?.let { event ->
        LaunchedEffect(event) {
            //println("[NewProductScreen] Created event: $event")
            android.util.Log.i("NewProductScreen.kt", "Created event: $event")
            viewModel.broadcastProductEvent(event) { relayResults ->
                val allOk = relayResults.values.all { it }
                if (allOk) {
                    broadcastSnackbarMessage = "Event published! ID: ${event.id}"
                    broadcastSnackbarIsError = false
                    showBroadcastSnackbar = true
                    broadcastError = false
                } else {
                    broadcastSnackbarMessage = errorState ?: "Broadcast failed!"
                    broadcastSnackbarIsError = true
                    showBroadcastSnackbar = true

                }
            } // for TEST
        }
    }*/
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
                                        viewModel.createAndBroadcastProductEvent(
                                            id = productId,
                                            stallId = stallId,
                                            name = name,
                                            description = description,
                                            images = images,
                                            currency = currency,
                                            price = price.toFloatOrNull(),
                                            quantity = quantity.toIntOrNull(),
                                            specs = specs,
                                            shipping = shipping,
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
                // Product ID (read-only)
                OutlinedTextField(
                    value = productId,
                    onValueChange = {},
                    label = { Text("Product ID (auto-generated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Stall ID (read-only, passed from navigation)
                OutlinedTextField(
                    value = stallId,
                    onValueChange = {},
                    label = { Text("Stall ID (from selected stall)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false
                )
                //TODO All those non editable fields must be just hide ????
                Spacer(modifier = Modifier.height(8.dp))
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
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
                        modifier = Modifier.fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
                Spacer(modifier = Modifier.height(8.dp))
                // Price
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Quantity
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                //Spacer(modifier = Modifier.height(8.dp))
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // Images (add/remove)
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
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val context = LocalContext.current
                            AsyncImage(
                                model = ImageRequest.Builder(context)
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
                // Specs (add/edit/delete)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Specs", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = {
                        if (newSpecKey.isNotBlank() && newSpecValue.isNotBlank()) {
                            specs = specs + listOf(listOf(newSpecKey.trim(), newSpecValue.trim()))
                            newSpecKey = ""
                            newSpecValue = ""
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Spec")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newSpecKey,
                        onValueChange = { newSpecKey = it },
                        label = { Text("Key") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = newSpecValue,
                        onValueChange = { newSpecValue = it },
                        label = { Text("Value") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                specs.forEachIndexed { idx, spec ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${spec.getOrNull(0) ?: ""}: ${spec.getOrNull(1) ?: ""}",
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                specs = specs.toMutableList().apply { removeAt(idx) }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Spec")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                // Shipping Zones (select from stall's zones)
                val availableZones = loadedStall?.shipping ?: emptyList()
                Text("Shipping Zones", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                availableZones.forEach { zone ->
                    val selected = shipping.any { it.id == zone.id }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selected,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    shipping = shipping + ProductShippingZoneUiModel(
                                        zone.id,
                                        zone.cost
                                    )
                                } else {
                                    shipping = shipping.filterNot { it.id == zone.id }
                                }
                            }
                        )
                        Text(zone.name ?: zone.id, modifier = Modifier.weight(1f))
                        // Show base cost from stall (uneditable)
                        OutlinedTextField(
                            value = zone.cost.toString(),
                            onValueChange = {},
                            label = { Text("Base Cost (Stall)") },
                            singleLine = true,
                            modifier = Modifier.width(100.dp),
                            enabled = false
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Show extra cost (editable if selected)
                        var extraCostText by remember(zone.id) { mutableStateOf("") }
                        OutlinedTextField(
                            value = extraCostText,
                            onValueChange = { newCost ->
                                extraCostText = newCost
                                val extraCost = newCost.toFloatOrNull() ?: 0f
                                if (selected) {
                                    shipping = shipping.map {
                                        if (it.id == zone.id) it.copy(cost = zone.cost + extraCost) else it
                                    }
                                }
                            },
                            label = { Text("Extra") },
                            placeholder = { Text("0.0") },
                            singleLine = true,
                            modifier = Modifier.width(100.dp),
                            //modifier = Modifier.weight(1f),
                            enabled = selected
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                // Categories (add/edit/delete)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Categories", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = {
                        if (newCategoryValue.isNotBlank() && categories.none { it.value == newCategoryValue }) {
                            categories = categories + TagUiModel(
                                type = "t",
                                value = newCategoryValue.trim()
                            )
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
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit Category"
                                    )
                                }
                                IconButton(onClick = {
                                    categories =
                                        categories.toMutableList().apply { removeAt(idx) }
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
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                val showTagsForDevelopment =
                    true // Set to true to see the tags section for development

                if (showTagsForDevelopment) {
                    // Tags (add/edit/delete)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tags", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.weight(1f))
                        Button(onClick = {
                            if (newTagType.isNotBlank() && newTagValue.isNotBlank() && tags.none { it.type == newTagType && it.value == newTagValue }) {
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
                                        Text(" ", style = MaterialTheme.typography.bodySmall)
                                    }
                                    IconButton(onClick = {
                                        editingTagIndex = idx
                                        newTagType = tag.type
                                        newTagValue = tag.value
                                    }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit Tag"
                                        )
                                    }
                                    IconButton(onClick = {
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
            }
        }
    }
}