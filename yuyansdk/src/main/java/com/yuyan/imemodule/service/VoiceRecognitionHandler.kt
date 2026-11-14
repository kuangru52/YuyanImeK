package com.yuyan.imemodule.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.inputmethod.InputConnection
import android.widget.Toast
import com.yuyan.imemodule.speech.AsrBackendFactory
import com.yuyan.imemodule.speech.RecognitionState
import com.yuyan.imemodule.speech.SpeechRecognitionManager

data class VoiceState(
    val voicePluginName: String = "X-ASR",
    val voiceRecognitionState: RecognitionState = RecognitionState.IDLE,
    val voiceRecognizedText: String = "",
    val isVoiceMode: Boolean = false
)

class VoiceRecognitionHandler(
    private val context: Context,
    private val onStateChanged: (VoiceState) -> Unit,
    private val getState: () -> VoiceState,
    private val getInputConnection: () -> InputConnection?,
    private val onVoiceComplete: () -> Unit = {},
    private val onAmplitudeChanged: (Float) -> Unit = {},
    private val onSpectrumChanged: (FloatArray) -> Unit = {},
    private val onComposingWritten: () -> Unit = {},
    private val managerFactory: (Context) -> SpeechRecognitionManager = { SpeechRecognitionManager(it) },
    private val mainHandler: Handler = Handler(Looper.getMainLooper())
) {
    companion object {
        private const val TAG = "VoiceRecognition"
        private const val FINISH_TIMEOUT_MS = 3000L
        private const val CJK_CHAR = "\u3400-\u4DBF\u4E00-\u9FFF\uF900-\uFAFF\u3040-\u309F\u30A0-\u30FF"

        private val MULTI_SPACE_REGEX = Regex("[\\s\\u00A0\\u3000]+")
        private val CJK_SPACE_REGEX = Regex("(?<=[$CJK_CHAR])\\s+(?=[$CJK_CHAR])")
        private val CJK_PUNCT_SPACE_REGEX = Regex("(?<=[，。！？；：、])\\s+(?=[$CJK_CHAR])")
        private val SPACE_BEFORE_PUNCT_REGEX = Regex("\\s+([,.!?;:，。！？；：、])")
        private val ASCII_PUNCT_FOLLOWED_LATIN_REGEX = Regex("([,;:!?])(?=[A-Za-z])")
        private val PERIOD_FOLLOWED_UPPER_REGEX = Regex("(?<=[a-z])\\.(?=[A-Z])")
        private val CJK_CHAR_REGEX = Regex("[$CJK_CHAR]")

        private val LATIN_QUESTION_STARTERS = setOf(
            "what", "why", "how", "who", "whom", "whose", "where", "when", "which",
            "do", "does", "did", "is", "are", "was", "were", "am",
            "can", "could", "will", "would", "shall", "should", "may", "might",
            "have", "has", "had"
        )
    }

    private lateinit var speechRecognitionManager: SpeechRecognitionManager

    var textBeforeVoiceInput = ""
    var textLengthBeforeVoiceInput = 0

    fun initialize() {
        speechRecognitionManager = managerFactory(context)

        speechRecognitionManager.setCallbacks(
            onResult = { text ->
                handleSpeechResult(text)
            },
            onPartialResult = { text ->
                handlePartialResult(text)
            },
            onStateChange = { state ->
                handleSpeechStateChange(state)
            },
            onError = { error, userVisible ->
                handleSpeechError(error, userVisible)
            },
            onAmplitude = { amplitude ->
                handleAmplitudeUpdate(amplitude)
            },
            onSpectrum = { spectrum ->
                handleSpectrumUpdate(spectrum)
            }
        )

        val providerName = resolveProviderName()
        onStateChanged(getState().copy(voicePluginName = providerName))

        if (AsrBackendFactory.getLocalName(context) != null) {
            Thread {
                AsrBackendFactory.warmup(context)
            }.start()
        }
    }

    private val delayedPreStartRunnable = Runnable {
        if (::speechRecognitionManager.isInitialized) {
            speechRecognitionManager.startPreStart()
        }
    }

    fun startDelayedPreStart(delayMs: Long = 150) {
        mainHandler.removeCallbacks(delayedPreStartRunnable)
        mainHandler.postDelayed(delayedPreStartRunnable, delayMs)
    }

    fun cancelPreStart() {
        mainHandler.removeCallbacks(delayedPreStartRunnable)
        if (::speechRecognitionManager.isInitialized) {
            speechRecognitionManager.cancelPreStart()
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun startRecognition() {
        if (!::speechRecognitionManager.isInitialized) {
            onStateChanged(getState().copy(
                isVoiceMode = false,
                voiceRecognitionState = RecognitionState.ERROR
            ))
            return
        }

        finishing = false
        mainHandler.removeCallbacks(finishTimeoutRunnable)
        suppressDuplicateFinal = false
        resetInputBoxBookkeeping()

        textBeforeVoiceInput = getInputConnection()?.getTextBeforeCursor(1000, 0)?.toString() ?: ""
        textLengthBeforeVoiceInput = textBeforeVoiceInput.length

        val providerName = resolveProviderName()
        onStateChanged(getState().copy(
            voicePluginName = providerName,
            voiceRecognitionState = RecognitionState.PROCESSING
        ))

        speechRecognitionManager.startRecognition()
    }

    fun stopRecognition() {
        if (::speechRecognitionManager.isInitialized) {
            speechRecognitionManager.stopRecognition()
        }
    }

    fun release() {
        if (::speechRecognitionManager.isInitialized) {
            speechRecognitionManager.release()
        }
    }

    fun isInitialized(): Boolean = ::speechRecognitionManager.isInitialized

    private fun resolveProviderName(): String = "X-ASR"

    private var lastPartialText = ""
    private var lastWrittenText = ""
    private var consumedEnginePrefix = ""
    private var writtenTextVerified = false
    private var inputBoxEchoProbed = false
    private var lastAmplitudeUpdate = 0L
    private var smoothedAmplitude = 0f
    private var smoothedSpectrum = FloatArray(16)
    private var suppressDuplicateFinal = false
    @Volatile
    private var finishing = false
    private var sessionAbandoned = false
    private var errorToast: Toast? = null

    private val finishTimeoutRunnable = Runnable { onFinishTimeout() }

    fun abandonSession() {
        sessionAbandoned = true
        finishing = false
        mainHandler.removeCallbacks(finishTimeoutRunnable)
        lastPartialText = ""
        resetInputBoxBookkeeping()
    }

    fun sealPendingForSend() {
        if (sessionAbandoned) return
        finishing = false
        mainHandler.removeCallbacks(finishTimeoutRunnable)
        commitPendingOnRelease()
        suppressDuplicateFinal = true
    }

    fun commitPendingOnRelease() {
        if (sessionAbandoned) return
        val ic = getInputConnection()
        val partial = lastPartialText
        if (ic == null) return
        if (partial.isEmpty()) return
        reconcileWithInputBox(ic)
        val visibleText = visibleEngineText(normalizeAsrText(partial))
        suppressDuplicateFinal = true
        if (visibleText.isEmpty()) {
            lastPartialText = ""
            resetInputBoxBookkeeping()
            return
        }
        val punctuatedText = addPunctuation(visibleText)
        commitFinal(ic, punctuatedText, lastWrittenText)
        lastPartialText = ""
        resetInputBoxBookkeeping()
    }

    fun finishRecognition() {
        if (!::speechRecognitionManager.isInitialized) return
        if (finishing) return
        if (sessionAbandoned) {
            speechRecognitionManager.stopRecognition()
            return
        }
        if (lastPartialText.isEmpty()) {
            speechRecognitionManager.stopRecognition()
            onVoiceComplete()
            return
        }
        finishing = true
        onStateChanged(getState().copy(voiceRecognitionState = RecognitionState.PROCESSING))
        mainHandler.removeCallbacks(finishTimeoutRunnable)
        mainHandler.postDelayed(finishTimeoutRunnable, FINISH_TIMEOUT_MS)
        speechRecognitionManager.stopRecognition()
    }

    private fun onFinishTimeout() {
        if (!finishing) return
        finishing = false
        commitPendingOnRelease()
        onVoiceComplete()
    }

    private fun handleSpeechResult(text: String) {
        val wasFinishing = finishing
        if (finishing) {
            finishing = false
            mainHandler.removeCallbacks(finishTimeoutRunnable)
        }

        if (sessionAbandoned) {
            sessionAbandoned = false
            lastPartialText = ""
            onVoiceComplete()
            return
        }

        if (suppressDuplicateFinal) {
            suppressDuplicateFinal = false
            lastPartialText = ""
            onVoiceComplete()
            return
        }

        val cleanText = normalizeAsrText(text)
        val ic = getInputConnection()
        if (ic != null && cleanText.isNotEmpty() && !cleanText.startsWith("错误:")) {
            reconcileWithInputBox(ic)
            val visibleText = visibleEngineText(cleanText)
            if (visibleText.isNotEmpty()) {
                commitFinal(ic, addPunctuation(visibleText), lastWrittenText)
            }
        }
        lastPartialText = ""
        resetInputBoxBookkeeping()

        if (!wasFinishing) return
        onVoiceComplete()
    }

    private fun commitFinal(ic: InputConnection, finalText: String, partial: String) {
        ic.finishComposingText()
        if (partial.isNotEmpty() && finalText.startsWith(partial)) {
            val remainder = finalText.substring(partial.length)
            if (remainder.isNotEmpty()) {
                ic.commitText(remainder, 1)
            }
        } else {
            if (partial.isNotEmpty()) {
                ic.deleteSurroundingText(partial.length, 0)
            }
            ic.commitText(finalText, 1)
        }
    }

    private fun resetInputBoxBookkeeping() {
        lastWrittenText = ""
        consumedEnginePrefix = ""
        writtenTextVerified = false
        inputBoxEchoProbed = false
    }

    private fun verifyInputBoxEcho(ic: InputConnection, written: String) {
        if (inputBoxEchoProbed || written.isEmpty()) return
        inputBoxEchoProbed = true
        val onScreen = runCatching {
            ic.getTextBeforeCursor(written.length, 0)?.toString()
        }.getOrNull() ?: return
        if (onScreen == written) {
            writtenTextVerified = true
        }
    }

    private fun reconcileWithInputBox(ic: InputConnection) {
        if (lastWrittenText.isEmpty()) return
        val onScreen = runCatching {
            ic.getTextBeforeCursor(lastWrittenText.length, 0)?.toString()
        }.getOrNull() ?: return
        if (onScreen == lastWrittenText) {
            writtenTextVerified = true
            return
        }
        if (!writtenTextVerified) return
        if (onScreen.length >= lastWrittenText.length) return
        consumedEnginePrefix = lastPartialText
        textLengthBeforeVoiceInput = onScreen.length
        lastWrittenText = ""
    }

    private fun visibleEngineText(engineText: String): String {
        if (consumedEnginePrefix.isEmpty()) return engineText
        val visible = if (engineText.startsWith(consumedEnginePrefix)) {
            engineText.substring(consumedEnginePrefix.length)
        } else {
            engineText
        }
        return visible.trimStart('，', '。', '、', ',', '.', '！', '？', '；', '：', ' ', '\n')
    }

    internal fun normalizeAsrText(text: String): String {
        if (text.isEmpty()) return ""
        var result = MULTI_SPACE_REGEX.replace(text, " ").trim()
        if (result.isEmpty()) return ""
        result = CJK_SPACE_REGEX.replace(result, "")
        result = CJK_PUNCT_SPACE_REGEX.replace(result, "")
        result = SPACE_BEFORE_PUNCT_REGEX.replace(result, "\$1")
        result = ASCII_PUNCT_FOLLOWED_LATIN_REGEX.replace(result, "\$1 ")
        result = PERIOD_FOLLOWED_UPPER_REGEX.replace(result, ". ")
        return result
    }

    private fun addPunctuation(text: String): String {
        val cleanText = normalizeAsrText(text)
        if (cleanText.isEmpty()) return text

        val last = cleanText.last()
        if (CJK_CHAR_REGEX.containsMatchIn(cleanText)) {
            if (last in "。！？；：，、,.!?;:") return cleanText
            return "$cleanText${heuristicPunctuation(cleanText)}"
        }

        if (last in "。！？，；：") {
            return cleanText.dropLast(1) + when (last) {
                '。' -> "."
                '！' -> "!"
                '？' -> "?"
                '，' -> ","
                '；' -> ";"
                else -> ":"
            }
        }
        if (last in ",.!?;:") return cleanText
        return cleanText + heuristicLatinPunctuation(cleanText)
    }

    private fun heuristicPunctuation(text: String): String {
        return when {
            text.any { it in "吗呢么吧" } || text.contains("什么") || text.contains("怎么") || text.contains("为什么") || text.contains("如何") || text.contains("哪") -> "？"
            text.length < 4 -> "，"
            else -> "。"
        }
    }

    private fun heuristicLatinPunctuation(text: String): String {
        val firstWord = text.trimStart()
            .substringBefore(' ')
            .trim('"', '\'', '(', '[')
            .lowercase()
        return if (firstWord in LATIN_QUESTION_STARTERS) "?" else "."
    }

    private fun handlePartialResult(text: String) {
        if (sessionAbandoned || suppressDuplicateFinal) return
        val normalized = normalizeAsrText(text)
        if (normalized == lastPartialText) return

        val ic = getInputConnection()
        if (ic != null) reconcileWithInputBox(ic)

        lastPartialText = normalized

        val cleanText = visibleEngineText(normalized)
        if (cleanText.isEmpty()) return
        
        if (ic != null) {
            onComposingWritten()
            ic.setComposingText(cleanText, 1)
            lastWrittenText = cleanText
            verifyInputBoxEcho(ic, cleanText)
        }
        onStateChanged(getState().copy(voiceRecognizedText = cleanText))
    }

    private fun handleSpeechStateChange(state: RecognitionState) {
        if (state == RecognitionState.LISTENING) {
            lastPartialText = ""
            suppressDuplicateFinal = false
            sessionAbandoned = false
            resetInputBoxBookkeeping()
        }
        if (finishing && state == RecognitionState.IDLE) return
        onStateChanged(getState().copy(voiceRecognitionState = state))
    }

    private fun handleSpeechError(error: String, userVisible: Boolean) {
        Log.e(TAG, "Speech error: $error")
        val wasFinishing = finishing
        finishing = false
        mainHandler.removeCallbacks(finishTimeoutRunnable)
        lastPartialText = ""
        resetInputBoxBookkeeping()
        if (!wasFinishing) {
            suppressDuplicateFinal = true
            if (::speechRecognitionManager.isInitialized) {
                speechRecognitionManager.stopRecognition()
            }
        }
        if (userVisible && error.isNotBlank()) {
            errorToast?.cancel()
            errorToast = Toast.makeText(context, error, Toast.LENGTH_LONG)
            errorToast?.show()
        }
        onVoiceComplete()
    }

    private fun handleAmplitudeUpdate(amplitude: Float) {
        val now = System.currentTimeMillis()
        if (now - lastAmplitudeUpdate < 80) return
        lastAmplitudeUpdate = now
        smoothedAmplitude = smoothedAmplitude * 0.45f + amplitude * 0.55f
        onAmplitudeChanged(smoothedAmplitude)
    }

    private fun handleSpectrumUpdate(spectrum: FloatArray) {
        val smoothed = smoothedSpectrum
        for (i in spectrum.indices) {
            smoothed[i] = smoothed[i] * 0.5f + spectrum[i] * 0.5f
        }
        onSpectrumChanged(smoothed.copyOf())
    }
}
