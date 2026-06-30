package com.abbas57.stockframe.data.repository

import android.net.Uri
import com.abbas57.stockframe.domain.model.Product
import com.abbas57.stockframe.domain.repository.ProductRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val PRODUCTS_COLLECTION = "products"
private const val TRANSACTIONS_COLLECTION = "inventory_transactions"
private const val PRODUCT_IMAGES_PATH = "product_images"

@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val firebaseAuth: FirebaseAuth
) : ProductRepository {

    private fun requireOwnerId(): String =
        firebaseAuth.currentUser?.uid
            ?: throw IllegalStateException("No signed-in user — product operations require auth")

    private fun baseQuery(): Query =
        firestore.collection(PRODUCTS_COLLECTION)
            .whereEqualTo("ownerId", requireOwnerId())
            .whereEqualTo("isActive", true)

    override fun observeProducts(): Flow<List<Product>> = callbackFlow {
        val registration = baseQuery().addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val products = snapshot?.documents?.mapNotNull { it.toProductOrNull() } ?: emptyList()
            trySend(products)
        }
        // Without this, the Firestore listener leaks past the Flow's
        // collector scope — navigating away from Product List would
        // otherwise leave it running indefinitely.
        awaitClose { registration.remove() }
    }

    override suspend fun getProductById(productId: String): Result<Product> = try {
        val doc = firestore.collection(PRODUCTS_COLLECTION).document(productId).get().await()
        val product = doc.toProductOrNull()
            ?: return Result.failure(NoSuchElementException("Product $productId not found"))
        Result.success(product)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun addProduct(product: Product, localImageUri: String?): Result<Product> = try {
        val ownerId = requireOwnerId()
        val docRef = firestore.collection(PRODUCTS_COLLECTION).document()
        val now = System.currentTimeMillis()

        val imageUrl = localImageUri?.let { uploadProductImage(docRef.id, it) }

        val finalProduct = product.copy(
            id = docRef.id,
            ownerId = ownerId,
            imageUrl = imageUrl,
            createdAt = now,
            updatedAt = now
        )

        docRef.set(finalProduct.toFirestoreMap()).await()
        Result.success(finalProduct)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateProduct(
        product: Product,
        previousQuantity: Int,
        newLocalImageUri: String?
    ): Result<Unit> = try {
        val imageUrl = newLocalImageUri?.let { uploadProductImage(product.id, it) } ?: product.imageUrl
        val updated = product.copy(imageUrl = imageUrl, updatedAt = System.currentTimeMillis())
        val quantityDelta = updated.quantity - previousQuantity

        // firestore.runTransaction guarantees the product write and the
        // transaction-log write succeed or fail together — exactly the
        // same atomicity guarantee Sprint 3's Stock Adjustment screen
        // will rely on. Without this, a crash or network drop between
        // two separate .set() calls could update quantity with no
        // matching log entry, or vice versa.
        firestore.runTransaction { txn ->
            txn.set(
                firestore.collection(PRODUCTS_COLLECTION).document(updated.id),
                updated.toFirestoreMap()
            )

            // Only log a real change. A no-op save (user opened Edit,
            // changed the name, left quantity untouched) must NOT create
            // a misleading "Adjustment: +0" entry in the product's history.
            if (quantityDelta != 0) {
                val txnRef = firestore.collection(TRANSACTIONS_COLLECTION).document()
                txn.set(
                    txnRef,
                    mapOf(
                        "productId" to updated.id,
                        "ownerId" to updated.ownerId,
                        "type" to "ADJUSTMENT",
                        "quantity" to quantityDelta,
                        "reason" to "Manual correction via product edit",
                        "note" to null,
                        "createdAt" to System.currentTimeMillis()
                    )
                )
            }
        }.await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteProduct(productId: String): Result<Unit> = try {
        firestore.collection(PRODUCTS_COLLECTION).document(productId)
            .update(mapOf("isActive" to false, "updatedAt" to System.currentTimeMillis()))
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun uploadProductImage(productId: String, localUri: String): String {
        val ref = storage.reference.child("$PRODUCT_IMAGES_PATH/${requireOwnerId()}/$productId.jpg")
        ref.putFile(Uri.parse(localUri)).await()
        return ref.downloadUrl.await().toString()
    }

    private fun DocumentSnapshot.toProductOrNull(): Product? = try {
        Product(
            id = id,
            ownerId = getString("ownerId") ?: return null,
            name = getString("name") ?: return null,
            sku = getString("sku") ?: return null,
            categoryId = getString("categoryId") ?: return null,
            description = getString("description"),
            imageUrl = getString("imageUrl"),
            unitPrice = getDouble("unitPrice") ?: 0.0,
            quantity = (getLong("quantity") ?: 0L).toInt(),
            minimumStock = (getLong("minimumStock") ?: 0L).toInt(),
            isActive = getBoolean("isActive") ?: true,
            createdAt = getLong("createdAt") ?: 0L,
            updatedAt = getLong("updatedAt") ?: 0L
        )
    } catch (e: Exception) {
        null
    }

    private fun Product.toFirestoreMap(): Map<String, Any?> = mapOf(
        "ownerId" to ownerId,
        "name" to name,
        "sku" to sku,
        "categoryId" to categoryId,
        "description" to description,
        "imageUrl" to imageUrl,
        "unitPrice" to unitPrice,
        "quantity" to quantity,
        "minimumStock" to minimumStock,
        "isActive" to isActive,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )
}