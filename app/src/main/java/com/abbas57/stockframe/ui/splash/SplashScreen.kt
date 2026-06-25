package com.abbas57.stockframe.ui.splash



import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    // LaunchedEffect(Unit) runs exactly once when this Composable first
    // enters the composition — correct for a one-time routing check,
    // as opposed to something keyed on changing state.
    LaunchedEffect(Unit) {
        if (viewModel.hasLoggedInUser()) {
            onNavigateToDashboard()
        } else {
            onNavigateToLogin()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Wordmark + spinner. Intentionally minimal — per the earlier
        // design review, a splash screen's job is the routing decision
        // above, not visual complexity; it's on screen for a fraction
        // of a second in the common case.
        Text("stockframe", style = MaterialTheme.typography.headlineMedium)
    }
}