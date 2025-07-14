package com.jhone.app_inventory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.fillMaxWidth
import com.google.firebase.Timestamp
import com.jhone.app_inventory.data.Product
import com.jhone.app_inventory.ui.viewmodel.ProductViewModel
import com.jhone.app_inventory.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    userRole: String,
    onCancel: () -> Unit,
    onProductAdded: () -> Unit,
    viewModel: ProductViewModel = hiltViewModel()
) {
    var proveedor by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var cantidadText by remember { mutableStateOf("") }
    var fechaVencimientoText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Estados observados del ViewModel
    val isLoading by viewModel.isLoading.collectAsState()

    // El asesor no puede ingresar precio boleta ni porcentaje
    val isAdmin = (userRole == "admin")

    // Usuario ingresa el precio boleta manualmente
    var precioBoletaText by remember { mutableStateOf(if (isAdmin) "" else "0") }
    // Usuario ingresa el porcentaje manualmente
    var porcentajeText by remember { mutableStateOf(if (isAdmin) "" else "0") }

    val precioBoleta = if (isAdmin) precioBoletaText.toDoubleOrNull() ?: 0.0 else 0.0
    val porcentajeValue = if (isAdmin) porcentajeText.toDoubleOrNull() ?: 0.0 else 0.0
    val precioCosto = precioBoleta * 1.02
    val precioProducto = precioCosto * (1 + (porcentajeValue / 100))

    val scrollState = rememberScrollState()

    // Colores y estilos
    val primaryColor = Color(0xFF9C84C9)
    val buttonColor = Color(0xFF7851A9)
    val fieldColors = TextFieldDefaults.outlinedTextFieldColors(
        focusedBorderColor = Color.Black,
        unfocusedBorderColor = Color.Black,
        focusedLabelColor = Color.Black,
        unfocusedLabelColor = Color.Black,
        cursorColor = Color.Black,
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(primaryColor)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 0.dp, max = 700.dp)
                .padding(horizontal = 16.dp)
                .align(Alignment.Center),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Nuevo Producto",
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
                    onValueChange = { proveedor = it },
                    label = { Text("Proveedor", fontSize = MaterialTheme.typography.bodySmall.fontSize) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    enabled = !isLoading
                )
                OutlinedTextField(
                    value = codigo,
                    onValueChange = { codigo = it },
                    label = { Text("Código del Producto", fontSize = MaterialTheme.typography.bodySmall.fontSize) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    enabled = !isLoading
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    enabled = !isLoading
                )
                OutlinedTextField(
                    value = cantidadText,
                    onValueChange = { cantidadText = it },
                    label = { Text("Cantidad") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    enabled = !isLoading
                )
                // Solo admin puede editar precio boleta y porcentaje
                if (isAdmin) {
                    OutlinedTextField(
                        value = precioBoletaText,
                        onValueChange = { precioBoletaText = it },
                        label = { Text("Precio Boleta") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors,
                        enabled = !isLoading
                    )
                    OutlinedTextField(
                        value = porcentajeText,
                        onValueChange = { porcentajeText = it },
                        label = { Text("Porcentaje (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors,
                        enabled = !isLoading
                    )
                }
                // Ocultamos campos de Precio Costo y Precio Público para asesores
                if (isAdmin) {
                    OutlinedTextField(
                        value = String.format("%.2f", precioCosto),
                        onValueChange = {},
                        label = { Text("Precio Costo") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        colors = fieldColors
                    )
                    OutlinedTextField(
                        value = String.format("%.2f", precioProducto),
                        onValueChange = {},
                        label = { Text("Precio Público") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        colors = fieldColors
                    )
                }
                // Fecha de Vencimiento
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

                        val product = Product(
                            codigo = codigo,
                            descripcion = descripcion,
                            cantidad = cantidad,
                            precioBoleta = precioBoleta,
                            precioCosto = precioCosto,
                            precioProducto = precioProducto,
                            proveedor = proveedor,
                            fechaVencimiento = parsedFechaVenc,
                            porcentaje = porcentajeValue
                        )

                        viewModel.addProduct(product) { success, error ->
                            if (success) {
                                onProductAdded()
                            } else {
                                errorMessage = error ?: "Error desconocido al agregar producto"
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
                            Text(text = "Guardando...", color = Color.White)
                        }
                    } else {
                        Text(text = "Guardar", color = Color.White)
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