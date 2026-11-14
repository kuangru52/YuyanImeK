package com.yuyan.imemodule.speech

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import java.util.ArrayDeque

class SpeechRecognitionManager(private val context: Context) {

    companion object {
        private const val TAG = "SpeechRecognitionManager"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_SECONDS = 0.1f
        private const val SPEECH_THRESHOLD = 25
        private const val JOIN_TIMEOUT_MS = 5000L
    }

    private var backend: AsrBackend? = null
    private var recordingThread: RecordingThread? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var sessionId = 0

    @Volatile
    private var loadingInProgress = false
    @Volatile
    private var loadingCancelled = false

    private var resultCallback: ((String) -> Unit)? = null
    private var partialResultCallback: ((String) -> Unit)? = null
    private var stateCallback: ((RecognitionState) -> Unit)? = null
    @Volatile
    private var currentState: RecognitionState = RecognitionState.IDLE
    private var errorCallback: ((String, Boolean) -> Unit)? = null
    private var amplitudeCallback: ((Float) -> Unit)? = null
    private var spectrumCallback: ((FloatArray) -> Unit)? = null

    private var preStartedRecord: AudioRecord? = null
    private val preStartTimeoutRunnable = Runnable { cancelPreStart() }

    fun setCallbacks(
        onResult: (String) -> Unit,
        onPartialResult: (String) -> Unit,
        onStateChange: (RecognitionState) -> Unit,
        onError: (String, Boolean) -> Unit,
        onAmplitude: (Float) -> Unit,
        onSpectrum: (FloatArray) -> Unit
    ) {
        resultCallback = onResult
        partialResultCallback = onPartialResult
        stateCallback = onStateChange
        errorCallback = onError
        amplitudeCallback = onAmplitude
        spectrumCallback = onSpectrum
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecognition() {
        if (recordingThread != null) {
            return
        }

        setState(RecognitionState.PROCESSING)

        val currentBackend = synchronized(preloadLock) { backend }
        if (currentBackend == null || synchronized(preloadLock) { isPreloading }) {
            synchronized(preloadLock) {
                if (loadingInProgress) {
                    loadingCancelled = false
                    return
                }
                loadingInProgress = true
                loadingCancelled = false
            }
            Thread {
                try {
                    synchronized(preloadLock) {
                        while (isPreloading) {
                            try {
                                preloadLock.wait()
                            } catch (_: InterruptedException) {
                                Thread.currentThread().interrupt()
                                return@Thread
                            }
                        }
                    }
                    val ok = preload()
                    if (!ok || synchronized(preloadLock) { backend } == null) {
                        mainHandler.post {
                            errorCallback?.invoke("未导入离线语音模型：请在【设置-方案与数据-语音识别设置】中导入模型包", true)
                            setState(RecognitionState.ERROR)
                        }
                        return@Thread
                    }
                    if (loadingCancelled) {
                        mainHandler.post {
                            setState(RecognitionState.IDLE)
                        }
                        return@Thread
                    }
                    mainHandler.post {
                        if (recordingThread == null && !loadingCancelled) {
                            startRecording()
                        }
                    }
                } finally {
                    synchronized(preloadLock) {
                        loadingInProgress = false
                        preloadLock.notifyAll()
                    }
                }
            }.start()
            return
        }

        startRecording()
    }

    private fun startRecording() {
        val currentBackend = synchronized(preloadLock) { backend } ?: run {
            setState(RecognitionState.ERROR)
            return
        }
        synchronized(preloadLock) { sessionId++ }

        var preStarted: AudioRecord? = null
        synchronized(this) {
            preStarted = preStartedRecord
            preStartedRecord = null
        }
        mainHandler.removeCallbacks(preStartTimeoutRunnable)

        recordingThread = RecordingThread(currentBackend, preStarted)
        recordingThread!!.setCallbacks(
            onResult = { text -> resultCallback?.invoke(text) },
            onPartialResult = { text -> partialResultCallback?.invoke(text) },
            onError = { err -> errorCallback?.invoke(err, false) }
        )
        currentBackend.setCallbacks(
            onResult = { text -> resultCallback?.invoke(text) },
            onPartialResult = { text -> partialResultCallback?.invoke(text) },
            onStateChange = { state -> setState(state) },
            onError = { err -> errorCallback?.invoke(err, false) }
        )
        recordingThread!!.start()
    }

    fun stopRecognition() {
        val thread = recordingThread
        if (thread == null) {
            if (loadingInProgress) {
                loadingCancelled = true
                setState(RecognitionState.IDLE)
            }
            return
        }
        thread.stopRequested = true
        recordingThread = null
        setState(RecognitionState.PROCESSING)

        Thread {
            try {
                thread.join(JOIN_TIMEOUT_MS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
            mainHandler.post {
                setState(RecognitionState.IDLE)
            }
        }.start()
    }

    fun release() {
        stopRecognition()
        val b = synchronized(preloadLock) {
            val tmp = backend
            backend = null
            tmp
        }
        if (b != null) {
            Thread { b.release() }.start()
        }
    }

    private fun setState(state: RecognitionState) {
        currentState = state
        stateCallback?.invoke(state)
    }

    private val preloadLock = Object()
    private var isPreloading = false

    fun startPreStart() {}

    fun cancelPreStart() {
        mainHandler.removeCallbacks(preStartTimeoutRunnable)
        synchronized(this) {
            preStartedRecord?.release()
            preStartedRecord = null
        }
    }

    private fun preload(): Boolean {
        synchronized(preloadLock) {
            if (backend != null) return true
            isPreloading = true
        }
        try {
            val b = AsrBackendFactory.create(context)
            if (b != null) {
                val ok = b.initialize()
                if (ok) {
                    synchronized(preloadLock) {
                        backend = b
                    }
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "preload failed", e)
        } finally {
            synchronized(preloadLock) {
                isPreloading = false
            }
        }
        return false
    }

    private fun isSpeech(data: ByteArray): Boolean {
        var energy = 0L
        for (i in data.indices step 2) {
            if (i + 1 >= data.size) break
            val s = (data[i].toInt() and 0xFF) or ((data[i + 1].toInt() and 0xFF) shl 8)
            val sample = if (s > 32767) s - 65536 else s
            energy += (sample * sample).toLong()
        }
        val rms = kotlin.math.sqrt((energy / (data.size / 2)).toDouble())
        return rms > SPEECH_THRESHOLD
    }

    private inner class RecordingThread(
        private val currentBackend: AsrBackend,
        private val preStarted: AudioRecord? = null
    ) : Thread("AsrRecording") {

        private val spectrumAnalyzer = SpectrumAnalyzer()

        @Volatile
        var stopRequested = false

        @Volatile
        private var audioRecord: AudioRecord? = null

        private var resResultCallback: ((String) -> Unit)? = null
        private var resPartialCallback: ((String) -> Unit)? = null
        private var resErrorCallback: ((String) -> Unit)? = null

        fun setCallbacks(
            onResult: (String) -> Unit,
            onPartialResult: (String) -> Unit,
            onError: (String) -> Unit
        ) {
            resResultCallback = onResult
            resPartialCallback = onPartialResult
            resErrorCallback = onError
        }

        override fun run() {
            val audioRecord = preStarted ?: (createAudioRecord() ?: run {
                mainHandler.post {
                    errorCallback?.invoke("无法启动录音", false)
                    setState(RecognitionState.ERROR)
                }
                return
            })
            this.audioRecord = audioRecord

            if (!currentBackend.start()) {
                try { audioRecord.stop(); audioRecord.release() } catch (_: Exception) {}
                mainHandler.post {
                    errorCallback?.invoke("启动引擎失败", false)
                    setState(RecognitionState.ERROR)
                }
                return
            }

            try {
                if (audioRecord.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.startRecording()
                }
            } catch (e: Exception) {
                mainHandler.post {
                    errorCallback?.invoke("录音启动失败，请检查麦克风权限", true)
                    setState(RecognitionState.ERROR)
                }
                return
            }
            mainHandler.post {
                setState(RecognitionState.LISTENING)
            }

            val buffer = ShortArray((SAMPLE_RATE * BUFFER_SIZE_SECONDS).toInt())
            val byteBuffer = ByteArray(buffer.size * 2)
            var speechDetected = false
            val preSpeechBuffer = ArrayDeque<ByteArray>()
            val maxPreSpeechChunks = 4

            try {
                while (!interrupted() && !stopRequested) {
                    val nread = audioRecord.read(buffer, 0, buffer.size)
                    if (nread > 0) {
                        var peak = 0
                        for (i in 0 until nread) {
                            val s = buffer[i].toInt()
                            byteBuffer[i * 2] = (s and 0xFF).toByte()
                            byteBuffer[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
                            val abs = if (s < 0) -s else s
                            if (abs > peak) peak = abs
                        }
                        val normalized = (peak / 32768f).coerceIn(0f, 1f)
                        val spectrum = spectrumAnalyzer.analyze(buffer, nread)
                        mainHandler.post {
                            amplitudeCallback?.invoke(normalized)
                            spectrumCallback?.invoke(spectrum)
                        }

                        val chunk = byteBuffer.copyOf(nread * 2)
                        if (!speechDetected) {
                            preSpeechBuffer.addLast(chunk)
                            if (preSpeechBuffer.size >= maxPreSpeechChunks) {
                                speechDetected = true
                                while (preSpeechBuffer.isNotEmpty()) {
                                    currentBackend.processAudioChunk(preSpeechBuffer.removeFirst())
                                }
                            } else if (isSpeech(chunk)) {
                                speechDetected = true
                                while (preSpeechBuffer.isNotEmpty()) {
                                    currentBackend.processAudioChunk(preSpeechBuffer.removeFirst())
                                }
                            }
                        } else {
                            currentBackend.processAudioChunk(chunk)
                        }
                    } else if (nread < 0) {
                        break
                    }
                }
            } catch (_: Exception) {
            } finally {
                try { audioRecord.stop() } catch (_: Exception) { }
                try { audioRecord.release() } catch (_: Exception) { }
                try { currentBackend.stop() } catch (_: Exception) { }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun createAudioRecord(): AudioRecord? {
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = maxOf(minBufferSize, SAMPLE_RATE * 2)
        try {
            val record = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
            if (record.state == AudioRecord.STATE_INITIALIZED) {
                return record
            }
            record.release()
        } catch (_: Exception) {
        }
        return null
    }
}
