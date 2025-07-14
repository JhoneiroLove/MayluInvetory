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
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.Timestamp

@Composable
fun EditProductScreen(
    userRole: String, // NUEVO
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
    // Ahora, el precio público se calcula automáticamente, por lo que no lo editamos directamente.
    var precioBoletaText by remember { mutableStateOf(if (isAdmin) product.precioBoleta.toString() else "") }
    var porcentajeText by remember { mutableStateOf(if (isAdmin) product.porcentaje.toString() else "") }

    val originalBoleta = product.precioBoleta
    val originalPorcentaje = product.porcentaje

    val fechaVenc = product.fechaVencimiento?.toDate()?.let {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
    } ?: ""
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
                OutlinedTextField(
                    value = proveedor,
                    onValueChange = { proveedor = it },
                    label = { Text("Proveedor") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = Color.Black),
                    colors = fieldColors
                )
                OutlinedTextField(
                    value = codigo,
                    onValueChange = { codigo = it },
                    label = { Text("Código del Producto") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = Color.Black),
                    colors = fieldColors
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = Color.Black),
                    colors = fieldColors
                )
                OutlinedTextField(
                    value = cantidadText,
                    onValueChange = { cantidadText = it },
                    label = { Text("Cantidad") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = Color.Black),
                    colors = fieldColors
                )
                if (isAdmin) {
                    OutlinedTextField(
                        value = precioBoletaText,
                        onValueChange = { precioBoletaText = it },
                        label = { Text("Precio Boleta") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors
                    )
                    OutlinedTextField(
                        value = porcentajeText,
                        onValueChange = { porcentajeText = it },
                        label = { Text("Porcentaje (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors
                    )
                }
                // NUEVO: Ocultamos Precio Costo y Precio Público para asesores
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
                    colors = fieldColors
                )
                Button(
                    onClick = {
                        val cantidad = cantidadText.toIntOrNull() ?: 0
                        val parsedFechaVenc = parseDate(fechaVencimientoText)
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
                        viewModel.updateProduct(updatedProduct) {
                            onProductUpdated()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Actualizar", color = Color.White)
                }
                OutlinedButton(
                    onClick = onCancel,
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