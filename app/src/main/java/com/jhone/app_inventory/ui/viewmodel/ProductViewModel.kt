package com.jhone.app_inventory.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FirebaseFirestoreException
import com.jhone.app_inventory.data.Movimiento
import com.jhone.app_inventory.data.Product
import com.jhone.app_inventory.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import java.util.UUID

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    private val _product = MutableStateFlow<Product?>(null)
    val product: StateFlow<Product?> = _product

    private val _movimientos = MutableStateFlow<List<Movimiento>>(emptyList())
    val movimientos: StateFlow<List<Movimiento>> = _movimientos

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // PREVENCIÓN DE DUPLICADOS
    private val movementMutex = Mutex() // Prevenir múltiples movimientos simultáneos
    private val pendingMovements = mutableSetOf<String>() // IDs de movimientos en proceso
    private val processedMovements = mutableSetOf<String>() // IDs de movimientos completados

    // CONFIGURACIÓN DE PAGINACIÓN
    private val pageSize = 20L
    private var lastVisibleDoc: DocumentSnapshot? = null
    private var allProductsLoaded = false
    private val loadedProductIds = mutableSetOf<String>()

    // Gestión mejorada de listeners
    private var movimientosListener: ListenerRegistration? = null
    private var currentProductId: String? = null

    init {
        initializeDataWithCache()
    }

    private fun initializeDataWithCache() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // OBSERVAR CACHÉ LOCAL
                viewModelScope.launch {
                    productRepository.getProductsFlow().collect { cachedProducts ->
                        _products.value = cachedProducts.sortedBy { it.codigo }
                        Log.d("ProductViewModel", "Productos desde caché: ${cachedProducts.size}")

                        loadedProductIds.clear()
                        loadedProductIds.addAll(cachedProducts.map { it.id })
                    }
                }

                // INICIALIZAR CACHÉ
                productRepository.initializeCache()
                    .onSuccess {
                        Log.d("ProductViewModel", "Caché inicializado correctamente")
                    }
                    .onFailure { error ->
                        Log.e("ProductViewModel", "Error inicializando caché", error)
                        _error.value = "Error cargando datos: ${error.message}"
                        loadFirstPageFallback()
                    }

            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error en inicialización híbrida", e)
                _error.value = "Error inicializando datos: ${e.message}"
                loadFirstPageFallback()
            } finally {
                _isLoading.value = false
            }
        }
    }

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

    fun loadNextPage() {
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

    fun searchProductsInServer(query: String, onComplete: (List<Product>) -> Unit) {
        if (query.isBlank()) {
            onComplete(emptyList())
            return
        }

        viewModelScope.launch {
            try {
                Log.d("ProductViewModel", "Iniciando búsqueda para: '$query'")

                val localResults = productRepository.searchProductsLocal(query)
                Log.d("ProductViewModel", "Resultados locales encontrados: ${localResults.size}")

                if (localResults.isNotEmpty()) {
                    Log.d("ProductViewModel", "Devolviendo ${localResults.size} resultados locales")
                    onComplete(localResults)
                    return@launch
                }

                Log.d("ProductViewModel", "No hay resultados locales, buscando en servidor...")
                val remoteResults = productRepository.searchProductsRemote(query)
                Log.d("ProductViewModel", "Resultados remotos encontrados: ${remoteResults.size}")

                if (remoteResults.isNotEmpty()) {
                    try {
                        remoteResults.forEach { product ->
                            productRepository.addProductToCache(product)
                        }
                        Log.d("ProductViewModel", "Resultados remotos guardados en caché")
                    } catch (e: Exception) {
                        Log.e("ProductViewModel", "Error guardando en caché", e)
                    }
                }

                onComplete(remoteResults)

            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error en búsqueda", e)
                onComplete(emptyList())
            }
        }
    }

    fun addProduct(product: Product, onComplete: (success: Boolean, error: String?) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                productRepository.addProduct(product)
                    .onSuccess { newProduct ->
                        Log.d("ProductViewModel", "Producto agregado exitosamente: ${newProduct.id}")
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

    fun updateProduct(product: Product, onComplete: (success: Boolean, error: String?) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                productRepository.updateProduct(product)
                    .onSuccess {
                        Log.d("ProductViewModel", "Producto actualizado exitosamente: ${product.id}")
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

    fun deleteProduct(product: Product, onComplete: (success: Boolean, error: String?) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

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

    fun clearMovimientosListener() {
        try {
            movimientosListener?.remove()
            movimientosListener = null
            currentProductId = null
            _movimientos.value = emptyList()
            Log.d("ProductViewModel", "Listeners de movimientos limpiados")
        } catch (e: Exception) {
            Log.e("ProductViewModel", "Error limpiando listeners", e)
        }
    }

    /**
     * FUNCIÓN CRÍTICA CORREGIDA: addMovimiento
     * Previene duplicados usando Mutex y IDs únicos
     */
    fun addMovimiento(movimiento: Movimiento, onComplete: (success: Boolean, error: String?) -> Unit) {
        viewModelScope.launch {
            movementMutex.withLock {
                try {
                    // Generar ID único para el movimiento
                    val movementId = UUID.randomUUID().toString()

                    // Verificar si ya está en proceso
                    if (pendingMovements.contains(movementId) || processedMovements.contains(movementId)) {
                        Log.w("ProductViewModel", "Movimiento duplicado detectado y bloqueado: $movementId")
                        onComplete(false, "Operación duplicada bloqueada")
                        return@withLock
                    }

                    // Marcar como pendiente
                    pendingMovements.add(movementId)
                    Log.d("ProductViewModel", "Iniciando movimiento único: $movementId")

                    val currentUserEmail = auth.currentUser?.email ?: "UsuarioDesconocido"
                    val productRef = db.collection("products").document(movimiento.loteId)
                    val movimientoRef = db.collection("movimientos").document(movementId)

                    val movimientoData = mapOf(
                        "id" to movementId, // Agregar ID explícito
                        "loteId" to movimiento.loteId,
                        "tipo" to movimiento.tipo,
                        "cantidad" to movimiento.cantidad,
                        "fecha" to Timestamp.now(), // Usar timestamp del servidor
                        "usuario" to currentUserEmail,
                        "observacion" to movimiento.observacion,
                        "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp() // Para ordenamiento
                    )

                    // TRANSACCIÓN ATÓMICA MEJORADA
                    val newQuantity = db.runTransaction { transaction ->
                        // 1. Leer estado actual del producto
                        val productSnapshot = transaction.get(productRef)
                        if (!productSnapshot.exists()) {
                            throw Exception("El producto no existe")
                        }

                        // 2. Verificar si el movimiento ya existe (prevención adicional)
                        val existingMovement = transaction.get(movimientoRef)
                        if (existingMovement.exists()) {
                            throw Exception("El movimiento ya existe en la base de datos")
                        }

                        val currentQuantity = productSnapshot.getLong("cantidad") ?: 0L

                        // 3. Calcular nueva cantidad
                        val calculatedQuantity = if (movimiento.tipo == "ingreso") {
                            currentQuantity + movimiento.cantidad
                        } else {
                            currentQuantity - movimiento.cantidad
                        }

                        // 4. Validaciones de negocio
                        if (calculatedQuantity < 0) {
                            throw Exception("Stock insuficiente. Stock actual: $currentQuantity, intentando ${movimiento.tipo}: ${movimiento.cantidad}")
                        }

                        // 5. Actualizar producto con nueva cantidad
                        transaction.update(productRef, mapOf(
                            "cantidad" to calculatedQuantity,
                            "lastUpdated" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        ))

                        // 6. Crear registro de movimiento
                        transaction.set(movimientoRef, movimientoData)

                        Log.d("ProductViewModel", "Transacción exitosa: $currentQuantity -> $calculatedQuantity")
                        calculatedQuantity.toInt()
                    }.await()

                    // 7. Actualizar caché local DESPUÉS de confirmar transacción
                    try {
                        productRepository.updateProductQuantity(movimiento.loteId, newQuantity)
                        Log.d("ProductViewModel", "Caché local actualizado: nueva cantidad $newQuantity")
                    } catch (e: Exception) {
                        Log.e("ProductViewModel", "Error actualizando caché local", e)
                        // No fallar por error de caché
                    }

                    // 8. Marcar como completado
                    pendingMovements.remove(movementId)
                    processedMovements.add(movementId)

                    // 9. Limpiar historial de procesados periódicamente
                    if (processedMovements.size > 100) {
                        val toRemove = processedMovements.take(50)
                        processedMovements.removeAll(toRemove.toSet())
                    }

                    Log.d("ProductViewModel", "Movimiento completado exitosamente: $movementId -> Nueva cantidad: $newQuantity")
                    onComplete(true, null)

                } catch (e: Exception) {
                    // Limpiar estado en caso de error
                    pendingMovements.clear()

                    when (e) {
                        is FirebaseFirestoreException -> {
                            Log.e("ProductViewModel", "Error de Firestore en movimiento", e)
                            onComplete(false, "Error de conectividad: ${e.message}")
                        }
                        else -> {
                            Log.e("ProductViewModel", "Error en transacción de movimiento", e)
                            onComplete(false, e.message ?: "Error desconocido")
                        }
                    }
                }
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                clearMovimientosListener()

                // Limpiar estados de duplicados
                pendingMovements.clear()
                processedMovements.clear()

                productRepository.forceSync()
                    .onSuccess {
                        Log.d("ProductViewModel", "Sincronización forzada exitosa")
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

    fun syncData() {
        refreshData()
    }

    fun listenToMovimientosForProduct(productId: String) {
        try {
            if (currentProductId == productId && movimientosListener != null) {
                Log.d("ProductViewModel", "Ya existe listener para producto: $productId")
                return
            }

            clearMovimientosListener()
            currentProductId = productId

            Log.d("ProductViewModel", "Configurando listener para producto: $productId")

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
                            Log.d("ProductViewModel", "Snapshot recibido con ${snapshot.documents.size} documentos")

                            if (snapshot.isEmpty) {
                                Log.d("ProductViewModel", "No hay movimientos para el producto $productId")
                                _movimientos.value = emptyList()
                                return@addSnapshotListener
                            }

                            val movimientosList = snapshot.documents.mapNotNull { doc ->
                                try {
                                    Log.d("ProductViewModel", "Procesando documento: ${doc.id}")
                                    Log.d("ProductViewModel", "Datos del documento: ${doc.data}")

                                    // Crear el movimiento con todos los campos
                                    val movimiento = Movimiento(
                                        id = doc.id,
                                        loteId = doc.getString("loteId") ?: "",
                                        tipo = doc.getString("tipo") ?: "",
                                        cantidad = doc.getLong("cantidad")?.toInt() ?: 0,
                                        fecha = doc.getTimestamp("fecha") ?: Timestamp.now(),
                                        usuario = doc.getString("usuario") ?: "",
                                        observacion = doc.getString("observacion") ?: ""
                                    )

                                    Log.d("ProductViewModel", "Movimiento parseado: ID=${movimiento.id}, Tipo=${movimiento.tipo}, Cantidad=${movimiento.cantidad}, Usuario=${movimiento.usuario}")
                                    movimiento

                                } catch (e: Exception) {
                                    Log.e("ProductViewModel", "Error al parsear movimiento ${doc.id}", e)
                                    null
                                }
                            }

                            // Eliminar duplicados por ID y ordenar por fecha
                            val uniqueMovimientos = movimientosList
                                .distinctBy { it.id }
                                .sortedByDescending { it.fecha.seconds }

                            Log.d("ProductViewModel", "Movimientos únicos procesados: ${uniqueMovimientos.size}")
                            uniqueMovimientos.forEachIndexed { index, mov ->
                                Log.d("ProductViewModel", "Movimiento $index: ${mov.tipo} - ${mov.cantidad} - ${mov.usuario}")
                            }

                            _movimientos.value = uniqueMovimientos

                        } else {
                            Log.d("ProductViewModel", "Snapshot es null")
                            _movimientos.value = emptyList()
                        }
                    } catch (e: Exception) {
                        Log.e("ProductViewModel", "Error al procesar movimientos", e)
                        _movimientos.value = emptyList()
                    }
                }

            Log.d("ProductViewModel", "Listener configurado para producto: $productId")

        } catch (e: Exception) {
            Log.e("ProductViewModel", "Error al configurar listener de movimientos", e)
            _movimientos.value = emptyList()
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

    fun clearError() {
        _error.value = null
    }

    fun resetStates() {
        try {
            _isLoading.value = false
            _isLoadingMore.value = false
            _error.value = null
            clearMovimientosListener()

            // Limpiar estados de duplicados
            pendingMovements.clear()
            processedMovements.clear()

            Log.d("ProductViewModel", "Estados reseteados")
        } catch (e: Exception) {
            Log.e("ProductViewModel", "Error al resetear estados", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            clearMovimientosListener()
            pendingMovements.clear()
            processedMovements.clear()
            Log.d("ProductViewModel", "ViewModel limpiado correctamente")
        } catch (e: Exception) {
            Log.e("ProductViewModel", "Error al limpiar ViewModel", e)
        }
    }
}