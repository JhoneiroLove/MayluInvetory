package com.jhone.app_inventory.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.jhone.app_inventory.data.Movimiento
import com.jhone.app_inventory.data.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    private val pageSize = 20L

    private var lastVisibleDoc: DocumentSnapshot? = null
    private var allProductsLoaded = false

    init {
        listenToProductsChanges()
    }

    private fun listenToProductsChanges() {
        db.collection("products")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ProductViewModel", "Error al escuchar cambios", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    _products.value = snapshot.documents.map { doc ->
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
                    }
                } else {
                    _products.value = emptyList()
                }
            }
    }

    fun loadProductsFromFirestore() {
        db.collection("products")
            .orderBy("codigo")
            .limit(20)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null) {
                    _products.value = snapshot.documents.map { doc ->
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
                    }
                    if (snapshot.documents.isNotEmpty()) {
                        lastVisibleDoc = snapshot.documents.last()
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProductViewModel", "Error al cargar productos", e)
            }
    }

    // Agregar un producto
    fun addProduct(product: Product, onComplete: () -> Unit) {
        // Convertir el objeto a un Map para subir a Firestore
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
            .add(productMap)
            .addOnSuccessListener { documentRef ->
                Log.d("ProductViewModel", "Producto añadido: ${documentRef.id}")
                loadProductsFromFirestore()
                onComplete()
            }
            .addOnFailureListener {
                Log.e("ProductViewModel", "Error al añadir producto", it)
            }
    }

    fun updateProduct(product: Product, onComplete: () -> Unit) {
        // Preparamos el Map con los datos actualizados
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
            .addOnSuccessListener {
                Log.d("ProductViewModel", "Producto actualizado: ${product.id}")
                loadProductsFromFirestore()
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.e("ProductViewModel", "Error al actualizar producto", e)
            }
    }

    fun deleteProduct(product: Product, onComplete: () -> Unit) {
        db.collection("products")
            .document(product.id)
            .delete()
            .addOnSuccessListener {
                Log.d("ProductViewModel", "Producto eliminado: ${product.id}")
                loadProductsFromFirestore()
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.e("ProductViewModel", "Error al eliminar producto", e)
            }
    }

    fun addMovimiento(movimiento: Movimiento, onComplete: () -> Unit) {
        val currentUserEmail = auth.currentUser?.email ?: "UsuarioDesconocido" // Sin hardcode

        // Referencia al documento del producto
        val productRef = db.collection("products")
            .document(movimiento.loteId) // loteId almacenará el product.id

        // Referencia para crear el movimiento (con id automático)
        val movimientoRef = db.collection("movimientos").document()

        // Ejecutamos una transacción
        db.runTransaction { transaction ->
            // 1) Leemos el producto
            val productSnapshot = transaction.get(productRef)
            val currentQuantity = productSnapshot.getLong("cantidad") ?: 0L

            // 2) Calculamos la nueva cantidad
            val newQuantity = if (movimiento.tipo == "ingreso") {
                currentQuantity + movimiento.cantidad
            } else {
                currentQuantity - movimiento.cantidad
            }
            if (newQuantity < 0) {
                throw Exception("No hay stock suficiente para realizar la salida.")
            }

            // 3) Actualizamos la cantidad del producto
            transaction.update(productRef, "cantidad", newQuantity)

            // 4) Registramos el movimiento, actualizando el campo 'usuario' con el email real
            val movimientoConUsuarioReal = movimiento.copy(
                id = movimientoRef.id,
                usuario = currentUserEmail
            )
            transaction.set(movimientoRef, movimientoConUsuarioReal)

            null // Las transacciones deben retornar algo
        }.addOnSuccessListener {
            // Refrescamos la lista local y avisamos que completó
            loadProductsFromFirestore()
            onComplete()
        }.addOnFailureListener { e ->
            Log.e("ProductViewModel", "Error en la transacción de movimiento", e)
        }
    }

    fun listenToMovimientosForProduct(productId: String) {
        db.collection("movimientos")
            .whereEqualTo("loteId", productId)
            .orderBy("fecha", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot: com.google.firebase.firestore.QuerySnapshot?, e: com.google.firebase.firestore.FirebaseFirestoreException? ->
                if (e != null) {
                    Log.e("ProductViewModel", "Error al escuchar movimientos", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    _movimientos.value = snapshot.documents.map { doc ->
                        Movimiento(
                            id = doc.id,
                            loteId = doc.getString("loteId") ?: "",
                            tipo = doc.getString("tipo") ?: "",
                            cantidad = doc.getLong("cantidad")?.toInt() ?: 0,
                            fecha = doc.getTimestamp("fecha") ?: com.google.firebase.Timestamp.now(),
                            usuario = doc.getString("usuario") ?: "",
                            observacion = doc.getString("observacion") ?: ""
                        )
                    }
                } else {
                    _movimientos.value = emptyList()
                }
            }
    }

    fun loadNextPage() {
        if (allProductsLoaded) return

        // Usamos la colección global "products" (ya no "users/{uid}/products")
        var query = db.collection("products") // NUEVO: Colección global
            .orderBy("codigo") // Ordena por "codigo" (puedes usar otro campo si prefieres)
            .limit(10)         // Número de productos por página

        if (lastVisibleDoc != null) {
            query = query.startAfter(lastVisibleDoc!!)
        }

        query.get().addOnSuccessListener { snapshot ->
            if (snapshot.isEmpty) {
                allProductsLoaded = true
                return@addOnSuccessListener
            }

            lastVisibleDoc = snapshot.documents.last()
            val newProducts = snapshot.documents.map { doc ->
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
            }
            val currentList = _products.value.toMutableList()
            _products.value = (currentList + newProducts).distinctBy { it.id }
        }.addOnFailureListener { e ->
            Log.e("ProductViewModel", "Error al cargar la siguiente página", e)
        }
    }

    // Sincronizar datos manualmente
    fun syncData() {
        loadProductsFromFirestore()
    }
}