package com.abbas57.stockframe.ui.splash



import androidx.lifecycle.ViewModel
import com.abbas57.stockframe.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    /**
     * Firebase Auth persists the signed-in session on-device automatically
     * (it's not something we wrote — it's default SDK behavior). That means
     * getCurrentUser() returning non-null here doesn't require a network
     * call; it's a local, synchronous check against the cached session.
     *
     * This is also exactly why this check belongs on Splash and not, say,
     * inside LoginViewModel's init block — Splash is the single entry point
     * for the whole app, so the routing decision should live in exactly
     * one place.
     */
    fun hasLoggedInUser(): Boolean = authRepository.getCurrentUser() != null
}