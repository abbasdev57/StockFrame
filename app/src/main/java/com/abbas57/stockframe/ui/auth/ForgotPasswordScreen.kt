package com.abbas57.stockframe.ui.auth


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel


@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Reset your password", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Enter your email and we'll send you a reset link.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        // Once the email is successfully sent, swap the form for a
        // confirmation message rather than navigating away immediately —
        // the user needs to actually see "check your inbox," not just
        // get bounced back to Login with no feedback.
        if (uiState is UiState.Success<*>) {
            Text(
                "If an account exists for that email, a reset link is on its way.",
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onNavigateBack) { Text("Back to log in") }
            return@Column
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        if (uiState is UiState.Error) {
            Text(
                text = (uiState as UiState.Error).message,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = { viewModel.sendResetEmail(email) },
            enabled = uiState !is UiState.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState is UiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text("Send reset link")
            }
        }
        Spacer(Modifier.height(16.dp))

        TextButton(onClick = onNavigateBack) { Text("Back to log in") }
    }
}