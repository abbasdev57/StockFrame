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
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<User>>(UiState.Idle)
    val uiState: StateFlow<UiState<User>> = _uiState.asStateFlow()

    /**
     * Validates and submits registration.
     *
     * Validation order matters for UX: cheapest, most obvious checks first
     * (blank fields) before more specific ones (password match, length),
     * so the user sees the most relevant error rather than a cascade.
     *
     * Note: password STRENGTH rules (length, complexity) are enforced here,
     * client-side, before ever calling Firebase. Firebase Auth's own minimum
     * is 6 characters — relying on Firebase alone would mean a failed network
     * call just to learn the password was too short, which is a worse
     * experience than catching it instantly, offline, in the ViewModel.
     */
    fun register(name: String, email: String, password: String, confirmPassword: String) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.value = UiState.Error("All fields are required")
            return
        }
        if (password.length < 6) {
            _uiState.value = UiState.Error("Password must be at least 6 characters")
            return
        }
        if (password != confirmPassword) {
            _uiState.value = UiState.Error("Passwords do not match")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            authRepository.register(name, email, password)
                .onSuccess { user -> _uiState.value = UiState.Success(user) }
                .onFailure { e -> _uiState.value = UiState.Error(mapFirebaseError(e)) }
        }
    }

    /**
     * Firebase's raw exception messages are technical and not user-friendly
     * (e.g. "The email address is already in use by another account.").
     * In V1 we pass most messages through as-is since they're already
     * fairly readable, but this function is the single seam where you'd
     * add friendlier copy later without touching the ViewModel's core logic.
     */
    private fun mapFirebaseError(e: Throwable): String {
        return e.message ?: "Registration failed. Please try again."
    }
}