package com.todolist.mobile.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.todolist.mobile.data.ApiClient
import com.todolist.mobile.data.AuthResponse
import com.todolist.mobile.data.Category
import com.todolist.mobile.data.CreateTaskRequest
import com.todolist.mobile.data.LoginRequest
import com.todolist.mobile.data.NameColorRequest
import com.todolist.mobile.data.Prefs
import com.todolist.mobile.data.RegisterRequest
import com.todolist.mobile.data.Tag
import com.todolist.mobile.data.Task
import com.todolist.mobile.data.UpdateTaskRequest
import kotlinx.coroutines.launch
import retrofit2.HttpException

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)

    var baseUrl by mutableStateOf(prefs.baseUrl)
        private set
    var token by mutableStateOf(prefs.token)
        private set
    var userName by mutableStateOf(prefs.userName)
        private set

    var tasks by mutableStateOf<List<Task>>(emptyList())
        private set
    var categories by mutableStateOf<List<Category>>(emptyList())
        private set
    var tags by mutableStateOf<List<Tag>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)

    private var api = ApiClient.create(baseUrl) { token }

    init {
        if (token != null) refresh()
    }

    fun setServer(url: String) {
        val clean = url.trim().trimEnd('/')
        if (clean.isBlank()) return
        prefs.baseUrl = clean
        baseUrl = clean
        api = ApiClient.create(clean) { token }
    }

    fun login(email: String, password: String) =
        auth { api.login(LoginRequest(email, password)) }

    fun register(name: String, email: String, password: String) =
        auth { api.register(RegisterRequest(name, email, password)) }

    private fun auth(block: suspend () -> AuthResponse) {
        viewModelScope.launch {
            loading = true
            error = null
            try {
                val res = block()
                prefs.token = res.token
                prefs.userName = res.user.name
                token = res.token
                userName = res.user.name
                refresh()
            } catch (e: Exception) {
                error = ApiClient.errorMessage(e)
            }
            loading = false
        }
    }

    fun logout() {
        prefs.token = null
        prefs.userName = null
        token = null
        userName = null
        tasks = emptyList()
        categories = emptyList()
        tags = emptyList()
        error = null
    }

    fun refresh() {
        viewModelScope.launch {
            loading = true
            error = null
            try {
                tasks = api.tasks()
                categories = api.categories()
                tags = api.tags()
            } catch (e: Exception) {
                handle(e)
            }
            loading = false
        }
    }

    private fun handle(e: Exception) {
        error = ApiClient.errorMessage(e)
        if (e is HttpException && e.code() == 401) logout()
    }

    private fun action(block: suspend () -> Unit) {
        viewModelScope.launch {
            error = null
            try {
                block()
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    fun addTask(title: String, description: String, categoryId: Int?, tagIds: List<Int>) = action {
        api.createTask(CreateTaskRequest(title, description.trim().ifBlank { null }, categoryId, tagIds))
        tasks = api.tasks()
    }

    fun toggleTask(task: Task) = action {
        api.updateTask(task.id, UpdateTaskRequest(completed = !task.completed))
        tasks = api.tasks()
    }

    fun editTask(id: Int, title: String, description: String) = action {
        api.updateTask(id, UpdateTaskRequest(title = title, description = description))
        tasks = api.tasks()
    }

    fun deleteTask(id: Int) = action {
        api.deleteTask(id)
        tasks = api.tasks()
    }

    fun addCategory(name: String, color: String) = action {
        api.createCategory(NameColorRequest(name, color))
        categories = api.categories()
    }

    fun deleteCategory(id: Int) = action {
        api.deleteCategory(id)
        categories = api.categories()
        tasks = api.tasks()
    }

    fun addTag(name: String, color: String) = action {
        api.createTag(NameColorRequest(name, color))
        tags = api.tags()
    }

    fun deleteTag(id: Int) = action {
        api.deleteTag(id)
        tags = api.tags()
        tasks = api.tasks()
    }
}
