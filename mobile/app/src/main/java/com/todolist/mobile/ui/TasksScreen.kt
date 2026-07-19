package com.todolist.mobile.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.todolist.mobile.data.Task

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(vm: AppViewModel, onOpenManage: () -> Unit) {
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Task?>(null) }
    var filterCategory by remember { mutableStateOf<Int?>(null) }
    var filterTag by remember { mutableStateOf<Int?>(null) }

    val filtered = vm.tasks.filter { t ->
        (filterCategory == null || t.category_id == filterCategory) &&
            (filterTag == null || t.tags.any { it.id == filterTag })
    }
    val pending = filtered.filter { !it.completed }
    val completed = filtered.filter { it.completed }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(vm.userName?.let { "Tarefas de $it" } ?: "Tarefas") },
                actions = {
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                    }
                    IconButton(onClick = onOpenManage) {
                        Icon(Icons.Default.Settings, contentDescription = "Categorias e tags")
                    }
                    IconButton(onClick = { vm.logout() }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Sair")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nova tarefa")
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (vm.loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            vm.error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            if (vm.categories.isNotEmpty() || vm.tags.isNotEmpty()) {
                Row(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    vm.categories.forEach { cat ->
                        FilterChip(
                            selected = filterCategory == cat.id,
                            onClick = {
                                filterCategory = if (filterCategory == cat.id) null else cat.id
                            },
                            label = { Text(cat.name) },
                            leadingIcon = { ColorDot(cat.color) }
                        )
                    }
                    vm.tags.forEach { tag ->
                        FilterChip(
                            selected = filterTag == tag.id,
                            onClick = { filterTag = if (filterTag == tag.id) null else tag.id },
                            label = { Text(tag.name) },
                            leadingIcon = { ColorDot(tag.color) }
                        )
                    }
                }
            }
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("${pending.size} pendentes", style = MaterialTheme.typography.labelLarge)
                Text("${completed.size} concluídas", style = MaterialTheme.typography.labelLarge)
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pending, key = { it.id }) { task ->
                    TaskCard(
                        task,
                        onToggle = { vm.toggleTask(task) },
                        onEdit = { editing = task },
                        onDelete = { vm.deleteTask(task.id) }
                    )
                }
                if (completed.isNotEmpty()) {
                    item {
                        Text(
                            "Concluídas",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(completed, key = { it.id }) { task ->
                        TaskCard(
                            task,
                            onToggle = { vm.toggleTask(task) },
                            onEdit = { editing = task },
                            onDelete = { vm.deleteTask(task.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddTaskDialog(vm, onDismiss = { showAdd = false })
    }
    editing?.let { task ->
        EditTaskDialog(
            task,
            onSave = { title, desc ->
                vm.editTask(task.id, title, desc)
                editing = null
            },
            onDismiss = { editing = null }
        )
    }
}

@Composable
private fun TaskCard(task: Task, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.completed, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else null
                )
                if (!task.description.isNullOrBlank()) {
                    Text(
                        task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (task.category_name != null || task.tags.isNotEmpty()) {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        task.category_name?.let { LabelChip(it, task.category_color) }
                        task.tags.forEach { LabelChip(it.name, it.color) }
                    }
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Editar")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Excluir")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddTaskDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<Int?>(null) }
    var tagIds by remember { mutableStateOf<List<Int>>(emptyList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova tarefa") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("O que precisa ser feito?") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição (opcional)") }
                )
                if (vm.categories.isNotEmpty()) {
                    Text("Categoria", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        vm.categories.forEach { cat ->
                            FilterChip(
                                selected = categoryId == cat.id,
                                onClick = {
                                    categoryId = if (categoryId == cat.id) null else cat.id
                                },
                                label = { Text(cat.name) },
                                leadingIcon = { ColorDot(cat.color) }
                            )
                        }
                    }
                }
                if (vm.tags.isNotEmpty()) {
                    Text("Tags", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        vm.tags.forEach { tag ->
                            FilterChip(
                                selected = tag.id in tagIds,
                                onClick = {
                                    tagIds = if (tag.id in tagIds) tagIds - tag.id else tagIds + tag.id
                                },
                                label = { Text(tag.name) },
                                leadingIcon = { ColorDot(tag.color) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    vm.addTask(title.trim(), description, categoryId, tagIds)
                    onDismiss()
                }
            ) { Text("Adicionar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun EditTaskDialog(task: Task, onSave: (String, String) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar tarefa") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição") }
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = { onSave(title.trim(), description) }
            ) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
