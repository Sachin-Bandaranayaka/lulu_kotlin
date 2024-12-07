package com.example.luluuu

import android.app.Application
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.security.ProviderInstaller
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class LuluuuApplication : Application() {
    companion object {
        private const val TAG = "LuluuuApplication"
    }

    override fun onCreate() {
        super.onCreate()
        
        if (initializeGooglePlayServices()) {
            initializeFirebase()
        }
    }

    private fun initializeGooglePlayServices(): Boolean {
        val availability = GoogleApiAvailability.getInstance()
        val resultCode = availability.isGooglePlayServicesAvailable(this)

        return when (resultCode) {
            ConnectionResult.SUCCESS -> {
                Log.d(TAG, "Google Play Services is available")
                // Initialize security provider
                try {
                    ProviderInstaller.installIfNeeded(this)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to install security provider", e)
                }
                true
            }
            else -> {
                Log.e(TAG, "Google Play Services is not available: $resultCode")
                if (availability.isUserResolvableError(resultCode)) {
                    Log.w(TAG, "Google Play Services error is resolvable")
                }
                false
            }
        }
    }

    private fun initializeFirebase() {
        try {
            // Initialize Firebase only if it hasn't been initialized yet
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                
                // Configure Firestore settings with offline persistence
                val settings = FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)  // This will be removed in a future release but is still supported
                    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build()
                
                FirebaseFirestore.getInstance().firestoreSettings = settings
                Log.d(TAG, "Firestore settings configured")

                // Sign in anonymously to ensure we have access
                FirebaseAuth.getInstance().signInAnonymously()
                    .addOnSuccessListener {
                        Log.d(TAG, "Anonymous auth successful")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Anonymous auth failed", e)
                    }
            }
            Log.d(TAG, "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase", e)
        }
    }
}