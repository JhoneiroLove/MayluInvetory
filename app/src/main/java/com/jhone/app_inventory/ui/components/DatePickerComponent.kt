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

    // Función para formatear fecha
    val formatDate = { timestamp: Long ->
        val date = Date(timestamp)
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        formatter.format(date)
    }

    // Función para parsear fecha desde el texto actual
    val parseCurrentDate = {
        try {
            if (value.isNotBlank()) {
                val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val date = formatter.parse(value)
                date?.time
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
            placeholder = { Text("dd/MM/yyyy", color = Color.Gray) },
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
                    text = if (isRequired) "Campo obligatorio" else "Formato: dd/MM/yyyy (Opcional)",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isRequired) Color.Red else Color(0xFF666666)
                )
            }
        )
    }

    // DatePicker Dialog Corregido
    if (showDatePicker) {
        CustomDatePickerDialog(
            onDateSelected = { selectedDateMillis ->
                selectedDateMillis?.let { timestamp ->
                    val formattedDate = formatDate(timestamp)
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
                        // CORREGIDO: Texto del título y mes en color oscuro
                        titleContentColor = Color(0xFF333333),
                        headlineContentColor = Color(0xFF333333),
                        // CORREGIDO: Días de la semana en color oscuro
                        weekdayContentColor = Color(0xFF333333),
                        // CORREGIDO: Subtítulos en color oscuro
                        subheadContentColor = Color(0xFF333333),
                        // Años
                        yearContentColor = Color(0xFF333333),
                        currentYearContentColor = Color.White,
                        selectedYearContainerColor = buttonColor,
                        // Días del calendario
                        dayContentColor = Color(0xFF333333),
                        selectedDayContentColor = Color.White,
                        selectedDayContainerColor = buttonColor,
                        todayContentColor = primaryColor,
                        todayDateBorderColor = primaryColor,
                        dayInSelectionRangeContentColor = Color.White,
                        dayInSelectionRangeContainerColor = primaryColor.copy(alpha = 0.3f),
                        // CORREGIDO: Navegación del mes en color oscuro
                        navigationContentColor = Color(0xFF333333)
                    )
                )

                // Vista previa de fecha seleccionada
                datePickerState.selectedDateMillis?.let { timestamp ->
                    val selectedDate = Date(timestamp)
                    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
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
                                text = formatter.format(selectedDate),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = buttonColor
                            )

                            Text(
                                text = dayFormatter.format(selectedDate).replaceFirstChar { it.uppercase() },
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
            // CORREGIDO: Botones horizontales que no colapsan
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

// Utilidad para validar formato de fecha (sin cambios)
object DateValidator {
    fun isValidDateFormat(dateString: String): Boolean {
        if (dateString.isBlank()) return true // Opcional

        return try {
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            formatter.isLenient = false
            formatter.parse(dateString)

            // Verificar que tenga el formato exacto
            val parts = dateString.split("/")
            parts.size == 3 &&
                    parts[0].length == 2 &&
                    parts[1].length == 2 &&
                    parts[2].length == 4
        } catch (e: Exception) {
            false
        }
    }

    fun getDateValidationMessage(dateString: String): String? {
        return when {
            dateString.isBlank() -> null // Es opcional
            !isValidDateFormat(dateString) -> "Formato de fecha inválido. Use dd/MM/yyyy"
            else -> null
        }
    }
}