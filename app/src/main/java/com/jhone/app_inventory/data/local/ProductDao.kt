package com.jhone.app_inventory.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM products ORDER BY codigo ASC")
    fun getAllProductsFlow(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY codigo ASC LIMIT :limit OFFSET :offset")
    suspend fun getProductsPaginated(limit: Int, offset: Int): List<ProductEntity>

    @Query("""
        SELECT * FROM products 
        WHERE LOWER(codigo) LIKE '%' || LOWER(:query) || '%' 
           OR LOWER(descripcion) LIKE '%' || LOWER(:query) || '%' 
           OR LOWER(proveedor) LIKE '%' || LOWER(:query) || '%'
        ORDER BY 
            CASE 
                WHEN LOWER(codigo) = LOWER(:query) THEN 1
                WHEN LOWER(codigo) LIKE LOWER(:query) || '%' THEN 2
                WHEN LOWER(descripcion) LIKE LOWER(:query) || '%' THEN 3
                WHEN LOWER(codigo) LIKE '%' || LOWER(:query) || '%' THEN 4
                WHEN LOWER(descripcion) LIKE '%' || LOWER(:query) || '%' THEN 5
                WHEN LOWER(proveedor) LIKE '%' || LOWER(:query) || '%' THEN 6
                ELSE 7 
            END,
            codigo ASC
        LIMIT 50
    """)
    suspend fun searchProducts(query: String): List<ProductEntity>

    // BÚSQUEDA POR MÚLTIPLES PALABRAS
    @Query("""
        SELECT * FROM products 
        WHERE (:word1 = '' OR LOWER(codigo || ' ' || descripcion || ' ' || proveedor) LIKE '%' || LOWER(:word1) || '%')
          AND (:word2 = '' OR LOWER(codigo || ' ' || descripcion || ' ' || proveedor) LIKE '%' || LOWER(:word2) || '%')
          AND (:word3 = '' OR LOWER(codigo || ' ' || descripcion || ' ' || proveedor) LIKE '%' || LOWER(:word3) || '%')
          AND (:word4 = '' OR LOWER(codigo || ' ' || descripcion || ' ' || proveedor) LIKE '%' || LOWER(:word4) || '%')
          AND (:word5 = '' OR LOWER(codigo || ' ' || descripcion || ' ' || proveedor) LIKE '%' || LOWER(:word5) || '%')
        ORDER BY 
            CASE 
                WHEN LOWER(codigo) LIKE LOWER(:word1) || '%' THEN 1
                WHEN LOWER(descripcion) LIKE LOWER(:word1) || '%' THEN 2
                ELSE 3
            END,
            codigo ASC
        LIMIT 100
    """)
    suspend fun searchProductsByWords(
        word1: String,
        word2: String = "",
        word3: String = "",
        word4: String = "",
        word5: String = ""
    ): List<ProductEntity>

    // BÚSQUEDA SIMPLE PARA CONSULTAS LARGAS
    @Query("""
        SELECT * FROM products 
        WHERE LOWER(codigo || ' ' || descripcion || ' ' || proveedor) LIKE '%' || LOWER(:query) || '%'
        ORDER BY 
            LENGTH(descripcion) ASC,
            codigo ASC
        LIMIT 50
    """)
    suspend fun searchProductsSimple(query: String): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: String): ProductEntity?

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getProductCount(): Int

    // OPERACIONES DE CACHÉ
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("UPDATE products SET cantidad = :newQuantity WHERE id = :productId")
    suspend fun updateProductQuantity(productId: String, newQuantity: Int)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :productId")
    suspend fun deleteProductById(productId: String)

    @Query("DELETE FROM products")
    suspend fun clearAll()

    // SINCRONIZACIÓN INTELIGENTE
    @Query("SELECT * FROM products WHERE lastSyncAt < :timestamp")
    suspend fun getProductsOlderThan(timestamp: Long): List<ProductEntity>

    @Query("UPDATE products SET lastSyncAt = :timestamp WHERE id = :productId")
    suspend fun updateSyncTimestamp(productId: String, timestamp: Long)

    // UTILIDADES
    @Query("SELECT EXISTS(SELECT 1 FROM products WHERE codigo = :codigo)")
    suspend fun existsByCode(codigo: String): Boolean

    @Query("SELECT * FROM products WHERE cantidad <= 5 ORDER BY cantidad ASC")
    suspend fun getLowStockProducts(): List<ProductEntity>
}