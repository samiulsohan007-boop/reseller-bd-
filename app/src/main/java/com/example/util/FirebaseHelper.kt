package com.example.util

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseHelper {

    fun initialize(context: Context) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                try {
                    FirebaseApp.initializeApp(context)
                } catch (e: Exception) {
                    Log.w("FirebaseHelper", "Default FirebaseApp init failed, initializing fallback options: ${e.message}")
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:102938475610:android:resellerbdapp")
                        .setApiKey("AIzaSyDemoApiKeyResellerBDApp1234567")
                        .setProjectId("reseller-bd-app")
                        .build()
                    FirebaseApp.initializeApp(context, options)
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseHelper", "Failed to initialize FirebaseApp: ${e.message}")
        }
    }

    fun getAuth(context: Context): FirebaseAuth? {
        initialize(context)
        return try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseAuth.getInstance()
            } else null
        } catch (e: Exception) {
            Log.e("FirebaseHelper", "FirebaseAuth instance error: ${e.message}")
            null
        }
    }

    fun getFirestore(context: Context): FirebaseFirestore? {
        initialize(context)
        return try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseFirestore.getInstance()
            } else null
        } catch (e: Exception) {
            Log.e("FirebaseHelper", "FirebaseFirestore instance error: ${e.message}")
            null
        }
    }
}
