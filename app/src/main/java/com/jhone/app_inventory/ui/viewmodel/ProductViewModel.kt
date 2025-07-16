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
import com.jhone.app_inventory.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val productRepository: ProductRepository, // Repository para caché
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    // ESTADOS DEL UI (mantienen la misma estructura)
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

    // CONFIGURACIÓN DE PAGINACIÓN
    private val pageSize = 20L
    private var lastVisibleDoc: DocumentSnapshot? = null
    private var allProductsLoaded = false
    private val loadedProductIds = mutableSetOf<String>()

    // LISTENER PARA MOVIMIENTOS
    private var movimientosListener: ListenerRegistration? = null

    init {
        initializeDataWithCache()
    }

    /**
     * INICIALIZACIÓN HÍBRIDA (Caché + Firebase)
     * Carga desde caché inmediatamente, sincroniza en background
     */
    private fun initializeDataWithCache() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // 1. OBSERVAR CACHÉ LOCAL (Flow continuo, instantáneo)
                viewModelScope.launch {
                    productRepository.getProductsFlow().collect { cachedProducts ->
                        _products.value = cachedProducts.sortedBy { it.codigo }
                        Log.d("ProductViewModel", "Productos desde caché: ${cachedProducts.size}")

                        // Actualizar tracking para compatibilidad con paginación existente
                        loadedProductIds.clear()
                        loadedProductIds.addAll(cachedProducts.map { it.id })
                    }
                }

                // 2. INICIALIZAR CACHÉ SI ES NECESARIO
                productRepository.initializeCache()
                    .onSuccess {
                        Log.d("ProductViewModel", "Caché inicializado correctamente")
                    }
                    .onFailure { error ->
                        Log.e("ProductViewModel", "Error inicializando caché", error)
                        _error.value = "Error cargando datos: ${error.message}"
                        // 📱 FALLBACK: Si falla caché, usar método original
                        loadFirstPageFallback()
                    }

            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error en inicialización híbrida", e)
                _error.value = "Error inicializando datos: ${e.message}"
                // 📱 FALLBACK: Usar método original si falla todo
                loadFirstPageFallback()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Método original como respaldo
     * Mantiene tu lógica actual si falla el caché
     */
    private fun loadFirstPageFallback() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                lastVisibleDoc = null
                allProductsLoaded = false
                loadedProductIds.clear()

                Log.d("ProductViewModel", "FALLBACK: Cargando primera página desde Firebase...")

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
                    Log.d("ProductViewModel", "FALLBACK: Primera página cargada: ${initialProducts.size} productos")

                } else {
                    _products.value = emptyList()
                    allProductsLoaded = true
                }

            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error en fallback", e)
                _error.value = "Error cargando productos: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * CARGAR SIGUIENTE PÁGINA (adaptado para híbrido)
     * Usa caché local si está disponible, Firebase como fallback
     */
    fun loadNextPage() {
        // Con caché local, esto es menos crítico, pero mantenemos para compatibilidad
        if (allProductsLoaded || _isLoadingMore.value) {
            Log.d("ProductViewModel", "loadNextPage: No se puede cargar más")
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

    /**
     * BÚSQUEDA HÍBRIDA (Local primero, luego remota)
     * 0 lecturas Firebase para búsquedas locales en cache
     */
    fun searchProductsInServer(query: String, onComplete: (List<Product>) -> Unit) {
        if (query.isBlank()) {
            onComplete(emptyList())
            return
        }

        viewModelScope.launch {
            try {
                // 1. VERIFICAR CACHÉ LOCAL PRIMERO
                val localResults = productRepository.searchProductsLocal(query)

                if (localResults.isNotEmpty()) {
                    onComplete(localResults)
                    return@launch
                }

                // 2. BÚSQUEDA REMOTA EN FIREBASE
                val cleanQuery = query.trim()
                val searchResults = mutableListOf<Product>()

                // 2.1 Búsqueda por código
                try {
                    val codeQuery = db.collection("products")
                        .orderBy("codigo")
                        .startAt(cleanQuery.uppercase())
                        .endAt(cleanQuery.uppercase() + "\uf8ff")
                        .limit(10)

                    val codeResults = codeQuery.get().await()
                    val codeProducts = codeResults.documents.mapNotNull { doc ->
                        parseProductFromDocument(doc)
                    }
                    searchResults.addAll(codeProducts)
                } catch (e: Exception) {
                }

                // 2.2 Búsqueda por descripción
                try {
                    // Búsqueda normal
                    val descQuery = db.collection("products")
                        .orderBy("descripcion")
                        .startAt(cleanQuery)
                        .endAt(cleanQuery + "\uf8ff")
                        .limit(10)

                    val descResults = descQuery.get().await()
                    val descProducts = descResults.documents.mapNotNull { doc ->
                        parseProductFromDocument(doc)
                    }

                    descProducts.forEach { product ->
                        if (searchResults.none { it.id == product.id }) {
                            searchResults.add(product)
                        }
                    }

                    // Si no encuentra, intentar con uppercase
                    if (descProducts.isEmpty()) {
                        val descQueryUpper = db.collection("products")
                            .orderBy("descripcion")
                            .startAt(cleanQuery.uppercase())
                            .endAt(cleanQuery.uppercase() + "\uf8ff")
                            .limit(10)

                        val descResultsUpper = descQueryUpper.get().await()
                        val descProductsUpper = descResultsUpper.documents.mapNotNull { doc ->
                            parseProductFromDocument(doc)
                        }

                        descProductsUpper.forEach { product ->
                            if (searchResults.none { it.id == product.id }) {
                                searchResults.add(product)
                            }
                        }
                    }

                    // Búsqueda manual en muestra
                    if (searchResults.isEmpty()) {
                        val sampleQuery = db.collection("products")
                            .limit(100)

                        val sampleResults = sampleQuery.get().await()
                        val manualMatches = sampleResults.documents.mapNotNull { doc ->
                            val product = parseProductFromDocument(doc)
                            if (product != null && (
                                        product.descripcion.contains(cleanQuery, ignoreCase = true) ||
                                                product.proveedor.contains(cleanQuery, ignoreCase = true)
                                        )) {
                                product
                            } else {
                                null
                            }
                        }

                        manualMatches.forEach { product ->
                            if (searchResults.none { it.id == product.id }) {
                                searchResults.add(product)
                            }
                        }
                    }

                } catch (e: Exception) {
                }

                onComplete(searchResults.take(20))

            } catch (e: Exception) {
                onComplete(emptyList())
            }
        }
    }

    /**
     * AGREGAR PRODUCTO (Repository + Firebase)
     * Actualiza caché automáticamente
     */
    fun addProduct(product: Product, onComplete: (success: Boolean, error: String?) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                productRepository.addProduct(product)
                    .onSuccess { newProduct ->
                        Log.d("ProductViewModel", "Producto agregado exitosamente: ${newProduct.id}")
                        // El caché se actualiza automáticamente vía Flow
                        onComplete(true, null)
                    }
                    .onFailure { error ->
                        Log.e("ProductViewModel", "Error agregando producto", error)
                        onComplete(false, error.message)
                    }

            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error en addProduct", e)
                onComplete(false, e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * ACTUALIZAR PRODUCTO (Repository + Firebase)
     * Actualiza caché automáticamente
     */
    fun updateProduct(product: Product, onComplete: (success: Boolean, error: String?) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                productRepository.updateProduct(product)
                    .onSuccess {
                        Log.d("ProductViewModel", "Producto actualizado exitosamente: ${product.id}")
                        // El caché se actualiza automáticamente vía Flow
                        onComplete(true, null)
                    }
                    .onFailure { error ->
                        Log.e("ProductViewModel", "Error actualizando producto", error)
                        onComplete(false, error.message)
                    }

            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error en updateProduct", e)
                onComplete(false, e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * ELIMINAR PRODUCTO (Repository + Firebase)
     * Limpia caché automáticamente
     */
    fun deleteProduct(product: Product, onComplete: (success: Boolean, error: String?) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Eliminar movimientos relacionados primero
                try {
                    val movimientosSnapshot = db.collection("movimientos")
                        .whereEqualTo("loteId", product.id)
                        .get()
                        .await()

                    val batch = db.batch()
                    for (doc in movimientosSnapshot.documents) {
                        batch.delete(doc.reference)
                    }
                    batch.commit().await()
                } catch (e: Exception) {
                    Log.e("ProductViewModel", "Error eliminando movimientos", e)
                }

                productRepository.deleteProduct(product.id)
                    .onSuccess {
                        Log.d("ProductViewModel", "Producto eliminado exitosamente: ${product.id}")
                        // El caché se actualiza automáticamente vía Flow
                        onComplete(true, null)
                    }
                    .onFailure { error ->
                        Log.e("ProductViewModel", "Error eliminando producto", error)
                        onComplete(false, error.message)
                    }

            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error en deleteProduct", e)
                onComplete(false, e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * MOVIMIENTO DE STOCK (con actualización en Repository)
     * Mantiene tu lógica de transacciones Firebase
     */
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

                // Actualizar cantidad en el Repository (caché local)
                val finalQuantity = if (movimiento.tipo == "ingreso") {
                    _products.value.find { it.id == movimiento.loteId }?.cantidad?.plus(movimiento.cantidad) ?: 0
                } else {
                    _products.value.find { it.id == movimiento.loteId }?.cantidad?.minus(movimiento.cantidad) ?: 0
                }

                productRepository.updateProductQuantity(movimiento.loteId, finalQuantity)

                Log.d("ProductViewModel", "Movimiento agregado exitosamente")
                onComplete(true, null)

            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error en la transacción de movimiento", e)
                onComplete(false, e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * REFRESCAR DATOS (Fuerza sincronización completa)
     * Usa Repository para sincronización optimizada
     */
    fun refreshData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                movimientosListener?.remove()

                productRepository.forceSync()
                    .onSuccess {
                        Log.d("ProductViewModel", "Sincronización forzada exitosa")
                        // Reset estado de paginación
                        lastVisibleDoc = null
                        allProductsLoaded = false
                        loadedProductIds.clear()
                    }
                    .onFailure { error ->
                        _error.value = "Error sincronizando: ${error.message}"
                        Log.e("ProductViewModel", "Error en sincronización forzada", error)
                    }

            } catch (e: Exception) {
                _error.value = "Error refrescando datos: ${e.message}"
                Log.e("ProductViewModel", "Error en refreshData", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * SINCRONIZAR (Alias de refresh)
     */
    fun syncData() {
        refreshData()
    }

    /**
     * ESCUCHAR MOVIMIENTOS (sin cambios)
     * Mantiene tu lógica actual para movimientos
     */
    fun listenToMovimientosForProduct(productId: String) {
        try {
            movimientosListener?.remove()

            movimientosListener = db.collection("movimientos")
                .whereEqualTo("loteId", productId)
                .orderBy("fecha", Query.Direction.DESCENDING)
                .limit(50)
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

    // FUNCIONES DE UTILIDAD

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
                createdBy = doc.getString("createdBy") ?: ""
            )
        } catch (e: Exception) {
            Log.e("ProductViewModel", "Error parseando producto ${doc.id}", e)
            null
        }
    }

    private fun updateProductInList(updatedProduct: Product) {
        val currentList = _products.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == updatedProduct.id }

        if (index != -1) {
            currentList[index] = updatedProduct
            _products.value = currentList.sortedBy { it.codigo }
            Log.d("ProductViewModel", "Producto actualizado en lista: ${updatedProduct.codigo}")
        }
    }

    private fun removeProductFromList(productId: String) {
        val currentList = _products.value.toMutableList()
        val removed = currentList.removeAll { it.id == productId }
        if (removed) {
            loadedProductIds.remove(productId)
            _products.value = currentList
            Log.d("ProductViewModel", "Producto eliminado de la lista: $productId")
        }
    }

    // LIMPIAR RECURSOS
    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        movimientosListener?.remove()
    }
}