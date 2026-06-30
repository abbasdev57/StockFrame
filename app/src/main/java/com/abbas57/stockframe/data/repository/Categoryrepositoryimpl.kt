package com.abbas57.stockframe.data.repository

import com.abbas57.stockframe.domain.model.Category
import com.abbas57.stockframe.domain.repository.CategoryRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val CATEGORIES_COLLECTION = "categories"

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : CategoryRepository {

    private fun requireOwnerId(): String =
        firebaseAuth.currentUser?.uid
            ?: throw IllegalStateException("No signed-in user — category operations require auth")

    private fun baseQuery(): Query =
        firestore.collection(CATEGORIES_COLLECTION)
            .whereEqualTo("ownerId", requireOwnerId())

    override fun observeCategories(): Flow<List<Category>> = callbackFlow {
        val registration = baseQuery().addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val categories = snapshot?.documents?.mapNotNull { it.toCategoryOrNull() } ?: emptyList()
            trySend(categories)
        }
        awaitClose { registration.remove() }
    }

    override suspend fun addCategory(name: String): Result<Category> = try {
        val docRef = firestore.collection(CATEGORIES_COLLECTION).document()
        val category = Category(
            id = docRef.id,
            name = name,
            ownerId = requireOwnerId(),
            createdAt = System.currentTimeMillis()
        )
        docRef.set(
            mapOf("name" to category.name, "ownerId" to category.ownerId, "createdAt" to category.createdAt)
        ).await()
        Result.success(category)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun DocumentSnapshot.toCategoryOrNull(): Category? = try {
        Category(
            id = id,
            name = getString("name") ?: return null,
            ownerId = getString("ownerId") ?: return null,
            createdAt = getLong("createdAt") ?: 0L
        )
    } catch (e: Exception) {
        null
    }
}