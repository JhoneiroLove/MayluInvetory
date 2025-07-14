package com.jhone.app_inventory.data

import com.google.firebase.Timestamp

data class Movimiento(
    val id: String = "",         // ID asignado para el movimiento
    val loteId: String = "",     // ID del lote relacionado
    val tipo: String = "",       // Tipo de movimiento: "ingreso" o "salida"
    val cantidad: Int = 0,       // Cantidad ingresada o retirada
    val fecha: Timestamp = Timestamp.now(), // Fecha y hora del movimiento
    val usuario: String = "",    // Usuario que realizó la operación
    val observacion: String = "" // Notas o detalles adicionales
) {
    constructor() : this("", "", "", 0, Timestamp.now(), "", "")
}