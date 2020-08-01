package com.uoooo.securestorage

import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.Foundation.NSData
import platform.Foundation.create

@Suppress("EXPERIMENTAL_API_USAGE")
internal fun CFDataRef.toNSData(): NSData = NSData.create(
    bytes = CFDataGetBytePtr(this),
    length = CFDataGetLength(this).toULong()
)