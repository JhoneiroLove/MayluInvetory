package com.jhone.app_inventory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jhone.app_inventory.data.Product
import com.jhone.app_inventory.ui.viewmodel.ProductViewModel
import com.jhone.app_inventory.utils.DateUtils
import com.google.firebase.Timestamp

@Composable
fun EditProductScreen(
    userRole: String,
    product: Product,
    onCancel: () -> Unit,
    onProductUpdated: () -> Unit,
    viewModel: ProductViewModel = hiltViewModel()
) {
    val isAdmin = (userRole == "admin")

    var proveedor by remember { mutableStateOf(product.proveedor) }
    var codigo by remember { mutableStateOf(product.codigo) }
    var descripcion by remember { mutableStateOf(product.descripcion) }
    var cantidadText by remember { mutableStateOf(product.cantidad.toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Estados observados del ViewModel
    val isLoading by viewModel.isLoading.collectAsState()

    // Ahora, el precio público se calcula automáticamente, por lo que no lo editamos directamente.
    var precioBoletaText by remember { mutableStateOf(if (isAdmin) product.precioBoleta.toString() else "") }
    var porcentajeText by remember { mutableStateOf(if (isAdmin) product.porcentaje.toString() else "") }

    val originalBoleta = product.precioBoleta
    val originalPorcentaje = product.porcentaje

    val fechaVenc = DateUtils.formatDate(product.fechaVencimiento)
    var fechaVencimientoText by remember { mutableStateOf(fechaVenc) }

    val precioBoleta = if (isAdmin) precioBoletaText.toDoubleOrNull() ?: 0.0 else originalBoleta
    val porcentajeValue = if (isAdmin) porcentajeText.toDoubleOrNull() ?: 0.0 else originalPorcentaje
    val precioCosto = precioBoleta * 1.02
    val precioProducto = precioCosto * (1 + (porcentajeValue / 100))

    val scrollState = rememberScrollState()

    val primaryColor = Color(0xFF9C84C9)
    val cardBackgroundColor = Color.White
    val buttonColor = Color(0xFF7851A9)
    val fieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        focusedIndicatorColor = Color.Black,
        unfocusedIndicatorColor = Color.Gray,
        cursorColor = Color.Black
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(primaryColor)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 0.dp, max = 700.dp)  // Limita la altura del card
                .padding(horizontal = 16.dp)
                .align(Alignment.Center),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Editar Producto",
                    style = MaterialTheme.typography.headlineSmall.copy(color = primaryColor)
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
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                OutlinedTextField(
                    value = proveedor,
                    onValueChange = {
                        proveedor = it
                        errorMessage = null
                    },
                    label = { Text("Proveedor") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = Color.Black),
                    colors = fieldColors,
                    enabled = !isLoading
                )
                OutlinedTextField(
                    value = codigo,
                    onValueChange = {
                        codigo = it
                        errorMessage = null
                    },
                    label = { Text("Código del Producto") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = Color.Black),
                    colors = fieldColors,
                    enabled = !isLoading
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = {
                        descripcion = it
                        errorMessage = null
                    },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = Color.Black),
                    colors = fieldColors,
                    enabled = !isLoading
                )
                OutlinedTextField(
                    value = cantidadText,
                    onValueChange = {
                        cantidadText = it
                        errorMessage = null
                    },
                    label = { Text("Cantidad") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = Color.Black),
                    colors = fieldColors,
                    enabled = !isLoading
                )
                if (isAdmin) {
                    OutlinedTextField(
                        value = precioBoletaText,
                        onValueChange = {
                            precioBoletaText = it
                            errorMessage = null
                        },
                        label = { Text("Precio Boleta") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors,
                        enabled = !isLoading
                    )
                    OutlinedTextField(
                        value = porcentajeText,
                        onValueChange = {
                            porcentajeText = it
                            errorMessage = null
                        },
                        label = { Text("Porcentaje (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors,
                        enabled = !isLoading
                    )
                }
                // Ocultamos Precio Costo y Precio Público para asesores
                if (isAdmin) {
                    OutlinedTextField(
                        value = String.format("%.2f", precioCosto),
                        onValueChange = {},
                        label = { Text("Precio Costo") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        textStyle = TextStyle(color = Color.Black),
                        colors = fieldColors
                    )
                    OutlinedTextField(
                        value = String.format("%.2f", precioProducto),
                        onValueChange = {},
                        label = { Text("Precio Público") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        textStyle = TextStyle(color = Color.Black),
                        colors = fieldColors
                    )
                }
                OutlinedTextField(
                    value = fechaVencimientoText,
                    onValueChange = { fechaVencimientoText = it },
                    label = { Text("Fecha Venc. (dd/MM/yyyy)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    enabled = !isLoading
                )
                Button(
                    onClick = {
                        // Validaciones básicas
                        when {
                            proveedor.isBlank() -> {
                                errorMessage = "El proveedor es obligatorio"
                                return@Button
                            }
                            codigo.isBlank() -> {
                                errorMessage = "El código es obligatorio"
                                return@Button
                            }
                            descripcion.isBlank() -> {
                                errorMessage = "La descripción es obligatoria"
                                return@Button
                            }
                            cantidadText.isBlank() || cantidadText.toIntOrNull() == null -> {
                                errorMessage = "La cantidad debe ser un número válido"
                                return@Button
                            }
                            isAdmin && (precioBoletaText.isBlank() || precioBoletaText.toDoubleOrNull() == null) -> {
                                errorMessage = "El precio boleta debe ser un número válido"
                                return@Button
                            }
                            isAdmin && (porcentajeText.isBlank() || porcentajeText.toDoubleOrNull() == null) -> {
                                errorMessage = "El porcentaje debe ser un número válido"
                                return@Button
                            }
                        }

                        errorMessage = null
                        val cantidad = cantidadText.toIntOrNull() ?: 0
                        val parsedFechaVenc = DateUtils.parseDate(fechaVencimientoText)

                        val updatedProduct = Product(
                            id = product.id,
                            codigo = codigo,
                            descripcion = descripcion,
                            cantidad = cantidad,
                            precioBoleta = precioBoleta,
                            precioCosto = precioCosto,
                            precioProducto = precioProducto,
                            proveedor = proveedor,
                            createdAt = product.createdAt,
                            fechaVencimiento = parsedFechaVenc,
                            porcentaje = porcentajeValue
                        )

                        viewModel.updateProduct(updatedProduct) { success, error ->
                            if (success) {
                                onProductUpdated()
                            } else {
                                errorMessage = error ?: "Error desconocido al actualizar producto"
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    shape = RoundedCornerShape(16.dp)
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
                            Text(text = "Actualizando...", color = Color.White)
                        }
                    } else {
                        Text("Actualizar", color = Color.White)
                    }
                }
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor)
                ) {
                    Text("Cancelar")
                }
            }
        }
    }
}