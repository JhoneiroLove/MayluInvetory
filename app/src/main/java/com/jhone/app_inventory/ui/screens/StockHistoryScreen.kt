package com.jhone.app_inventory.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jhone.app_inventory.data.Movimiento
import com.jhone.app_inventory.data.Product
import com.jhone.app_inventory.ui.viewmodel.ProductViewModel
import com.jhone.app_inventory.utils.DateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockHistoryScreen(
    product: Product,
    onBack: () -> Unit,
    viewModel: ProductViewModel = hiltViewModel()
) {
    // Estados locales
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Escuchar movimientos del ViewModel
    LaunchedEffect(product.id) {
        Log.d("StockHistoryScreen", "Configurando listener para producto: ${product.id}")
        viewModel.clearMovimientosListener() // Limpiar primero
        delay(300) // Pequeña pausa
        viewModel.listenToMovimientosForProduct(product.id)
    }

    val movimientos by viewModel.movimientos.collectAsState()

    // Debug log
    LaunchedEffect(movimientos) {
        Log.d("StockHistoryScreen", "Movimientos actualizados: ${movimientos.size}")
        movimientos.forEachIndexed { index, mov ->
            Log.d("StockHistoryScreen", "Movimiento $index: ${mov.tipo} - ${mov.cantidad} - ${mov.usuario} - ${mov.observacion}")
        }
    }

    // Limpiar al salir
    DisposableEffect(product.id) {
        onDispose {
            viewModel.clearMovimientosListener()
        }
    }

    val primaryColor = Color(0xFF9C84C9)
    val backgroundColor = Color(0xFFF7F7F7)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Historial de Movimientos",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = product.descripcion.take(25) + if (product.descripcion.length > 25) "..." else "",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
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
                actions = {
                    IconButton(
                        onClick = {
                            isRefreshing = true
                            // Forzar recarga del listener
                            viewModel.clearMovimientosListener()
                            scope.launch {
                                delay(500)
                                viewModel.listenToMovimientosForProduct(product.id)
                                delay(1000)
                                isRefreshing = false
                            }
                        },
                        enabled = !isRefreshing
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Actualizar",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = primaryColor,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = backgroundColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Información del producto
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Código: ${product.codigo}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = Color.Black
                                )
                                Text(
                                    text = "Stock actual: ${product.cantidad}",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = primaryColor
                                )
                            }

                            Text(
                                text = "${movimientos.size} movimientos",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // Lista de movimientos o mensaje vacío
                if (movimientos.isEmpty() && !isRefreshing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "📊",
                                style = MaterialTheme.typography.headlineLarge
                            )
                            Text(
                                text = "No hay movimientos registrados",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.Black
                            )
                            Text(
                                text = "Los ingresos y salidas aparecerán aquí",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )

                            Button(
                                onClick = {
                                    isRefreshing = true
                                    viewModel.clearMovimientosListener()
                                    scope.launch {
                                        delay(500)
                                        viewModel.listenToMovimientosForProduct(product.id)
                                        delay(1000)
                                        isRefreshing = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryColor
                                )
                            ) {
                                Text("Actualizar", color = Color.White)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = movimientos,
                            key = { it.id }
                        ) { movimiento ->
                            MovimientoItemCard(movimiento = movimiento)
                        }

                        // Espacio al final
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MovimientoItemCard(movimiento: Movimiento) {
    val fechaStr = DateUtils.formatDateTime(movimiento.fecha)
    val isIngreso = movimiento.tipo.lowercase() == "ingreso"

    val cardBackground = Color.White
    // COLORES CONSISTENTES CON LA APP
    val headerColor = if (isIngreso) Color(0xFF9C84C9) else Color(0xFF7851A9) // Tonos morados de la app
    val textColor = Color.Black

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground)
    ) {
        Column {
            // Encabezado con tipo de movimiento
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerColor)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${if (isIngreso) "📈" else "📉"} ${movimiento.tipo.replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "${if (isIngreso) "+" else "-"}${movimiento.cantidad}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                }
            }

            // Contenido principal
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Usuario:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = movimiento.usuario.takeIf { it.isNotEmpty() } ?: "Sistema",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = textColor
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Fecha:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = fechaStr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                }

                if (movimiento.observacion.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            // Fondo sutil con tono morado claro
                            containerColor = Color(0xFFF8F6FF)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "Observaciones:",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF7851A9) // Color morado para consistencia
                            )
                            Text(
                                text = movimiento.observacion,
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }
    }
}