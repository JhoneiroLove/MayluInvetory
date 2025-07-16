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
                WHEN LOWER(codigo) LIKE LOWER(:query) || '%' THEN 1
                WHEN LOWER(descripcion) LIKE LOWER(:query) || '%' THEN 2
                WHEN LOWER(codigo) LIKE '%' || LOWER(:query) || '%' THEN 3
                WHEN LOWER(descripcion) LIKE '%' || LOWER(:query) || '%' THEN 4
                WHEN LOWER(proveedor) LIKE '%' || LOWER(:query) || '%' THEN 5
                ELSE 6 
            END,
            codigo ASC
        LIMIT 50
    """)
    suspend fun searchProducts(query: String): List<ProductEntity>

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