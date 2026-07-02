package com.abbas57.stockframe.di


import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Tells Hilt how to construct dependencies it doesn't own — specifically,
 * third-party SDK classes (FirebaseAuth, FirebaseFirestore) that don't
 * have an @Inject constructor of their own, since they come from Google's
 * library code, not ours.
 *
 * @InstallIn(SingletonComponent::class) scopes this module to the app's
 * root Hilt component, meaning everything provided here lives for the
 * entire app process — matching FirebaseAuth/Firestore's own singleton
 * nature (you should never have more than one instance of either).
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        val firestore = FirebaseFirestore.getInstance()
        // Explicit, not just relying on the (already-on) default. Setting an
        // unlimited cache size here is a deliberate V1 choice — Stockframe's
        // data volume (one owner's products + transactions) will never
        // realistically approach a size where this matters, so there's no
        // reason to impose Firestore's smaller default cache limit and risk
        // older offline data getting evicted.
        firestore.firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                com.google.firebase.firestore.PersistentCacheSettings.newBuilder()
                    .setSizeBytes(com.google.firebase.firestore.FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build()
            )
            .build()
        return firestore
    }
}