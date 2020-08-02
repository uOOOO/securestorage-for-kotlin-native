package com.uoooo.securestorage

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA512
import platform.CoreCrypto.CC_SHA512_DIGEST_LENGTH
import platform.Foundation.NSData
import platform.Foundation.NSMutableData
import platform.Foundation.dataWithLength
import platform.posix.memcpy

internal fun NSData.toSHA512(): NSData {
    return NSMutableData.dataWithLength(CC_SHA512_DIGEST_LENGTH)!!.apply {
        @Suppress("EXPERIMENTAL_API_USAGE")
        (CC_SHA512(
            this@toSHA512.bytes,
            this@toSHA512.length.toUInt(),
            mutableBytes!!.reinterpret()
        ))
    }
}

internal fun NSData.toKString(): String = memScoped {
    @Suppress("EXPERIMENTAL_API_USAGE")
    val result = ByteArray(this@toKString.length.toInt()).apply {
        usePinned { memcpy(it.addressOf(0), this@toKString.bytes, this@toKString.length) }
    }
    return result.decodeToString()
}