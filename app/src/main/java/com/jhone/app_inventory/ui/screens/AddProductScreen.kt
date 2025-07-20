package com.jhone.app_inventory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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

    // Estados observados del ViewModel
    val isLoading by viewModel.isLoading.collectAsState()

    // Calcular isAdmin una sola vez
    val isAdmin = remember(userRole) { userRole == "admin" }

    // Estados de precio con valores por defecto optimizados
    var precioBoletaText by rememberSaveable {
        mutableStateOf(if (isAdmin) "" else "0")
    }
    var porcentajeText by rememberSaveable {
        mutableStateOf(if (isAdmin) "" else "0")
    }

    // Cálculos derivados
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
            // Header con icono y título
            ModernHeader()

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
                            text = "Nuevo Producto",
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
                            ModernErrorCard(error = error)
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
                                    onValueChange = { proveedor = it },
                                    label = "Proveedor",
                                    icon = Icons.Default.Person,
                                    enabled = !isLoading
                                )

                                ModernTextField(
                                    value = codigo,
                                    onValueChange = { codigo = it },
                                    label = "Código del Producto",
                                    icon = Icons.Default.Create,
                                    enabled = !isLoading
                                )

                                ModernTextField(
                                    value = descripcion,
                                    onValueChange = { descripcion = it },
                                    label = "Descripción",
                                    icon = Icons.Default.Info,
                                    enabled = !isLoading,
                                    isRequired = true
                                )

                                ModernTextField(
                                    value = cantidadText,
                                    onValueChange = { cantidadText = it },
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
                                        onValueChange = { precioBoletaText = it },
                                        label = "Precio Boleta",
                                        icon = Icons.Default.AttachMoney,
                                        enabled = !isLoading,
                                        keyboardType = KeyboardType.Number,
                                        placeholder = "0.00"
                                    )

                                    ModernTextField(
                                        value = porcentajeText,
                                        onValueChange = { porcentajeText = it },
                                        label = "Porcentaje (%)",
                                        icon = Icons.Default.Build,
                                        enabled = !isLoading,
                                        keyboardType = KeyboardType.Number,
                                        placeholder = "0"
                                    )

                                    // Campos calculados automáticamente
                                    ModernDisplayField(
                                        label = "Precio Costo",
                                        value = "S/ ${String.format("%.2f", precioCosto)}",
                                        icon = Icons.Default.Calculate
                                    )

                                    ModernDisplayField(
                                        label = "Precio Público",
                                        value = "S/ ${String.format("%.2f", precioProducto)}",
                                        icon = Icons.Default.AttachMoney
                                    )
                                }

                                // Campo de fecha
                                ModernDatePicker(
                                    value = fechaVencimientoText,
                                    onValueChange = {
                                        fechaVencimientoText = it
                                        if (errorMessage?.contains("fecha") == true) {
                                            errorMessage = null
                                        }
                                    },
                                    enabled = !isLoading
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Botones de acción
                        ModernActionButtons(
                            isLoading = isLoading,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
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
    }
}

@Composable
private fun ModernHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Agregar Producto",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.White
            )
        )

        Text(
            text = "Complete la información del producto",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp
            )
        )
    }
}

@Composable
private fun ModernErrorCard(error: String) {
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

@Composable
private fun ModernSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
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
            // Header de la sección
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Color(0xFF9C84C9).copy(alpha = 0.1f),
                            RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color(0xFF7851A9),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E2E2E)
                    )
                )
            }

            // Divider sutil
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF9C84C9).copy(alpha = 0.3f),
                                Color.Transparent,
                                Color(0xFF9C84C9).copy(alpha = 0.3f)
                            )
                        )
                    )
            )

            // Contenido de la sección
            content()
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
private fun ModernDisplayField(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernDatePicker(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean
) {
    DatePickerField(
        value = value,
        onValueChange = onValueChange,
        label = "Fecha de Vencimiento",
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
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

@Composable
private fun ModernActionButtons(
    isLoading: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Botón principal de guardar
        Button(
            onClick = onSave,
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
                        text = "Guardando...",
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
                        imageVector = Icons.Default.Done,
                        contentDescription = "Guardar",
                        tint = Color.White
                    )
                    Text(
                        text = "Guardar Producto",
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