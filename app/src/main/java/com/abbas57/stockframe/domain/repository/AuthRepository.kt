package com.abbas57.stockframe.domain.repository

import com.abbas57.stockframe.domain.model.User


/**
 * Contract for all authentication operations.
 *
 * ViewModels depend on THIS interface only — never on AuthRepositoryImpl
 * or on Firebase types directly. That separation is what makes ViewModels
 * unit-testable later: a test can provide a fake AuthRepository that
 * returns canned Result values, with no real Firebase calls involved.
 *
 * Result<T> is used instead of throwing exceptions across this boundary.
 * That lets every caller handle outcomes uniformly with
 * .onSuccess { }.onFailure { } instead of try/catch scattered through
 * ViewModel code, and maps directly onto the sealed UiState pattern
 * (Success / Error) used in every screen.
 */
interface AuthRepository {

    /** Creates a new Firebase Auth account AND the matching /users/{uid} Firestore document. */
    suspend fun register(name: String, email: String, password: String): Result<User>

    /** Signs in an existing user with email + password. */
    suspend fun login(email: String, password: String): Result<User>

    /** Triggers Firebase's built-in password reset email flow. */
    suspend fun sendPasswordReset(email: String): Result<Unit>

    /**
     * Synchronous, local check — reads Firebase Auth's on-device cached
     * session, does NOT make a network call. Returns null if no user
     * is currently signed in. Used by Splash for routing.
     */
    fun getCurrentUser(): User?

    /** Signs out the current user, clearing the cached session. */
    fun logout()
}