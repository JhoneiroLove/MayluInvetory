package com.jhone.app_inventory.ui.screens

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

@Composable
fun StockMovementScreen(
    product: Product,
    onCancel: () -> Unit,
    onMovementAdded: () -> Unit,
    viewModel: ProductViewModel = hiltViewModel()
) {
    // Estados observados del ViewModel
    val isLoading by viewModel.isLoading.collectAsState()

    val primaryColor = Color(0xFF9C84C9)
    val cardBackgroundColor = Color.White
    val buttonColor = Color(0xFF7851A9)

    // Configuración de colores para los campos de texto
    val fieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        focusedIndicatorColor = Color.Black,
        unfocusedIndicatorColor = Color.Gray,
        cursorColor = Color.Black
    )

    // Estados locales para tipo de movimiento, cantidad y observaciones
    var movementType by remember { mutableStateOf("ingreso") }
    var quantityText by remember { mutableStateOf("") }
    var observation by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(primaryColor)  // Fondo de la pantalla con color principal
    ) {
        // Tarjeta centrada sobre el fondo
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
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
                    text = product.descripcion,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.Black,
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Center
                )

                // Mostrar stock actual
                Text(
                    text = "Stock actual: ${product.cantidad}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = primaryColor,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )

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
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Selección del tipo de movimiento: Ingreso o Salida
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val selectedButtonColors = ButtonDefaults.buttonColors(containerColor = buttonColor)
                    val unselectedButtonColors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)

                    Button(
                        onClick = {
                            if (!isLoading) {
                                movementType = "ingreso"
                                errorMessage = null
                            }
                        },
                        enabled = !isLoading,
                        colors = if (movementType == "ingreso") selectedButtonColors else unselectedButtonColors,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(text = "Ingreso", color = Color.White)
                    }
                    Button(
                        onClick = {
                            if (!isLoading) {
                                movementType = "salida"
                                errorMessage = null
                            }
                        },
                        enabled = !isLoading,
                        colors = if (movementType == "salida") selectedButtonColors else unselectedButtonColors,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(text = "Salida", color = Color.White)
                    }
                }

                // Campo: Cantidad
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { newValue ->
                        quantityText = newValue
                        errorMessage = null
                    },
                    label = { Text("Cantidad", color = Color.Black) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = fieldColors,
                    enabled = !isLoading,
                    textStyle = LocalTextStyle.current.copy(color = Color.Black)
                )

                // Campo: Observaciones
                OutlinedTextField(
                    value = observation,
                    onValueChange = { observation = it },
                    label = { Text("Observaciones", color = Color.Black) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    enabled = !isLoading,
                    textStyle = LocalTextStyle.current.copy(color = Color.Black)
                )

                // Botones: Cancelar y Guardar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor)
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            // Validaciones
                            val quantity = quantityText.toIntOrNull()
                            when {
                                quantityText.isBlank() || quantity == null -> {
                                    errorMessage = "La cantidad debe ser un número válido"
                                    return@Button
                                }
                                quantity <= 0 -> {
                                    errorMessage = "La cantidad debe ser mayor a cero"
                                    return@Button
                                }
                                movementType == "salida" && quantity > product.cantidad -> {
                                    errorMessage = "No hay suficiente stock. Stock actual: ${product.cantidad}"
                                    return@Button
                                }
                            }

                            errorMessage = null
                            val movimiento = Movimiento(
                                loteId = product.id,
                                tipo = movementType,
                                cantidad = quantity,
                                fecha = Timestamp.now(),
                                usuario = "",
                                observacion = observation
                            )

                            // Llamada para registrar el movimiento
                            viewModel.addMovimiento(movimiento) { success, error ->
                                if (success) {
                                    onMovementAdded() // Cierra la interfaz
                                } else {
                                    errorMessage = error ?: "Error desconocido al procesar el movimiento"
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
                    ) {
                        if (isLoading) {
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
                            Text(text = "Guardar", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}