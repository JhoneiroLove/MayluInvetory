package com.jhone.app_inventory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.jhone.app_inventory.ui.viewmodel.ProductViewModel
import com.jhone.app_inventory.utils.SyncUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun SyncDiagnosticDialog(
    onDismiss: () -> Unit,
    viewModel: ProductViewModel = hiltViewModel()
) {
    var diagnosticData by remember { mutableStateOf<List<DiagnosticItem>>(emptyList()) }
    var isRunningDiagnostic by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Ejecutar diagnóstico al abrir
    LaunchedEffect(Unit) {
        runDiagnostic(
            onUpdate = { diagnosticData = it },
            onLoading = { isRunningDiagnostic = it }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Diagnóstico de Sincronización",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7851A9)
                        )
                    )

                    IconButton(
                        onClick = {
                            scope.launch {
                                runDiagnostic(
                                    onUpdate = { diagnosticData = it },
                                    onLoading = { isRunningDiagnostic = it }
                                )
                            }
                        },
                        enabled = !isRunningDiagnostic
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar diagnóstico",
                            tint = Color(0xFF7851A9)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Estado de carga
                if (isRunningDiagnostic) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFF7851A9)
                            )
                            Text("Ejecutando diagnóstico...")
                        }
                    }
                } else {
                    // Lista de diagnósticos
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(diagnosticData) { item ->
                            DiagnosticItemCard(item)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón de sincronización forzada
                    Button(
                        onClick = {
                            scope.launch {
                                SyncUtils.performGlobalSync(
                                    db = FirebaseFirestore.getInstance(),
                                    onProgress = { /* Mostrar progreso si es necesario */ },
                                    onComplete = { success, error ->
                                        if (success) {
                                            // Ejecutar diagnóstico nuevamente
                                            scope.launch {
                                                delay(1000)
                                                runDiagnostic(
                                                    onUpdate = { diagnosticData = it },
                                                    onLoading = { isRunningDiagnostic = it }
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        },
                        enabled = !isRunningDiagnostic && !SyncUtils.isSyncRunning(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7851A9)
                        )
                    ) {
                        Text("Sincronizar", color = Color.White)
                    }

                    // Botón cerrar
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticItemCard(item: DiagnosticItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (item.status) {
                DiagnosticStatus.SUCCESS -> Color(0xFFE8F5E8)
                DiagnosticStatus.WARNING -> Color(0xFFFFF3E0)
                DiagnosticStatus.ERROR -> Color(0xFFFFEBEE)
                DiagnosticStatus.INFO -> Color(0xFFE3F2FD)
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = when (item.status) {
                    DiagnosticStatus.SUCCESS -> Icons.Default.CheckCircle
                    DiagnosticStatus.WARNING -> Icons.Default.Warning
                    DiagnosticStatus.ERROR -> Icons.Default.Error
                    DiagnosticStatus.INFO -> Icons.Default.Info
                },
                contentDescription = item.status.name,
                tint = when (item.status) {
                    DiagnosticStatus.SUCCESS -> Color(0xFF4CAF50)
                    DiagnosticStatus.WARNING -> Color(0xFFFF8F00)
                    DiagnosticStatus.ERROR -> Color(0xFFD32F2F)
                    DiagnosticStatus.INFO -> Color(0xFF2196F3)
                }
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.Black
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                if (item.value.isNotEmpty()) {
                    Text(
                        text = item.value,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.Black
                    )
                }
            }
        }
    }
}

// Función para ejecutar el diagnóstico
private suspend fun runDiagnostic(
    onUpdate: (List<DiagnosticItem>) -> Unit,
    onLoading: (Boolean) -> Unit
) {
    onLoading(true)
    val diagnostics = mutableListOf<DiagnosticItem>()

    try {
        val db = FirebaseFirestore.getInstance()

        // 1. Verificar conectividad a Firebase
        try {
            val testQuery = db.collection("products").limit(1).get().await()
            diagnostics.add(
                DiagnosticItem(
                    title = "Conectividad Firebase",
                    description = "Conexión a la base de datos",
                    value = "✓ Conectado",
                    status = DiagnosticStatus.SUCCESS
                )
            )
        } catch (e: Exception) {
            diagnostics.add(
                DiagnosticItem(
                    title = "Conectividad Firebase",
                    description = "Error de conexión",
                    value = "✗ ${e.message}",
                    status = DiagnosticStatus.ERROR
                )
            )
        }

        // 2. Verificar cantidad de productos
        try {
            val productsSnapshot = db.collection("products").get().await()
            val productCount = productsSnapshot.size()
            diagnostics.add(
                DiagnosticItem(
                    title = "Productos en servidor",
                    description = "Total de productos en Firebase",
                    value = "$productCount productos",
                    status = if (productCount > 0) DiagnosticStatus.SUCCESS else DiagnosticStatus.WARNING
                )
            )
        } catch (e: Exception) {
            diagnostics.add(
                DiagnosticItem(
                    title = "Productos en servidor",
                    description = "Error al contar productos",
                    value = "Error: ${e.message}",
                    status = DiagnosticStatus.ERROR
                )
            )
        }

        // 3. Verificar movimientos recientes
        try {
            val now = com.google.firebase.Timestamp.now()
            val fiveMinutesAgo = com.google.firebase.Timestamp(now.seconds - 300, 0)

            val recentMovements = db.collection("movimientos")
                .whereGreaterThan("fecha", fiveMinutesAgo)
                .get()
                .await()

            diagnostics.add(
                DiagnosticItem(
                    title = "Movimientos recientes",
                    description = "Últimos 5 minutos",
                    value = "${recentMovements.size()} movimientos",
                    status = if (recentMovements.size() < 10) DiagnosticStatus.SUCCESS else DiagnosticStatus.WARNING
                )
            )
        } catch (e: Exception) {
            diagnostics.add(
                DiagnosticItem(
                    title = "Movimientos recientes",
                    description = "Error al verificar movimientos",
                    value = "Error: ${e.message}",
                    status = DiagnosticStatus.ERROR
                )
            )
        }

        // 4. Verificar duplicados en movimientos
        try {
            val allMovements = db.collection("movimientos")
                .orderBy("fecha", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()

            val movementGroups = allMovements.documents.groupBy { doc ->
                "${doc.getString("loteId")}_${doc.getString("tipo")}_${doc.getLong("cantidad")}_${doc.getString("usuario")}"
            }

            val duplicates = movementGroups.values.count { it.size > 1 }

            diagnostics.add(
                DiagnosticItem(
                    title = "Duplicados detectados",
                    description = "Movimientos duplicados en últimos 100",
                    value = "$duplicates grupos duplicados",
                    status = if (duplicates == 0) DiagnosticStatus.SUCCESS else DiagnosticStatus.WARNING
                )
            )
        } catch (e: Exception) {
            diagnostics.add(
                DiagnosticItem(
                    title = "Verificación de duplicados",
                    description = "Error al verificar duplicados",
                    value = "Error: ${e.message}",
                    status = DiagnosticStatus.ERROR
                )
            )
        }

        // 5. Estado de caché local
        diagnostics.add(
            DiagnosticItem(
                title = "Caché local",
                description = "Sistema de caché híbrido",
                value = "Activo",
                status = DiagnosticStatus.INFO
            )
        )

        // 6. Listeners activos
        val activeListeners = SyncUtils.getActiveListenersCount()
        diagnostics.add(
            DiagnosticItem(
                title = "Listeners activos",
                description = "Conexiones en tiempo real",
                value = "$activeListeners listeners",
                status = if (activeListeners <= 3) DiagnosticStatus.SUCCESS else DiagnosticStatus.WARNING
            )
        )

        // 7. Estado de sincronización
        val isSyncRunning = SyncUtils.isSyncRunning()
        diagnostics.add(
            DiagnosticItem(
                title = "Sincronización",
                description = "Estado actual de sync",
                value = if (isSyncRunning) "En progreso" else "Inactiva",
                status = if (isSyncRunning) DiagnosticStatus.INFO else DiagnosticStatus.SUCCESS
            )
        )

        // 8. Verificar inconsistencias de stock
        try {
            val products = db.collection("products").get().await()
            val negativeStockProducts = products.documents.count { doc ->
                (doc.getLong("cantidad") ?: 0) < 0
            }

            diagnostics.add(
                DiagnosticItem(
                    title = "Consistencia de stock",
                    description = "Productos con stock negativo",
                    value = "$negativeStockProducts productos",
                    status = if (negativeStockProducts == 0) DiagnosticStatus.SUCCESS else DiagnosticStatus.ERROR
                )
            )
        } catch (e: Exception) {
            diagnostics.add(
                DiagnosticItem(
                    title = "Consistencia de stock",
                    description = "Error al verificar consistencia",
                    value = "Error: ${e.message}",
                    status = DiagnosticStatus.ERROR
                )
            )
        }

        // 9. Información del usuario actual
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        diagnostics.add(
            DiagnosticItem(
                title = "Usuario actual",
                description = "Sesión activa",
                value = currentUser?.email ?: "No autenticado",
                status = if (currentUser != null) DiagnosticStatus.SUCCESS else DiagnosticStatus.ERROR
            )
        )

        // 10. Timestamp de la verificación
        val currentTime = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        diagnostics.add(
            DiagnosticItem(
                title = "Última verificación",
                description = "Timestamp del diagnóstico",
                value = currentTime,
                status = DiagnosticStatus.INFO
            )
        )

    } catch (e: Exception) {
        diagnostics.add(
            DiagnosticItem(
                title = "Error general",
                description = "Error durante el diagnóstico",
                value = e.message ?: "Error desconocido",
                status = DiagnosticStatus.ERROR
            )
        )
    }

    onUpdate(diagnostics)
    onLoading(false)
}

// Clases de datos para el diagnóstico
data class DiagnosticItem(
    val title: String,
    val description: String,
    val value: String,
    val status: DiagnosticStatus
)

enum class DiagnosticStatus {
    SUCCESS, WARNING, ERROR, INFO
}