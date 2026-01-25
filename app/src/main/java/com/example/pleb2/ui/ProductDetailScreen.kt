package com.example.pleb2.ui
//TODO 'val LocalClipboardManager: ProvidableCompositionLocal<ClipboardManager>' is deprecated. Use LocalClipboard instead which supports suspend functions.
//read OrderScreen for this TODO

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ConnectWithoutContact
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import com.example.pleb2.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    // productId: String,
    productEventId: String,
    navController: androidx.navigation.NavController,
    // setBottomBarVisibility: (Boolean) -> Unit
) {
    val navGraphRoute = navController.graph.findStartDestination().route!!
    val parentEntry = remember(navController.currentBackStackEntry) {
        navController.getBackStackEntry(navGraphRoute)
    }
    val viewModel: AndroidProductDetailViewModel = hiltViewModel(parentEntry)

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var selectedImageIdx by rememberSaveable { mutableIntStateOf(0) }

    val productUiModel by viewModel.shared.productUiModel.collectAsState()
    val mainImageUrl = productUiModel?.images?.getOrNull(selectedImageIdx)

    val imageLoader = ImageLoader.Builder(androidx.compose.ui.platform.LocalContext.current)
        .components {
            add(ImageDecoderDecoder.Factory())
        }
        .build()

    // NO REMOVE TODO add deduplication logic, this already in the data layer dumb
    // Log when ProductDetailScreen is shown
    LaunchedEffect(Unit) {
        Log.i(
            "ProductDetail events",
            "debug workflow: ProductDetailScreen shown for productId=$productEventId"
        )
    }
    /*
    DisposableEffect(Unit) {
        setBottomBarVisibility(false)
        onDispose {
            setBottomBarVisibility(true)
        }
    }
    */

    val scrollState = rememberScrollState()
    LaunchedEffect(productEventId) {
        if (viewModel.shared.productUiModel.value?.event_id != productEventId) {
            Log.i("nostr data requested", "Requesting product by event-id: $productEventId")
            viewModel.shared.loadProductAndStall(productEventId)
        }
    }
    val isLoadingState = viewModel.shared.isLoading.collectAsState()
    val isLoading = isLoadingState.value
    val error by viewModel.shared.error.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    productUiModel?.let {
                        Text(
                            text = it.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
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
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()

        ) {
            // Show loading indicator above Snackbar
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
            if (error != null) {
                val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
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
                            androidx.compose.material3.TextButton(onClick = {
                                viewModel.shared.loadProductAndStall(
                                    productEventId
                                )
                            }) {
                                Text("Retry", color = MaterialTheme.colorScheme.onError)
                            }
                            androidx.compose.material3.TextButton(onClick = { viewModel.shared.clearError() }) {
                                Text("Dismiss", color = MaterialTheme.colorScheme.onError)
                            }
                            // TODO: Add a 'Report' action here to let users report the error directly
                            androidx.compose.material3.TextButton(onClick = {
                                clipboardManager.setText(
                                    androidx.compose.ui.text.AnnotatedString(
                                        error ?: ""
                                    )
                                )
                            }) {
                                Text("Copy", color = MaterialTheme.colorScheme.onError)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.foundation.text.selection.SelectionContainer {
                            Text(
                                error ?: "Unknown error",
                                color = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            val productUiModel by viewModel.shared.productUiModel.collectAsState()
            val loadedStall by viewModel.shared.loadedStall.collectAsState()

            /* LaunchedEffect(productUiModel) {
            val product = productUiModel
            if (product != null) {
                val stallId = product.stall_id
                val pubkey = product.pubkey
                if (stallId.isNotEmpty() && pubkey.isNotEmpty()) {
                    Log.i("ProductDetail events", "debug workflow: Product loaded, now fetching stall with stallId=$stallId and pubkey=$pubkey")
                    viewModel.shared.fetchStallByStallId(stallId, pubkey)
                }
            }
        } */

            if (isLoading) {
                /*
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator()
            }
            */
            } else {
                productUiModel?.let { product ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 6.dp),
                            //.padding(top = 32.dp), // Add top padding to ensure enough space for 4:3 image
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val state = rememberTransformableState { zoomChange, panChange, _ ->
                                val oldScale = scale
                                val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                                // Calculate the center shift to keep image centered on zoom
                                if (zoomChange != 1f) {
                                    val centerX =
                                        0f // Center for translation (could use size/2 if needed)
                                    val centerY = 0f
                                    offsetX =
                                        ((offsetX - centerX) * (newScale / oldScale)) + centerX
                                    offsetY =
                                        ((offsetY - centerY) * (newScale / oldScale)) + centerY
                                }
                                scale = newScale
                                // Only allow panning if zoomed in
                                if (scale > 1f) {
                                    offsetX += panChange.x
                                    offsetY += panChange.y
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                            val doubleTapModifier = Modifier.pointerInput(scale) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                )
                            }
                            // Main image (GIF support)
                            if (!mainImageUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = mainImageUrl,
                                    imageLoader = imageLoader,
                                    contentDescription = stringResource(R.string.product_image),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(250.dp)
                                        .padding(top = 16.dp, start = 8.dp, end = 8.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .aspectRatio(4f / 3f)
                                        .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                            translationX = offsetX,
                                            translationY = offsetY
                                        )
                                        .then(doubleTapModifier)
                                        .transformable(state),
                                    contentScale = ContentScale.Fit, // Show full image, never cut
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.no_image),
                                    contentDescription = stringResource(R.string.product_image),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(250.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                            translationX = offsetX,
                                            translationY = offsetY
                                        )
                                        .then(doubleTapModifier)
                                        .transformable(state),
                                    contentScale = ContentScale.Fit // Fill area, crop if needed
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            // Thumbnails (GIF support)
                            if (!product.images.isNullOrEmpty()) {
                                Row(
                                    modifier = Modifier.horizontalScroll(scrollState),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    product.images?.forEachIndexed { idx, url ->
                                        AsyncImage(
                                            model = url,
                                            imageLoader = imageLoader,
                                            contentDescription = stringResource(R.string.thumbnail),
                                            modifier = Modifier
                                                .size(60.dp)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .border(
                                                    2.dp,
                                                    if (idx == selectedImageIdx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable { selectedImageIdx = idx },
                                            contentScale = ContentScale.Crop // Fill area, crop if needed
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            // Details Card
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val price = product.price
                                    if (price > 0f) {
                                        Text(
                                            text = stringResource(
                                                R.string.price_label,
                                                price,
                                                product.currency
                                            ),
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                color = MaterialTheme.colorScheme.tertiary, // Use theme color
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                    // Stock
                                    val quantity = product.quantity
                                    val stockText =
                                        if (quantity != null && quantity > 0) quantity.toString() else "Out of stock"
                                    val stockColor =
                                        if (quantity != null && quantity > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    Text(
                                        text = stringResource(R.string.stock_label, stockText),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = stockColor,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    // Shipping (show if not empty)
                                    if (product.shipping.isNotEmpty()) {
                                        val shippingZoneMap =
                                            loadedStall?.shipping?.associateBy { it.id }
                                        /* val shippingNames = if (shippingZoneMap != null) {
                                        product.shipping.mapNotNull { productShipping ->
                                            shippingZoneMap[productShipping.id]?.name
                                        }.joinToString(", ")
                                    } else {
                                        null // Indicate that stall data is not yet loaded
                                    } */
                                        val shippingRegions = if (shippingZoneMap != null) {
                                            product.shipping.flatMap { productShipping ->
                                                shippingZoneMap[productShipping.id]?.regions
                                                    ?: emptyList()
                                            }.joinToString(", ")
                                        } else {
                                            null // Indicate that stall data is not yet loaded
                                        }

                                        // Use names if available and not empty, otherwise fall back to IDs
                                        /* val shippingInfo = if (!shippingNames.isNullOrEmpty()) {
                                        shippingNames
                                    } else {
                                        product.shipping.joinToString { it.id }
                                    } */
                                        val shippingInfo = if (!shippingRegions.isNullOrEmpty()) {
                                            shippingRegions
                                        } else {
                                            product.shipping.joinToString { it.id }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.LocalShipping,
                                                contentDescription = stringResource(
                                                    R.string.shipping_label,
                                                    ""
                                                ),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = stringResource(
                                                    R.string.shipping_label,
                                                    shippingInfo
                                                ),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                    // Stall
                                    if (product.stall_id.isNotEmpty()) {
                                        val stallName = loadedStall?.name ?: product.stall_id
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Store,
                                                contentDescription = stringResource(
                                                    R.string.stall_label,
                                                    stallName
                                                ),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = stringResource(
                                                    R.string.stall_label,
                                                    stallName
                                                ),
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.clickable {
                                                    navController.navigate("stallDetail/${product.stall_id}")
                                                }
                                            )
                                        }
                                    }
                                    // Pubkey and createdAt from envelope
                                    val createdAt = product.created_at
                                    /* val pubkey = product.pubkey
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Store, contentDescription = stringResource(R.string.seller_label, pubkey.take(12)), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.seller_label, pubkey.take(12)),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                } */
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(
                                            R.string.created_label,
                                            product.formattedCreatedAt ?: ""
                                        ),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(
                                        onClick = {
                                            //TODO define what subject id / conversation id we will use for orders
                                            val conversationId = productEventId
                                            val partnerPubkey = product.pubkey
                                            navController.navigate("order/$conversationId/$partnerPubkey")
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.tertiary, // Use theme color
                                            contentColor = MaterialTheme.colorScheme.onTertiary
                                        )
                                    ) {
                                        Icon(
                                            //Icons.Default.ShoppingCart,
                                            Icons.Default.ConnectWithoutContact,
                                            contentDescription = stringResource(R.string.buy),
                                            tint = MaterialTheme.colorScheme.onTertiary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            stringResource(R.string.buy),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))

                            // Specifications Section
                            if (product.specs.isNotEmpty()) {
                                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    Text(
                                        text = stringResource(R.string.specifications),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    product.specs.forEach { spec ->
                                        if (spec.size >= 2) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = spec[0].replaceFirstChar {
                                                        if (it.isLowerCase()) it.titlecase(
                                                            java.util.Locale.getDefault()
                                                        ) else it.toString()
                                                    },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = spec[1],
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    textAlign = TextAlign.End,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            androidx.compose.material3.HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outline.copy(
                                                    alpha = 0.2f
                                                )
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                            }

                            // Description Section
                            Text(
                                text = product.description ?: "",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                maxLines = Int.MAX_VALUE,
                                overflow = TextOverflow.Visible,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Start
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            // "Sold by" Card at the bottom
                            /*
                        loadedStall?.let { stall ->
                            Text(
                                text = "Sold by:",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                                textAlign = TextAlign.Start
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        navController.navigate("stallDetail/${stall.stall_id}")
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = stall.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = stall.description ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        */
                        }
                    }
                } ?: run {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Product not found", modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}