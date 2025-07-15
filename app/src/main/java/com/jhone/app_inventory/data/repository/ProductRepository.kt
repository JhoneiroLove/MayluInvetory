package com.jhone.app_inventory.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jhone.app_inventory.data.Product
import com.jhone.app_inventory.data.local.ProductDao
import com.jhone.app_inventory.data.local.ProductEntity
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
        private const val TAG = "ProductRepository"
        private const val PAGE_SIZE = 20L
        private const val SYNC_THRESHOLD_HOURS = 6 // Sincronizar cada 6 horas
    }

    // FLUJO PRINCIPAL - Lee desde caché local
    fun getProductsFlow(): Flow<List<Product>> {
        return productDao.getAllProductsFlow()
            .map { entities -> entities.map { it.toProduct() } }
    }

    // BÚSQUEDA LOCAL INSTANTÁNEA (0 lecturas Firebase)
    suspend fun searchProductsLocal(query: String): List<Product> {
        return try {
            if (query.isBlank()) {
                productDao.getProductsPaginated(50, 0).map { it.toProduct() }
            } else {
                productDao.searchProducts(query.trim()).map { it.toProduct() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en búsqueda local", e)
            emptyList()
        }
    }

    // SINCRONIZACIÓN INICIAL (solo si caché vacío)
    suspend fun initializeCache(): Result<Unit> {
        return try {
            val localCount = productDao.getProductCount()
            Log.d(TAG, "Productos en caché: $localCount")

            if (localCount == 0) {
                Log.d(TAG, "Caché vacío, sincronizando desde Firebase...")
                syncFromFirebase()
            } else {
                Log.d(TAG, "Usando caché existente")
                syncIfNeeded()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error inicializando caché", e)
            Result.failure(e)
        }
    }

    // SINCRONIZACIÓN COMPLETA DESDE FIREBASE
    private suspend fun syncFromFirebase() {
        try {
            Log.d(TAG, "Iniciando sincronización completa...")

            val query = firestore.collection("products")
                .orderBy("codigo")
                .limit(200) // Limitar para evitar muchas lecturas

            val snapshot = query.get().await()
            Log.d(TAG, "Obtenidos ${snapshot.documents.size} productos de Firebase")

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
                    Log.e(TAG, "Error parseando producto ${doc.id}", e)
                    null
                }
            }

            // Guardar en caché local
            productDao.clearAll()
            productDao.insertProducts(products.map { it.toEntity() })

            Log.d(TAG, "Sincronización completa exitosa: ${products.size} productos")
        } catch (e: Exception) {
            Log.e(TAG, "Error en sincronización completa", e)
            throw e
        }
    }

    // SINCRONIZACIÓN CONDICIONAL (solo si es necesario)
    private suspend fun syncIfNeeded() {
        try {
            val sixHoursAgo = System.currentTimeMillis() - (SYNC_THRESHOLD_HOURS * 60 * 60 * 1000)
            val oldProducts = productDao.getProductsOlderThan(sixHoursAgo)

            if (oldProducts.isNotEmpty()) {
                Log.d(TAG, "Sincronizando ${oldProducts.size} productos antiguos...")
                // Aquí podrías implementar sincronización selectiva
                // Por ahora, solo actualizar timestamp
                oldProducts.forEach {
                    productDao.updateSyncTimestamp(it.id, System.currentTimeMillis())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en sincronización condicional", e)
        }
    }

    // AGREGAR PRODUCTO (Firebase + Caché)
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

            Log.d(TAG, "Producto agregado: ${docRef.id}")
            Result.success(newProduct)

        } catch (e: Exception) {
            Log.e(TAG, "Error agregando producto", e)
            Result.failure(e)
        }
    }

    // ACTUALIZAR PRODUCTO (Firebase + Caché)
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

            Log.d(TAG, "Producto actualizado: ${product.id}")
            Result.success(product)

        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando producto", e)
            Result.failure(e)
        }
    }

    // ELIMINAR PRODUCTO (Firebase + Caché)
    suspend fun deleteProduct(productId: String): Result<Unit> {
        return try {
            firestore.collection("products")
                .document(productId)
                .delete()
                .await()

            // Eliminar de caché local
            productDao.deleteProductById(productId)

            Log.d(TAG, "Producto eliminado: $productId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando producto", e)
            Result.failure(e)
        }
    }

    // ACTUALIZAR CANTIDAD (para movimientos de stock)
    suspend fun updateProductQuantity(productId: String, newQuantity: Int): Result<Unit> {
        return try {
            firestore.collection("products")
                .document(productId)
                .update("cantidad", newQuantity)
                .await()

            // Actualizar caché local
            productDao.updateProductQuantity(productId, newQuantity)

            Log.d(TAG, "Cantidad actualizada para producto: $productId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando cantidad", e)
            Result.failure(e)
        }
    }

    // BÚSQUEDA REMOTA (solo si no encuentra local)
    suspend fun searchProductsRemote(query: String): List<Product> {
        return try {
            Log.d(TAG, "Búsqueda remota: $query")

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

            Log.d(TAG, "Búsqueda remota completada: ${searchResults.size} resultados")
            searchResults.take(20)

        } catch (e: Exception) {
            Log.e(TAG, "Error en búsqueda remota", e)
            emptyList()
        }
    }

    // FORZAR SINCRONIZACIÓN COMPLETA
    suspend fun forceSync(): Result<Unit> {
        return try {
            syncFromFirebase()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}