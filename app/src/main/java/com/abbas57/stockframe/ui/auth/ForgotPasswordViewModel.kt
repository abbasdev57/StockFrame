package com.abbas57.stockframe.ui.auth



import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abbas57.stockframe.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // UiState<Unit> rather than UiState<User> — a password reset has no
    // meaningful payload on success, it's a pure side effect (an email gets
    // sent). Reusing the same generic UiState<T> here instead of writing a
    // separate sealed class is the actual payoff of making it generic earlier.
    private val _uiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val uiState: StateFlow<UiState<Unit>> = _uiState.asStateFlow()

    fun sendResetEmail(email: String) {
        if (email.isBlank()) {
            _uiState.value = UiState.Error("Please enter your email address")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            authRepository.sendPasswordReset(email)
                .onSuccess { _uiState.value = UiState.Success(Unit) }
                .onFailure { e -> _uiState.value = UiState.Error(e.message ?: "Could not send reset email") }
        }
    }
}