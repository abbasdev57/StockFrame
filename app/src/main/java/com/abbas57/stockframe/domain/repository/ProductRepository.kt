package com.abbas57.stockframe.domain.repository

import com.abbas57.stockframe.domain.model.Product
import kotlinx.coroutines.flow.Flow

/**
 * Contract for all product CRUD + reads. Same rule as AuthRepository:
 * ViewModels depend on this interface only, never on ProductRepositoryImpl,
 * Firestore, or Storage types — that's what keeps ProductListViewModel
 * and friends unit-testable with a fake.
 *
 * BOTH a Flow and a one-shot read are exposed deliberately, not as
 * redundant API surface. This is the direct result of your Settings
 * answer: Product List defaults to the live Firestore listener
 * (observeProducts), but a Settings toggle can switch the screen to
 * manual fetch + pull-to-refresh (getProductsOnce) for anyone who'd
 * rather control reads than have an always-on snapshot listener. Baking
 * this into the contract NOW means ProductListViewModel can pick a mode
 * later without anyone touching this interface or its implementation
 * again — exactly the kind of change that's expensive if deferred and
 * free if decided up front.
 */
interface ProductRepository {

    /**
     * Live Firestore snapshot listener, scoped to the current owner,
     * filtered to isActive == true. Emits a new list on every remote
     * change — additions, edits, and soft-deletes all surface here
     * without the caller re-querying.
     */
    fun observeProducts(): Flow<List<Product>>

    /** Single Firestore read, same filter as above. Used by the manual-refresh mode and anywhere a one-time snapshot is enough (e.g. resolving a product for Stock Adjustment's search). */
    suspend fun getProductsOnce(): Result<List<Product>>

    /** Fetches one product by ID — used by Product Detail and Add/Edit Product (edit mode). */
    suspend fun getProductById(productId: String): Result<Product>

    /**
     * Creates a new product document. localImageUri, if provided, is a
     * String URI (from ActivityResultContracts.PickVisualMedia) for the
     * data layer to upload to Firebase Storage and resolve into the
     * product's imageUrl before/while writing the Firestore document.
     * Kept as a plain String here, not android.net.Uri, so this interface
     * has no Android-framework import — Uri parsing is a data-layer detail.
     */
    suspend fun addProduct(product: Product, localImageUri: String?): Result<Product>

    /**
     * Updates an existing product. newLocalImageUri is null when the user
     * didn't change the image on edit — the implementation must leave the
     * existing imageUrl untouched in that case, not null it out.
     */
    suspend fun updateProduct(product: Product, newLocalImageUri: String?): Result<Unit>

    /**
     * Soft delete only — flips isActive to false. Never issues a Firestore
     * document delete. See Product.kt's class doc for why: a hard delete
     * would orphan any inventory_transactions still referencing this
     * product's ID.
     */
    suspend fun deleteProduct(productId: String): Result<Unit>
}