package com.uoooo.securestorage

expect class SecureStorage {
    fun put(key: String, value: String)
    fun get(key: String, default: String? = null): String?
    fun remove(key: String)
}