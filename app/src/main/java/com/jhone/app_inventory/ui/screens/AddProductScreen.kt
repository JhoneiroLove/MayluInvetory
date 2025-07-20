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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.fillMaxWidth
import com.jhone.app_inventory.data.Product
import com.jhone.app_inventory.ui.viewmodel.ProductViewModel
import com.jhone.app_inventory.ui.components.DatePickerField
import com.jhone.app_inventory.ui.components.DateValidator
import com.jhone.app_inventory.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    userRole: String,
    onCancel: () -> Unit,
    onProductAdded: () -> Unit,
    viewModel: ProductViewModel = hiltViewModel()
) {
    // Estados con rememberSaveable para persistir en recomposiciones
    var proveedor by rememberSaveable { mutableStateOf("") }
    var codigo by rememberSaveable { mutableStateOf("") }
    var descripcion by rememberSaveable { mutableStateOf("") }
    var cantidadText by rememberSaveable { mutableStateOf("") }
    var fechaVencimientoText by rememberSaveable { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Estados observados del ViewModel (solo cuando sea necesario)
    val isLoading by viewModel.isLoading.collectAsState()

    // Calcular isAdmin una sola vez
    val isAdmin = remember(userRole) { userRole == "admin" }

    // OPTIMIZACIÓN 4: Estados de precio con valores por defecto optimizados
    var precioBoletaText by rememberSaveable {
        mutableStateOf(if (isAdmin) "" else "0")
    }
    var porcentajeText by rememberSaveable {
        mutableStateOf(if (isAdmin) "" else "0")
    }

    // solo recalcular cuando cambien los inputs
    val precioBoleta by remember {
        derivedStateOf {
            if (isAdmin) precioBoletaText.toDoubleOrNull() ?: 0.0 else 0.0
        }
    }
    val porcentajeValue by remember {
        derivedStateOf {
            if (isAdmin) porcentajeText.toDoubleOrNull() ?: 0.0 else 0.0
        }
    }
    val precioCosto by remember {
        derivedStateOf { precioBoleta * 1.02 }
    }
    val precioProducto by remember {
        derivedStateOf { precioCosto * (1 + (porcentajeValue / 100)) }
    }

    // ScrollState con remember simple
    val scrollState = rememberScrollState()

    // Colores definidos una sola vez
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

    // Función de validación dentro del Composable
    fun validateAndSubmit(): Boolean {
        return when {
            descripcion.isBlank() -> {
                errorMessage = "La descripción es obligatoria"
                false
            }
            cantidadText.isBlank() || cantidadText.toIntOrNull() == null -> {
                errorMessage = "La cantidad debe ser un número válido"
                false
            }
            isAdmin && precioBoletaText.isNotBlank() && precioBoletaText.toDoubleOrNull() == null -> {
                errorMessage = "El precio boleta debe ser un número válido"
                false
            }
            isAdmin && porcentajeText.isNotBlank() && porcentajeText.toDoubleOrNull() == null -> {
                errorMessage = "El porcentaje debe ser un número válido"
                false
            }
            fechaVencimientoText.isNotBlank() && !DateValidator.isValidDateFormat(fechaVencimientoText) -> {
                errorMessage = DateValidator.getDateValidationMessage(fechaVencimientoText)
                false
            }
            else -> {
                errorMessage = null
                true
            }
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

                // Error message como composable separado para evitar recomposición innecesaria
                errorMessage?.let { error ->
                    ErrorMessage(error = error)
                }

                // Campos de texto optimizados
                ProductTextField(
                    value = proveedor,
                    onValueChange = { proveedor = it },
                    label = "Proveedor",
                    fieldColors = fieldColors,
                    enabled = !isLoading
                )

                ProductTextField(
                    value = codigo,
                    onValueChange = { codigo = it },
                    label = "Código del Producto",
                    fieldColors = fieldColors,
                    enabled = !isLoading
                )

                ProductTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = "Descripción",
                    fieldColors = fieldColors,
                    enabled = !isLoading
                )

                ProductTextField(
                    value = cantidadText,
                    onValueChange = { cantidadText = it },
                    label = "Cantidad",
                    fieldColors = fieldColors,
                    enabled = !isLoading,
                    keyboardType = KeyboardType.Number
                )

                // Campos de admin solo si es necesario
                if (isAdmin) {
                    AdminPriceFields(
                        precioBoletaText = precioBoletaText,
                        onPrecioBoletaChange = { precioBoletaText = it },
                        porcentajeText = porcentajeText,
                        onPorcentajeChange = { porcentajeText = it },
                        precioCosto = precioCosto,
                        precioProducto = precioProducto,
                        fieldColors = fieldColors,
                        enabled = !isLoading
                    )
                }

                // DatePicker como composable separado
                OptimizedDatePicker(
                    value = fechaVencimientoText,
                    onValueChange = {
                        fechaVencimientoText = it
                        // Limpiar error si había uno relacionado con fecha
                        if (errorMessage?.contains("fecha") == true) {
                            errorMessage = null
                        }
                    },
                    fieldColors = fieldColors,
                    enabled = !isLoading
                )

                // Botones optimizados
                ActionButtons(
                    isLoading = isLoading,
                    buttonColor = buttonColor,
                    primaryColor = primaryColor,
                    onSave = {
                        if (validateAndSubmit()) {
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
                        }
                    },
                    onCancel = onCancel
                )
            }
        }
    }
}

// Composables auxiliares para reducir recomposiciones
@Composable
private fun ErrorMessage(error: String) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    fieldColors: androidx.compose.material3.TextFieldColors,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = MaterialTheme.typography.bodySmall.fontSize) },
        modifier = Modifier.fillMaxWidth(),
        colors = fieldColors,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminPriceFields(
    precioBoletaText: String,
    onPrecioBoletaChange: (String) -> Unit,
    porcentajeText: String,
    onPorcentajeChange: (String) -> Unit,
    precioCosto: Double,
    precioProducto: Double,
    fieldColors: androidx.compose.material3.TextFieldColors,
    enabled: Boolean
) {
    OutlinedTextField(
        value = precioBoletaText,
        onValueChange = onPrecioBoletaChange,
        label = { Text("Precio Boleta") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
        colors = fieldColors,
        enabled = enabled,
        placeholder = { Text("0", color = Color.Gray) },
        singleLine = true
    )

    OutlinedTextField(
        value = porcentajeText,
        onValueChange = onPorcentajeChange,
        label = { Text("Porcentaje (%)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
        colors = fieldColors,
        enabled = enabled,
        placeholder = { Text("0", color = Color.Gray) },
        singleLine = true
    )

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptimizedDatePicker(
    value: String,
    onValueChange: (String) -> Unit,
    fieldColors: androidx.compose.material3.TextFieldColors,
    enabled: Boolean
) {
    DatePickerField(
        value = value,
        onValueChange = onValueChange,
        label = "Fecha de Vencimiento",
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        isRequired = false,
        colors = fieldColors
    )
}

@Composable
private fun ActionButtons(
    isLoading: Boolean,
    buttonColor: Color,
    primaryColor: Color,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Button(
        onClick = onSave,
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