package com.abbas57.stockframe.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.abbas57.stockframe.ui.navigation.bottomNavItems
import com.abbas57.stockframe.ui.theme.Blue400
import com.abbas57.stockframe.ui.theme.Neutral500

/**
 * Wraps screen content with the persistent bottom bar. Only the 4
 * bottomNavItems destinations use this — Login, Register, AddEditProduct,
 * etc. render full-screen with no bottom bar, which is why this is a
 * wrapper StockframeNavHost calls selectively per-route, not something
 * every composable destination uses automatically.
 */
@Composable
fun StockframeScaffold(
    navController: NavController,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        bottomBar = { StockframeBottomBar(navController) }
    ) { innerPadding ->
        // innerPadding is intentionally NOT applied here as a wrapping
        // Box/Column padding. Each individual screen (ProductListScreen,
        // DashboardScreen, etc.) is responsible for its own padding,
        // since some screens (e.g. a future full-bleed image gallery)
        // may deliberately want content to run edge-to-edge under the
        // bar rather than be uniformly inset.
        content(innerPadding)
    }
}

@Composable
private fun StockframeBottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(containerColor = androidx.compose.ui.graphics.Color.White) {
        bottomNavItems.forEach { item ->
            // hierarchy check (not a direct route == comparison) so a tab
            // stays visually "selected" while the user is on a sub-screen
            // pushed from it — relevant once ProductDetail/AddEditProduct
            // are reachable FROM the Products tab and shouldn't make the
            // bar look like it deselected everything.
            val selected = currentDestination?.hierarchy?.any { it.route == item.destination.route } == true

            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.destination.route) {
                        // Avoids stacking duplicate copies of the same tab
                        // if the user repeatedly taps it.
                        launchSingleTop = true
                        // Returns to the tab's already-existing state
                        // (e.g. scroll position, form input) rather than
                        // recreating it fresh every time.
                        restoreState = true
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Blue400,
                    selectedTextColor = Blue400,
                    unselectedIconColor = Neutral500,
                    unselectedTextColor = Neutral500
                )
            )
        }
    }
}