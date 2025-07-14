package com.jhone.app_inventory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.jhone.app_inventory.data.Movimiento
import com.jhone.app_inventory.data.Product
import com.jhone.app_inventory.ui.viewmodel.ProductViewModel
import com.jhone.app_inventory.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockHistoryScreen(
    product: Product,
    onBack: () -> Unit,
    viewModel: ProductViewModel = hiltViewModel()
) {
    // Escuchar movimientos
    LaunchedEffect(product.id) {
        viewModel.listenToMovimientosForProduct(product.id)
    }
    val movimientos by viewModel.movimientos.collectAsState()

    val primaryColor = Color(0xFF9C84C9) // Color principal de tu app
    val backgroundColor = Color(0xFFF7F7F7)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Historial de Movimientos",
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = primaryColor,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = backgroundColor // Fondo general de la pantalla
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (movimientos.isEmpty()) {
                // Mensaje si no hay movimientos
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No hay movimientos registrados para este producto.",
                        color = Color.Black
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    items(movimientos, key = { it.id }) { movimiento ->
                        MovimientoItem(movimiento)
                    }
                }
            }
        }
    }
}

@Composable
fun MovimientoItem(movimiento: Movimiento) {
    val fechaStr = DateUtils.formatDateTime(movimiento.fecha)

    // Colores de ejemplo
    val cardBackground = Color.White
    val headerColor = Color(0xFF9C84C9)
    val textColor = Color.Black

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground)
    ) {
        Column {
            // Encabezado de la tarjeta (por si quieres mostrar "Ingreso" o "Salida" en un color distinto)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerColor)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Tipo: ${movimiento.tipo.capitalize()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
            // Contenido principal
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Cantidad: ${movimiento.cantidad}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
                Text(
                    text = "Usuario: ${movimiento.usuario}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
                Text(
                    text = "Fecha: $fechaStr",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
                if (movimiento.observacion.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Observaciones: ${movimiento.observacion}",
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor
                    )
                }
            }
        }
    }
}