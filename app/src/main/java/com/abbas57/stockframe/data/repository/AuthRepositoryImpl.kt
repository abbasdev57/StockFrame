package com.abbas57.stockframe.data.repository


//import com.abbas57.stockframe.domain.model.User
import com.abbas57.stockframe.domain.model.User
import com.abbas57.stockframe.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of AuthRepository, backed by Firebase Auth +
 * Firestore. This is the ONLY class in the entire app that imports both
 * FirebaseAuth and FirebaseFirestore types directly for authentication —
 * everything above this layer (ViewModels, UI) works through the
 * AuthRepository interface only.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override suspend fun register(name: String, email: String, password: String): Result<User> {
        return try {
            // Step A: create the Firebase Auth credential itself.
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(IllegalStateException("Registration succeeded but returned no user"))

            // Step B: write the matching /users/{uid} Firestore document.
            // Auth and Firestore are SEPARATE systems — creating the auth
            // account does NOT automatically create a Firestore profile.
            // Skipping this step is a common beginner gap: the login would
            // still "work" but any screen that reads user.name from
            // Firestore later would find nothing there.
            val user = User(uid = firebaseUser.uid, name = name, email = email)
            firestore.collection("users").document(user.uid)
                .set(
                    mapOf(
                        "uid" to user.uid,
                        "name" to name,
                        "email" to email,
                        "createdAt" to System.currentTimeMillis()
                    )
                )
                .await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(IllegalStateException("Login succeeded but returned no user"))

            // V1 simplification: we don't re-fetch the Firestore /users/{uid}
            // document here. firebaseUser.displayName is usually empty since
            // we never call updateProfile() during registration. This is fine
            // for V1 because Login only needs to confirm WHO is signed in,
            // not display their name — that need arrives later (Dashboard/
            // Settings), at which point fetch the Firestore doc there instead
            // of adding an extra read to every single login.
            Result.success(
                User(uid = firebaseUser.uid, name = firebaseUser.displayName ?: "", email = email)
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCurrentUser(): User? {
        val firebaseUser = firebaseAuth.currentUser ?: return null
        return User(
            uid = firebaseUser.uid,
            name = firebaseUser.displayName ?: "",
            email = firebaseUser.email ?: ""
        )
    }

    override fun logout() = firebaseAuth.signOut()
}