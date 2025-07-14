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
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    navController: NavController,
    onLogout: () -> Unit,
    onAddClick: () -> Unit,
    userRole: String, // NUEVO: Recibe el rol del usuario ("admin" o "asesor")
    viewModel: ProductViewModel = hiltViewModel()
) {
    // Observamos la lista completa de productos desde el ViewModel
    val products by viewModel.products.collectAsState()

    // Estado para el buscador
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val filteredProducts = remember(products, searchQuery) {
        val filteredList = if (searchQuery.isEmpty()) {
            products
        } else {
            products.filter { product ->
                product.codigo.contains(searchQuery, ignoreCase = true) ||
                        product.descripcion.contains(searchQuery, ignoreCase = true)
            }
        }
        // Ordenamos la lista por proveedor (de A-Z)
        filteredList.sortedBy { it.proveedor }
    }

    Scaffold(
        topBar = {
            InventoryNavbar(
                onAddClick = onAddClick,
                onSyncClick = { viewModel.loadProductsFromFirestore() },
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
                // Buscador: OutlinedTextField para ingresar el término de búsqueda
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Buscar por código o descripción") },
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color.Black,
                        unfocusedLabelColor = Color.DarkGray,
                        cursorColor = Color.Black
                    ),
                    placeholder = { Text("Ingrese búsqueda...", color = Color.DarkGray) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Lista de productos filtrados
                LazyColumn(modifier = Modifier.weight(if (filteredProducts.isEmpty()) 1f else 0.9f)) {
                    items(
                        items = filteredProducts,
                        key = { product -> product.id }
                    ) { product ->
                        ProductItem(
                            product = product,
                            userRole = userRole,
                            onEditClick = { selectedProduct ->
                                navController.navigate("editProductScreen/${selectedProduct.id}")
                            },
                            onDeleteClick = { selectedProduct ->
                                viewModel.deleteProduct(selectedProduct) { }
                            },
                            onManageStockClick = { selectedProduct ->
                                navController.navigate("stockMovementScreen/${selectedProduct.id}")
                            },
                            onHistoryClick = { selectedProduct ->
                                navController.navigate("stockHistoryScreen/${selectedProduct.id}")
                            }
                        )
                    }
                    // Botón para cargar más productos
                    item {
                        Button(
                            onClick = { viewModel.loadNextPage() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7851A9))
                        ) {
                            Text("Cargar Más", color = Color.White)
                        }
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
    userRole: String, // NUEVO: Recibe el rol para condicionar botones
    onEditClick: (Product) -> Unit,
    onDeleteClick: (Product) -> Unit,
    onManageStockClick: (Product) -> Unit,
    onHistoryClick: (Product) -> Unit
) {
    val showDialog = remember { mutableStateOf(false) }

    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = { showDialog.value = false },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Estás seguro de eliminar el producto \"${product.descripcion}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteClick(product)
                    showDialog.value = false
                }) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog.value = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // NUEVO: Solo admin puede borrar y ver historial
    val canDelete = (userRole == "admin")
    val canSeeHistory = (userRole == "admin")

    val gradientColors = listOf(Color(0xFF9C84C9), Color(0xFFB89EDC))
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
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
                        IconButton(onClick = { onEditClick(product) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar producto",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { onManageStockClick(product) }) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Gestionar Stock",
                                tint = Color.White
                            )
                        }
                        if (canSeeHistory) { // NUEVO: Solo admin puede ver historial
                            IconButton(onClick = { onHistoryClick(product) }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Ver Historial",
                                    tint = Color.White
                                )
                            }
                        }
                        if (canDelete) { // NUEVO: Solo admin puede borrar
                            IconButton(onClick = { showDialog.value = true }) {
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
                if (userRole == "admin") { // NUEVO: Solo admin ve estos precios
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
                    val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val vencStr = format.format(ts.toDate())
                    Text(
                        text = "Vencimiento: $vencStr",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black
                    )
                }
                product.createdAt?.let { ts ->
                    val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    val createdStr = format.format(ts.toDate())
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