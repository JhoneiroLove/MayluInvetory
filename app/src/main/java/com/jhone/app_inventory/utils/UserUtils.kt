package com.jhone.app_inventory.utils

object UserUtils {

    /**
     * Convierte el email del usuario a un nombre amigable para mostrar
     * @param createdByEmail Email del usuario que creó el producto
     * @param currentUserEmail Email del usuario actual (quien está viendo)
     * @return String amigable: "Yo", nombre del usuario, o "Usuario Desconocido"
     */
    fun getDisplayName(createdByEmail: String, currentUserEmail: String?): String {
        return when {
            createdByEmail.isEmpty() || createdByEmail == "Usuario Desconocido" -> {
                "Sistema" // Para productos antiguos sin creador
            }
            createdByEmail == currentUserEmail -> {
                "Yo" // Si el usuario actual creó el producto
            }
            else -> {
                // Extraer nombre del email (antes del @)
                val userName = createdByEmail.substringBefore("@")
                    .replace(".", " ")
                    .split(" ")
                    .joinToString(" ") { word ->
                        word.replaceFirstChar { it.uppercaseChar() }
                    }
                userName.ifEmpty { "Usuario Desconocido" }
            }
        }
    }

    /**
     * Obtiene un color para el badge del usuario
     * @param createdByEmail Email del usuario que creó
     * @param currentUserEmail Email del usuario actual
     * @return Color para el badge
     */
    fun getUserBadgeColor(createdByEmail: String, currentUserEmail: String?): androidx.compose.ui.graphics.Color {
        return when {
            createdByEmail == currentUserEmail -> {
                androidx.compose.ui.graphics.Color(0xFF4CAF50) // Verde para "Yo"
            }
            createdByEmail.isEmpty() || createdByEmail == "Usuario Desconocido" -> {
                androidx.compose.ui.graphics.Color(0xFF9E9E9E) // Gris para sistema
            }
            else -> {
                androidx.compose.ui.graphics.Color(0xFF2196F3) // Azul para otros usuarios
            }
        }
    }
}