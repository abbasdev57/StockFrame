package com.abbas57.stockframe.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.abbas57.stockframe.ui.auth.ForgotPasswordScreen
import com.abbas57.stockframe.ui.auth.LoginScreen
import com.abbas57.stockframe.ui.auth.RegisterScreen
import com.abbas57.stockframe.ui.splash.SplashScreen

@Composable
fun StockframeNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = NavigationDestination.Splash.route
    ) {
        composable(NavigationDestination.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(NavigationDestination.Login.route) {
                        // removes Splash from the back stack so the user
                        // can't press Back and land on it again
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

        composable(NavigationDestination.Dashboard.route) {
            // Placeholder until Sprint 3 — Sprint 1's job is just proving the
            // auth → dashboard flow works end to end.
            Box { Text("Dashboard placeholder — Sprint 3 builds this") }
        }
    }
}