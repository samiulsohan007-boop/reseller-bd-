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
                    Log.w("FirebaseHelper", "Default FirebaseApp init failed: ${e.message}, falling back to explicit options.")
                }
                
                if (FirebaseApp.getApps(context).isEmpty()) {
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:300749102398:android:06afc60c0f6963c02ae06f")
                        .setApiKey("AIzaSyCLdaZsKJqxd33flk2iTVbQF_SELQRO4hc")
                        .setGcmSenderId("300749102398")
                        .setProjectId("reseller-bd-app")
                        .setStorageBucket("reseller-bd-app.firebasestorage.app")
                        .build()
                    FirebaseApp.initializeApp(context, options)
                    Log.i("FirebaseHelper", "FirebaseApp initialized with explicit FirebaseOptions.")
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseHelper", "Failed to initialize FirebaseApp: ${e.message}", e)
        }
    }

    fun getAuth(context: Context): FirebaseAuth {
        initialize(context)
        return FirebaseAuth.getInstance()
    }

    fun getFirestore(context: Context): FirebaseFirestore {
        initialize(context)
        return FirebaseFirestore.getInstance()
    }
}
