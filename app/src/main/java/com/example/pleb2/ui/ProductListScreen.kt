package com.example.pleb2.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.pleb2.R
import com.example.pleb2.ui.components.AppDrawerContent
import com.example.pleb2.ui.components.DrawerNavItem
import com.example.pleb2.ui.components.ProductCard
import com.example.pleb2.ui.theme.LocalThemeState
import com.example.data.uiModels.ProductUiModel
import com.example.data.viewmodel.ProductListNavigationEvent
import com.example.data.viewmodel.ProductListUiEffect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    navController: androidx.navigation.NavController,
    nestedNavController: androidx.navigation.NavController,
    // setBottomBarVisibility: (Boolean) -> Unit,
    homeTapEvents: kotlinx.coroutines.flow.SharedFlow<Unit>,
    onboardingViewModel: AndroidOnboardingViewModel
) {
    val navGraphRoute = nestedNavController.graph.findStartDestination().route!!
    val parentEntry = remember(nestedNavController.currentBackStackEntry) {
        nestedNavController.getBackStackEntry(navGraphRoute)
    }
    val viewModel: AndroidProductListViewModel = hiltViewModel(parentEntry)

    val localFocusManager = LocalFocusManager.current
    val filterState by viewModel.filterState.collectAsState()
    val searchText = filterState.searchText
    val selectedTag = filterState.selectedTag
    val showTagSuggestions: Boolean by viewModel.showTagSuggestions.collectAsState()
    val tagSuggestions: List<String> by viewModel.tagSuggestions.collectAsState(emptyList())
    val renderedCount by viewModel.renderedCount.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val productUiModels: List<ProductUiModel> by viewModel.productUiModels.collectAsState()
    val error by viewModel.error.collectAsState()
    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
// THIS IS JUST FOR REFERENCE DONT UNCOMMENT THIS CODE TRASH
    // This effect will scroll to the top of the list whenever the loading process finishes.
    // This ensures that after the initial load or a refresh, the newest items are visible.
//    LaunchedEffect(isLoading) {
//        if (!isLoading) {
//            // Using scope.launch to call a suspend function from a non-suspend context
//            scope.launch {
//                listState.scrollToItem(0)
//            }
//        }
//    }

    LaunchedEffect(Unit) {
        Log.d("debug workflow", "ProductListScreen.kt loaded")
    }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collectLatest {
            when (it) {
                is ProductListUiEffect.ClearFocus -> localFocusManager.clearFocus()
                is ProductListUiEffect.ScrollToTop -> {
                    scope.launch {
                        listState.scrollToItem(0)
                        //listState.animateScrollToItem(0) //slow af
                    }
                }
            }
        }
    }

    LaunchedEffect(listState) {
        var lastScrollIndex = 0
        var lastScrollOffset = 0
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                // --- FIX: Re-enable bottom bar logic, now guarded by isLoading ---
//                if (!isLoading) {
//                    val isScrollingDown =
//                        index > lastScrollIndex || (index == lastScrollIndex && offset > lastScrollOffset + 8)
//                    val isScrollingUp =
//                        index < lastScrollIndex || (index == lastScrollIndex && offset < lastScrollOffset - 8)
//
//                    if (isScrollingDown) {
//                        setBottomBarVisibility(false)
//                    } else if (isScrollingUp) {
//                        setBottomBarVisibility(true)
//                    }
//                }
                // --------------------------------------------------------------------

                lastScrollIndex = index
                lastScrollOffset = offset

                viewModel.onScroll(
                    firstVisibleItemIndex = index,
                    firstVisibleItemScrollOffset = offset,
                    visibleItemsCount = listState.layoutInfo.visibleItemsInfo.size,
                    totalItemsCount = productUiModels.size
                )
            }
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collectLatest { event ->
            when (event) {
                is ProductListNavigationEvent.NavigateToProductDetail -> {
                    // navController.navigate("productDetail/${event.productId}") // Old: used productId
                    navController.navigate("productDetail/${event.event_id}") // CRITICAL CHANGE: use eventId
                }
                // Exhaustive when
            }
        }
    }

    LaunchedEffect(homeTapEvents) {
        homeTapEvents.collect {
            if (isLoading) return@collect // Prevent action if already loading

            val shouldReset = filterState.searchText.isNotEmpty() || filterState.selectedTag != "All"
            if (shouldReset) {
                viewModel.resetFilters()
            }
            // Always scroll to top on home tap
            scope.launch {
                listState.scrollToItem(0)
            }
        }
    }

    // Intercept back button to clear focus or hide suggestions or search instead of closing the app
    BackHandler(enabled = showTagSuggestions || searchText.isNotEmpty()) {
        if (showTagSuggestions) {
            // Just hide suggestions, keep the text
            viewModel.hideSuggestions()
        } else if (searchText.isNotEmpty()) {
            viewModel.resetFilters()
            scope.launch {
                listState.scrollToItem(0)
            }
        }
        localFocusManager.clearFocus()
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
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
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
                    text = "🌶️",
                    fontSize = 32.sp,
                    modifier = Modifier
                        .clickable { scope.launch { drawerState.open() } }
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { value ->
                        viewModel.onSearchTextChanged(value)
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            viewModel.onSearchSubmitted(searchText)
                            localFocusManager.clearFocus()
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    placeholder = {
                        Text(
                            stringResource(R.string.search_placeholder),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp)
                        )
                    }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            // --- Error Snackbar (Material3, theme error colors, Retry) ---
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
                            TextButton(
                                onClick = {
                                    viewModel.reloadProducts()
                                }
                            ) {
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
                        androidx.compose.foundation.text.selection.SelectionContainer {
                            Text(
                                error ?: "Unknown error",
                                color = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                }
            }
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
            // In the tag suggestion dropdown, after selecting a suggestion, clear focus and ensure dropdown is hidden
            Box(modifier = Modifier.fillMaxWidth()) {
                if (showTagSuggestions && tagSuggestions.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 0.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    ) {
                        tagSuggestions.forEach { tag ->
                            Text(
                                text = tag,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        Log.i("ProductList events", "Tag suggestion clicked: $tag")
                                        viewModel.updateSelectedTag(tag)
                                        scope.launch {
                                            listState.scrollToItem(0)
                                        }
                                        // Hide keyboard and clear focus to ensure dropdown disappears
                                        localFocusManager.clearFocus()
                                    }
                                    .padding(12.dp),
                                fontSize = 13.sp,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp)
                            )
                        }
                    }
                }
            }
            ProductListContent(
                modifier = Modifier.weight(1f),
                isLoading = isLoading,
                productUiModels = productUiModels,
                renderedCount = renderedCount,
                listState = listState,
                onProductClicked = { event_id -> // CRITICAL CHANGE: pass event.id instead of product_id
                    // viewModel.shared.onProductClicked(model.product_id) // Old: used product_id
                    viewModel.onProductClicked(event_id) // Now using event.id
                },
                onVerificationFailed = { eventId ->
                    viewModel.reportVerificationError(eventId)
                }
            )
        }
    }
}

@Composable
fun ProductListContent(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    productUiModels: List<ProductUiModel>,
    renderedCount: Int,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onProductClicked: (String) -> Unit,
    onVerificationFailed: (String) -> Unit
) {
    if (isLoading && productUiModels.isEmpty()) {
        // Intentionally blank. The top LinearProgressIndicator is the only loading indicator.
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier // CRITICAL FIX: Removed .fillMaxSize()
        ) {
            items(
                items = productUiModels.sortedByDescending { it.created_at }.take(renderedCount),
                key = { it.event_id }
            ) { model ->
                ProductCard(
                    model = model,
                    onClick = { onProductClicked(model.event_id) },
                    onVerificationResult = { isVerified ->
                        if (!isVerified) {
                            onVerificationFailed(model.event_id)
                        }
                    }
                )
            }
        }
    }
}
