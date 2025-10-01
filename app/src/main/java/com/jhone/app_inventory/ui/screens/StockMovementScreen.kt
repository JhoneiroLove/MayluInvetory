package com.jhone.app_inventory.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.jhone.app_inventory.data.Movimiento
import com.jhone.app_inventory.data.Product
import com.jhone.app_inventory.ui.viewmodel.ProductViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockMovementScreen(
    product: Product,
    onCancel: () -> Unit,
    onMovementAdded: () -> Unit,
    viewModel: ProductViewModel = hiltViewModel()
) {
    // 🔥 ACTIVAR LISTENER EN TIEMPO REAL PARA ESTE PRODUCTO
    LaunchedEffect(product.id) {
        Log.d("StockMovementScreen", "🔄 Activando listener para producto: ${product.id}")
        viewModel.listenToProductUpdates(product.id)
    }

    // 🧹 LIMPIAR LISTENERS AL SALIR
    DisposableEffect(product.id) {
        onDispose {
            Log.d("StockMovementScreen", "🧹 Limpiando listeners para producto: ${product.id}")
            viewModel.clearProductListener(product.id)
            viewModel.clearMovimientosListener()
        }
    }

    // ESTADO LOCAL SIN ACTUALIZACIONES OPTIMISTAS
    var isLocalLoading by remember { mutableStateOf(false) }
    var hasMovementBeenProcessed by remember { mutableStateOf(false) }

    // Usar el producto original sin modificaciones locales
    val currentProduct = remember(product) { product }

    // 🔥 OBSERVAR PRODUCTOS ACTUALIZADOS DEL VIEWMODEL PARA MOSTRAR STOCK REAL
    val products by viewModel.products.collectAsState()
    val updatedProduct = remember(products, currentProduct.id) {
        products.find { it.id == currentProduct.id } ?: currentProduct
    }

    val primaryColor = Color(0xFF9C84C9)
    val cardBackgroundColor = Color.White
    val buttonColor = Color(0xFF7851A9)

    val fieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent, // Mantener transparente cuando está deshabilitado
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        disabledTextColor = Color.Gray, // Texto gris cuando está deshabilitado, no negro
        focusedIndicatorColor = Color(0xFF7851A9), // Color morado de la app
        unfocusedIndicatorColor = Color.Gray,
        disabledIndicatorColor = Color.LightGray, // Borde gris claro cuando está deshabilitado
        cursorColor = Color(0xFF7851A9), // Cursor morado
        focusedLabelColor = Color(0xFF7851A9), // Label morado cuando tiene foco
        unfocusedLabelColor = Color.Black,
        disabledLabelColor = Color.Gray // Label gris cuando está deshabilitado
    )

    // Estados del formulario
    var movementType by remember { mutableStateOf("ingreso") }
    var quantityText by remember { mutableStateOf("") }
    var observation by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 🔥 FUNCIÓN PARA MANEJAR MOVIMIENTO CON LOGGING DETALLADO
    val handleMovement = { quantity: Int ->
        if (!hasMovementBeenProcessed) {
            isLocalLoading = true
            errorMessage = null
            hasMovementBeenProcessed = true

            Log.d("StockMovementScreen", "🚀 INICIANDO MOVIMIENTO: Producto=${updatedProduct.codigo}, Tipo=$movementType, Cantidad=$quantity, Stock Actual=${updatedProduct.cantidad}")

            val movimiento = Movimiento(
                loteId = currentProduct.id,
                tipo = movementType,
                cantidad = quantity,
                fecha = Timestamp.now(),
                usuario = "", // Se establecerá en el ViewModel
                observacion = observation
            )

            viewModel.addMovimiento(movimiento) { success, error ->
                isLocalLoading = false

                if (success) {
                    Log.d("StockMovementScreen", "✅ MOVIMIENTO EXITOSO: Cerrando pantalla")
                    // Solo cerrar la pantalla si fue exitoso
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        delay(500) // Dar tiempo para que se actualice la base de datos
                        onMovementAdded()
                    }
                } else {
                    Log.e("StockMovementScreen", "❌ ERROR EN MOVIMIENTO: $error")
                    // Resetear estado para permitir reintento
                    hasMovementBeenProcessed = false
                    errorMessage = error ?: "Error desconocido al procesar el movimiento"
                }
            }
        } else {
            errorMessage = "Movimiento ya procesado"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(primaryColor)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .align(Alignment.Center),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Título principal
                Text(
                    text = "Gestionar Stock",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = primaryColor,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )

                // Subtítulo: nombre o descripción del producto
                Text(
                    text = updatedProduct.descripcion,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.Black,
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Center
                )

                // 🔥 MOSTRAR STOCK ACTUAL REAL (SIN OPTIMIZACIONES) + LOGGING
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF8F6FF) // Fondo morado muy claro de la app
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Stock actual: ${updatedProduct.cantidad}",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Color(0xFF7851A9), // Color morado de la app
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }

                // 🔍 DEBUG: Mostrar info adicional en desarrollo
                if (updatedProduct.id != currentProduct.id || updatedProduct.cantidad != currentProduct.cantidad) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE3F2FD)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Text(
                                text = "🔍 DEBUG INFO:",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF1976D2)
                            )
                            Text(
                                text = "Original: ${currentProduct.cantidad}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF1976D2)
                            )
                            Text(
                                text = "Actualizado: ${updatedProduct.cantidad}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF1976D2)
                            )
                        }
                    }
                }

                // Indicador de estado de carga
                if (isLocalLoading) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE3F2FD)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = primaryColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Procesando movimiento...",
                                color = primaryColor,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Mostrar mensaje de error si existe
                errorMessage?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFEBEE)
                        )
                    ) {
                        Text(
                            text = error,
                            color = Color.Red,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Selección del tipo de movimiento: Ingreso o Salida
                Text(
                    text = "Tipo de movimiento:",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.Black
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val selectedButtonColors = ButtonDefaults.buttonColors(containerColor = buttonColor)
                    val unselectedButtonColors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)

                    Button(
                        onClick = {
                            if (!isLocalLoading && !hasMovementBeenProcessed) {
                                movementType = "ingreso"
                                errorMessage = null
                                Log.d("StockMovementScreen", "🔄 Tipo cambiado a: $movementType")
                            }
                        },
                        enabled = !isLocalLoading && !hasMovementBeenProcessed,
                        colors = if (movementType == "ingreso") selectedButtonColors else unselectedButtonColors,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Ingreso", color = Color.White)
                    }

                    Button(
                        onClick = {
                            if (!isLocalLoading && !hasMovementBeenProcessed) {
                                movementType = "salida"
                                errorMessage = null
                                Log.d("StockMovementScreen", "🔄 Tipo cambiado a: $movementType")
                            }
                        },
                        enabled = !isLocalLoading && !hasMovementBeenProcessed,
                        colors = if (movementType == "salida") selectedButtonColors else unselectedButtonColors,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Salida", color = Color.White)
                    }
                }

                // Campo: Cantidad
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { newValue ->
                        if (!isLocalLoading && !hasMovementBeenProcessed) {
                            quantityText = newValue
                            errorMessage = null
                            Log.d("StockMovementScreen", "📝 Cantidad cambiada a: $newValue")
                        }
                    },
                    label = { Text("Cantidad", color = Color.Black) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = fieldColors,
                    enabled = !isLocalLoading && !hasMovementBeenProcessed,
                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
                    singleLine = true
                )

                // Campo: Observaciones
                OutlinedTextField(
                    value = observation,
                    onValueChange = {
                        if (!isLocalLoading && !hasMovementBeenProcessed) {
                            observation = it
                            Log.d("StockMovementScreen", "📝 Observación cambiada")
                        }
                    },
                    label = { Text("Observaciones", color = Color.Black) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    enabled = !isLocalLoading && !hasMovementBeenProcessed,
                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
                    maxLines = 3
                )

                // Validación en tiempo real para salidas
                if (movementType == "salida" && quantityText.isNotEmpty()) {
                    val requestedQuantity = quantityText.toIntOrNull() ?: 0
                    if (requestedQuantity > updatedProduct.cantidad) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFF3E0)
                            )
                        ) {
                            Text(
                                text = "⚠️ Cantidad solicitada ($requestedQuantity) mayor al stock disponible (${updatedProduct.cantidad})",
                                color = Color(0xFFFF8F00),
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Botones: Cancelar y Guardar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (!isLocalLoading) {
                                Log.d("StockMovementScreen", "❌ Cancelando operación")
                                onCancel()
                            }
                        },
                        enabled = !isLocalLoading,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            val quantity = quantityText.toIntOrNull()

                            Log.d("StockMovementScreen", "🔍 VALIDANDO: quantity=$quantity, movementType=$movementType, stock=${updatedProduct.cantidad}")

                            // Validaciones mejoradas
                            val validationError = when {
                                hasMovementBeenProcessed -> "Movimiento ya procesado"
                                quantityText.isBlank() || quantity == null -> "La cantidad debe ser un número válido"
                                quantity <= 0 -> "La cantidad debe ser mayor a cero"
                                movementType == "salida" && quantity > updatedProduct.cantidad ->
                                    "Stock insuficiente. Stock actual: ${updatedProduct.cantidad}"
                                else -> null
                            }

                            if (validationError != null) {
                                Log.w("StockMovementScreen", "⚠️ VALIDACIÓN FALLIDA: $validationError")
                                errorMessage = validationError
                            } else {
                                Log.d("StockMovementScreen", "✅ VALIDACIÓN EXITOSA: Procesando movimiento")
                                // Procesar movimiento
                                handleMovement(quantity!!)
                            }
                        },
                        enabled = !isLocalLoading && !hasMovementBeenProcessed,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
                    ) {
                        if (isLocalLoading) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White
                                )
                                Text(text = "Guardando...", color = Color.White)
                            }
                        } else {
                            Text(
                                text = if (hasMovementBeenProcessed) "Procesando..." else "Guardar",
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}