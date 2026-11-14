package com.yuyan.imemodule.keyboard.container

import android.annotation.SuppressLint
import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.yuyan.imemodule.data.theme.ThemeManager
import com.yuyan.imemodule.entity.keyboard.SoftKey
import com.yuyan.imemodule.keyboard.InputView
import com.yuyan.imemodule.service.DictDecoder
import com.yuyan.imemodule.service.VoiceRecognitionHandler
import com.yuyan.imemodule.speech.RecognitionState
import com.yuyan.imemodule.ui.keyboard.VoiceKeyboardLayout
import com.yuyan.imemodule.utils.DevicesUtils

@SuppressLint("ViewConstructor")
class VoiceKeyboardContainer(context: Context, inputView: InputView) : BaseContainer(context, inputView) {

    private var voiceState by mutableStateOf(RecognitionState.IDLE)
    private var voiceAmplitude by mutableFloatStateOf(0f)
    private var voiceSpectrum by mutableStateOf(FloatArray(16))
    private var isVoiceActive by mutableStateOf(false)

    private val voiceHandler = VoiceRecognitionHandler(
        context = context,
        onStateChanged = { state ->
            voiceState = state.voiceRecognitionState
            if (state.voiceRecognitionState == RecognitionState.ERROR) {
                isVoiceActive = false
                val isModelReady = com.yuyan.imemodule.speech.AsrModelManager(context).isModelReady()
                val msg = if (isModelReady) "语音引擎启动异常，请重试" else "未导入离线语音模型：请在【设置-方案与数据-语音识别设置】中导入模型包"
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        },
        getState = { com.yuyan.imemodule.service.VoiceState(voiceRecognitionState = voiceState) },
        getInputConnection = { inputView.imeService.currentInputConnection },
        onVoiceComplete = { isVoiceActive = false },
        onAmplitudeChanged = { amp -> voiceAmplitude = amp },
        onSpectrumChanged = { spec -> voiceSpectrum = spec }
    )

    init {
        voiceHandler.initialize()

        val composeView = ComposeView(context).apply {
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
            ).apply {
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            }
            setViewTreeLifecycleOwner(inputView.imeService)
            setViewTreeSavedStateRegistryOwner(inputView.imeService)
            setViewTreeViewModelStoreOwner(inputView.imeService)
            setContent {
                val theme = ThemeManager.activeTheme
                VoiceKeyboardLayout(
                    keyBackgroundColor = androidx.compose.ui.graphics.Color(theme.keyBackgroundColor),
                    keyTextColor = androidx.compose.ui.graphics.Color(theme.keyTextColor),
                    specialKeyBackgroundColor = androidx.compose.ui.graphics.Color(theme.functionKeyBackgroundColor),
                    keyboardBackgroundColor = androidx.compose.ui.graphics.Color(theme.keyboardColor),
                    accentKeyBackgroundColor = androidx.compose.ui.graphics.Color(theme.accentKeyBackgroundColor),
                    keyPressHighlightColor = androidx.compose.ui.graphics.Color(theme.keyPressHighlightColor),
                    isDarkTheme = theme.isDark,
                    bottomActive = isVoiceActive || voiceState == RecognitionState.LISTENING || voiceState == RecognitionState.PROCESSING,
                    recognitionState = voiceState,
                    amplitude = voiceAmplitude,
                    spectrum = voiceSpectrum,
                    onDeleteClick = {
                        DevicesUtils.tryVibrate(this@VoiceKeyboardContainer)
                        DevicesUtils.tryPlayKeyDown(KeyEvent.KEYCODE_DEL)
                        inputView.responseKeyEvent(SoftKey(KeyEvent.KEYCODE_DEL))
                    },
                    onDeleteRepeat = {
                        inputView.responseKeyEvent(SoftKey(KeyEvent.KEYCODE_DEL))
                    },
                    onClearAll = {
                        DevicesUtils.tryVibrate(this@VoiceKeyboardContainer)
                        DevicesUtils.tryPlayKeyDown(KeyEvent.KEYCODE_DEL)
                        if (!DictDecoder.isCandidatesEmpty) {
                            DictDecoder.reset()
                            inputView.updateCandidateBar()
                        } else {
                            inputView.imeService.deleteSurroundingText(10000, 10000)
                        }
                    },
                    onSendClick = {
                        DevicesUtils.tryVibrate(this@VoiceKeyboardContainer)
                        DevicesUtils.tryPlayKeyDown(KeyEvent.KEYCODE_ENTER)
                        voiceHandler.sealPendingForSend()
                        inputView.imeService.sendEnterKeyEvent()
                    },
                    onStartVoice = {
                        if (!isVoiceActive) {
                            checkAndStartVoice()
                        }
                    },
                    onStopVoice = {
                        if (isVoiceActive) {
                            isVoiceActive = false
                            DevicesUtils.tryVibrate(this@VoiceKeyboardContainer, isRelease = true)
                            voiceHandler.finishRecognition()
                        }
                    },
                    onCursorMove = { direction ->
                        inputView.imeService.moveCursorSafely(direction)
                    }
                )
            }
        }
        addView(composeView)
    }

    private fun checkAndStartVoice() {
        val permission = Permission.RECORD_AUDIO
        if (XXPermissions.isGranted(context, permission)) {
            isVoiceActive = true
            DevicesUtils.tryVibrate(this@VoiceKeyboardContainer, isRelease = false)
            DevicesUtils.tryPlayKeyDown(KeyEvent.KEYCODE_SPACE)
            voiceHandler.startRecognition()
        } else {
            Toast.makeText(context, "语音输入需要麦克风权限，请允许录音权限", Toast.LENGTH_LONG).show()
            XXPermissions.with(context)
                .permission(permission)
                .request(object : OnPermissionCallback {
                    override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                        if (allGranted) {
                            isVoiceActive = true
                            DevicesUtils.tryVibrate(this@VoiceKeyboardContainer, isRelease = false)
                            DevicesUtils.tryPlayKeyDown(KeyEvent.KEYCODE_SPACE)
                            voiceHandler.startRecognition()
                        }
                    }

                    override fun onDenied(permissions: MutableList<String>, doNotAskAgain: Boolean) {
                        Toast.makeText(context, "未获取麦克风权限，请在系统设置中允许录音权限", Toast.LENGTH_SHORT).show()
                        XXPermissions.startPermissionActivity(context, permissions)
                    }
                })
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        return super.dispatchTouchEvent(ev)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!voiceHandler.isInitialized()) {
            voiceHandler.initialize()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isVoiceActive = false
        voiceHandler.abandonSession()
        voiceHandler.stopRecognition()
    }
}
