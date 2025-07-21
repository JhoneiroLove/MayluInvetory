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

                // Intentar búsqueda directa optimizada
                val directResults = productDao.searchProducts(cleanQuery)

                // Si no hay resultados o pocos, intentar búsqueda por palabras
                val finalResults = if (directResults.size < 3) {
                    val wordResults = searchByMultipleWords(cleanQuery)
                    if (wordResults.isNotEmpty()) {
                        // Combinar resultados sin duplicados
                        val combined = (directResults + wordResults).distinctBy { it.id }
                        combined
                    } else {
                        directResults
                    }
                } else {
                    directResults
                }

                val products = finalResults.map { it.toProduct() }

                // Ordenar por relevancia mejorada
                products.sortedWith(compareBy<Product> { product ->
                    calculateRelevanceScore(product, cleanQuery)
                }.thenBy { it.codigo })
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Búsqueda por múltiples palabras
    private suspend fun searchByMultipleWords(query: String): List<com.jhone.app_inventory.data.local.ProductEntity> {
        val words = query.lowercase().split(" ").filter { it.isNotBlank() }

        return when (words.size) {
            1 -> productDao.searchProductsSimple(words[0])
            2 -> productDao.searchProductsByWords(words[0], words[1])
            3 -> productDao.searchProductsByWords(words[0], words[1], words[2])
            4 -> productDao.searchProductsByWords(words[0], words[1], words[2], words[3])
            else -> productDao.searchProductsByWords(
                words.getOrNull(0) ?: "",
                words.getOrNull(1) ?: "",
                words.getOrNull(2) ?: "",
                words.getOrNull(3) ?: "",
                words.getOrNull(4) ?: ""
            )
        }
    }

    // Función para calcular puntuación de relevancia
    private fun calculateRelevanceScore(product: Product, query: String): Int {
        val queryLower = query.lowercase()
        val queryWords = queryLower.split(" ").filter { it.isNotBlank() }
        val codigoLower = product.codigo.lowercase()
        val descripcionLower = product.descripcion.lowercase()
        val proveedorLower = product.proveedor.lowercase()
        val allText = "$codigoLower $descripcionLower $proveedorLower"

        return when {
            // Coincidencia exacta en código
            codigoLower == queryLower -> 1

            // Código empieza con la consulta
            codigoLower.startsWith(queryLower) -> 2

            // Descripción empieza con la consulta
            descripcionLower.startsWith(queryLower) -> 3

            // Código contiene la consulta
            codigoLower.contains(queryLower) -> 4

            // Todas las palabras están en la descripción
            queryWords.all { word -> descripcionLower.contains(word) } -> 5

            // Descripción contiene la consulta completa
            descripcionLower.contains(queryLower) -> 6

            // Todas las palabras están en todo el texto
            queryWords.all { word -> allText.contains(word) } -> 7

            // Proveedor contiene la consulta
            proveedorLower.contains(queryLower) -> 8

            // Al menos la mitad de las palabras están presentes
            queryWords.count { word -> allText.contains(word) } >= (queryWords.size / 2.0) -> 9

            // Al menos una palabra está presente
            queryWords.any { word -> allText.contains(word) } -> 10

            else -> 11
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
            val words = cleanQuery.lowercase().split(" ").filter { it.isNotBlank() }

            // Búsqueda por código exacto
            val codeQuery = firestore.collection("products")
                .whereEqualTo("codigo", cleanQuery.uppercase())
                .limit(5)

            val codeResults = codeQuery.get().await()
            searchResults.addAll(
                codeResults.documents.mapNotNull { doc -> parseFirebaseProduct(doc) }
            )

            // Búsqueda por código que empiece con la consulta
            if (searchResults.isEmpty()) {
                val codeStartQuery = firestore.collection("products")
                    .orderBy("codigo")
                    .startAt(cleanQuery.uppercase())
                    .endAt(cleanQuery.uppercase() + "\uf8ff")
                    .limit(10)

                val codeStartResults = codeStartQuery.get().await()
                searchResults.addAll(
                    codeStartResults.documents.mapNotNull { doc -> parseFirebaseProduct(doc) }
                )
            }

            // Búsqueda por descripción
            if (searchResults.size < 5) {
                // Buscar por la primera palabra más significativa
                val mainWord = words.maxByOrNull { it.length } ?: words.firstOrNull()
                if (mainWord != null && mainWord.length >= 3) {
                    val descQuery = firestore.collection("products")
                        .orderBy("descripcion")
                        .startAt(mainWord.replaceFirstChar { it.uppercase() })
                        .endAt(mainWord.replaceFirstChar { it.uppercase() } + "\uf8ff")
                        .limit(15)

                    val descResults = descQuery.get().await()
                    val filteredDescResults = descResults.documents.mapNotNull { doc ->
                        val product = parseFirebaseProduct(doc)
                        // Filtrar solo productos que contengan al menos 2 palabras de la búsqueda
                        if (product != null) {
                            val matchCount = words.count { word ->
                                product.descripcion.contains(word, ignoreCase = true) ||
                                        product.codigo.contains(word, ignoreCase = true) ||
                                        product.proveedor.contains(word, ignoreCase = true)
                            }
                            if (matchCount >= 2 || words.size == 1) product else null
                        } else null
                    }

                    filteredDescResults.forEach { product ->
                        if (searchResults.none { it.id == product.id }) {
                            searchResults.add(product)
                        }
                    }
                }
            }

            // Búsqueda manual en muestra si aún no hay resultados
            if (searchResults.isEmpty()) {
                val sampleQuery = firestore.collection("products")
                    .limit(200) // Aumentar muestra

                val sampleResults = sampleQuery.get().await()
                val manualMatches = sampleResults.documents.mapNotNull { doc ->
                    val product = parseFirebaseProduct(doc)
                    if (product != null) {
                        val allText = "${product.codigo} ${product.descripcion} ${product.proveedor}".lowercase()
                        val matchCount = words.count { word -> allText.contains(word) }

                        // Requerir al menos la mitad de las palabras para búsquedas largas
                        val requiredMatches = if (words.size > 3) (words.size / 2.0).toInt() else 1

                        if (matchCount >= requiredMatches) product else null
                    } else null
                }

                searchResults.addAll(manualMatches.take(10))
            }

            // Ordenar por relevancia
            searchResults.sortedWith(compareBy<Product> { product ->
                calculateRelevanceScore(product, cleanQuery)
            }.thenBy { it.codigo }).take(20)

        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseFirebaseProduct(doc: com.google.firebase.firestore.DocumentSnapshot): Product? {
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
            null
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

    // AGREGAR PRODUCTO AL CACHÉ
    suspend fun addProductToCache(product: Product) {
        try {
            productDao.insertProduct(product.toEntity())
        } catch (e: Exception) {
            // Silenciar errores de caché
        }
    }
}