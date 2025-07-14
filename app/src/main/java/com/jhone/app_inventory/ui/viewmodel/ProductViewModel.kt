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

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val pageSize = 20L
    private var lastVisibleDoc: DocumentSnapshot? = null
    private var allProductsLoaded = false

    // Listener para productos
    private var productsListener: ListenerRegistration? = null
    private var movimientosListener: ListenerRegistration? = null

    init {
        setupProductsListener()
    }

    private fun setupProductsListener() {
        try {
            productsListener?.remove() // Remover listener anterior si existe

            productsListener = db.collection("products")
                .orderBy("codigo")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("ProductViewModel", "Error al escuchar cambios", e)
                        _error.value = "Error al cargar productos: ${e.message}"
                        return@addSnapshotListener
                    }

                    try {
                        if (snapshot != null) {
                            val productsList = snapshot.documents.mapNotNull { doc ->
                                try {
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
                                    Log.e("ProductViewModel", "Error al parsear producto ${doc.id}", e)
                                    null
                                }
                            }
                            _products.value = productsList
                            _error.value = null
                            Log.d("ProductViewModel", "Productos actualizados: ${productsList.size}")
                        } else {
                            _products.value = emptyList()
                        }
                    } catch (e: Exception) {
                        Log.e("ProductViewModel", "Error al procesar productos", e)
                        _error.value = "Error al procesar productos"
                    }
                }
        } catch (e: Exception) {
            Log.e("ProductViewModel", "Error al configurar listener", e)
            _error.value = "Error al configurar la escucha de productos"
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

                db.collection("products").add(productMap).await()
                Log.d("ProductViewModel", "Producto añadido exitosamente")
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
            movimientosListener?.remove() // Remover listener anterior

            movimientosListener = db.collection("movimientos")
                .whereEqualTo("loteId", productId)
                .orderBy("fecha", Query.Direction.DESCENDING)
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

    fun loadNextPage() {
        if (allProductsLoaded || _isLoading.value) return

        viewModelScope.launch {
            try {
                _isLoading.value = true

                var query = db.collection("products")
                    .orderBy("codigo")
                    .limit(10)

                if (lastVisibleDoc != null) {
                    query = query.startAfter(lastVisibleDoc!!)
                }

                val snapshot = query.get().await()

                if (snapshot.isEmpty) {
                    allProductsLoaded = true
                    return@launch
                }

                lastVisibleDoc = snapshot.documents.last()
                val newProducts = snapshot.documents.mapNotNull { doc ->
                    try {
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
                        Log.e("ProductViewModel", "Error al parsear producto en paginación", e)
                        null
                    }
                }

                val currentList = _products.value.toMutableList()
                val updatedList = (currentList + newProducts).distinctBy { it.id }
                _products.value = updatedList

            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error al cargar la siguiente página", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Función para refrescar manualmente
    fun syncData() {
        setupProductsListener() // Reinicia el listener
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