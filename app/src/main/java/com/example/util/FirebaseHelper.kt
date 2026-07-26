package com.example.util

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseHelper {

    fun initialize(context: Context) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
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
