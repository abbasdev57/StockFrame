package com.abbas57.stockframe.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abbas57.stockframe.domain.model.User
import com.abbas57.stockframe.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<User>>(UiState.Idle)
    val uiState: StateFlow<UiState<User>> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        // Basic client-side guard before hitting the network —
        // saves a round trip for the most obvious empty-field case.
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = UiState.Error("Email and password are required")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            authRepository.login(email, password)
                .onSuccess { user -> _uiState.value = UiState.Success(user) }
                .onFailure { e -> _uiState.value = UiState.Error(e.message ?: "Login failed") }
        }
    }
}