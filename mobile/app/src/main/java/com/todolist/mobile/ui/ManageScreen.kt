package com.todolist.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val palette = listOf(
    "#4361ee", "#10b981", "#ef4444", "#f59e0b",
    "#8b5cf6", "#ec4899", "#06b6d4", "#64748b"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageScreen(vm: AppViewModel, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categorias & Tags") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            vm.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            SectionEditor(
                title = "Categorias",
                items = vm.categories.map { Triple(it.id, it.name, it.color) },
                defaultColor = "#4361ee",
                onAdd = { name, color -> vm.addCategory(name, color) },
                onDelete = { vm.deleteCategory(it) }
            )
            SectionEditor(
                title = "Tags",
                items = vm.tags.map { Triple(it.id, it.name, it.color) },
                defaultColor = "#10b981",
                onAdd = { name, color -> vm.addTag(name, color) },
                onDelete = { vm.deleteTag(it) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SectionEditor(
    title: String,
    items: List<Triple<Int, String, String>>,
    defaultColor: String,
    onAdd: (String, String) -> Unit,
    onDelete: (Int) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(defaultColor) }

    Text(title, style = MaterialTheme.typography.titleLarge)
    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Nome") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        palette.forEach { hex ->
            val selected = color == hex
            androidx.compose.foundation.layout.Box(
                Modifier
                    .size(if (selected) 32.dp else 26.dp)
                    .background(parseHexColor(hex), CircleShape)
                    .border(
                        width = if (selected) 3.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.onSurface else Color.LightGray,
                        shape = CircleShape
                    )
                    .clickable { color = hex }
            )
        }
    }
    Button(
        enabled = name.isNotBlank(),
        onClick = {
            onAdd(name.trim(), color)
            name = ""
        }
    ) { Text("Adicionar") }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { (id, itemName, itemColor) ->
            InputChip(
                selected = false,
                onClick = { onDelete(id) },
                label = { Text(itemName) },
                leadingIcon = { ColorDot(itemColor) },
                trailingIcon = {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Excluir $itemName",
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}
