package com.example.pleb2.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * A data class that holds the current theme state, including whether dark mode is enabled
 * and a function to toggle it. This is provided by a CompositionLocal.
 */
data class ThemeState(
    val isDarkMode: Boolean = true,
    val onToggleDarkMode: () -> Unit = {}
)

/**
 * A CompositionLocal that provides the [ThemeState] to the entire app.
 * This allows any composable to access the current theme state without prop drilling.
 */
val LocalThemeState = staticCompositionLocalOf { ThemeState() }

