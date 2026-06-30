package com.abbas57.stockframe.domain.model

/**
 * Flat list, no hierarchy — matches the planning doc's scope cut: managed
 * inline from the product form, no separate CRUD screen in V1.
 */
data class Category(
    val id: String,
    val name: String,
    val ownerId: String,
    val createdAt: Long
)