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
private const val PRODUCT_IMAGES_PATH = "product_images"

@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val firebaseAuth: FirebaseAuth
) : ProductRepository {

    /**
     * Every query in this class is scoped to the signed-in owner. This
     * mirrors Firestore security rules (which independently enforce the
     * same ownerId == request.auth.uid check server-side) — the client
     * filter is for correct UI/empty-state behavior, the rule is what
     * actually prevents one user reading another's products. Throwing
     * here instead of silently returning an empty list is deliberate:
     * every screen that calls into this repository sits behind the auth
     * graph, so a null uid means something is badly wrong with navigation,
     * not a normal empty state to swallow quietly.
     */
    private fun requireOwnerId(): String =
        firebaseAuth.currentUser?.uid
            ?: throw IllegalStateException("No signed-in user — product operations require auth")

    private fun baseQuery(): Query =
        firestore.collection(PRODUCTS_COLLECTION)
            .whereEqualTo("ownerId", requireOwnerId())
            .whereEqualTo("isActive", true)

    override fun observeProducts(): Flow<List<Product>> = callbackFlow {
        // addSnapshotListener fires immediately with the current cached/
        // server state, then again on every subsequent change — this is
        // what makes Product List "live" without the ViewModel re-querying.
        val registration = baseQuery().addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val products = snapshot?.documents?.mapNotNull { it.toProductOrNull() } ?: emptyList()
            trySend(products)
        }
        // Critical: without this, the Firestore listener leaks past the
        // Flow's collector scope — every screen navigation away from
        // Product List would otherwise leave a dangling listener running.
        awaitClose { registration.remove() }
    }

    override suspend fun getProductsOnce(): Result<List<Product>> = try {
        val snapshot = baseQuery().get().await()
        Result.success(snapshot.documents.mapNotNull { it.toProductOrNull() })
    } catch (e: Exception) {
        Result.failure(e)
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
        // .document() with no argument generates a locally-unique ID right
        // now, client-side — this is what lets the eventual Sprint 4 offline
        // write queue insert a product into Room and reference a real,
        // stable ID before the Firestore write ever reaches the server.
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

    override suspend fun updateProduct(product: Product, newLocalImageUri: String?): Result<Unit> = try {
        // If the user didn't pick a new image, product.imageUrl already
        // holds the original value — it was loaded via getProductById and
        // carried through the edit form untouched. Only upload + overwrite
        // when there's actually a new local URI to push.
        val imageUrl = newLocalImageUri?.let { uploadProductImage(product.id, it) } ?: product.imageUrl

        val updated = product.copy(
            imageUrl = imageUrl,
            updatedAt = System.currentTimeMillis()
        )

        firestore.collection(PRODUCTS_COLLECTION).document(product.id)
            .set(updated.toFirestoreMap())
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteProduct(productId: String): Result<Unit> = try {
        // Soft delete only — see Product.kt's class doc for why a hard
        // delete here would be wrong (orphaned inventory_transactions).
        firestore.collection(PRODUCTS_COLLECTION).document(productId)
            .update(
                mapOf(
                    "isActive" to false,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Fixed path per product ID (not a timestamped filename) is deliberate:
     * re-uploading on edit overwrites the same Storage object instead of
     * accumulating orphaned old images with no product ever referencing
     * them again. The tradeoff is no image version history — acceptable
     * for V1, since nothing in the feature list asks for one.
     */
    private suspend fun uploadProductImage(productId: String, localUri: String): String {
        val ref = storage.reference.child("$PRODUCT_IMAGES_PATH/${requireOwnerId()}/$productId.jpg")
        ref.putFile(Uri.parse(localUri)).await()
        return ref.downloadUrl.await().toString()
    }

    private fun DocumentSnapshot.toProductOrNull(): Product? {
        // mapNotNull + null-on-malformed-doc, rather than throwing mid-list,
        // so one corrupt document can't take down the entire Product List
        // screen for every other valid product.
        return try {
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