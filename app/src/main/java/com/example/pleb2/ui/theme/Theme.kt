package com.example.pleb2.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    //primary = Red80_test, // Used for ProductCard price, Add button (SettingsScreen), toggle thumb (SettingsScreen)
    //secondary = RedGrey80_test, // Used for secondary UI elements, toggle track (SettingsScreen)
    primary = Purple80, // Used for ProductCard price, Add button (SettingsScreen), toggle thumb (SettingsScreen)
    secondary = RedGrey80, // Used for secondary UI elements, toggle track (SettingsScreen)
    tertiary = PurpleGrey80 , // Used for accent UI, toggle track background (SettingsScreen)
    onSurfaceVariant = Color.White, // Changed to white for unselected nav bar icons
    //onSurfaceVariant = Color(0xFF757575), // Changed to neutral gray for unselected nav bar icons
    primaryContainer = Color(0xFF757575), // Used for toggle track background (SettingsScreen)

    //primaryContainer = Color(0xFFFFFBFE), // Used for toggle track background (SettingsScreen)
    onPrimary = Color.White, // Used for button text (Add button, etc.)
    onSecondary = Color.White, // Used for secondary button text
    onTertiary = Color.White, // Used for tertiary button text
    background = Color(0xFF2C2B2F), // Used for app background
    surface = Color(0xFF2C2B2F), // Used for card backgrounds
    onBackground = Color.White, // Used for text on background
    onSurface = Color.White // Used for text on surfaces
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40, // Restore to your red for selected nav bar icon
    secondary = RedGrey40, // Used for secondary UI elements, toggle track (SettingsScreen)
    tertiary = Purple40, // Used for accent UI, toggle track background (SettingsScreen)
    //onSurfaceVariant = Color(0xFF757575), // Changed to neutral gray for unselected nav bar icons
    onSurfaceVariant = Purple40, // Changed to neutral gray for unselected nav bar icons

    primaryContainer = RedGrey40, // Used for toggle track background (SettingsScreen)

    //primaryContainer = Color.White, // Used for toggle track background (SettingsScreen)
    onPrimary = Color(0xFF1C1B1F), // Used for button text (Add button, etc.)
    onSecondary = Color.White, // Used for secondary button text
    onTertiary = Color.White, // Used for tertiary button text
    background = Color(0xFFFFFBFE), // Used for app background
    surface = Color(0xFFFFFBFE), // Used for card backgrounds
    onBackground = Color(0xFF1C1B1F), // Used for text on background
    onSurface = Color(0xFF1C1B1F) // Used for text on surfaces


    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun Pleb2Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb() // Suppress deprecation warning
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.setBackgroundDrawableResource(android.R.color.transparent)
            @Suppress("DEPRECATION")
            window.navigationBarColor = colorScheme.background.toArgb() // Suppress deprecation warning
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}