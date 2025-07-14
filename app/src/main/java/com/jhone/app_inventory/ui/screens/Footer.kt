package com.jhone.app_inventory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun InventoryFooter() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF9C84C9)) // Rosa claro
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "© 2025 Control de Inventario",
            color = Color.White,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
