package com.uoooo.securestorage

import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSUserDefaults
import platform.Security.SecKeyCreateDecryptedData
import platform.Security.SecKeyCreateEncryptedData
import platform.Security.kSecKeyAlgorithmECIESEncryptionCofactorX963SHA256AESGCM
import platform.posix.memcpy

actual class SecureStorage(applicationTag: String) {
    private val keyManager by lazy { KeyManager(applicationTag) }
    private val userDefaults by lazy { NSUserDefaults.standardUserDefaults }

    @Suppress("UNCHECKED_CAST")
    private fun encrypt(decryptedData: NSData): NSData = memScoped {
        val error = alloc<CFErrorRefVar>()
        val publicKey = keyManager.getPublicKey()
        val data = CFBridgingRetain(decryptedData)
        val encryptedValue = SecKeyCreateEncryptedData(
            publicKey,
            kSecKeyAlgorithmECIESEncryptionCofactorX963SHA256AESGCM,
            data as CFDataRef,
            error.ptr
        )
        CFBridgingRelease(data)
        CFRelease(publicKey)

        if (error.value != null || encryptedValue == null) {
            throw IllegalStateException("Encrypt error")
        }

        return encryptedValue.toNSData().also { CFRelease(encryptedValue) }
    }

    @Suppress("UNCHECKED_CAST", "EXPERIMENTAL_API_USAGE")
    private fun decrypt(encryptedData: NSData): NSData? = memScoped {
        val error = alloc<CFErrorRefVar>()
        val privateKey = keyManager.getPrivateKey()
        val data = CFBridgingRetain(encryptedData)
        val decryptedData = SecKeyCreateDecryptedData(
            privateKey,
            kSecKeyAlgorithmECIESEncryptionCofactorX963SHA256AESGCM,
            data as CFDataRef,
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
        userDefaults.setObject(encrypt(value.toNSData()), key.toSHA256Base64())
    }

    @Throws
    actual fun get(key: String, default: String?): String? {
        val data = userDefaults.dataForKey(key.toSHA256Base64()) ?: return default
        val decryptedData = decrypt(data) ?: return default

        @Suppress("EXPERIMENTAL_API_USAGE")
        val result = ByteArray(decryptedData.length.toInt()).apply {
            usePinned { memcpy(it.addressOf(0), decryptedData.bytes, decryptedData.length) }
        }

        return result.decodeToString()
    }

    actual fun remove(key: String) {
        userDefaults.removeObjectForKey(key.toSHA256Base64())
    }
}