package com.jhone.app_inventory.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jhone.app_inventory.data.Product
import com.google.firebase.Timestamp

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey
    val id: String,
    val codigo: String,
    val descripcion: String,
    val cantidad: Int,
    val precioProducto: Double,
    val precioBoleta: Double,
    val precioCosto: Double,
    val proveedor: String,
    val createdAt: Long?, // Timestamp como Long para Room
    val fechaVencimiento: Long?, // Timestamp como Long para Room
    val porcentaje: Double,
    val createdBy: String,
    val lastSyncAt: Long = System.currentTimeMillis() // Para tracking de sincronización
)

// Extensiones para conversión entre Product y ProductEntity
fun Product.toEntity(): ProductEntity = ProductEntity(
    id = id,
    codigo = codigo,
    descripcion = descripcion,
    cantidad = cantidad,
    precioProducto = precioProducto,
    precioBoleta = precioBoleta,
    precioCosto = precioCosto,
    proveedor = proveedor,
    createdAt = createdAt?.seconds,
    fechaVencimiento = fechaVencimiento?.seconds,
    porcentaje = porcentaje,
    createdBy = createdBy
)

fun ProductEntity.toProduct(): Product = Product(
    id = id,
    codigo = codigo,
    descripcion = descripcion,
    cantidad = cantidad,
    precioProducto = precioProducto,
    precioBoleta = precioBoleta,
    precioCosto = precioCosto,
    proveedor = proveedor,
    createdAt = createdAt?.let { Timestamp(it, 0) },
    fechaVencimiento = fechaVencimiento?.let { Timestamp(it, 0) },
    porcentaje = porcentaje,
    createdBy = createdBy
)