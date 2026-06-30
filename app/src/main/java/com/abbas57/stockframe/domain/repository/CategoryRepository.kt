package com.abbas57.stockframe.domain.repository

import com.abbas57.stockframe.domain.model.Category
import kotlinx.coroutines.flow.Flow

/**
 * Thinnest repository in the app — V1 categories are read + inline-create
 * only, no update, no delete. If a category is mistyped, V1's answer is
 * "add a new one," not an edit flow — a real scope cut, kept here as a
 * plain statement rather than narrated as something resolved in a
 * specific past conversation it wasn't actually part of.
 */
interface CategoryRepository {

    /** Live listener — keeps Add/Edit Product's category dropdown current without requiring the screen to be reopened. */
    fun observeCategories(): Flow<List<Category>>

    /**
     * Inline creation from Add/Edit Product's "Add new category" action.
     * Returns the created Category (with its generated Firestore doc ID)
     * so the caller can immediately select it without a second read.
     */
    suspend fun addCategory(name: String): Result<Category>
}