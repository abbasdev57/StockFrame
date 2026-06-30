package com.abbas57.stockframe.domain.model

/**
 * Represents a single stock movement event. Immutable once written —
 * inventory_transactions records are NEVER updated or deleted, only
 * appended. This is the audit trail that makes quantity trustworthy:
 * you can always reconstruct the current stock by summing all
 * transactions for a product, which is a useful sanity-check query
 * if a quantity ever looks wrong.
 *
 * type drives the UI treatment everywhere this appears:
 *   IN         → green, positive quantity delta, "Restock" / "Initial stock"
 *   OUT        → red, negative quantity delta, "Sale" / "Damaged" / "Expired"
 *   ADJUSTMENT → neutral blue, signed delta, "Manual correction via product edit"
 *
 * quantity is always stored as a SIGNED integer:
 *   IN  → positive (e.g. +50)
 *   OUT → negative (e.g. -12)
 *   ADJUSTMENT → signed delta (e.g. -3 or +7)
 * This means current stock = sum(quantity) across all transactions,
 * without needing to know the type to get the sign right.
 */
data class InventoryTransaction(
    val id: String,
    val productId: String,
    val productName: String,  // denormalized for History screen display —
    // same rationale as categoryName on Product:
    // avoids a per-row join against products
    // collection just to render the global list
    val ownerId: String,
    val type: TransactionType,
    val quantity: Int,        // signed — see class doc
    val reason: String,
    val note: String?,
    val createdAt: Long
)

enum class TransactionType {
    IN, OUT, ADJUSTMENT;

    /** Human-readable label for UI display. */
    fun displayLabel(): String = when (this) {
        IN -> "Stock In"
        OUT -> "Stock Out"
        ADJUSTMENT -> "Adjustment"
    }
}