package com.example.luluuu

import android.app.Application
import com.google.firebase.FirebaseApp

class LuluuuApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
} 