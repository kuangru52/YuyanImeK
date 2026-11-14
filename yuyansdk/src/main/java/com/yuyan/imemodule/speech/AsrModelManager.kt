package com.yuyan.imemodule.speech

import android.content.Context
import com.yuyan.imemodule.speech.models.AsrModelProfile
import com.yuyan.imemodule.speech.models.AsrModelRegistry
import com.yuyan.imemodule.utils.queryFileName
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

class AsrModelManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "asr_model"
        private const val KEY_SELECTED_MODEL = "selected_model"
        private const val TOKENS_FILE = "tokens.txt"

        private fun AsrModelProfile.toAsrModelInfo(): AsrModelInfo = AsrModelInfo(
            id = id,
            name = name,
            description = description,
            language = language,
            size = size,
            downloadUrl = downloadUrl,
            modelType = "transducer",
            files = listOf(encoderFile, decoderFile, joinerFile, tokensFile),
            encoderFile = encoderFile,
            decoderFile = decoderFile,
            joinerFile = joinerFile
        )
    }

    data class AsrModelInfo(
        val id: String,
        val name: String,
        val description: String = "",
        val language: String,
        val size: String,
        val downloadUrl: String,
        val modelType: String = "transducer",
        val files: List<String>,
        val encoderFile: String = "",
        val decoderFile: String = "",
        val joinerFile: String = "",
        val needsAutoPunctuation: Boolean = true
    )

    fun getAsrModels(): List<AsrModelInfo> {
        return AsrModelRegistry.profiles.map { it.toAsrModelInfo() }
    }

    fun isModelReady(): Boolean {
        return getSelectedModelInfo() != null
    }

    fun ensureAssetsModelInstalled(modelId: String = "zipformer-zh-int8") {
        // 已解除内置模型自动安装，由用户手动导入模型包
    }

    fun getModelInfo(modelId: String): AsrModelInfo =
        getAsrModels().find { it.id == modelId }
            ?: AsrModelRegistry.profileOrDefault(modelId).toAsrModelInfo()

    fun findEncoderFile(dir: File): File? = findFileMatching(dir) { it.name.contains("encoder", ignoreCase = true) && it.name.endsWith(".onnx", ignoreCase = true) }
    fun findDecoderFile(dir: File): File? = findFileMatching(dir) { it.name.contains("decoder", ignoreCase = true) && it.name.endsWith(".onnx", ignoreCase = true) }
    fun findJoinerFile(dir: File): File? = findFileMatching(dir) { it.name.contains("joiner", ignoreCase = true) && it.name.endsWith(".onnx", ignoreCase = true) }
    fun findTokensFile(dir: File): File? = findFileMatching(dir) { it.name.contains("tokens", ignoreCase = true) && it.name.endsWith(".txt", ignoreCase = true) }

    fun findFileMatching(dir: File, predicate: (File) -> Boolean): File? {
        dir.listFiles()?.forEach { child ->
            if (child.isFile && predicate(child) && child.length() > 0) return child
            if (child.isDirectory) {
                val found = findFileMatching(child, predicate)
                if (found != null) return found
            }
        }
        return null
    }

    fun flattenModelDirectory(targetDir: File) {
        if (!targetDir.isDirectory) return

        fun promoteFiles(dir: File) {
            dir.listFiles()?.forEach { child ->
                if (child.isDirectory) {
                    promoteFiles(child)
                    child.delete()
                } else if (child.isFile) {
                    if (child.parentFile != targetDir) {
                        val dest = File(targetDir, child.name)
                        child.renameTo(dest)
                    }
                }
            }
        }
        promoteFiles(targetDir)
        targetDir.listFiles()?.forEach { file ->
            if (file.isFile) {
                val name = file.name.lowercase()
                when {
                    name.contains("encoder") && name.endsWith(".onnx") && file.name != "encoder.int8.onnx" && file.name != "encoder.onnx" -> {
                        val targetName = if (name.contains("int8")) "encoder.int8.onnx" else "encoder.onnx"
                        file.renameTo(File(targetDir, targetName))
                    }
                    name.contains("decoder") && name.endsWith(".onnx") && file.name != "decoder.onnx" -> {
                        file.renameTo(File(targetDir, "decoder.onnx"))
                    }
                    name.contains("joiner") && name.endsWith(".onnx") && file.name != "joiner.int8.onnx" && file.name != "joiner.onnx" -> {
                        val targetName = if (name.contains("int8")) "joiner.int8.onnx" else "joiner.onnx"
                        file.renameTo(File(targetDir, targetName))
                    }
                    name.contains("tokens") && name.endsWith(".txt") && file.name != "tokens.txt" -> {
                        file.renameTo(File(targetDir, "tokens.txt"))
                    }
                }
            }
        }
    }

    fun isModelInstalled(modelId: String): Boolean {
        val dir = getModelDir(modelId)
        if (!dir.isDirectory) {
            android.util.Log.d("AsrModelManager", "isModelInstalled: $modelId dir does not exist (${dir.absolutePath})")
            return false
        }
        val enc = findEncoderFile(dir)
        val dec = findDecoderFile(dir)
        val jn = findJoinerFile(dir)
        val tok = findTokensFile(dir)
        val installed = enc != null && dec != null && jn != null && tok != null
        android.util.Log.d("AsrModelManager", "isModelInstalled: $modelId -> enc=$enc, dec=$dec, jn=$jn, tok=$tok => installed=$installed")
        return installed
    }

    fun getModelDir(modelId: String): File {
        return File(context.filesDir, "models/$modelId")
    }

    fun getSelectedModelDir(): File? {
        val info = getSelectedModelInfo() ?: return null
        val dir = getModelDir(info.id)
        if (dir.isDirectory && isModelInstalled(info.id)) return dir
        val modelsRoot = File(context.filesDir, "models")
        if (modelsRoot.isDirectory) {
            modelsRoot.listFiles()?.forEach { sub ->
                if (sub.isDirectory && isModelInstalled(sub.name)) {
                    return sub
                }
            }
        }
        return dir
    }

    fun getSelectedModelId(): String? {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getString(KEY_SELECTED_MODEL, null)?.takeIf { it.isNotBlank() }
    }

    fun getSelectedModelInfo(): AsrModelInfo? {
        val models = getAsrModels()
        val selectedId = getSelectedModelId()
        if (selectedId != null && isModelInstalled(selectedId)) {
            return models.find { it.id == selectedId }
                ?: AsrModelRegistry.profileOrDefault(selectedId).toAsrModelInfo()
        }
        val found = models.find { isModelInstalled(it.id) }
        if (found != null) return found

        val modelsRoot = File(context.filesDir, "models")
        if (modelsRoot.isDirectory) {
            modelsRoot.listFiles()?.forEach { dir ->
                if (dir.isDirectory && isModelInstalled(dir.name)) {
                    return getModelInfo(dir.name)
                }
            }
        }
        return null
    }

    fun setModel(modelId: String) {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().putString(KEY_SELECTED_MODEL, modelId).apply()
    }

    fun importModelFromArchive(uri: android.net.Uri, modelId: String = "zipformer-zh-int8"): Boolean {
        val targetDir = getModelDir(modelId)
        if (targetDir.exists()) {
            targetDir.deleteRecursively()
        }
        targetDir.mkdirs()

        val cr = context.contentResolver
        return try {
            val rawStream = cr.openInputStream(uri) ?: return false
            val success = BufferedInputStream(rawStream, 128 * 1024).use { bufferedStream ->
                bufferedStream.mark(4)
                val header = ByteArray(4)
                val readCount = bufferedStream.read(header, 0, 4)
                bufferedStream.reset()

                val fileName = cr.queryFileName(uri)?.lowercase() ?: ""

                when {
                    (readCount >= 2 && header[0] == 'B'.code.toByte() && header[1] == 'Z'.code.toByte()) || fileName.contains(".bz2") -> {
                        extractTarBz2(bufferedStream, targetDir)
                    }
                    (readCount >= 2 && header[0] == 0x1F.toByte() && header[1] == 0x8B.toByte()) || fileName.contains(".gz") -> {
                        extractTarGz(bufferedStream, targetDir)
                    }
                    (readCount >= 4 && header[0] == 'P'.code.toByte() && header[1] == 'K'.code.toByte() && header[2] == 3.toByte() && header[3] == 4.toByte()) || fileName.endsWith(".zip") -> {
                        extractZip(bufferedStream, targetDir)
                    }
                    else -> {
                        extractTarBz2(bufferedStream, targetDir) || extractZip(bufferedStream, targetDir)
                    }
                }
            }
            if (success) {
                flattenModelDirectory(targetDir)
                setModel(modelId)
            }
            val installed = isModelInstalled(modelId)
            android.util.Log.i("AsrModelManager", "importModelFromArchive: modelId=$modelId, installed=$installed, files=${targetDir.list()?.joinToString()}")
            installed
        } catch (e: Exception) {
            android.util.Log.e("AsrModelManager", "importModelFromArchive failed", e)
            false
        }
    }

    private fun extractZip(inputStream: InputStream, targetDir: File): Boolean {
        return try {
            val zipIn = ZipInputStream(inputStream)
            var entry = zipIn.nextEntry
            var count = 0
            val buffer = ByteArray(64 * 1024)
            while (entry != null) {
                val fName = File(entry.name).name
                if (!entry.isDirectory && fName.isNotBlank()) {
                    val destFile = File(targetDir, fName)
                    BufferedOutputStream(destFile.outputStream(), 64 * 1024).use { out ->
                        var len: Int
                        while (zipIn.read(buffer).also { len = it } != -1) {
                            out.write(buffer, 0, len)
                        }
                    }
                    count++
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
            zipIn.close()
            count > 0
        } catch (_: Exception) {
            false
        }
    }

    private fun extractTarBz2(inputStream: InputStream, targetDir: File): Boolean {
        return try {
            val bz2In = BZip2CompressorInputStream(inputStream, true)
            val tarIn = TarArchiveInputStream(bz2In)
            extractTarStream(tarIn, targetDir)
        } catch (_: Exception) {
            false
        }
    }

    private fun extractTarGz(inputStream: InputStream, targetDir: File): Boolean {
        return try {
            val gzIn = GzipCompressorInputStream(inputStream, true)
            val tarIn = TarArchiveInputStream(gzIn)
            extractTarStream(tarIn, targetDir)
        } catch (_: Exception) {
            false
        }
    }

    private fun extractTarStream(tarIn: TarArchiveInputStream, targetDir: File): Boolean {
        return try {
            var entry = tarIn.nextTarEntry
            var count = 0
            val buffer = ByteArray(64 * 1024)
            val maxFileSize = 500 * 1024 * 1024L
            while (entry != null) {
                val fName = File(entry.name).name
                if (!entry.isDirectory && fName.isNotBlank() && entry.size < maxFileSize) {
                    val destFile = File(targetDir, fName)
                    BufferedOutputStream(destFile.outputStream(), 64 * 1024).use { out ->
                        var len: Int
                        while (tarIn.read(buffer).also { len = it } != -1) {
                            out.write(buffer, 0, len)
                        }
                    }
                    count++
                }
                entry = tarIn.nextTarEntry
            }
            tarIn.close()
            count > 0
        } catch (_: Exception) {
            false
        }
    }
}
