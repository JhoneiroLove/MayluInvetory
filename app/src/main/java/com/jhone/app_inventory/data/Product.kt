package com.jhone.app_inventory.data

import com.google.firebase.Timestamp

data class Product(
    val id: String = "",             // ID asignado por Firestore
    val codigo: String = "",         // Código del producto
    val descripcion: String = "",    // Descripción del producto
    val cantidad: Int = 0,           // Cantidad en stock
    val precioProducto: Double = 0.0, // Precio de lista o "precio producto", precioCosto * (1 + porcentaje/100)
    val precioBoleta: Double = 0.0,   // Precio que se mostrará en la boleta
    val precioCosto: Double = 0.0,    // Precio costo calculado automáticamente (precioBoleta + 2%)
    val proveedor: String = "",      // Proveedor del producto
    val createdAt: Timestamp? = null, // Fecha de creación del registro
    val fechaVencimiento: Timestamp? = null, // Fecha de vencimiento del producto
    val porcentaje: Double = 0.0,     // Porcentaje para precio producto
    val createdBy: String = ""        // Email del usuario que creó el producto
) {
    constructor() : this("", "", "", 0, 0.0, 0.0, 0.0, "", null, null, 0.0, "")
}