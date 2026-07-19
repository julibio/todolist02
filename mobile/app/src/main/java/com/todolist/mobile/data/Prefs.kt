package com.todolist.mobile.data

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("todolist", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = sp.getString("baseUrl", "http://192.168.15.15:3002")!!
        set(value) = sp.edit().putString("baseUrl", value).apply()

    var token: String?
        get() = sp.getString("token", null)
        set(value) = sp.edit().putString("token", value).apply()

    var userName: String?
        get() = sp.getString("userName", null)
        set(value) = sp.edit().putString("userName", value).apply()
}
