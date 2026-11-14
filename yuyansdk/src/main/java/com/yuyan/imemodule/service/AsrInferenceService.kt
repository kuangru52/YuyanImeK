package com.yuyan.imemodule.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.yuyan.imemodule.speech.AsrModelManager
import com.yuyan.imemodule.speech.AsrNative
import java.io.File

class AsrInferenceService : Service() {

    companion object {
        private const val TAG = "AsrInferenceService"
        private const val IDLE_RELEASE_DELAY_MS = 60_000L

        init {
            try {
                System.loadLibrary("onnxruntime")
                System.loadLibrary("onnx_env")
                System.loadLibrary("asr_jni")
                Log.i(TAG, "Loaded native libraries in AsrInferenceService companion init")
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to load native libraries in AsrInferenceService companion init", e)
            }
        }

        internal fun shouldReloadHandle(handle: Long, loadedDir: String?, requestedDir: String): Boolean =
            handle == 0L || loadedDir != requestedDir
    }

    private var asrHandle: Long = 0L
    private var asrCallback: IInferenceAsrCallback? = null
    private val asrLock = Any()

    private var loadedModelDir: String? = null

    @Volatile
    private var keepModelAlive = false

    private val idleHandler = Handler(Looper.getMainLooper())
    private val idleReleaseRunnable = Runnable {
        synchronized(asrLock) {
            if (asrHandle != 0L) {
                AsrNative.nativeRelease(asrHandle)
                asrHandle = 0L
                asrCallback = null
                loadedModelDir = null
            }
        }
    }

    private fun scheduleIdleRelease() {
        if (keepModelAlive) return
        idleHandler.removeCallbacks(idleReleaseRunnable)
        idleHandler.postDelayed(idleReleaseRunnable, IDLE_RELEASE_DELAY_MS)
    }

    private fun cancelIdleRelease() {
        idleHandler.removeCallbacks(idleReleaseRunnable)
    }

    private val binder = object : IInferenceAsrService.Stub() {

        override fun startAsr(modelDir: String, callback: IInferenceAsrCallback): Boolean {
            cancelIdleRelease()
            val needReload = synchronized(asrLock) {
                shouldReloadHandle(asrHandle, loadedModelDir, modelDir)
            }
            if (needReload) {
                val handle = createAsrHandle(modelDir)
                if (handle == 0L) {
                    Log.e(TAG, "Failed to create ASR recognizer from $modelDir")
                    return false
                }
                synchronized(asrLock) {
                    if (shouldReloadHandle(asrHandle, loadedModelDir, modelDir)) {
                        if (asrHandle != 0L) {
                            AsrNative.nativeRelease(asrHandle)
                        }
                        asrHandle = handle
                        loadedModelDir = modelDir
                    } else {
                        AsrNative.nativeRelease(handle)
                    }
                }
            }
            return try {
                synchronized(asrLock) {
                    asrCallback = callback
                    if (asrHandle != 0L) {
                        AsrNative.nativeReset(asrHandle)
                    }
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "startAsr failed", e)
                false
            }
        }

        override fun pushAsrAudio(audioData: ByteArray) {
            val partial: String
            val cb: IInferenceAsrCallback?
            synchronized(asrLock) {
                if (asrHandle == 0L) return
                AsrNative.nativeAcceptPcm(asrHandle, audioData)
                partial = AsrNative.nativeGetPartial(asrHandle)
                cb = asrCallback
            }
            if (cb != null && partial.isNotEmpty()) {
                try {
                    cb.onPartialResult(partial)
                } catch (_: Exception) {}
            }
        }

        override fun stopAsr(): String {
            val text = try {
                synchronized(asrLock) {
                    if (asrHandle == 0L) ""
                    else AsrNative.nativeFinalize(asrHandle)
                }
            } catch (e: Exception) {
                Log.e(TAG, "stopAsr failed", e)
                ""
            }
            scheduleIdleRelease()
            synchronized(asrLock) { asrCallback = null }
            return text
        }

        override fun cancelAsr() {
            synchronized(asrLock) {
                if (asrHandle != 0L) AsrNative.nativeReset(asrHandle)
                asrCallback = null
            }
            scheduleIdleRelease()
        }

        override fun releaseAsr() {
            cancelIdleRelease()
            synchronized(asrLock) {
                if (asrHandle != 0L) {
                    AsrNative.nativeRelease(asrHandle)
                    asrHandle = 0L
                }
                asrCallback = null
                loadedModelDir = null
            }
        }

        override fun setKeepModelAlive(keepAlive: Boolean) {
            keepModelAlive = keepAlive
            if (keepAlive) {
                cancelIdleRelease()
            } else if (synchronized(asrLock) { asrHandle } != 0L) {
                scheduleIdleRelease()
            }
        }
    }

    private fun createAsrHandle(modelDir: String): Long {
        return try {
            val dir = File(modelDir)
            val manager = AsrModelManager(this)
            manager.flattenModelDirectory(dir)

            val encoder = manager.findEncoderFile(dir)
            val decoder = manager.findDecoderFile(dir)
            val joiner = manager.findJoinerFile(dir)
            val tokens = manager.findTokensFile(dir)

            Log.i(TAG, "createAsrHandle dir=${dir.absolutePath}, enc=$encoder, dec=$decoder, jn=$joiner, tok=$tokens")

            if (encoder == null || decoder == null || joiner == null || tokens == null) {
                Log.e(TAG, "ASR model files incomplete in $modelDir")
                return 0L
            }

            val handle = AsrNative.nativeCreate(
                encoder.absolutePath,
                decoder.absolutePath,
                joiner.absolutePath,
                tokens.absolutePath
            )
            Log.i(TAG, "nativeCreate result handle=$handle")
            handle
        } catch (e: Throwable) {
            Log.e(TAG, "createAsrHandle failed", e)
            0L
        }
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onDestroy() {
        cancelIdleRelease()
        synchronized(asrLock) {
            if (asrHandle != 0L) {
                AsrNative.nativeRelease(asrHandle)
                asrHandle = 0L
            }
            asrCallback = null
            loadedModelDir = null
        }
        super.onDestroy()
    }
}
