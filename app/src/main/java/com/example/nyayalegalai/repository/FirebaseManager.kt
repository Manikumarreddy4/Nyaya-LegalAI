package com.example.nyayalegalai.repository

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.nyayalegalai.models.UserProfile
import com.example.nyayalegalai.utils.NetworkUtils
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.storage.FirebaseStorage

object FirebaseManager {

    fun init(context: Context) {
        try {
            // Check if Firebase is already initialized
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
                Log.d("FirebaseManager", "FirebaseApp initialized manually.")
            }

            // Optimize Firestore offline caching
            val firestore = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            firestore.firestoreSettings = settings

            Log.d("FirebaseManager", "Firestore and Auth initialized with persistence.")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error initializing Firebase gracefully", e)
        }
    }

    fun handleFirestoreError(context: Context, e: Exception) {
        Log.e("FirebaseManager", "Firestore Error occurred", e)
        if (!NetworkUtils.isInternetAvailable(context)) {
            Toast.makeText(context, "No internet connection. Showing offline legal data.", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Cloud data sync issue. Check your connection.", Toast.LENGTH_SHORT).show()
        }
    }
}
