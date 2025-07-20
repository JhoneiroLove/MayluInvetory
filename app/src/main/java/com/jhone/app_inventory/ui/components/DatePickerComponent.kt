package com.jhone.app_inventory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isRequired: Boolean = false,
    colors: TextFieldColors = TextFieldDefaults.outlinedTextFieldColors()
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    // Colores del sistema
    val primaryColor = Color(0xFF9C84C9)

    // Función para formatear fecha CORREGIDA para zona horaria
    val formatDate = { timestamp: Long ->
        val date = Date(timestamp)
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        // Ajustar para zona horaria local
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        formatter.format(calendar.time)
    }

    // Función mejorada para parsear fecha
    val parseCurrentDate = {
        try {
            if (value.isNotBlank()) {
                val cleanedValue = DateValidator.normalizeDate(value)
                if (cleanedValue != null) {
                    // Parsear directamente sin conversión de zona horaria
                    val parts = cleanedValue.split("/")
                    if (parts.size == 3) {
                        val day = parts[0].toInt()
                        val month = parts[1].toInt() - 1 // Calendar.MONTH es 0-based
                        val year = parts[2].toInt()

                        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        utcCalendar.set(year, month, day, 0, 0, 0)
                        utcCalendar.set(Calendar.MILLISECOND, 0)
                        utcCalendar.timeInMillis
                    } else {
                        null
                    }
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Establecer fecha inicial en el picker
    LaunchedEffect(showDatePicker) {
        if (showDatePicker) {
            val currentTimestamp = parseCurrentDate()
            if (currentTimestamp != null) {
                datePickerState.selectedDateMillis = currentTimestamp
            }
        }
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = {
                Text(
                    text = if (isRequired) "$label *" else label,
                    color = if (isRequired) Color.Red else Color.Black
                )
            },
            modifier = Modifier.fillMaxWidth(),
            colors = colors,
            enabled = enabled,
            placeholder = { Text("d/M/yyyy o dd/MM/yyyy", color = Color.Gray) },
            trailingIcon = {
                IconButton(
                    onClick = {
                        if (enabled) {
                            showDatePicker = true
                        }
                    },
                    enabled = enabled
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Seleccionar fecha",
                        tint = if (enabled) primaryColor else Color.Gray
                    )
                }
            },
            supportingText = {
                Text(
                    text = if (isRequired) "Campo obligatorio" else "Formato: dd/MM/yyyy",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isRequired) Color.Red else Color(0xFF666666)
                )
            }
        )
    }

    // DatePicker Dialog
    if (showDatePicker) {
        CustomDatePickerDialog(
            onDateSelected = { selectedDateMillis ->
                selectedDateMillis?.let { timestamp ->
                    // Extraer día, mes, año directamente
                    val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                    utcCalendar.timeInMillis = timestamp

                    val day = utcCalendar.get(Calendar.DAY_OF_MONTH)
                    val month = utcCalendar.get(Calendar.MONTH) + 1 // Calendar.MONTH es 0-based
                    val year = utcCalendar.get(Calendar.YEAR)

                    // Formatear directamente sin conversión de zona horaria
                    val formattedDate = String.format("%02d/%02d/%04d", day, month, year)
                    onValueChange(formattedDate)
                }
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
            datePickerState = datePickerState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDatePickerDialog(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit,
    datePickerState: DatePickerState
) {
    // Colores del sistema
    val primaryColor = Color(0xFF9C84C9)
    val buttonColor = Color(0xFF7851A9)
    val gradientColors = listOf(primaryColor, Color(0xFFB89EDC))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp)
            ) {
                // Header personalizado con gradiente
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(brush = Brush.horizontalGradient(gradientColors))
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Seleccionar Fecha de Vencimiento",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // DatePicker con colores corregidos
                DatePicker(
                    state = datePickerState,
                    modifier = Modifier.fillMaxWidth(),
                    showModeToggle = true,
                    colors = DatePickerDefaults.colors(
                        containerColor = Color.White,
                        titleContentColor = Color(0xFF333333),
                        headlineContentColor = Color(0xFF333333),
                        weekdayContentColor = Color(0xFF333333),
                        subheadContentColor = Color(0xFF333333),
                        yearContentColor = Color(0xFF333333),
                        currentYearContentColor = Color.White,
                        selectedYearContainerColor = buttonColor,
                        dayContentColor = Color(0xFF333333),
                        selectedDayContentColor = Color.White,
                        selectedDayContainerColor = buttonColor,
                        todayContentColor = primaryColor,
                        todayDateBorderColor = primaryColor,
                        dayInSelectionRangeContentColor = Color.White,
                        dayInSelectionRangeContainerColor = primaryColor.copy(alpha = 0.3f),
                        navigationContentColor = Color(0xFF333333)
                    )
                )

                // Vista previa de fecha seleccionada
                datePickerState.selectedDateMillis?.let { timestamp ->
                    // Extraer fecha directamente en UTC sin conversión
                    val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                    utcCalendar.timeInMillis = timestamp

                    val day = utcCalendar.get(Calendar.DAY_OF_MONTH)
                    val month = utcCalendar.get(Calendar.MONTH) + 1
                    val year = utcCalendar.get(Calendar.YEAR)

                    val formattedDate = String.format("%02d/%02d/%04d", day, month, year)

                    // Para mostrar el día de la semana, usar la fecha formateada
                    val displayCalendar = Calendar.getInstance()
                    displayCalendar.set(year, month - 1, day) // Los meses en Calendar son 0-based
                    val dayFormatter = SimpleDateFormat("EEEE", Locale.getDefault())

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = primaryColor.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Fecha seleccionada:",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = buttonColor
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = buttonColor
                            )

                            Text(
                                text = dayFormatter.format(displayCalendar.time).replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyMedium,
                                color = primaryColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Primera fila: Botón principal
                Button(
                    onClick = {
                        onDateSelected(datePickerState.selectedDateMillis)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Seleccionar",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Segunda fila: Botones secundarios
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón Limpiar
                    OutlinedButton(
                        onClick = {
                            onDateSelected(null)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFE53935)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Limpiar",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }

                    // Botón Cancelar
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF666666)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Cancelar",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        dismissButton = null,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

// Utilidad mejorada para validar formato de fecha
object DateValidator {

    /**
     * Normaliza una fecha a formato dd/MM/yyyy
     */
    fun normalizeDate(dateString: String): String? {
        if (dateString.isBlank()) return null

        try {
            // Limpiar espacios y caracteres extraños
            val cleaned = dateString.trim().replace("\\s+".toRegex(), "")

            // Dividir por "/"
            val parts = cleaned.split("/")
            if (parts.size != 3) return null

            val day = parts[0].toIntOrNull() ?: return null
            val month = parts[1].toIntOrNull() ?: return null
            val yearInput = parts[2].toIntOrNull() ?: return null

            // Validar rangos básicos
            if (day < 1 || day > 31) return null
            if (month < 1 || month > 12) return null

            // Manejar años de 2 dígitos
            val year = when {
                yearInput < 50 -> 2000 + yearInput  // 00-49 -> 2000-2049
                yearInput < 100 -> 1900 + yearInput // 50-99 -> 1950-1999
                else -> yearInput
            }

            // Validar año razonable
            if (year < 1900 || year > 2100) return null

            // Formatear con ceros a la izquierda
            return String.format("%02d/%02d/%04d", day, month, year)

        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Valida si un string de fecha es válido después de normalizarlo
     */
    fun isValidDateFormat(dateString: String): Boolean {
        if (dateString.isBlank()) return true // Opcional

        val normalized = normalizeDate(dateString) ?: return false

        return try {
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            formatter.isLenient = false
            formatter.parse(normalized)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Obtiene mensaje de validación de fecha
     */
    fun getDateValidationMessage(dateString: String): String? {
        return when {
            dateString.isBlank() -> null // Es opcional
            !isValidDateFormat(dateString) -> "Formato de fecha inválido. Use d/M/yyyy"
            else -> null
        }
    }
}