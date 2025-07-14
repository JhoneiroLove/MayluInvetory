package com.jhone.app_inventory.utils

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

object DateUtils {

    /**
     * Convierte una fecha en formato "dd/MM/yyyy" a Timestamp de Firebase
     * @param dateStr String con formato "dd/MM/yyyy"
     * @return Timestamp o null si la conversión falla
     */
    fun parseDate(dateStr: String): Timestamp? {
        return try {
            if (dateStr.isBlank()) return null
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = format.parse(dateStr)
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