package com.example.pleb2

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pleb2.ui.AndroidOnboardingViewModel
import com.example.pleb2.ui.MyStallScreen
import com.example.pleb2.ui.NewProductScreen
import com.example.pleb2.ui.NewStallScreen
import com.example.pleb2.ui.OrderBookScreen
import com.example.pleb2.ui.OrderScreen
import com.example.pleb2.ui.ProductDetailScreen
import com.example.pleb2.ui.ProductListScreen
import com.example.pleb2.ui.RelayScreen
import com.example.pleb2.ui.components.BottomNavigationBar
import com.example.pleb2.ui.theme.LocalThemeState
import com.example.pleb2.ui.theme.Pleb2Theme
import com.example.pleb2.ui.theme.ThemeState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Removed FLAG_SECURE for global app, now handled per-screen in AccountScreen
        enableEdgeToEdge()
        Log.i("pleb2-debug", "MainActivity with Compose navigation onCreate called")
        setContent {
            var darkMode by rememberSaveable { mutableStateOf(true) } // Default to dark mode
            val themeState = ThemeState(isDarkMode = darkMode) { darkMode = !darkMode }

            CompositionLocalProvider(LocalThemeState provides themeState) {
                Pleb2Theme(darkTheme = LocalThemeState.current.isDarkMode) {
                    val surfaceColor = MaterialTheme.colorScheme.surface
                    // This SideEffect will update the system bar icons color based on the theme
                    val view = LocalView.current
                    val isDarkTheme = LocalThemeState.current.isDarkMode
                    if (!view.isInEditMode) {
                        SideEffect {
                            @Suppress("DEPRECATION")
                            window.statusBarColor = surfaceColor.toArgb() // Use standard API, WindowCompat does not have setStatusBarColor
                            WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = !isDarkTheme
                            WindowInsetsControllerCompat(window, view).isAppearanceLightNavigationBars = !isDarkTheme
                        }
                    }

                    Surface(modifier = Modifier.fillMaxSize(), color = surfaceColor) {
                        AppNavHost()
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    // Onboarding logic
    val onboardingViewModel: AndroidOnboardingViewModel = viewModel()
    val hasKey by onboardingViewModel.shared.hasKey.collectAsState()
    var onboardingChecked by remember { mutableStateOf(false) }

    // Only check onboarding once
    LaunchedEffect(Unit) {
        onboardingViewModel.shared.checkKey()
        onboardingChecked = true
    }

    if (onboardingChecked && hasKey == false) {
        // Show onboarding screen if no key
        com.example.pleb2.ui.OnboardingScreen(
            onOnboardingComplete = {
                // After onboarding, navigate to main screen
                navController.navigate("main") {
                    popUpTo("main") { inclusive = true }
                }
            }
        )
        return
    }

    NavHost(
        navController = navController,
        startDestination = "main",
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        composable("main") {
            MainScreen(
                rootNavController = navController,
                onboardingViewModel = onboardingViewModel
            )
        }
        // Other Screens that should NOT have a bottom bar
        composable(
            "productDetail/{productEventId}",
            arguments = listOf(navArgument("productEventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productEventId = backStackEntry.arguments?.getString("productEventId") ?: ""
            ProductDetailScreen(
                productEventId = productEventId,
                navController = navController,
                // setBottomBarVisibility = { } // No-op, will be removed later
            )
        }
        composable("relay") {
            RelayScreen(
                navController = navController
            )
        }
        composable(
            "order/{conversationId}/{partnerPubkeys}",
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType },
                navArgument("partnerPubkeys") { type = NavType.StringType }
            )
        ) {
            OrderScreen(
                navController = navController,
                // setBottomBarVisibility = { } // No-op, will be removed later
            )
        }
        composable("account") {
            com.example.pleb2.ui.AccountScreen(
                navController = navController,
                onboardingViewModel = onboardingViewModel,
                onLogout = {
                    // After logout, navigate to onboarding (handled by top-level logic)
                    navController.navigate("main") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }
        composable(
            "stallDetail/{stallId}",
            arguments = listOf(navArgument("stallId") { type = NavType.StringType })
        ) { backStackEntry ->
            val stallId = backStackEntry.arguments?.getString("stallId") ?: ""
            com.example.pleb2.ui.StallDetailScreen(
                stallId = stallId,
                navController = navController
            )
        }
        composable(
            "myStallDetail/{stallEventId}",
            arguments = listOf(navArgument("stallEventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val stallEventId = backStackEntry.arguments?.getString("stallEventId") ?: ""
            com.example.pleb2.ui.MyStallDetailScreen(
                eventId = stallEventId,
                navController = navController
            )
        }
        composable("createStall") {
            NewStallScreen(
                // setBottomBarVisibility = { }, // No-op, will be removed later
                navController = navController
            )
        }
        composable(
            "editStall/{stallEventId}",
            arguments = listOf(navArgument("stallEventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val stallEventId = backStackEntry.arguments?.getString("stallEventId")
            NewStallScreen(
                stallEventId = stallEventId,
                // setBottomBarVisibility = { }, // No-op, will be removed later
                navController = navController
            )
        }
        composable(
            "createProduct/{stallEventId}",
            arguments = listOf(navArgument("stallEventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val stallEventId = backStackEntry.arguments?.getString("stallEventId")
            NewProductScreen(
                navController = navController,
                stallEventId = stallEventId,
                // setBottomBarVisibility = { } // No-op, will be removed later
            )
        }
        composable(
            "editProduct/{stallEventId}/{productEventId}",
            arguments = listOf(
                navArgument("stallEventId") { type = NavType.StringType },
                navArgument("productEventId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val stallEventId = backStackEntry.arguments?.getString("stallEventId")
            val productEventId = backStackEntry.arguments?.getString("productEventId")
            NewProductScreen(
                navController = navController,
                stallEventId = stallEventId,
                productEventId = productEventId,
                // setBottomBarVisibility = { } // No-op, will be removed later
            )
        }
    }
}

@Composable
fun MainScreen(
    rootNavController: androidx.navigation.NavController,
    onboardingViewModel: AndroidOnboardingViewModel
) {
    val nestedNavController = rememberNavController()
    val navBackStackEntry by nestedNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // SharedFlow to notify Home button taps
    val homeTapFlow = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
    val homeTapEvents: SharedFlow<Unit> = homeTapFlow.asSharedFlow()

    // SharedFlow to notify Stall button taps
    val stallTapFlow = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                selectedRoute = currentRoute ?: "productList",
                onHomeClick = {
                    if (nestedNavController.currentBackStackEntry?.destination?.route == "productList") {
                        homeTapFlow.tryEmit(Unit)
                    } else {
                        nestedNavController.navigate("productList") {
                            popUpTo(nestedNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                onStallClick = {
                    if (nestedNavController.currentBackStackEntry?.destination?.route == "myStall") {
                        stallTapFlow.tryEmit(Unit)
                    } else {
                        // onboardingViewModel.shared.checkKey()
                        if (onboardingViewModel.shared.hasKey.value == true) {
                            nestedNavController.navigate("myStall") {
                                popUpTo(nestedNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                },
                onBookClick = {
                    nestedNavController.navigate("book") {
                        popUpTo(nestedNavController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = nestedNavController,
            startDestination = "productList",
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) {
            composable("productList") {
                ProductListScreen(
                    navController = rootNavController, // Use root controller for external navigation
                    nestedNavController = nestedNavController,
                    // setBottomBarVisibility = { }, // No-op, will be removed later
                    homeTapEvents = homeTapEvents,
                    onboardingViewModel = onboardingViewModel
                )
            }
            composable("myStall") {
                MyStallScreen(
                    navController = rootNavController,
                    nestedNavController = nestedNavController
                )
            }
            composable("book") {
                OrderBookScreen(
                    navController = rootNavController,
                    nestedNavController = nestedNavController
                )
            }
        }
    }
}
