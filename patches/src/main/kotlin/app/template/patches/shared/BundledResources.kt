package app.template.patches.shared

import java.io.InputStream

/** Loads a file packed under patches/src/main/resources/<folder>/<name>. */
internal fun inputStreamFromBundledResource(folder: String, name: String): InputStream? {
    val path = "$folder/$name"
    return Thread.currentThread().contextClassLoader?.getResourceAsStream(path)
        ?: object {}.javaClass.classLoader?.getResourceAsStream(path)
        ?: ClassLoader.getSystemResourceAsStream(path)
}
