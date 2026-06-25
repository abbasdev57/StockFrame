package com.abbas57.stockframe.ui.navigation


// Sealed interface — same pattern from your Inventory codelab, extended.
// Each screen is a singleton object implementing this, giving you
// compile-time safety on routes instead of raw string literals scattered
// through the codebase.
sealed interface NavigationDestination {
    val route: String

    object Splash : NavigationDestination {
        override val route = "splash"
    }

    object Login : NavigationDestination {
        override val route = "login"
    }

    object Register : NavigationDestination {
        override val route = "register"
    }

    object ForgotPassword : NavigationDestination {
        override val route = "forgot_password"
    }

    object Dashboard : NavigationDestination {
        override val route = "dashboard"
    }
}