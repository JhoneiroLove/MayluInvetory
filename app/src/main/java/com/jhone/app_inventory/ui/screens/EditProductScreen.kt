package com.jhone.app_inventory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jhone.app_inventory.data.Product
import com.jhone.app_inventory.ui.viewmodel.ProductViewModel
import com.jhone.app_inventory.ui.components.DatePickerField
import com.jhone.app_inventory.ui.components.DateValidator
import com.jhone.app_inventory.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
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

    // Estados de precio
    var precioBoletaText by remember { mutableStateOf(if (isAdmin) product.precioBoleta.toString() else "") }
    var porcentajeText by remember { mutableStateOf(if (isAdmin) product.porcentaje.toString() else "") }

    val originalBoleta = product.precioBoleta
    val originalPorcentaje = product.porcentaje

    // Inicializar con la fecha formateada del producto
    val fechaVenc = DateUtils.formatDate(product.fechaVencimiento)
    var fechaVencimientoText by remember { mutableStateOf(fechaVenc) }

    // Cálculos de precios
    val precioBoleta = if (isAdmin) precioBoletaText.toDoubleOrNull() ?: 0.0 else originalBoleta
    val porcentajeValue = if (isAdmin) porcentajeText.toDoubleOrNull() ?: 0.0 else originalPorcentaje
    val precioCosto = precioBoleta * 1.02
    val precioProducto = precioCosto * (1 + (porcentajeValue / 100))

    val scrollState = rememberScrollState()

    // Colores del sistema
    val primaryColor = Color(0xFF9C84C9)
    val secondaryColor = Color(0xFF7851A9)
    val accentColor = Color(0xFFB89EDC)
    val surfaceColor = Color(0xFFF8F6FF)

    // Gradientes
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(primaryColor, accentColor),
        startY = 0f,
        endY = Float.POSITIVE_INFINITY
    )

    val cardGradient = Brush.verticalGradient(
        colors = listOf(Color.White, surfaceColor),
        startY = 0f,
        endY = 1000f
    )

    // Función de validación
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
            .background(backgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header simplificado
            EditProductHeader(productName = product.descripcion)

            Spacer(modifier = Modifier.height(24.dp))

            // Card principal con contenido
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = primaryColor.copy(alpha = 0.3f)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(cardGradient)
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Título del formulario
                        Text(
                            text = "Editar Producto",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = secondaryColor
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Mensaje de error estilizado
                        errorMessage?.let { error ->
                            ErrorCard(error = error)
                        }

                        // Todos los campos en un solo card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.9f)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Campos básicos
                                ModernTextField(
                                    value = proveedor,
                                    onValueChange = {
                                        proveedor = it
                                        errorMessage = null
                                    },
                                    label = "Proveedor",
                                    icon = Icons.Default.Person,
                                    enabled = !isLoading
                                )

                                ModernTextField(
                                    value = codigo,
                                    onValueChange = {
                                        codigo = it
                                        errorMessage = null
                                    },
                                    label = "Código del Producto",
                                    icon = Icons.Default.Create,
                                    enabled = !isLoading
                                )

                                ModernTextField(
                                    value = descripcion,
                                    onValueChange = {
                                        descripcion = it
                                        errorMessage = null
                                    },
                                    label = "Descripción",
                                    icon = Icons.Default.Info,
                                    enabled = !isLoading,
                                    isRequired = true
                                )

                                ModernTextField(
                                    value = cantidadText,
                                    onValueChange = {
                                        cantidadText = it
                                        errorMessage = null
                                    },
                                    label = "Cantidad",
                                    icon = Icons.Default.ShoppingCart,
                                    enabled = !isLoading,
                                    keyboardType = KeyboardType.Number,
                                    isRequired = true
                                )

                                // Campos de precios (solo para admin)
                                if (isAdmin) {
                                    ModernTextField(
                                        value = precioBoletaText,
                                        onValueChange = {
                                            precioBoletaText = it
                                            errorMessage = null
                                        },
                                        label = "Precio Boleta",
                                        icon = Icons.Default.AttachMoney,
                                        enabled = !isLoading,
                                        keyboardType = KeyboardType.Number,
                                        placeholder = "0.00"
                                    )

                                    ModernTextField(
                                        value = porcentajeText,
                                        onValueChange = {
                                            porcentajeText = it
                                            errorMessage = null
                                        },
                                        label = "Porcentaje (%)",
                                        icon = Icons.Default.Build,
                                        enabled = !isLoading,
                                        keyboardType = KeyboardType.Number,
                                        placeholder = "0"
                                    )

                                    // Campos calculados automáticamente
                                    DisplayField(
                                        label = "Precio Costo",
                                        value = "S/ ${String.format("%.2f", precioCosto)}",
                                        icon = Icons.Default.Calculate
                                    )

                                    DisplayField(
                                        label = "Precio Público",
                                        value = "S/ ${String.format("%.2f", precioProducto)}",
                                        icon = Icons.Default.AttachMoney
                                    )
                                }

                                // Campo de fecha
                                DatePickerField(
                                    value = fechaVencimientoText,
                                    onValueChange = {
                                        fechaVencimientoText = it
                                        if (errorMessage?.contains("fecha") == true) {
                                            errorMessage = null
                                        }
                                    },
                                    label = "Fecha de Vencimiento",
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isLoading,
                                    isRequired = false,
                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                        focusedBorderColor = Color(0xFF7851A9),
                                        unfocusedBorderColor = Color(0xFFE0E0E0),
                                        focusedLabelColor = Color(0xFF7851A9),
                                        unfocusedLabelColor = Color(0xFF2E2E2E),
                                        cursorColor = Color(0xFF7851A9),
                                        focusedTextColor = Color(0xFF2E2E2E),
                                        unfocusedTextColor = Color(0xFF2E2E2E),
                                        containerColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Botones de acción
                        ActionButtons(
                            isLoading = isLoading,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
                            onUpdate = {
                                if (validateAndSubmit()) {
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
                                }
                            },
                            onCancel = onCancel
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditProductHeader(productName: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Editar Producto",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.White
            )
        )

        Text(
            text = productName.take(30) + if (productName.length > 30) "..." else "",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorCard(error: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Error",
                tint = Color(0xFFD32F2F),
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = error,
                color = Color(0xFFD32F2F),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String = "",
    isRequired: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = if (isRequired) "$label *" else label,
                color = if (isRequired) Color(0xFFD32F2F) else Color(0xFF2E2E2E)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) Color(0xFF7851A9) else Color(0xFFBDBDBD)
            )
        },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        placeholder = {
            if (placeholder.isNotEmpty()) {
                Text(placeholder, color = Color(0xFF9E9E9E))
            }
        },
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = Color(0xFF7851A9),
            unfocusedBorderColor = Color(0xFFE0E0E0),
            focusedLabelColor = Color(0xFF7851A9),
            unfocusedLabelColor = Color(0xFF2E2E2E),
            cursorColor = Color(0xFF7851A9),
            focusedTextColor = Color(0xFF2E2E2E),
            unfocusedTextColor = Color(0xFF2E2E2E),
            containerColor = Color.White,
            disabledBorderColor = Color(0xFFE0E0E0),
            disabledLabelColor = Color(0xFFBDBDBD),
            disabledTextColor = Color(0xFFBDBDBD)
        ),
        shape = RoundedCornerShape(16.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DisplayField(
    label: String,
    value: String,
    icon: ImageVector
) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        label = { Text(label, color = Color(0xFF2E2E2E)) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF4CAF50)
            )
        },
        modifier = Modifier.fillMaxWidth(),
        readOnly = true,
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = Color(0xFF4CAF50),
            unfocusedBorderColor = Color(0xFF4CAF50),
            focusedLabelColor = Color(0xFF4CAF50),
            unfocusedLabelColor = Color(0xFF4CAF50),
            focusedTextColor = Color(0xFF2E2E2E),
            unfocusedTextColor = Color(0xFF2E2E2E),
            containerColor = Color(0xFFE8F5E8)
        ),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun ActionButtons(
    isLoading: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    onUpdate: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Botón principal de actualizar
        Button(
            onClick = onUpdate,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = secondaryColor,
                disabledContainerColor = Color(0xFFE0E0E0)
            ),
            shape = RoundedCornerShape(28.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 6.dp,
                pressedElevation = 8.dp
            )
        ) {
            if (isLoading) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Actualizando...",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Update,
                        contentDescription = "Actualizar",
                        tint = Color.White
                    )
                    Text(
                        text = "Actualizar Producto",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                    )
                }
            }
        }

        // Botón secundario de cancelar
        OutlinedButton(
            onClick = onCancel,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = secondaryColor,
                disabledContentColor = Color(0xFFBDBDBD)
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                width = 2.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(primaryColor, secondaryColor)
                )
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancelar",
                    tint = secondaryColor
                )
                Text(
                    text = "Cancelar",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                )
            }
        }
    }
}