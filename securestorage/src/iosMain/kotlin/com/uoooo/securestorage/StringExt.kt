package com.uoooo.securestorage

import com.soywiz.krypto.SHA256
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes

@ExperimentalUnsignedTypes
internal fun String.toNSData(): NSData {
    memScoped {
        return NSData.dataWithBytes(bytes = this@toNSData.cstr.ptr, length = this@toNSData.length.toULong())
    }
}

internal fun String.toSHA256Base64(): String = SHA256.digest(this.encodeToByteArray()).base64