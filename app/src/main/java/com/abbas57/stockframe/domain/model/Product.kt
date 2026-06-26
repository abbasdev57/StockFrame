package com.abbas57.stockframe.domain.model


/**
 * Plain Kotlin data class — zero Firestore/Storage imports, same rule as
 * User in the auth module. ProductRepositoryImpl is the only place that
 * knows this maps onto a Firestore document; ViewModels and UI never see
 * a DocumentSnapshot or a Storage reference, only this.
 *
 * Field-by-field decisions locked in this thread (not guesses):
 * - description: added in this sprint (was missing from the original
 *   planning-doc schema, confirmed to keep here, not deferred).
 * - categoryId only, no embedded category name. The Stitch mock shows
 *   "Furniture / Seating" under the product name, but that's a DISPLAY
 *   concern — ProductListViewModel resolves categoryId -> Category.name
 *   by joining against CategoryRepository's list, rather than this model
 *   denormalizing a category name it would have to keep in sync on every
 *   category rename. Keeps this model as the one source of truth.
 * - NO warehouse, supplier, leadTime, location, pendingOrders. All five
 *   appeared in the Stitch Product Detail / Stock Adjustment mocks but
 *   are confirmed cut from V1 (multi-warehouse + supplier management are
 *   explicit Phase-2 items in Section 1.3 of the planning doc). Adding
 *   them as nullable fields "just in case" would be scope creep disguised
 *   as cheap optionality — they come back when that phase is actually
 *   scoped, not before.
 * - quantity is NEVER written directly by Add/Edit Product after creation.
 *   Per the planning doc, only Stock Adjustment (Sprint 3) mutates it, and
 *   only inside a Firestore transaction alongside an inventory_transactions
 *   write. Add/Edit Product sets it once at creation time and never again.
 * - isActive implements soft delete. "Deleting" a product in the UI sets
 *   this false; it never disappears from Firestore. This preserves
 *   inventory_transactions history integrity — a transaction referencing
 *   a hard-deleted product would have nothing to join against later.
 * - stockStatus is NOT a stored field — see the computed property below.
 *   Storing it would mean a write fan-out every time quantity changes
 *   relative to minimumStock, for a value that's trivial to derive on read.
 */
data class Product(
    val id: String,
    val ownerId: String,
    val name: String,
    val sku: String,
    val categoryId: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val unitPrice: Double,
    val quantity: Int,
    val minimumStock: Int,
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long
) {
    /**
     * Derived, not stored — see class doc. Computed here so every screen
     * (Product List badges, Product Detail header, Dashboard low-stock
     * count) reads the exact same rule instead of each reimplementing the
     * <= / == comparison and risking the two definitions drifting apart.
     */
    val stockStatus: StockStatus
        get() = when {
            quantity <= 0 -> StockStatus.OUT_OF_STOCK
            quantity <= minimumStock -> StockStatus.LOW_STOCK
            else -> StockStatus.IN_STOCK
        }
}

enum class StockStatus {
    IN_STOCK,
    LOW_STOCK,
    OUT_OF_STOCK
}