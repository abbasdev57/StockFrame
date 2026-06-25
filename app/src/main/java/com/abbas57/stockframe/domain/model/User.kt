package com.abbas57.stockframe.domain.model


/**
 * Plain Kotlin data class representing an authenticated user.
 *
 * Deliberately has ZERO Firebase imports or annotations. This is the
 * whole point of the domain layer in Clean Architecture: ViewModels and
 * UI work only with this class, never with FirebaseUser directly. That
 * means if Stockframe ever swapped Firebase Auth for something else,
 * only the data/ layer would need to change — domain and ui stay untouched.
 *
 * Matches the /users/{uid} Firestore schema from the planning document,
 * minus createdAt (a domain User doesn't need to carry that around at
 * runtime; it's written once at registration and otherwise irrelevant
 * to the UI).
 */
data class User(
    val uid: String,
    val name: String,
    val email: String
)