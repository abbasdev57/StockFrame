package com.abbas57.stockframe.ui.auth



// Same Loading/Success/Error pattern from your codelabs.
// Generic so Login, Register, and ForgotPassword can each plug in
// whatever success payload they need (or Unit if there's none).
sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
    object Idle : UiState<Nothing>  // before any action has been triggered
}