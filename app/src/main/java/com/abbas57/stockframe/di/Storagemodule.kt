package com.abbas57.stockframe.di

import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Separate module from AppModule rather than extended into it. AppModule
 * currently provides FirebaseAuth + FirebaseFirestore — both auth-module
 * dependencies from Sprint 1. Storage is a Sprint 2, product-image-specific
 * addition; keeping it in its own module means anyone reading AppModule
 * later doesn't have to mentally separate "this is auth-era" from
 * "this got added for products" — the file boundary does that for free.
 */
@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()
}