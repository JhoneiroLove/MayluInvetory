package com.jhone.app_inventory.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.jhone.app_inventory.R
import com.jhone.app_inventory.data.Product
import com.jhone.app_inventory.ui.viewmodel.ProductViewModel
import com.jhone.app_inventory.utils.DateUtils
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    navController: NavController,
    onLogout: () -> Unit,
    onAddClick: () -> Unit,
    userRole: String,
    viewModel: ProductViewModel = hiltViewModel()
) {
    // Estados del ViewModel
    val products by viewModel.products.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Estado para el buscador
    var searchQuery by rememberSaveable { mutableStateOf("") }

    // Estado para controlar la eliminación
    var isDeleting by remember { mutableStateOf(false) }

    // Filtrado de productos - Simplificado y más estable
    val filteredProducts = remember(products, searchQuery) {
        if (searchQuery.isEmpty()) {
            products.sortedBy { it.proveedor }
        } else {
            products.filter { product ->
                product.codigo.contains(searchQuery, ignoreCase = true) ||
                        product.descripcion.contains(searchQuery, ignoreCase = true) ||
                        product.proveedor.contains(searchQuery, ignoreCase = true)
            }.sortedBy { it.proveedor }
        }
    }

    // Mostrar errores
    LaunchedEffect(error) {
        if (error != null) {
            // Aquí podrías mostrar un Snackbar o Toast
            println("Error: $error")
            delay(5000)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            InventoryNavbar(
                onAddClick = onAddClick,
                onSyncClick = { viewModel.syncData() },
                onLogoutClick = onLogout
            )
        },
        bottomBar = { InventoryFooterWithImage() }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF7F7F7))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Buscador
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { newValue ->
                        searchQuery = newValue
                    },
                    label = { Text("Buscar por código, descripción o proveedor") },
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isDeleting, // Deshabilitar durante eliminación
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color.Black,
                        unfocusedLabelColor = Color.DarkGray,
                        cursorColor = Color.Black,
                        disabledBorderColor = Color.LightGray,
                        disabledLabelColor = Color.LightGray
                    ),
                    placeholder = { Text("Ingrese búsqueda...", color = Color.DarkGray) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Mostrar indicador de carga
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFF9C84C9)
                            )
                            Text("Cargando...", color = Color.Gray)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Mostrar error si existe
                error?.let { errorMsg ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFEBEE)
                        )
                    ) {
                        Text(
                            text = "Error: $errorMsg",
                            color = Color.Red,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Lista de productos
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = filteredProducts,
                        key = { product -> product.id }
                    ) { product ->
                        ProductItem(
                            product = product,
                            userRole = userRole,
                            isDeleting = isDeleting,
                            onEditClick = { selectedProduct ->
                                if (!isDeleting) {
                                    navController.navigate("editProductScreen/${selectedProduct.id}")
                                }
                            },
                            onDeleteClick = { selectedProduct ->
                                if (!isDeleting) {
                                    isDeleting = true
                                    viewModel.deleteProduct(selectedProduct) { success, error ->
                                        isDeleting = false
                                        if (!success) {
                                            println("Error al eliminar: $error")
                                        }
                                    }
                                }
                            },
                            onManageStockClick = { selectedProduct ->
                                if (!isDeleting) {
                                    navController.navigate("stockMovementScreen/${selectedProduct.id}")
                                }
                            },
                            onHistoryClick = { selectedProduct ->
                                if (!isDeleting) {
                                    navController.navigate("stockHistoryScreen/${selectedProduct.id}")
                                }
                            }
                        )
                    }

                    // Botón para cargar más productos
                    if (filteredProducts.isNotEmpty()) {
                        item {
                            Button(
                                onClick = {
                                    if (!isLoading) {
                                        viewModel.loadNextPage()
                                    }
                                },
                                enabled = !isLoading && !isDeleting,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7851A9))
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White
                                    )
                                } else {
                                    Text("Cargar Más", color = Color.White)
                                }
                            }
                        }
                    }
                }

                // Mensaje cuando no hay productos
                if (filteredProducts.isEmpty() && !isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isEmpty()) {
                                "No hay productos registrados"
                            } else {
                                "No se encontraron productos que coincidan con '$searchQuery'"
                            },
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InventoryFooterWithImage() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.flower_background),
            contentDescription = "Decoración floral",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun ProductItem(
    product: Product,
    userRole: String,
    isDeleting: Boolean,
    onEditClick: (Product) -> Unit,
    onDeleteClick: (Product) -> Unit,
    onManageStockClick: (Product) -> Unit,
    onHistoryClick: (Product) -> Unit
) {
    val showDialog = remember { mutableStateOf(false) }

    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = {
                if (!isDeleting) {
                    showDialog.value = false
                }
            },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Estás seguro de eliminar el producto \"${product.descripcion}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClick(product)
                        showDialog.value = false
                    },
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text("Eliminar")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!isDeleting) {
                            showDialog.value = false
                        }
                    },
                    enabled = !isDeleting
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    val canDelete = (userRole == "admin")
    val canSeeHistory = (userRole == "admin")

    val gradientColors = listOf(Color(0xFF9C84C9), Color(0xFFB89EDC))

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush = Brush.horizontalGradient(gradientColors))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Código: ${product.codigo}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                    Row {
                        IconButton(
                            onClick = { onEditClick(product) },
                            enabled = !isDeleting
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar producto",
                                tint = if (isDeleting) Color.Gray else Color.White
                            )
                        }
                        IconButton(
                            onClick = { onManageStockClick(product) },
                            enabled = !isDeleting
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Gestionar Stock",
                                tint = if (isDeleting) Color.Gray else Color.White
                            )
                        }
                        if (canSeeHistory) {
                            IconButton(
                                onClick = { onHistoryClick(product) },
                                enabled = !isDeleting
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Ver Historial",
                                    tint = if (isDeleting) Color.Gray else Color.White
                                )
                            }
                        }
                        if (canDelete) {
                            IconButton(
                                onClick = { showDialog.value = true },
                                enabled = !isDeleting
                            ) {
                                if (isDeleting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Borrar producto",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Descripción: ${product.descripcion}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Cantidad: ${product.cantidad}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Precio Público: S/ ${String.format("%.2f", product.precioProducto)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
                if (userRole == "admin") {
                    Text(
                        text = "Precio Boleta: S/ ${product.precioBoleta}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black
                    )
                    Text(
                        text = "Precio Costo: S/ ${String.format("%.2f", product.precioCosto)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black
                    )
                    Text(
                        text = "Porcentaje: ${product.porcentaje}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black
                    )
                }
                // Mostrar la fecha de vencimiento si está disponible
                product.fechaVencimiento?.let { ts ->
                    val vencStr = DateUtils.formatDate(ts)
                    Text(
                        text = "Vencimiento: $vencStr",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black
                    )
                }
                product.createdAt?.let { ts ->
                    val createdStr = DateUtils.formatDateTime(ts)
                    Text(
                        text = "Fecha de Creación: $createdStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Proveedor: ${product.proveedor}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}