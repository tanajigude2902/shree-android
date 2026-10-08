package com.shree.ai.core.security
import android.content.Context
import android.content.SharedPreferences

class EncryptedStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("shree_vault", Context.MODE_PRIVATE)

    fun putString(key: String, value: String) = prefs.edit().putString(key, value).apply()
    fun getString(key: String): String? = prefs.getString(key, null)
    fun remove(key: String) = prefs.edit().remove(key).apply()
}
