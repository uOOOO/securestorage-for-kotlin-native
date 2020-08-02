package com.uoooo.securestorage

import kotlinx.cinterop.*
import platform.Foundation.*

@ExperimentalUnsignedTypes
internal fun String.toNSData(): NSData {
    memScoped {
        return NSData.dataWithBytes(bytes = this@toNSData.cstr.ptr, length = this@toNSData.length.toULong())
    }
}