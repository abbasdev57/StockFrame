package com.abbas57.stockframe.ui.auth


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.abbas57.stockframe.ui.common.UiState

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateBackToLogin: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is UiState.Success<*>) onRegisterSuccess()
    }

    Column(
        // verticalScroll matters here specifically: this form has 4 fields
        // plus a button, which can exceed screen height on smaller devices
        // once the on-screen keyboard is up. Login (2 fields) doesn't need
        // this, but Register does — worth noting as a deliberate difference,
        // not an inconsistency.
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Create your account", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Start managing your inventory today.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        if (uiState is UiState.Error) {
            Text(
                text = (uiState as UiState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = { viewModel.register(name, email, password, confirmPassword) },
            enabled = uiState !is UiState.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState is UiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text("Register")
            }
        }
        Spacer(Modifier.height(16.dp))

        // Note: the Stitch mockup's "Or continue with — SSO / Cloud" block
        // is deliberately omitted here. Per the V1 feature scope, only
        // email/password auth ships in this version.

        TextButton(onClick = onNavigateBackToLogin) {
            Text("Already have an account? Log in")
        }
    }
}