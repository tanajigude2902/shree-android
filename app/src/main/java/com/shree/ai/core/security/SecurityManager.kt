package com.shree.ai.core.security

class SecurityManager(private val store: EncryptedStore) {
    fun saveApiKey(provider: String, key: String) = store.putString("key_" + provider, key.trim())
    fun getApiKey(provider: String): String? = store.getString("key_" + provider)
    fun hasApiKey(provider: String): Boolean = !getApiKey(provider).isNullOrBlank()

    fun maskApiKey(key: String?): String {
        if (key.isNullOrBlank()) return "Not configured"
        if (key.length <= 8) return "••••••••"
        return key.take(4) + "••••" + key.takeLast(4)
    }
}
