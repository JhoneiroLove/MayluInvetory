package com.jhone.app_inventory.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Movimiento(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("loteId") @set:PropertyName("loteId")
    var loteId: String = "",

    @get:PropertyName("tipo") @set:PropertyName("tipo")
    var tipo: String = "",

    @get:PropertyName("cantidad") @set:PropertyName("cantidad")
    var cantidad: Int = 0,

    @get:PropertyName("fecha") @set:PropertyName("fecha")
    var fecha: Timestamp = Timestamp.now(),

    @get:PropertyName("usuario") @set:PropertyName("usuario")
    var usuario: String = "",

    @get:PropertyName("observacion") @set:PropertyName("observacion")
    var observacion: String = ""
) {
    constructor() : this("", "", "", 0, Timestamp.now(), "", "")
}