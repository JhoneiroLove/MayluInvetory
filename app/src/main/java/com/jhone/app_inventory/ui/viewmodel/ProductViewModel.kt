package com.jhone.app_inventory.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration
import com.jhone.app_inventory.data.Movimiento
import com.jhone.app_inventory.data.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    private val _movimientos = MutableStateFlow<List<Movimiento>>(emptyList())
    val movimientos: StateFlow<List<Movimiento>> = _movimientos

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Configuración de paginación
    private val pageSize = 20L
    private var lastVisibleDoc: DocumentSnapshot? = null
    private var allProductsLoaded = false

    // Set para tracking de IDs cargados
    private val loadedProductIds = mutableSetOf<String>()

    // Listener para movimientos solamente
    private var movimientosListener: ListenerRegistration? = null

    init {
        loadFirstPage()
    }

    /**
     * Carga la primera página de productos
     * Solo paginación manual, SIN listeners conflictivos
     */
    private fun loadFirstPage() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Resetear estado
                lastVisibleDoc = null
                allProductsLoaded = false
                loadedProductIds.clear()

                Log.d("ProductViewModel", "Cargando primera página...")

                val firstPageQuery = db.collection("products")
                    .orderBy("codigo")
                    .limit(pageSize)

                val snapshot = firstPageQuery.get().await()

                if (snapshot.documents.isNotEmpty()) {
                    lastVisibleDoc = snapshot.documents.last()

                    val initialProducts = snapshot.documents.mapNotNull { doc ->
                        parseProductFromDocument(doc)?.also { product ->
                            loadedProductIds.add(product.id)
                        }
                    }

                    _products.value = initialProducts.sortedBy { it.codigo }
                    Log.d("ProductViewModel", "Primera página cargada: ${initialProducts.size} productos")

                } else {
                    _products.value = emptyList()
                    allProductsLoaded = true
                }

            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error cargando primera página", e)
                _error.value = "Error cargando productos: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Cargar siguiente página de productos
     * Evita duplicados y mantiene orden consistente
     */
    fun loadNextPage() {
        if (allProductsLoaded || _isLoadingMore.value) {
            Log.d("ProductViewModel", "No se puede cargar más: allLoaded=$allProductsLoaded, isLoading=${_isLoadingMore.value}")
            return
        }

        viewModelScope.launch {
            try {
                _isLoadingMore.value = true
                Log.d("ProductViewModel", "Cargando siguiente página...")

                var query = db.collection("products")
                    .orderBy("codigo")
                    .limit(pageSize)

                if (lastVisibleDoc != null) {
                    query = query.startAfter(lastVisibleDoc!!)
                }

                val snapshot = query.get().await()

                if (snapshot.isEmpty) {
                    allProductsLoaded = true
                    Log.d("ProductViewModel", "No hay más productos para cargar")
                    return@launch
                }

                lastVisibleDoc = snapshot.documents.last()
                val newProducts = snapshot.documents.mapNotNull { doc ->
                    parseProductFromDocument(doc)?.takeIf { product ->
                        !loadedProductIds.contains(product.id)
                    }?.also { product ->
                        loadedProductIds.add(product.id)
                    }
                }

                if (newProducts.isNotEmpty()) {
                    // Mantener orden correcto al agregar
                    val currentList = _products.value.toMutableList()
                    currentList.addAll(newProducts)
                    _products.value = currentList.sortedBy { it.codigo }

                    Log.d("ProductViewModel", "Página cargada: ${newProducts.size} productos nuevos")
                } else {
                    Log.d("ProductViewModel", "No hay productos nuevos únicos")
                }

                if (snapshot.documents.size < pageSize) {
                    allProductsLoaded = true
                    Log.d("ProductViewModel", "Última página cargada")
                }

            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error cargando siguiente página", e)
                _error.value = "Error cargando más productos: ${e.message}"
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    private fun parseProductFromDocument(doc: DocumentSnapshot): Product? {
        return try {
            Product(
                id = doc.id,
                codigo = doc.getString("codigo") ?: "",
                descripcion = doc.getString("descripcion") ?: "",
                cantidad = doc.getLong("cantidad")?.toInt() ?: 0,
                precioBoleta = doc.getDouble("precioBoleta") ?: 0.0,
                precioCosto = doc.getDouble("precioCosto") ?: 0.0,
                precioProducto = doc.getDouble("precioProducto") ?: 0.0,
                proveedor = doc.getString("proveedor") ?: "",
                createdAt = doc.getTimestamp("timestamp"),
                fechaVencimiento = doc.getTimestamp("fechaVencimiento"),
                porcentaje = doc.getDouble("porcentaje") ?: 0.0,
                createdBy = doc.getString("createdBy") ?: "" // Leer usuario creador
            )
        } catch (e: Exception) {
            Log.e("ProductViewModel", "Error parseando producto ${doc.id}", e)
            null
        }
    }

    /**
     * Agregar un producto
     * Mantener orden y evita duplicados
     */
    fun addProduct(product: Product, onComplete: (success: Boolean, error: String?) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val currentUserEmail = auth.currentUser?.email ?: "Usuario Desconocido"

                val productMap = mapOf(
                    "codigo" to product.codigo,
                    "descripcion" to product.descripcion,
                    "cantidad" to product.cantidad,
                    "precioBoleta" to product.precioBoleta,
                    "precioCosto" to product.precioCosto,
                    "precioProducto" to product.precioProducto,
                    "proveedor" to product.proveedor,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "fechaVencimiento" to product.fechaVencimiento,
                    "porcentaje" to product.porcentaje,
                    "createdBy" to currentUserEmail // Guardar quién creó el producto
                )

                val docRef = db.collection("products").add(productMap).await()

                // Agregar a la lista local en el lugar correcto por orden
                val newProduct = product.copy(
                    id = docRef.id,
                    createdBy = currentUserEmail // Asegurar que el objeto local tenga el creador
                )
                if (!loadedProductIds.contains(newProduct.id)) {
                    loadedProductIds.add(newProduct.id)
                    val currentList = _products.value.toMutableList()
                    currentList.add(newProduct)
                    _products.value = currentList.sortedBy { it.codigo }
                }

                Log.d("ProductViewModel", "Producto añadido exitosamente: ${docRef.id}")
                onComplete(true, null)

            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error al añadir producto", e)
                onComplete(false, e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Actualizar un producto
     * Refresca desde servidor para evitar inconsistencias
     */
    fun updateProduct(product: Product, onComplete: (success: Boolean, error: String?) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val productMap = mapOf(
                    "codigo" to product.codigo,
                    "descripcion" to product.descripcion,
                    "cantidad" to product.cantidad,
                    "precioBoleta" to product.precioBoleta,
                    "precioCosto" to product.precioCosto,
                    "precioProducto" to product.precioProducto,
                    "proveedor" to product.proveedor,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "fechaVencimiento" to product.fechaVencimiento,
                    "porcentaje" to product.porcentaje
                )

                db.collection("products")
                    .document(product.id)
                    .update(productMap)
                    .await()

                // Obtener el producto actualizado desde el servidor
                val updatedDoc = db.collection("products")
                    .document(product.id)
                    .get()
                    .await()

                if (updatedDoc.exists()) {
                    val updatedProduct = parseProductFromDocument(updatedDoc)
                    if (updatedProduct != null) {
                        updateProductInList(updatedProduct)
                    }
                }

                Log.d("ProductViewModel", "Producto actualizado: ${product.id}")
                onComplete(true, null)

            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error al actualizar producto", e)
                onComplete(false, e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Eliminar un producto
     * Eliminación limpia de listas y tracking
     */
    fun deleteProduct(product: Product, onComplete: (success: Boolean, error: String?) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Eliminar producto
                db.collection("products")
                    .document(product.id)
                    .delete()
                    .await()

                // También eliminar movimientos relacionados
                val movimientosSnapshot = db.collection("movimientos")
                    .whereEqualTo("loteId", product.id)
                    .get()
                    .await()

                val batch = db.batch()
                for (doc in movimientosSnapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().await()

                // Remover de la lista local y tracking
                removeProductFromList(product.id)

                Log.d("ProductViewModel", "Producto y movimientos eliminados: ${product.id}")
                onComplete(true, null)

            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error al eliminar producto", e)
                onComplete(false, e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Actualizar producto en lista local
     * Mantiene orden
     */
    private fun updateProductInList(updatedProduct: Product) {
        val currentList = _products.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == updatedProduct.id }

        if (index != -1) {
            currentList[index] = updatedProduct
            _products.value = currentList.sortedBy { it.codigo }
            Log.d("ProductViewModel", "Producto actualizado en lista: ${updatedProduct.codigo}")
        }
    }

    /**
     * Remover producto de lista local
     * Limpia tracking
     */
    private fun removeProductFromList(productId: String) {
        val currentList = _products.value.toMutableList()
        val removed = currentList.removeAll { it.id == productId }
        if (removed) {
            loadedProductIds.remove(productId)
            _products.value = currentList
            Log.d("ProductViewModel", "Producto eliminado de la lista: $productId")
        }
    }

    fun addMovimiento(movimiento: Movimiento, onComplete: (success: Boolean, error: String?) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val currentUserEmail = auth.currentUser?.email ?: "UsuarioDesconocido"
                val productRef = db.collection("products").document(movimiento.loteId)
                val movimientoRef = db.collection("movimientos").document()

                db.runTransaction { transaction ->
                    val productSnapshot = transaction.get(productRef)
                    if (!productSnapshot.exists()) {
                        throw Exception("El producto no existe")
                    }

                    val currentQuantity = productSnapshot.getLong("cantidad") ?: 0L
                    val newQuantity = if (movimiento.tipo == "ingreso") {
                        currentQuantity + movimiento.cantidad
                    } else {
                        currentQuantity - movimiento.cantidad
                    }

                    if (newQuantity < 0) {
                        throw Exception("No hay stock suficiente para realizar la salida.")
                    }

                    transaction.update(productRef, "cantidad", newQuantity)

                    val movimientoConUsuarioReal = movimiento.copy(
                        id = movimientoRef.id,
                        usuario = currentUserEmail
                    )
                    transaction.set(movimientoRef, movimientoConUsuarioReal)

                    null
                }.await()

                // Actualizar cantidad en la lista local
                val currentList = _products.value.toMutableList()
                val productIndex = currentList.indexOfFirst { it.id == movimiento.loteId }
                if (productIndex != -1) {
                    val updatedProduct = currentList[productIndex].copy(
                        cantidad = if (movimiento.tipo == "ingreso") {
                            currentList[productIndex].cantidad + movimiento.cantidad
                        } else {
                            currentList[productIndex].cantidad - movimiento.cantidad
                        }
                    )
                    currentList[productIndex] = updatedProduct
                    _products.value = currentList
                }

                Log.d("ProductViewModel", "Movimiento añadido exitosamente")
                onComplete(true, null)

            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error en la transacción de movimiento", e)
                onComplete(false, e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun listenToMovimientosForProduct(productId: String) {
        try {
            movimientosListener?.remove()

            movimientosListener = db.collection("movimientos")
                .whereEqualTo("loteId", productId)
                .orderBy("fecha", Query.Direction.DESCENDING)
                .limit(50) // Limitar a 50 movimientos más recientes
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("ProductViewModel", "Error al escuchar movimientos", e)
                        return@addSnapshotListener
                    }

                    try {
                        if (snapshot != null) {
                            val movimientosList = snapshot.documents.mapNotNull { doc ->
                                try {
                                    Movimiento(
                                        id = doc.id,
                                        loteId = doc.getString("loteId") ?: "",
                                        tipo = doc.getString("tipo") ?: "",
                                        cantidad = doc.getLong("cantidad")?.toInt() ?: 0,
                                        fecha = doc.getTimestamp("fecha") ?: com.google.firebase.Timestamp.now(),
                                        usuario = doc.getString("usuario") ?: "",
                                        observacion = doc.getString("observacion") ?: ""
                                    )
                                } catch (e: Exception) {
                                    Log.e("ProductViewModel", "Error al parsear movimiento ${doc.id}", e)
                                    null
                                }
                            }
                            _movimientos.value = movimientosList
                        } else {
                            _movimientos.value = emptyList()
                        }
                    } catch (e: Exception) {
                        Log.e("ProductViewModel", "Error al procesar movimientos", e)
                    }
                }
        } catch (e: Exception) {
            Log.e("ProductViewModel", "Error al configurar listener de movimientos", e)
        }
    }

    /**
     * Función para refrescar completamente los datos
     * Limpia todo el estado antes de recargar
     */
    fun refreshData() {
        viewModelScope.launch {
            movimientosListener?.remove()
            _products.value = emptyList()
            loadedProductIds.clear()
            loadFirstPage()
        }
    }

    /**
     * Función para sincronizar (alias de refresh)
     */
    fun syncData() {
        refreshData()
    }

    // Función para buscar productos específicos en el servidor
    fun searchProductsInServer(query: String, onComplete: (List<Product>) -> Unit) {
        if (query.isBlank()) {
            onComplete(emptyList())
            return
        }

        viewModelScope.launch {
            try {
                val cleanQuery = query.trim()
                val searchResults = mutableListOf<Product>()

                Log.d("ProductViewModel", "Buscando '$cleanQuery' en servidor...")

                // Búsqueda por código
                val codeQuery = db.collection("products")
                    .orderBy("codigo")
                    .startAt(cleanQuery.uppercase())
                    .endAt(cleanQuery.uppercase() + "\uf8ff")
                    .limit(10)

                val codeResults = codeQuery.get().await()
                searchResults.addAll(
                    codeResults.documents.mapNotNull { parseProductFromDocument(it) }
                )

                // Búsqueda por descripción (si no encontró suficientes por código)
                if (searchResults.size < 5) {
                    val descQuery = db.collection("products")
                        .orderBy("descripcion")
                        .startAt(cleanQuery.uppercase())
                        .endAt(cleanQuery.uppercase() + "\uf8ff")
                        .limit(10)

                    val descResults = descQuery.get().await()
                    val descProducts = descResults.documents.mapNotNull { parseProductFromDocument(it) }

                    // Agregar solo productos que no están ya en los resultados
                    descProducts.forEach { product ->
                        if (searchResults.none { it.id == product.id }) {
                            searchResults.add(product)
                        }
                    }
                }

                // Búsqueda por proveedor (si aún no encontró suficientes)
                if (searchResults.size < 5) {
                    val provQuery = db.collection("products")
                        .orderBy("proveedor")
                        .startAt(cleanQuery.uppercase())
                        .endAt(cleanQuery.uppercase() + "\uf8ff")
                        .limit(10)

                    val provResults = provQuery.get().await()
                    val provProducts = provResults.documents.mapNotNull { parseProductFromDocument(it) }

                    provProducts.forEach { product ->
                        if (searchResults.none { it.id == product.id }) {
                            searchResults.add(product)
                        }
                    }
                }

                Log.d("ProductViewModel", "Búsqueda en servidor completada: ${searchResults.size} resultados")
                onComplete(searchResults.take(20)) // Limitar a 20 resultados

            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error en búsqueda en servidor", e)
                onComplete(emptyList())
            }
        }
    }

    // Limpiar recursos
    override fun onCleared() {
        super.onCleared()
        movimientosListener?.remove()
    }

    // Función para limpiar errores
    fun clearError() {
        _error.value = null
    }
}