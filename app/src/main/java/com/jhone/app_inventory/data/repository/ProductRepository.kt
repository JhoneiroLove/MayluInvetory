package com.jhone.app_inventory.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.jhone.app_inventory.data.Product
import com.jhone.app_inventory.data.local.ProductDao
import com.jhone.app_inventory.data.local.toEntity
import com.jhone.app_inventory.data.local.toProduct
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val productDao: ProductDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    companion object {
        private const val SYNC_THRESHOLD_HOURS = 6
    }

    // FLUJO PRINCIPAL - Lee desde caché local
    fun getProductsFlow(): Flow<List<Product>> {
        return productDao.getAllProductsFlow()
            .map { entities -> entities.map { it.toProduct() } }
    }

    // BÚSQUEDA LOCAL INSTANTÁNEA
    suspend fun searchProductsLocal(query: String): List<Product> {
        return try {
            if (query.isBlank()) {
                productDao.getProductsPaginated(50, 0).map { it.toProduct() }
            } else {
                val cleanQuery = query.trim()
                val searchResults = productDao.searchProducts(cleanQuery)
                val products = searchResults.map { it.toProduct() }

                // Ordenar por relevancia
                products.sortedWith(compareBy<Product> { product ->
                    when {
                        product.codigo.startsWith(cleanQuery, ignoreCase = true) -> 1
                        product.descripcion.startsWith(cleanQuery, ignoreCase = true) -> 2
                        product.codigo.contains(cleanQuery, ignoreCase = true) -> 3
                        product.descripcion.contains(cleanQuery, ignoreCase = true) -> 4
                        product.proveedor.contains(cleanQuery, ignoreCase = true) -> 5
                        else -> 6
                    }
                }.thenBy { it.codigo })
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // SINCRONIZACIÓN INICIAL
    suspend fun initializeCache(): Result<Unit> {
        return try {
            val localCount = productDao.getProductCount()

            if (localCount == 0) {
                syncFromFirebase()
            } else {
                syncIfNeeded()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // SINCRONIZACIÓN COMPLETA DESDE FIREBASE
    private suspend fun syncFromFirebase() {
        try {
            val query = firestore.collection("products")
                .orderBy("codigo")
                .limit(1000)

            val snapshot = query.get().await()

            val products = snapshot.documents.mapNotNull { doc ->
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
                        porcentaje = doc.getDouble("porcentaje") ?: 0.0,
                        createdBy = doc.getString("createdBy") ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            }

            // Guardar en caché local
            productDao.clearAll()
            productDao.insertProducts(products.map { it.toEntity() })

        } catch (e: Exception) {
            throw e
        }
    }

    // BÚSQUEDA DE PRODUCTO ESPECÍFICO
    suspend fun findSpecificProduct(codigo: String): Product? {
        return try {
            // Primero buscar en caché local
            val localProduct = productDao.searchProducts(codigo).firstOrNull()
            if (localProduct != null) {
                return localProduct.toProduct()
            }

            // Si no está en caché, buscar directamente en Firebase
            val snapshot = firestore.collection("products")
                .whereEqualTo("codigo", codigo)
                .limit(1)
                .get()
                .await()

            if (snapshot.documents.isNotEmpty()) {
                val doc = snapshot.documents.first()
                val product = Product(
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

                // Guardar en caché para futuras búsquedas
                productDao.insertProduct(product.toEntity())
                return product
            }

            null
        } catch (e: Exception) {
            null
        }
    }

    // SINCRONIZACIÓN CONDICIONAL
    private suspend fun syncIfNeeded() {
        try {
            val sixHoursAgo = System.currentTimeMillis() - (SYNC_THRESHOLD_HOURS * 60 * 60 * 1000)
            val oldProducts = productDao.getProductsOlderThan(sixHoursAgo)

            if (oldProducts.isNotEmpty()) {
                oldProducts.forEach {
                    productDao.updateSyncTimestamp(it.id, System.currentTimeMillis())
                }
            }
        } catch (e: Exception) {
        }
    }

    // AGREGAR PRODUCTO
    suspend fun addProduct(product: Product): Result<Product> {
        return try {
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
                "createdBy" to currentUserEmail
            )

            val docRef = firestore.collection("products").add(productMap).await()

            val newProduct = product.copy(
                id = docRef.id,
                createdBy = currentUserEmail
            )

            // Guardar en caché local inmediatamente
            productDao.insertProduct(newProduct.toEntity())

            Result.success(newProduct)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ACTUALIZAR PRODUCTO
    suspend fun updateProduct(product: Product): Result<Product> {
        return try {
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

            firestore.collection("products")
                .document(product.id)
                .update(productMap)
                .await()

            // Actualizar caché local
            productDao.updateProduct(product.toEntity())

            Result.success(product)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ELIMINAR PRODUCTO
    suspend fun deleteProduct(productId: String): Result<Unit> {
        return try {
            firestore.collection("products")
                .document(productId)
                .delete()
                .await()

            // Eliminar de caché local
            productDao.deleteProductById(productId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ACTUALIZAR CANTIDAD
    suspend fun updateProductQuantity(productId: String, newQuantity: Int): Result<Unit> {
        return try {
            firestore.collection("products")
                .document(productId)
                .update("cantidad", newQuantity)
                .await()

            // Actualizar caché local
            productDao.updateProductQuantity(productId, newQuantity)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // BÚSQUEDA REMOTA
    suspend fun searchProductsRemote(query: String): List<Product> {
        return try {
            val cleanQuery = query.trim()
            val searchResults = mutableListOf<Product>()

            // Búsqueda por código
            val codeQuery = firestore.collection("products")
                .orderBy("codigo")
                .startAt(cleanQuery.uppercase())
                .endAt(cleanQuery.uppercase() + "\uf8ff")
                .limit(10)

            val codeResults = codeQuery.get().await()
            searchResults.addAll(
                codeResults.documents.mapNotNull { doc ->
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
                            porcentaje = doc.getDouble("porcentaje") ?: 0.0,
                            createdBy = doc.getString("createdBy") ?: ""
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            )

            searchResults.take(20)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // FORZAR SINCRONIZACIÓN
    suspend fun forceSync(): Result<Unit> {
        return try {
            syncFromFirebase()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}