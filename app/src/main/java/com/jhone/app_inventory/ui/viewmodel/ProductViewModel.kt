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
    private var isFirstLoad = true

    // Listener para productos
    private var productsListener: ListenerRegistration? = null
    private var movimientosListener: ListenerRegistration? = null

    init {
        loadFirstPage()
    }

    /**
     * Carga la primera página de productos con listener
     */
    private fun loadFirstPage() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Resetear estado de paginación
                lastVisibleDoc = null
                allProductsLoaded = false
                isFirstLoad = true

                Log.d("ProductViewModel", "Cargando primera página...")

                // Cargar primera página sin listener
                val firstPageQuery = db.collection("products")
                    .orderBy("codigo")
                    .limit(pageSize)

                val snapshot = firstPageQuery.get().await()

                if (snapshot.documents.isNotEmpty()) {
                    lastVisibleDoc = snapshot.documents.last()

                    val initialProducts = snapshot.documents.mapNotNull { doc ->
                        parseProductFromDocument(doc)
                    }

                    _products.value = initialProducts
                    Log.d("ProductViewModel", "Primera página cargada: ${initialProducts.size} productos")

                    // Después de cargar la primera página, setup listener para cambios en tiempo real
                    setupRealtimeListener()
                } else {
                    _products.value = emptyList()
                    allProductsLoaded = true
                }

            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error cargando primera página", e)
                _error.value = "Error cargando productos: ${e.message}"
            } finally {
                _isLoading.value = false
                isFirstLoad = false
            }
        }
    }

    /**
     * Setup listener solo para cambios en tiempo real de productos ya cargados
     */
    private fun setupRealtimeListener() {
        try {
            productsListener?.remove()

            // Solo escuchar cambios en productos que ya tenemos cargados
            val productIds = _products.value.map { it.id }

            if (productIds.isNotEmpty()) {
                // Escuchar cambios solo en los productos actuales
                productsListener = db.collection("products")
                    .whereIn("__name__", productIds.take(10)) // Firestore limit para whereIn
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            Log.e("ProductViewModel", "Error en listener de cambios", e)
                            return@addSnapshotListener
                        }

                        // Actualizar solo productos modificados
                        snapshot?.documentChanges?.forEach { change ->
                            when (change.type) {
                                com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                    updateProductInList(change.document)
                                }
                                com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                    removeProductFromList(change.document.id)
                                }
                                else -> {
                                }
                            }
                        }
                    }
            }
        } catch (e: Exception) {
            Log.e("ProductViewModel", "Error configurando listener", e)
        }
    }

    private fun updateProductInList(document: DocumentSnapshot) {
        val updatedProduct = parseProductFromDocument(document) ?: return
        val currentList = _products.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == updatedProduct.id }

        if (index != -1) {
            currentList[index] = updatedProduct
            _products.value = currentList
            Log.d("ProductViewModel", "Producto actualizado: ${updatedProduct.codigo}")
        }
    }

    private fun removeProductFromList(productId: String) {
        val currentList = _products.value.toMutableList()
        val removed = currentList.removeAll { it.id == productId }
        if (removed) {
            _products.value = currentList
            Log.d("ProductViewModel", "Producto eliminado de la lista: $productId")
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
                porcentaje = doc.getDouble("porcentaje") ?: 0.0
            )
        } catch (e: Exception) {
            Log.e("ProductViewModel", "Error parseando producto ${doc.id}", e)
            null
        }
    }

    /**
     * Cargar siguiente página de productos
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
                    parseProductFromDocument(doc)
                }

                // Agregar nuevos productos evitando duplicados
                val currentList = _products.value.toMutableList()
                val uniqueNewProducts = newProducts.filter { newProduct ->
                    currentList.none { it.id == newProduct.id }
                }

                _products.value = currentList + uniqueNewProducts
                Log.d("ProductViewModel", "Página cargada: ${uniqueNewProducts.size} productos nuevos")

                // Si cargamos menos productos que el pageSize, probablemente no hay más
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

    // Agregar un producto
    fun addProduct(product: Product, onComplete: (success: Boolean, error: String?) -> Unit) {
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

                val docRef = db.collection("products").add(productMap).await()

                // Agregar el nuevo producto al inicio de la lista local
                val newProduct = product.copy(id = docRef.id)
                val currentList = _products.value.toMutableList()
                currentList.add(0, newProduct)
                _products.value = currentList

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

                // Actualizar en la lista local
                updateProductInList(db.collection("products").document(product.id).get().await())

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

                // Remover de la lista local
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
     */
    fun refreshData() {
        productsListener?.remove()
        _products.value = emptyList()
        loadFirstPage()
    }

    /**
     * Función para sincronizar (alias de refresh)
     */
    fun syncData() {
        refreshData()
    }

    // Función para buscar productos específicos
    fun searchProducts(query: String, onComplete: (List<Product>) -> Unit) {
        if (query.isBlank()) {
            onComplete(emptyList())
            return
        }

        viewModelScope.launch {
            try {
                val cleanQuery = query.trim().lowercase()

                // Buscar por código (más eficiente con índice)
                val codeResults = db.collection("products")
                    .orderBy("codigo")
                    .startAt(cleanQuery)
                    .endAt(cleanQuery + "\uf8ff")
                    .limit(20)
                    .get()
                    .await()

                val searchResults = codeResults.documents.mapNotNull { doc ->
                    parseProductFromDocument(doc)
                }

                onComplete(searchResults)

            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error en búsqueda", e)
                onComplete(emptyList())
            }
        }
    }

    // Limpiar recursos
    override fun onCleared() {
        super.onCleared()
        productsListener?.remove()
        movimientosListener?.remove()
    }

    // Función para limpiar errores
    fun clearError() {
        _error.value = null
    }
}