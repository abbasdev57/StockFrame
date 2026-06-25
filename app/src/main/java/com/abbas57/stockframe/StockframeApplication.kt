package com.abbas57.stockframe



import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point for Hilt's dependency graph.
 *
 * The @HiltAndroidApp annotation is the trigger for Hilt's annotation
 * processor: at compile time, it generates a base Dagger component
 * (Hilt calls it ApplicationComponent / SingletonComponent) that lives
 * for the entire process lifetime. Every @Singleton-scoped dependency
 * (FirebaseAuth, FirebaseFirestore, AuthRepositoryImpl, etc.) is held
 * inside that generated component.
 *
 * This class intentionally has no other code. Its only job is carrying
 * the annotation — do not add business logic here.
 *
 * Must be registered in AndroidManifest.xml:
 *   <application android:name=".StockframeApplication" ... >
 * If that registration is missing, every @HiltViewModel and @Inject
 * site in the app will fail to compile/inject, since Hilt has no
 * generated component to attach to.
 */
@HiltAndroidApp
class StockframeApplication : Application()