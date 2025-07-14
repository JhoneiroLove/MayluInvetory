package com.jhone.app_inventory.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jhone.app_inventory.ui.screens.AddProductScreen
import com.jhone.app_inventory.ui.screens.EditProductScreen
import com.jhone.app_inventory.ui.screens.LoginScreen
import com.jhone.app_inventory.ui.screens.MainScreen
import com.jhone.app_inventory.ui.screens.ProductListScreen
import com.jhone.app_inventory.ui.screens.RegisterScreen
import com.jhone.app_inventory.ui.screens.SplashScreen
import com.jhone.app_inventory.ui.screens.StockHistoryScreen
import com.jhone.app_inventory.ui.screens.StockMovementScreen
import com.jhone.app_inventory.ui.viewmodel.AuthViewModel
import com.jhone.app_inventory.ui.viewmodel.ProductViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(
                authViewModel = authViewModel,
                navController = navController
            )
        }
        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate("productList") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onGoToRegister = { navController.navigate("register") }
            )
        }
        composable("register") {
            RegisterScreen(
                viewModel = authViewModel,
                onRegisterSuccess = {
                    // Una vez registrado, puedes redirigir al usuario a la pantalla principal o volver al login
                    navController.navigate("productList") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                onGoToLogin = { navController.navigate("login") }
            )
        }
        composable("productList") {
            ProductListScreen(
                navController = navController,
                onLogout = {
                    authViewModel.signOut()
                    navController.navigate("login") {
                        popUpTo("productList") { inclusive = true }
                    }
                },
                onAddClick = {
                    navController.navigate("addProduct")
                },
                userRole = authViewModel.userRole ?: "asesor" // O donde guardes el rol
            )
        }
        composable("addProduct") {
            AddProductScreen(
                userRole = authViewModel.userRole ?: "asesor",
                onCancel = { navController.popBackStack() },
                onProductAdded = { navController.popBackStack() }
            )
        }
        composable("editProductScreen/{productId}") { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId")
            // Obtén el ProductViewModel y la lista de productos
            val productViewModel: ProductViewModel = hiltViewModel()
            val productsState = productViewModel.products.collectAsState()
            val products = productsState.value
            // Busca el producto por su id
            val product = products.firstOrNull { it.id == productId }
            if (product != null) {
                EditProductScreen(
                    userRole = authViewModel.userRole ?: "asesor",
                    product = product,
                    onCancel = { navController.popBackStack() },
                    onProductUpdated = { navController.popBackStack() },
                    viewModel = productViewModel
                )
            } else {
                Text("Producto no encontrado")
            }
        }
        composable("stockMovementScreen/{productId}") { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId")
            val productViewModel: ProductViewModel = hiltViewModel()
            // Observamos la lista de productos
            val productsState by productViewModel.products.collectAsState()
            // Extraemos la lista real
            val products = productsState
            // Buscamos el producto
            val product = products.firstOrNull { it.id == productId }
            if (product != null) {
                StockMovementScreen(
                    product = product,
                    onCancel = { navController.popBackStack() },
                    onMovementAdded = { navController.popBackStack() },
                    viewModel = productViewModel
                )
            } else {
                Text("Producto no encontrado")
            }
        }
        composable("stockHistoryScreen/{productId}") { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId")
            val productViewModel: ProductViewModel = hiltViewModel()
            val productsState by productViewModel.products.collectAsState()
            val products = productsState
            val product = products.firstOrNull { it.id == productId }
            if (product != null) {
                StockHistoryScreen(
                    product = product,
                    onBack = { navController.popBackStack() },
                    viewModel = productViewModel
                )
            } else {
                Text("Producto no encontrado")
            }
        }
        composable("main") {
            MainScreen(
                navController = navController,
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }
        // ... Resto de pantallas
    }
}