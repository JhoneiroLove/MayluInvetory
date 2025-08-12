package com.jhone.app_inventory.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Utilidades para manejar sincronización en tiempo real
 * y prevenir duplicados/inconsistencias
 */
object SyncUtils {

    private val isGlobalSyncRunning = AtomicBoolean(false)
    private val activeListeners = mutableMapOf<String, ListenerRegistration>()

    /**
     * Verifica si un producto tiene movimientos pendientes
     * antes de permitir nuevos movimientos
     */
    suspend fun checkPendingMovements(productId: String, db: FirebaseFirestore): Boolean {
        return try {
            val pendingQuery = db.collection("movimientos")
                .whereEqualTo("loteId", productId)
                .whereEqualTo("status", "pending") // Si implementas estado de movimientos
                .limit(1)
                .get()
                .await()

            pendingQuery.isEmpty
        } catch (e: Exception) {
            Log.e("SyncUtils", "Error verificando movimientos pendientes", e)
            true // Permitir si no se puede verificar
        }
    }

    /**
     * Sincronización global con bloqueo para evitar múltiples sincronizaciones
     */
    suspend fun performGlobalSync(
        db: FirebaseFirestore,
        onProgress: (String) -> Unit = {},
        onComplete: (Boolean, String?) -> Unit
    ) {
        if (!isGlobalSyncRunning.compareAndSet(false, true)) {
            onComplete(false, "Sincronización ya en progreso")
            return
        }

        try {
            onProgress("Iniciando sincronización...")

            // 1. Verificar conectividad
            onProgress("Verificando conectividad...")
            val testQuery = db.collection("products").limit(1).get().await()

            // 2. Limpiar listeners activos
            onProgress("Limpiando listeners...")
            cleanupAllListeners()

            // 3. Forzar caché
            onProgress("Actualizando caché...")
            db.clearPersistence() // Limpiar caché local de Firestore

            onProgress("Sincronización completada")
            onComplete(true, null)

        } catch (e: Exception) {
            Log.e("SyncUtils", "Error en sincronización global", e)
            onComplete(false, "Error: ${e.message}")
        } finally {
            isGlobalSyncRunning.set(false)
        }
    }

    /**
     * Registra un listener de forma segura
     */
    fun registerListener(key: String, listener: ListenerRegistration) {
        activeListeners[key]?.remove() // Limpiar listener anterior
        activeListeners[key] = listener
        Log.d("SyncUtils", "Listener registrado: $key")
    }

    /**
     * Limpia un listener específico
     */
    fun cleanupListener(key: String) {
        activeListeners[key]?.let { listener ->
            listener.remove()
            activeListeners.remove(key)
            Log.d("SyncUtils", "Listener limpiado: $key")
        }
    }

    /**
     * Limpia todos los listeners activos
     */
    fun cleanupAllListeners() {
        activeListeners.values.forEach { it.remove() }
        activeListeners.clear()
        Log.d("SyncUtils", "Todos los listeners limpiados")
    }

    /**
     * Obtiene el estado de sincronización
     */
    fun isSyncRunning(): Boolean = isGlobalSyncRunning.get()

    /**
     * Obtiene el número de listeners activos
     */
    fun getActiveListenersCount(): Int = activeListeners.size
}

/**
 * Clase para manejar operaciones de stock de forma thread-safe
 */
class StockOperationManager {
    private val operationsInProgress = mutableSetOf<String>()
    private val lock = Any()

    /**
     * Intenta iniciar una operación de stock
     * @return true si se puede proceder, false si ya hay una operación en curso
     */
    fun startOperation(productId: String): Boolean {
        synchronized(lock) {
            return if (operationsInProgress.contains(productId)) {
                Log.w("StockOperationManager", "Operación duplicada bloqueada para producto: $productId")
                false
            } else {
                operationsInProgress.add(productId)
                Log.d("StockOperationManager", "Operación iniciada para producto: $productId")
                true
            }
        }
    }

    /**
     * Finaliza una operación de stock
     */
    fun finishOperation(productId: String) {
        synchronized(lock) {
            operationsInProgress.remove(productId)
            Log.d("StockOperationManager", "Operación finalizada para producto: $productId")
        }
    }

    /**
     * Verifica si hay una operación en curso para un producto
     */
    fun isOperationInProgress(productId: String): Boolean {
        synchronized(lock) {
            return operationsInProgress.contains(productId)
        }
    }

    /**
     * Limpia todas las operaciones (para casos de error)
     */
    fun clearAllOperations() {
        synchronized(lock) {
            operationsInProgress.clear()
            Log.d("StockOperationManager", "Todas las operaciones limpiadas")
        }
    }
}

/**
 * Extension function para logging detallado
 */
fun Any.logDetailed(tag: String, message: String, level: LogLevel = LogLevel.DEBUG) {
    when (level) {
        LogLevel.DEBUG -> Log.d(tag, message)
        LogLevel.INFO -> Log.i(tag, message)
        LogLevel.WARN -> Log.w(tag, message)
        LogLevel.ERROR -> Log.e(tag, message)
    }
}

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}