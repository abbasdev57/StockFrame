package com.abbas57.stockframe.data.repository

import com.abbas57.stockframe.domain.model.InventoryTransaction
import com.abbas57.stockframe.domain.model.TransactionType
import com.abbas57.stockframe.domain.repository.InventoryTransactionRepository
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

private const val TRANSACTIONS_COLLECTION = "inventory_transactions"
private const val PRODUCTS_COLLECTION = "products"

@Singleton
class InventoryTransactionRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : InventoryTransactionRepository {

    private fun requireOwnerId(): String =
        firebaseAuth.currentUser?.uid
            ?: throw IllegalStateException("No signed-in user — transaction operations require auth")

    override fun observeTransactions(typeFilter: TransactionType?): Flow<List<InventoryTransaction>> =
        callbackFlow {
            var query: Query = firestore.collection(TRANSACTIONS_COLLECTION)
                .whereEqualTo("ownerId", requireOwnerId())
                .orderBy("createdAt", Query.Direction.DESCENDING)

            // Apply server-side type filter only when one is selected —
            // null means "All" on the History screen, so no filter clause
            // is added and all types come through.
            if (typeFilter != null) {
                query = query.whereEqualTo("type", typeFilter.name)
            }

            val registration = query.addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val transactions = snapshot?.documents?.mapNotNull { it.toTransactionOrNull() }
                    ?: emptyList()
                trySend(transactions)
            }
            awaitClose { registration.remove() }
        }

    override fun observeTransactionsForProduct(productId: String): Flow<List<InventoryTransaction>> =
        callbackFlow {
            val registration = firestore.collection(TRANSACTIONS_COLLECTION)
                .whereEqualTo("ownerId", requireOwnerId())
                .whereEqualTo("productId", productId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) { close(error); return@addSnapshotListener }
                    val transactions = snapshot?.documents?.mapNotNull { it.toTransactionOrNull() }
                        ?: emptyList()
                    trySend(transactions)
                }
            awaitClose { registration.remove() }
        }

    override suspend fun recordAdjustment(
        productId: String,
        productName: String,
        type: TransactionType,
        quantity: Int,
        reason: String,
        note: String?
    ): Result<Unit> = try {
        val ownerId = requireOwnerId()
        val now = System.currentTimeMillis()

        // Sign the quantity based on type before storing — UI always
        // passes a positive number ("add 10", "remove 5"), this layer
        // converts it to the signed delta that makes the audit trail
        // mathematically consistent (sum of all quantities = current stock).
        val signedQuantity = when (type) {
            TransactionType.IN -> quantity
            TransactionType.OUT -> -quantity
            TransactionType.ADJUSTMENT -> quantity  // already signed when coming from product edit
        }

        // runTransaction guarantees both writes succeed or both fail —
        // quantity on the product document and the transaction log record
        // are always in sync, no partial-write state is possible.
        firestore.runTransaction { txn ->
            val productRef = firestore.collection(PRODUCTS_COLLECTION).document(productId)
            val productSnapshot = txn.get(productRef)
            val currentQuantity = (productSnapshot.getLong("quantity") ?: 0L).toInt()
            val newQuantity = currentQuantity + signedQuantity

            // Guard: quantity can never go below zero. The ViewModel
            // validates this before calling, but the transaction
            // enforces it server-side as a second line of defense —
            // important because Firestore security rules alone can't
            // express "don't allow if result would be negative."
            if (newQuantity < 0) {
                throw IllegalArgumentException(
                    "Insufficient stock: current=$currentQuantity, requested removal=${-signedQuantity}"
                )
            }

            txn.update(productRef, mapOf("quantity" to newQuantity, "updatedAt" to now))

            val txnRef = firestore.collection(TRANSACTIONS_COLLECTION).document()
            txn.set(
                txnRef,
                mapOf(
                    "productId" to productId,
                    "productName" to productName,
                    "ownerId" to ownerId,
                    "type" to type.name,
                    "quantity" to signedQuantity,
                    "reason" to reason,
                    "note" to note,
                    "createdAt" to now
                )
            )
        }.await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun DocumentSnapshot.toTransactionOrNull(): InventoryTransaction? = try {
        val typeString = getString("type") ?: return null
        val type = try { TransactionType.valueOf(typeString) } catch (e: IllegalArgumentException) { return null }
        InventoryTransaction(
            id = id,
            productId = getString("productId") ?: return null,
            productName = getString("productName") ?: "",
            ownerId = getString("ownerId") ?: return null,
            type = type,
            quantity = (getLong("quantity") ?: 0L).toInt(),
            reason = getString("reason") ?: "",
            note = getString("note"),
            createdAt = getLong("createdAt") ?: 0L
        )
    } catch (e: Exception) {
        null
    }
}