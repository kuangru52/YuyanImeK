package com.yuyan.imemodule.speech

import android.content.Context
import com.yuyan.imemodule.service.AsrInferenceClient
import kotlinx.coroutines.runBlocking

class OfflineAsrBackend(private val context: Context) : AsrBackend {

    override val name: String
        get() = AsrModelManager(context).getSelectedModelInfo()?.name ?: "本地语音识别"

    private val client = AsrInferenceClient(context)
    private var initialized = false

    private var resultCallback: ((String) -> Unit)? = null
    private var partialResultCallback: ((String) -> Unit)? = null
    private var stateCallback: ((RecognitionState) -> Unit)? = null
    private var errorCallback: ((String) -> Unit)? = null

    override fun setCallbacks(
        onResult: (String) -> Unit,
        onPartialResult: ((String) -> Unit)?,
        onStateChange: (RecognitionState) -> Unit,
        onError: (String) -> Unit
    ) {
        resultCallback = onResult
        partialResultCallback = onPartialResult
        stateCallback = onStateChange
        errorCallback = onError
    }

    private val asrCallback = object : AsrInferenceClient.AsrCallback {
        override fun onPartialResult(text: String) {
            partialResultCallback?.invoke(text)
        }

        override fun onFinalResult(text: String) {
            resultCallback?.invoke(text)
        }

        override fun onError(message: String) {
            errorCallback?.invoke(message)
        }
    }

    override fun initialize(): Boolean {
        return try {
            val ok = runBlocking { client.ensureBound() }
            if (!ok) {
                return false
            }
            initialized = true
            try {
                val modelManager = AsrModelManager(context)
                val modelDir = modelManager.getSelectedModelDir()?.absolutePath
                if (modelManager.isModelReady() && modelDir != null) {
                    val preStartOk = runBlocking { client.startAsr(modelDir, asrCallback) }
                    if (!preStartOk) {
                        android.util.Log.e("OfflineAsrBackend", "Pre-start ASR failed for $modelDir")
                    } else {
                        runBlocking { client.stopAsr() }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("OfflineAsrBackend", "Error pre-starting ASR model", e)
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("OfflineAsrBackend", "Error binding ASR service", e)
            false
        }
    }

    override fun start(): Boolean {
        if (!initialized) return false
        return try {
            val modelManager = AsrModelManager(context)
            if (!modelManager.isModelReady()) {
                errorCallback?.invoke("离线语音模型未下载，请先在设置中下载模型")
                return false
            }
            val modelDir = modelManager.getSelectedModelDir()?.absolutePath ?: return false
            runBlocking { client.startAsr(modelDir, asrCallback) }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        } catch (_: Exception) {
            false
        }
    }

    override fun processAudioChunk(buffer: ByteArray) {
        if (!initialized) return
        client.pushAsrAudio(buffer)
    }

    override fun stop() {
        if (!initialized) return
        val text = try {
            runBlocking { client.stopAsr() }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            ""
        } catch (_: Exception) {
            ""
        }
        if (text.isNotEmpty()) {
            resultCallback?.invoke(text)
        }
        stateCallback?.invoke(RecognitionState.IDLE)
    }

    override fun cancel() {
        if (initialized) {
            client.cancelAsr()
        }
    }

    override fun release() {
        if (initialized) {
            try {
                runBlocking { client.releaseAsr() }
            } catch (_: Exception) {}
            client.unbind()
            initialized = false
        }
    }
}
