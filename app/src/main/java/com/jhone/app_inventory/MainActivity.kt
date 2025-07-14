package com.jhone.app_inventory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jhone.app_inventory.ui.navigation.AppNavigation
import com.jhone.app_inventory.ui.theme.App_inventoryTheme
import com.google.android.gms.security.ProviderInstaller
import dagger.hilt.android.AndroidEntryPoint
import android.util.Log

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            ProviderInstaller.installIfNeeded(applicationContext)
            Log.d("ProviderInstaller", "Provider instalado correctamente")
        } catch (e: Exception) {
            Log.e("ProviderInstaller", "Error instalando Provider: ${e.message}", e)
        }
        enableEdgeToEdge()
        setContent {
            App_inventoryTheme {
                // Se invoca la navegación completa
                AppNavigation()
            }
        }
    }
}
