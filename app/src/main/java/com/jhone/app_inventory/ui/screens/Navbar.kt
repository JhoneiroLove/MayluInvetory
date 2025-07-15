package com.jhone.app_inventory.ui.screens

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import com.jhone.app_inventory.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryNavbar(
    onAddClick: () -> Unit,
    onSyncClick: () -> Unit,
    onLogoutClick: () -> Unit,
    userRole: String = "asesor"
) {
    val isAdmin = (userRole == "admin")

    TopAppBar(
        title = {
            Text(
                text = "Control de Inventario",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF9C84C9),
            titleContentColor = Color.White
        ),
        actions = {
            // AGREGAR PRODUCTO - Solo admin
            if (isAdmin) {
                IconButton(onClick = onAddClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Agregar Producto",
                        tint = Color.White
                    )
                }
            }

            // SINCRONIZAR - Todos pueden
            IconButton(onClick = onSyncClick) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Sincronizar Datos",
                    tint = Color.White
                )
            }

            // CERRAR SESIÓN - Todos pueden
            IconButton(onClick = onLogoutClick) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Cerrar Sesión",
                    tint = Color.White
                )
            }
        }
    )
}
