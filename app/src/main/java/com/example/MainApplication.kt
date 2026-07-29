package com.example

import android.app.Application
import com.example.util.FirebaseHelper

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseHelper.initialize(this)
    }
}
