package com.jhone.app_inventory.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.jhone.app_inventory.ui.viewmodel.AuthViewModel
import com.jhone.app_inventory.ui.viewmodel.ProductViewModel

@Composable
fun MainScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel(),
    productViewModel: ProductViewModel = hiltViewModel(),
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            InventoryNavbar(
                onAddClick = {
                    navController.navigate("addProduct")
                },
                onSyncClick = { productViewModel.syncData() },
                onLogoutClick = {
                    authViewModel.signOut()
                    onLogout() // Redirige a la pantalla de login
                }
            )
        },
        bottomBar = {
            InventoryFooter()
        },
        content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Aquí irán las Cards en el futuro
            }
        }
    )
}
