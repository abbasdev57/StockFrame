package com.abbas57.stockframe.ui.settings

import androidx.lifecycle.ViewModel
import com.abbas57.stockframe.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    /**
     * Read once, not as a Flow — the signed-in email doesn't change
     * during a session (changing it would require re-authentication,
     * a feature not in V1 scope), so there's no need for live
     * observation here, unlike Product List or Dashboard.
     */
    val userEmail: String
        get() = authRepository.getCurrentUser()?.email ?: ""

    fun logout() {
        authRepository.logout()
    }
}