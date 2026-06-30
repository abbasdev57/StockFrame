package com.abbas57.stockframe.domain.repository

import com.abbas57.stockframe.domain.model.InventoryTransaction
import com.abbas57.stockframe.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface InventoryTransactionRepository {

    /**
     * Global movement list across all of this owner's products,
     * ordered newest-first. Used by History screen.
     * typeFilter null = all types; non-null = specific type only.
     * Filtering is done server-side via Firestore query, not client-side,
     * so only matching records come down the wire regardless of
     * how many total transactions exist.
     */
    fun observeTransactions(typeFilter: TransactionType? = null): Flow<List<InventoryTransaction>>

    /**
     * Per-product movement list, ordered newest-first. Used by
     * Product Detail's movement history section. Scoped Firestore
     * query (whereEqualTo productId) so only this product's records
     * are fetched — not the full collection filtered client-side.
     */
    fun observeTransactionsForProduct(productId: String): Flow<List<InventoryTransaction>>

    /**
     * Writes a new Stock In or Stock Out transaction AND atomically
     * updates the product's quantity in the same Firestore runTransaction.
     * This is the ONLY path through which quantity changes from the
     * Stock Adjustment screen — not a direct product field edit.
     * (Add/Edit Product's quantity edit also writes a transaction,
     * but via ProductRepository.updateProduct, not here.)
     */
    suspend fun recordAdjustment(
        productId: String,
        productName: String,
        type: TransactionType,
        quantity: Int,       // always positive from the UI — this layer applies the sign
        reason: String,
        note: String?
    ): Result<Unit>
}