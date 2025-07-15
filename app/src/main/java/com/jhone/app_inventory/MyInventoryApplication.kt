package com.jhone.app_inventory

import android.app.Application
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyInventoryApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicialización de Firebase
        FirebaseApp.initializeApp(this)

        // Room se inicializa automáticamente por Hilt cuando se necesite
        android.util.Log.d("MyInventoryApplication", "Aplicación inicializada con Room + Firebase")
    }
}