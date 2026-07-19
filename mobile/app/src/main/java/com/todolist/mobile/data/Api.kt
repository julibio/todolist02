package com.todolist.mobile.data

import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

data class User(val id: Int, val name: String, val email: String)
data class AuthResponse(val user: User, val token: String)
data class Category(val id: Int, val name: String, val color: String)
data class Tag(val id: Int, val name: String, val color: String)
data class Task(
    val id: Int,
    val title: String,
    val description: String?,
    val completed: Boolean,
    val category_id: Int?,
    val category_name: String?,
    val category_color: String?,
    val tags: List<Tag> = emptyList(),
)

data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val name: String, val email: String, val password: String)
data class NameColorRequest(val name: String, val color: String)
data class CreateTaskRequest(
    val title: String,
    val description: String?,
    val category_id: Int?,
    val tag_ids: List<Int>,
)

// Gson omite campos nulos, e o backend usa COALESCE no UPDATE:
// só os campos enviados são alterados.
data class UpdateTaskRequest(
    val title: String? = null,
    val description: String? = null,
    val completed: Boolean? = null,
    val category_id: Int? = null,
    val tag_ids: List<Int>? = null,
)

interface ApiService {
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @GET("api/tasks")
    suspend fun tasks(): List<Task>

    @POST("api/tasks")
    suspend fun createTask(@Body body: CreateTaskRequest): Task

    @PUT("api/tasks/{id}")
    suspend fun updateTask(@Path("id") id: Int, @Body body: UpdateTaskRequest): Task

    @DELETE("api/tasks/{id}")
    suspend fun deleteTask(@Path("id") id: Int)

    @GET("api/categories")
    suspend fun categories(): List<Category>

    @POST("api/categories")
    suspend fun createCategory(@Body body: NameColorRequest): Category

    @DELETE("api/categories/{id}")
    suspend fun deleteCategory(@Path("id") id: Int)

    @GET("api/tags")
    suspend fun tags(): List<Tag>

    @POST("api/tags")
    suspend fun createTag(@Body body: NameColorRequest): Tag

    @DELETE("api/tags/{id}")
    suspend fun deleteTag(@Path("id") id: Int)
}

object ApiClient {

    fun create(baseUrl: String, tokenProvider: () -> String?): ApiService {
        val authInterceptor = Interceptor { chain ->
            val token = tokenProvider()
            val request = if (token != null) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .build()
        return Retrofit.Builder()
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    fun errorMessage(e: Throwable): String = when (e) {
        is HttpException -> try {
            val body = e.response()?.errorBody()?.string()
            Gson().fromJson(body, Map::class.java)?.get("error")?.toString()
                ?: "Erro ${e.code()}"
        } catch (_: Exception) {
            "Erro ${e.code()}"
        }
        else -> e.message ?: "Erro de conexão com o servidor"
    }
}
