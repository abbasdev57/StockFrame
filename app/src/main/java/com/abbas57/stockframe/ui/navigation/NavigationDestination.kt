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

    object ProductList : NavigationDestination {
        override val route = "product_list"
    }

    object History : NavigationDestination {
        override val route = "history"
    }

    object Settings : NavigationDestination {
        override val route = "settings"
    }

    /**
     * Parameterized route — productId is nullable in the OBJECT call
     * (AddEditProduct(null) for add-mode) but the actual NavHost route
     * pattern always includes the argument slot; null gets encoded as
     * the literal string "new" so NavHost's non-nullable String arg
     * extraction doesn't have to special-case a missing segment.
     */
    object AddEditProduct : NavigationDestination {
        override val route = "add_edit_product/{productId}"
        fun createRoute(productId: String?) = "add_edit_product/${productId ?: "new"}"
    }

    object ProductDetail : NavigationDestination {
        override val route = "product_detail/{productId}"
        fun createRoute(productId: String) = "product_detail/$productId"
    }

    object StockAdjustment : NavigationDestination {
        override val route = "stock_adjustment/{productId}"
        fun createRoute(productId: String) = "stock_adjustment/$productId"
    }

}