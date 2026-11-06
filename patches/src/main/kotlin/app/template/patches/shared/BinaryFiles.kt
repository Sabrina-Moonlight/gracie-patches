package app.template.patches.shared

internal val DEX_MAGIC = byteArrayOf(0x64, 0x65, 0x78, 0x0a)
internal const val DEX_FILE_SIZE_OFFSET = 32
internal const val DEX_HEADER_SIZE = 0x70

internal fun ByteArray.readInt(offset: Int) =
    (this[offset].toInt() and 0xFF) or
        ((this[offset + 1].toInt() and 0xFF) shl 8) or
        ((this[offset + 2].toInt() and 0xFF) shl 16) or
        ((this[offset + 3].toInt() and 0xFF) shl 24)
