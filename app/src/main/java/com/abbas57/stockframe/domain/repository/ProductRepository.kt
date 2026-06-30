package com.abbas57.stockframe.domain.repository

import com.abbas57.stockframe.domain.model.Product
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Contract for all product CRUD + reads. Same rule as AuthRepository:
 * ViewModels depend on this interface only, never on ProductRepositoryImpl,
 * Firestore, or Storage types directly — keeps ViewModels unit-testable
 * with a fake implementation.
 *
 * Flow-only reads (no one-shot fetch) — locked decision: live Firestore
 * listeners everywhere data is displayed, no Settings toggle for V1.
 * A one-shot fetch can be added later by calling .first() on the same
 * Flow if a genuine need shows up; it does not require touching this
 * interface again.
 */
interface ProductRepository {

    /**
     * Live Firestore snapshot listener, scoped to the current owner,
     * filtered to isActive == true. Emits a new list on every remote
     * change — additions, edits, soft-deletes, and quantity adjustments
     * all surface here automatically.
     */
    fun observeProducts(): Flow<List<Product>>

    /** Fetches one product by ID — used by Product Detail and Add/Edit Product (edit mode). */
    suspend fun getProductById(productId: String): Result<Product>

    /**
     * Creates a new product document. localImageUri, if provided, is a
     * String URI (from ActivityResultContracts.PickVisualMedia) for the
     * data layer to upload to Firebase Storage and resolve into the
     * product's imageUrl before writing the Firestore document. Kept as
     * a plain String here, not android.net.Uri, so this interface has no
     * Android-framework import — Uri parsing is a data-layer detail.
     *
     * The initial quantity is written here with NO accompanying
     * inventory_transactions record — there's no "previous quantity" to
     * adjust from on creation, so there's nothing meaningful to log yet.
     */
//    suspend fun addProduct(product: Product, localImageUri: String?): Result<Product>

    /**
     * Updates an existing product, INCLUDING quantity (per the decision
     * to allow direct quantity edits from this form, not just Stock
     * Adjustment). previousQuantity is required so the implementation can
     * detect a real change and write a matching inventory_transactions
     * record — without it, there'd be no way to tell "user changed
     * quantity" from "user saved the form without touching it," and the
     * audit trail would either log every no-op save or miss real changes.
     *
     * newLocalImageUri is null when the user didn't change the image —
     * the implementation must leave the existing imageUrl untouched in
     * that case, not null it out.
     */
//    suspend fun updateProduct(
//        product: Product,
//        previousQuantity: Int,
//        newLocalImageUri: String?
//    ): Result<Unit>
    /** localImageFile is a real File on disk (app cache), not a content URI — see AddEditProductViewModel for the Uri-to-File conversion this requires upstream. */
    suspend fun addProduct(product: Product, localImageFile: File?): Result<Product>

    suspend fun updateProduct(product: Product, previousQuantity: Int, newLocalImageFile: File?): Result<Unit>

    /**
     * Soft delete only — flips isActive to false, never issues a Firestore
     * document delete. A hard delete would orphan any inventory_transactions
     * still referencing this product's ID.
     */
    suspend fun deleteProduct(productId: String): Result<Unit>
}