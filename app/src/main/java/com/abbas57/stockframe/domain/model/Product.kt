package com.abbas57.stockframe.domain.model

/**
 * Plain Kotlin data class — zero Firestore/Storage imports. Same rule as
 * User in the auth module: ViewModels and UI never see a DocumentSnapshot
 * or Storage reference, only this.
 *
 * categoryId only, no embedded category name — ProductListViewModel joins
 * against CategoryRepository's list at read time instead, so renaming a
 * category updates every product row with no extra write anywhere.
 *
 * No warehouse, supplier, leadTime, location, or pendingOrders fields.
 * These appeared in the Stitch mocks but multi-warehouse and supplier
 * management are Phase-2 items per the planning doc — they get added
 * when that phase is actually scoped.
 *
 * quantity CAN be edited directly via Add/Edit Product (a deliberate
 * choice, not the original plan) — see ProductRepository.updateProduct's
 * doc for how the audit trail stays intact when that happens.
 *
 * isActive implements soft delete — never hard-deleted, to avoid orphaning
 * inventory_transactions that still reference this product's ID.
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
     * Derived, not stored — every screen reads the same rule instead of
     * each reimplementing the comparison and risking drift between them.
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