package com.todolist.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AppRoot(vm: AppViewModel) {
    var showManage by remember { mutableStateOf(false) }
    when {
        vm.token == null -> AuthScreen(vm)
        showManage -> ManageScreen(vm, onBack = { showManage = false })
        else -> TasksScreen(vm, onOpenManage = { showManage = true })
    }
}

fun parseHexColor(hex: String?): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    Color(0xFF4361EE)
}

@Composable
fun ColorDot(hex: String?) {
    Box(
        Modifier
            .size(10.dp)
            .background(parseHexColor(hex), CircleShape)
    )
}

@Composable
fun LabelChip(text: String, hex: String?) {
    val color = parseHexColor(hex)
    Text(
        text,
        color = color,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
