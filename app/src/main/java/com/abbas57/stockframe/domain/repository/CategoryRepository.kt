package com.abbas57.stockframe.domain.repository

import com.abbas57.stockframe.domain.model.Category
import kotlinx.coroutines.flow.Flow

/**
 * Deliberately the thinnest repository in the app — categories in V1 are
 * read + inline-create only (Section 2.2: "no separate CRUD screen").
 * No update, no delete. If a user mistypes a category name, V1's answer
 * is "add a new one and stop using the old one," not an edit flow — that
 * scope cut is intentional, not an oversight, so don't add update/delete
 * methods here without that being a real decision first.
 */
interface CategoryRepository {

    /** Live listener — Add/Edit Product's category dropdown stays current if a category is added from another device/session without the user reopening the screen. */
    fun observeCategories(): Flow<List<Category>>

    /** One-shot read, same rationale as ProductRepository's getProductsOnce — needed for the same Settings-driven manual-fetch mode. */
    suspend fun getCategoriesOnce(): Result<List<Category>>

    /**
     * Inline creation from the Add/Edit Product form's "Add new category"
     * action. Returns the created Category (with its generated Firestore
     * doc ID) so the caller can immediately select it in the dropdown
     * without a second read.
     */
    suspend fun addCategory(name: String): Result<Category>
}