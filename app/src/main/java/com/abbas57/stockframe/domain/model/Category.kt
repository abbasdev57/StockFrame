package com.abbas57.stockframe.domain.model

/**
 * Flat list, no hierarchy, no parent/child — matches the planning doc's
 * Section 2.2 cut: "Flat list, managed inline from product form, no
 * separate CRUD screen in V1." A mock showing "Furniture / Seating" as
 * a single category string is just that — one literal string a user
 * typed — not evidence of a two-level taxonomy we need to model.
 */
data class Category(
    val id: String,
    val name: String,
    val ownerId: String,
    val createdAt: Long
)