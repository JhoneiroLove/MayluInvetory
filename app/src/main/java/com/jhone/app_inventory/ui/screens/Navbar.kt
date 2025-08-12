package com.jhone.app_inventory.ui.screens

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import com.jhone.app_inventory.ui.components.SyncDiagnosticDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryNavbar(
    onAddClick: () -> Unit,
    onSyncClick: () -> Unit,
    onLogoutClick: () -> Unit,
    userRole: String = "asesor"
) {
    var showDiagnostic by remember { mutableStateOf(false) }
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
            // AGREGAR PRODUCTO - admin y asesor pueden agregar
            IconButton(onClick = onAddClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar Producto",
                    tint = Color.White
                )
            }

            // SINCRONIZAR - Todos pueden
            IconButton(onClick = onSyncClick) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Sincronizar Datos",
                    tint = Color.White
                )
            }

            // DIAGNÓSTICO - Solo admin puede ver
            if (isAdmin) {
                IconButton(onClick = { showDiagnostic = true }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Diagnóstico de Sincronización",
                        tint = Color.White
                    )
                }
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

    // Mostrar diálogo de diagnóstico
    if (showDiagnostic) {
        SyncDiagnosticDialog(
            onDismiss = { showDiagnostic = false }
        )
    }
}