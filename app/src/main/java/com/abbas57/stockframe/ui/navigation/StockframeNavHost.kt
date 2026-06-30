package com.abbas57.stockframe.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.abbas57.stockframe.ui.auth.ForgotPasswordScreen
import com.abbas57.stockframe.ui.auth.LoginScreen
import com.abbas57.stockframe.ui.auth.RegisterScreen
import com.abbas57.stockframe.ui.common.StockframeScaffold
import com.abbas57.stockframe.ui.products.AddEditProductScreen
import com.abbas57.stockframe.ui.products.ProductDetailScreen
import com.abbas57.stockframe.ui.splash.SplashScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import com.abbas57.stockframe.ui.product.ProductListScreen

@Composable
fun StockframeNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = NavigationDestination.Splash.route
    ) {
        // ---- Full-screen destinations: NO bottom bar ----

        composable(NavigationDestination.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(NavigationDestination.Login.route) {
                        popUpTo(NavigationDestination.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToDashboard = {
                    navController.navigate(NavigationDestination.Dashboard.route) {
                        popUpTo(NavigationDestination.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavigationDestination.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(NavigationDestination.Dashboard.route) {
                        popUpTo(NavigationDestination.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(NavigationDestination.Register.route) },
                onNavigateToForgotPassword = { navController.navigate(NavigationDestination.ForgotPassword.route) }
            )
        }

        composable(NavigationDestination.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(NavigationDestination.Dashboard.route) {
                        popUpTo(NavigationDestination.Login.route) { inclusive = true }
                    }
                },
                onNavigateBackToLogin = { navController.popBackStack() }
            )
        }

        composable(NavigationDestination.ForgotPassword.route) {
            ForgotPasswordScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(NavigationDestination.AddEditProduct.route) { backStackEntry ->
            val rawId = backStackEntry.arguments?.getString("productId")
            val productId = if (rawId == "new") null else rawId
            // No StockframeScaffold wrapper — this is a full-screen form,
            // matches Login/Register's treatment, not a bottom-nav tab.
            AddEditProductScreen(
                productId = productId,
                onSaveComplete = { navController.popBackStack() },
                onDiscard = { navController.popBackStack() }
            )
        }

        composable(
            route = NavigationDestination.ProductDetail.route,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId")
                ?: return@composable // malformed nav call; nothing sensible to render
            ProductDetailScreen(
                productId = productId,
                onEditClick = { id -> navController.navigate(NavigationDestination.AddEditProduct.createRoute(id)) },
                onStockAdjustmentClick = { /* Sprint 3 destination — no-op for now */ },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ---- Bottom-nav tab destinations: wrapped in StockframeScaffold ----

        composable(NavigationDestination.Dashboard.route) {
            StockframeScaffold(navController) {
                Box { Text("Dashboard placeholder — Sprint 3 builds this") }
            }
        }

        composable(NavigationDestination.ProductList.route) {
            StockframeScaffold(navController) {padding->
                ProductListScreen(
                    contentPadding = padding,
                    onProductClick = { id -> navController.navigate(NavigationDestination.ProductDetail.createRoute(id)) },
                    onAddProductClick = { navController.navigate(NavigationDestination.AddEditProduct.createRoute(null)) }
                )
            }
        }

        composable(NavigationDestination.History.route) {
            StockframeScaffold(navController) {
                Box { Text("History placeholder — Sprint 3 builds this") }
            }
        }

        composable(NavigationDestination.Settings.route) {
            StockframeScaffold(navController) {
                Box { Text("Settings placeholder") }
            }
        }
    }
}