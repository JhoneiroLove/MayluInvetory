package com.jhone.app_inventory.utils

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

object DateUtils {

    /**
     * Convierte una fecha con formato flexible a Timestamp de Firebase
     * @param dateStr String con formato flexible
     * @return Timestamp o null si la conversión falla
     */
    fun parseDate(dateStr: String): Timestamp? {
        return try {
            if (dateStr.isBlank()) return null

            // Usar el normalizador del DateValidator
            val normalizedDate = com.jhone.app_inventory.ui.components.DateValidator.normalizeDate(dateStr)
            if (normalizedDate == null) return null

            val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            format.isLenient = false
            val date = format.parse(normalizedDate)
            if (date != null) Timestamp(date) else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Convierte un Timestamp a String con formato "dd/MM/yyyy"
     * @param timestamp Timestamp de Firebase
     * @return String con formato "dd/MM/yyyy" o cadena vacía si es null
     */
    fun formatDate(timestamp: Timestamp?): String {
        return try {
            if (timestamp == null) return ""
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            format.format(timestamp.toDate())
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Convierte un Timestamp a String con formato "dd/MM/yyyy HH:mm"
     * @param timestamp Timestamp de Firebase
     * @return String con formato "dd/MM/yyyy HH:mm" o cadena vacía si es null
     */
    fun formatDateTime(timestamp: Timestamp?): String {
        return try {
            if (timestamp == null) return ""
            val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            format.format(timestamp.toDate())
        } catch (e: Exception) {
            ""
        }
    }
}