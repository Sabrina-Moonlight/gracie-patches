package app.template.patches.shared

import app.morphe.patcher.patch.PatchException

/**
 * Convert a string representing a pattern of hexadecimal bytes to a byte array.
 */
fun byteArrayOf(pattern: String): ByteArray {
    val stripped = pattern.replace(Regex("\\s"), "")
    check(stripped.length % 2 == 0) { "Must have an even length" }

    try {
        return stripped
            .chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    } catch (e: NumberFormatException) {
        throw PatchException(
            "Could not parse pattern: $pattern. A pattern is a sequence of case insensitive strings " +
                "representing hexadecimal bytes separated by spaces",
            e,
        )
    }
}
