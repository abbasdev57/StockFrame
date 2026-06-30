package com.abbas57.stockframe.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The 4 persistent bottom-nav destinations. Deliberately a SEPARATE list
 * from NavigationDestination's full sealed interface — Splash, Login,
 * Register, ForgotPassword, AddEditProduct, and ProductDetail all exist
 * as real destinations but never appear in the bottom bar, so mixing them
 * into one "everything" list would mean filtering it down every time the
 * bar renders. This list IS the bar's source of truth, nothing more.
 */
data class BottomNavItem(
    val destination: NavigationDestination,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(NavigationDestination.Dashboard, "Dashboard", Icons.Filled.Dashboard),
    BottomNavItem(NavigationDestination.ProductList, "Products", Icons.Filled.Inventory2),
    BottomNavItem(NavigationDestination.History, "History", Icons.Filled.History),
    BottomNavItem(NavigationDestination.Settings, "Settings", Icons.Filled.Settings)
)