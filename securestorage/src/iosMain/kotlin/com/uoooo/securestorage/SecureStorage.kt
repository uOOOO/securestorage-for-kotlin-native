package com.uoooo.securestorage

import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Foundation.*
import platform.Security.SecKeyCreateDecryptedData
import platform.Security.SecKeyCreateEncryptedData
import platform.Security.kSecKeyAlgorithmECIESEncryptionCofactorX963SHA256AESGCM

actual class SecureStorage(applicationTag: String) {
    private val keyManager by lazy { KeyManager(applicationTag) }
    private val userDefaults by lazy { NSUserDefaults.standardUserDefaults }

    private fun encrypt(decryptedData: NSData): NSData = memScoped {
        val error = alloc<CFErrorRefVar>()
        val publicKey = keyManager.getPublicKey()
        val data = CFBridgingRetain(decryptedData)
        val encryptedValue = SecKeyCreateEncryptedData(
            publicKey,
            kSecKeyAlgorithmECIESEncryptionCofactorX963SHA256AESGCM,
            data?.reinterpret(),
            error.ptr
        )
        CFBridgingRelease(data)
        CFRelease(publicKey)

        if (error.value != null || encryptedValue == null) {
            throw IllegalStateException("Encrypt error")
        }

        return encryptedValue.toNSData().also { CFRelease(encryptedValue) }
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    private fun decrypt(encryptedData: NSData): NSData? = memScoped {
        val error = alloc<CFErrorRefVar>()
        val privateKey = keyManager.getPrivateKey()
        val data = CFBridgingRetain(encryptedData)
        val decryptedData = SecKeyCreateDecryptedData(
            privateKey,
            kSecKeyAlgorithmECIESEncryptionCofactorX963SHA256AESGCM,
            data?.reinterpret(),
            error.ptr
        )
        CFBridgingRelease(data)
        CFRelease(privateKey)

        if (error.value != null) {
            throw IllegalStateException("Decrypt error")
        }

        if (decryptedData == null) {
            return null
        }

        return decryptedData.toNSData().also { CFRelease(decryptedData) }
    }

    @Throws
    actual fun put(key: String, value: String) {
        @Suppress("EXPERIMENTAL_API_USAGE")
        userDefaults.setObject(encrypt(value.toNSData()), key.toNSData().toSHA512().base64Encoding())
    }

    @Throws
    actual fun get(key: String, default: String?): String? {
        @Suppress("EXPERIMENTAL_API_USAGE")
        val data = userDefaults.dataForKey(key.toNSData().toSHA512().base64Encoding()) ?: return default
        val decryptedData = decrypt(data) ?: return default
        return decryptedData.toKString()
    }

    actual fun remove(key: String) {
        @Suppress("EXPERIMENTAL_API_USAGE")
        userDefaults.removeObjectForKey(key.toNSData().toSHA512().base64Encoding())
    }
}