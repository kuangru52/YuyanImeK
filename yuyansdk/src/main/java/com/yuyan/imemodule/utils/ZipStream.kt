package com.yuyan.imemodule.utils

import java.io.File
import java.util.zip.ZipInputStream

/**
 * @return top-level files in zip file
 */
fun ZipInputStream.extract(destDir: File): List<File> {
    var entry = nextEntry
    val canonicalDest = destDir.canonicalPath
    while (entry != null) {
        val targetFile = File(destDir, entry.name)
        if (!targetFile.canonicalPath.startsWith(canonicalDest)) throw SecurityException("Zip Slip detected: ${entry.name}")
        if (!entry.isDirectory) {
            targetFile.parentFile?.mkdirs()
            targetFile.outputStream().use { out -> copyTo(out) }
        } else {
            targetFile.mkdirs()
        }
        entry = nextEntry
    }
    return destDir.listFiles()?.toList() ?: emptyList()
}
