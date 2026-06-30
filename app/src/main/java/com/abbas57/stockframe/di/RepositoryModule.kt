package com.abbas57.stockframe.di


import com.abbas57.stockframe.data.repository.AuthRepositoryImpl
import com.abbas57.stockframe.data.repository.CategoryRepositoryImpl
import com.abbas57.stockframe.data.repository.InventoryTransactionRepositoryImpl
import com.abbas57.stockframe.data.repository.ProductRepositoryImpl
import com.abbas57.stockframe.domain.repository.AuthRepository
import com.abbas57.stockframe.domain.repository.CategoryRepository
import com.abbas57.stockframe.domain.repository.InventoryTransactionRepository
import com.abbas57.stockframe.domain.repository.ProductRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * @Binds tells Hilt which concrete class to hand over whenever something
 * asks for the AuthRepository interface. This is the one piece manual DI
 * made implicit (you'd just write `val authRepository: AuthRepository =
 * AuthRepositoryImpl(...)` and move on) — Hilt makes this mapping
 * explicit and centralized in one file, which is easier to audit later
 * when there are many repositories instead of just one.
 *
 * Module is `abstract class`, not `object`, because @Binds methods must
 * be abstract — they're a mapping declaration, not executable code (unlike
 * @Provides functions in AppModule, which DO contain real construction logic).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
    @Binds
    abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository

    @Binds
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    abstract fun bindInventoryTransactionRepository(
        impl: InventoryTransactionRepositoryImpl
    ): InventoryTransactionRepository
}