package com.uoooo.securestorage

import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSNumber
import platform.Security.*

internal class KeyManager(private val applicationTag: String) {
    init {
        getPrivateKey()?.also { CFRelease(it) } ?: createNewKey()
    }

    private fun createNewKey() = memScoped {
        getPrivateKey()?.also {
            CFRelease(it)
            return@memScoped
        }

        val accessControl = SecAccessControlCreateWithFlags(
            kCFAllocatorDefault,
            kSecAttrAccessibleAlways,
            kSecAccessControlPrivateKeyUsage,
            null
        ) ?: throw IllegalStateException("Can't create access control")

        @Suppress("EXPERIMENTAL_API_USAGE")
        val applicationTag = CFBridgingRetain(applicationTag.toNSData())
        val privateKeyAttrs = CFDictionaryCreateMutable(kCFAllocatorDefault, 0, null, null)
            .apply {
                CFDictionaryAddValue(this, kSecAttrIsPermanent, kCFBooleanTrue)
                CFDictionaryAddValue(this, kSecAttrApplicationTag, applicationTag)
                CFDictionaryAddValue(this, kSecAttrAccessControl, accessControl)
            }

        val keySizeInBits = CFBridgingRetain(NSNumber(int = 256))
        val parameters = CFDictionaryCreateMutable(kCFAllocatorDefault, 0, null, null)
            .apply {
                CFDictionaryAddValue(this, kSecAttrKeyType, kSecAttrKeyTypeECSECPrimeRandom)
                CFDictionaryAddValue(this, kSecAttrKeySizeInBits, keySizeInBits)
                CFDictionaryAddValue(this, kSecPrivateKeyAttrs, privateKeyAttrs)
                @Suppress("ConstantConditionIf")
                if (!isSimulator) CFDictionaryAddValue(this, kSecAttrTokenID, kSecAttrTokenIDSecureEnclave)
            }

        val error = alloc<CFErrorRefVar>()
        // Create new key pair
        val privateKey =
            SecKeyCreateRandomKey(parameters, error.ptr) ?: throw IllegalStateException("Couldn't create private key")

        // Check public key
        val publicKey = SecKeyCopyPublicKey(privateKey) ?: throw IllegalStateException("Not found public key")

        CFBridgingRelease(applicationTag)
        CFBridgingRelease(keySizeInBits)
        CFBridgingRelease(accessControl)
        CFBridgingRelease(privateKeyAttrs)
        CFBridgingRelease(parameters)
        CFRelease(privateKey)
        CFRelease(publicKey)
    }

    fun getPrivateKey(): SecKeyRef? = memScoped {
        val keySizeInBits = CFBridgingRetain(NSNumber(int = 256))

        @Suppress("EXPERIMENTAL_API_USAGE")
        val applicationTag = CFBridgingRetain(applicationTag.toNSData())
        val query = CFDictionaryCreateMutable(kCFAllocatorDefault, 0, null, null)
            .apply {
                CFDictionaryAddValue(this, kSecClass, kSecClassKey)
                CFDictionaryAddValue(this, kSecAttrKeyClass, kSecAttrKeyClassPrivate)
                CFDictionaryAddValue(this, kSecAttrKeyType, kSecAttrKeyTypeECSECPrimeRandom)
                CFDictionaryAddValue(this, kSecAttrKeySizeInBits, keySizeInBits)
                CFDictionaryAddValue(this, kSecAttrApplicationTag, applicationTag)
                CFDictionaryAddValue(this, kSecReturnRef, kCFBooleanTrue)
            }

        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query, result.ptr)

        CFBridgingRelease(keySizeInBits)
        CFBridgingRelease(applicationTag)
        CFBridgingRelease(query)

        if (status == errSecItemNotFound) {
            return null
        }
        if (status != errSecSuccess) {
            throw IllegalStateException("Failed to get private key; reason=$status")
        }

        return result.value?.reinterpret()
    }

    fun getPublicKey(): SecKeyRef? {
        val privateKey =
            getPrivateKey() ?: throw IllegalStateException("There is no private key")
        return SecKeyCopyPublicKey(privateKey).apply { CFRelease(privateKey) }
    }
}