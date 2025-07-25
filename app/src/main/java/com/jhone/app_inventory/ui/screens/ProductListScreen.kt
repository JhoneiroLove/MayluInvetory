package com.jhone.app_inventory.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.jhone.app_inventory.R
import com.jhone.app_inventory.data.Product
import com.jhone.app_inventory.ui.viewmodel.ProductViewModel
import com.jhone.app_inventory.utils.DateUtils
import com.jhone.app_inventory.utils.UserUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    navController: NavController,
    onLogout: () -> Unit,
    onAddClick: () -> Unit,
    userRole: String,
    currentUserEmail: String? = null, // Email del usuario actual
    viewModel: ProductViewModel = hiltViewModel()
) {
    // Estados del ViewModel
    val products by viewModel.products.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val error by viewModel.error.collectAsState()

    // Estado para el buscador
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var serverSearchResults by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isSearchingServer by remember { mutableStateOf(false) }

    // Estado para controlar la eliminación
    var isDeleting by remember { mutableStateOf(false) }

    // Estado del scroll para infinite scroll
    val listState = rememberLazyListState()

    // Pull to refresh state
    var isRefreshing by remember { mutableStateOf(false) }

    // Filtrado de productos - Híbrido (local + servidor)
    val filteredProducts = remember(products, searchQuery, serverSearchResults) {
        if (searchQuery.isEmpty()) {
            products.sortedBy { it.proveedor }
        } else {
            // Primero buscar en productos locales
            val localResults = products.filter { product ->
                product.codigo.contains(searchQuery, ignoreCase = true) ||
                        product.descripcion.contains(searchQuery, ignoreCase = true) ||
                        product.proveedor.contains(searchQuery, ignoreCase = true)
            }

            // Si hay resultados del servidor, combinar sin duplicar
            if (serverSearchResults.isNotEmpty()) {
                val combinedResults = mutableListOf<Product>()
                combinedResults.addAll(localResults)

                // Agregar resultados del servidor que no estén en locales
                serverSearchResults.forEach { serverProduct ->
                    if (combinedResults.none { it.id == serverProduct.id }) {
                        combinedResults.add(serverProduct)
                    }
                }
                combinedResults.sortedBy { it.codigo }
            } else {
                localResults.sortedBy { it.codigo }
            }
        }
    }

    // Búsqueda en servidor cuando no hay resultados locales
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            // Primero verificar si hay resultados locales
            val localResults = products.filter { product ->
                product.codigo.contains(searchQuery, ignoreCase = true) ||
                        product.descripcion.contains(searchQuery, ignoreCase = true) ||
                        product.proveedor.contains(searchQuery, ignoreCase = true)
            }

            // Si no hay resultados locales, buscar en servidor
            if (localResults.isEmpty()) {
                isSearchingServer = true
                delay(300) // Debounce
                viewModel.searchProductsInServer(searchQuery) { results ->
                    serverSearchResults = results
                    isSearchingServer = false
                }
            } else {
                serverSearchResults = emptyList() // Limpiar resultados del servidor
                isSearchingServer = false
            }
        } else {
            serverSearchResults = emptyList()
            isSearchingServer = false
        }
    }

    // Scroll infinito - Cargar más cuando llegamos al final
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                val lastVisibleItem = visibleItems.lastOrNull()
                val totalItems = listState.layoutInfo.totalItemsCount

                // Si estamos en los últimos 3 elementos y no hay búsqueda activa
                if (lastVisibleItem != null &&
                    lastVisibleItem.index >= totalItems - 3 &&
                    !isLoadingMore &&
                    !isLoading &&
                    searchQuery.isEmpty() &&
                    products.isNotEmpty()) {
                    viewModel.loadNextPage()
                }
            }
    }

    // Mostrar errores
    LaunchedEffect(error) {
        if (error != null) {
            delay(5000)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            InventoryNavbar(
                onAddClick = onAddClick,
                onSyncClick = {
                    if (!isRefreshing) {
                        isRefreshing = true
                        viewModel.refreshData()
                        // Simular delay para better UX
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                            delay(1000)
                            isRefreshing = false
                        }
                    }
                },
                onLogoutClick = onLogout,
                userRole = userRole // Pasar el rol al navbar
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
                modifier = Modifier.fillMaxSize()
            ) {
                // Área de búsqueda y contadores
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Buscador
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { newValue ->
                            searchQuery = newValue
                            // Limpiar resultados del servidor cuando se cambia la búsqueda
                            if (newValue.length < 2) {
                                serverSearchResults = emptyList()
                            }
                        },
                        label = { Text("Buscar por código, descripción o proveedor") },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isDeleting,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color.Black,
                            unfocusedLabelColor = Color.DarkGray,
                            cursorColor = Color.Black,
                            disabledBorderColor = Color.LightGray,
                            disabledLabelColor = Color.LightGray
                        ),
                        placeholder = { Text("Ingrese búsqueda...", color = Color.DarkGray) },
                        trailingIcon = {
                            if (isSearchingServer) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color(0xFF9C84C9)
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Información de estado y contadores
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) {
                                val localCount = products.count { product ->
                                    product.codigo.contains(searchQuery, ignoreCase = true) ||
                                            product.descripcion.contains(searchQuery, ignoreCase = true) ||
                                            product.proveedor.contains(searchQuery, ignoreCase = true)
                                }
                                val serverCount = serverSearchResults.size
                                if (serverCount > 0) {
                                    "Encontrados: ${filteredProducts.size} (${localCount} locales + ${serverCount} servidor)"
                                } else {
                                    "Encontrados: ${filteredProducts.size} de ${products.size}"
                                }
                            } else {
                                "Mostrando: ${products.size} productos"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )

                        // Indicador de estado
                        when {
                            isLoading -> {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color(0xFF9C84C9)
                                    )
                                    Text(
                                        text = "Cargando...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF9C84C9)
                                    )
                                }
                            }
                            isSearchingServer -> {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color(0xFF2196F3)
                                    )
                                    Text(
                                        text = "Buscando...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF2196F3)
                                    )
                                }
                            }
                            isRefreshing -> {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color(0xFF9C84C9)
                                    )
                                    Text(
                                        text = "Actualizando...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF9C84C9)
                                    )
                                }
                            }
                        }
                    }

                    // Mostrar error si existe
                    error?.let { errorMsg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFEBEE)
                            )
                        ) {
                            Text(
                                text = "Error: $errorMsg",
                                color = Color.Red,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Lista de productos
                Box(modifier = Modifier.weight(1f)) {
                    if (filteredProducts.isEmpty() && !isLoading) {
                        // Mensaje cuando no hay productos
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (searchQuery.isEmpty()) {
                                        "No hay productos registrados"
                                    } else {
                                        "No se encontraron productos"
                                    },
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Toca el botón de sincronizar para actualizar",
                                        color = Color.LightGray,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp),
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
                                    currentUserEmail = currentUserEmail, // Pasar email actual
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

                            // Indicador de carga al final
                            if (isLoadingMore && searchQuery.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = Color(0xFF9C84C9)
                                            )
                                            Text(
                                                text = "Cargando más productos...",
                                                color = Color.Gray,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }

                            // Indicador de final de lista
                            if (products.isNotEmpty() && !isLoadingMore && searchQuery.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "• • •",
                                            color = Color.LightGray,
                                            style = MaterialTheme.typography.bodyMedium,
                                            letterSpacing = 4.sp // usando .sp
                                        )
                                    }
                                }
                            }
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
    userRole: String,
    isDeleting: Boolean,
    currentUserEmail: String?, // Email del usuario actual
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

    val isAdmin = (userRole == "admin")
    val canEdit = isAdmin      // Solo admin puede editar
    val canDelete = isAdmin
    val canSeeHistory = isAdmin

    val gradientColors = listOf(Color(0xFF9C84C9), Color(0xFFB89EDC))

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
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
                        // BOTÓN EDITAR - Solo admin
                        if (canEdit) {
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
                        }

                        // GESTIONAR STOCK - Todos pueden
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

                        // HISTORIAL - Solo admin
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

                        // ELIMINAR - Solo admin
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
                if (isAdmin) {
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

                if (isAdmin && product.createdBy.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))

                    val displayName = UserUtils.getDisplayName(product.createdBy, currentUserEmail)
                    val badgeColor = UserUtils.getUserBadgeColor(product.createdBy, currentUserEmail)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Creado por:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )

                        // Badge del usuario
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = badgeColor),
                            modifier = Modifier.padding(0.dp)
                        ) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}