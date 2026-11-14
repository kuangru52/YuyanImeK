package com.yuyan.imemodule.service

import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.text.InputType
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.lifecycle.Observer
import com.yuyan.imemodule.callback.IKeyboardView
import com.yuyan.imemodule.candidate.CandidateView
import com.yuyan.imemodule.data.emojicon.YuyanEmojiCompat
import com.yuyan.imemodule.data.theme.Theme
import com.yuyan.imemodule.data.theme.ThemeManager.OnThemeChangeListener
import com.yuyan.imemodule.data.theme.ThemeManager.addOnChangedListener
import com.yuyan.imemodule.data.theme.ThemeManager.onSystemDarkModeChange
import com.yuyan.imemodule.data.theme.ThemeManager.removeOnChangedListener
import com.yuyan.imemodule.keyboard.InputView
import com.yuyan.imemodule.keyboard.KeyboardManager
import com.yuyan.imemodule.keyboard.container.ClipBoardContainer
import com.yuyan.imemodule.manager.InputModeSwitcher
import com.yuyan.imemodule.prefs.AppPrefs.Companion.getInstance
import com.yuyan.imemodule.prefs.behavior.SkbMenuMode
import com.yuyan.imemodule.singleton.EnvironmentSingleton
import com.yuyan.imemodule.utils.KeyboardLoaderUtil
import com.yuyan.imemodule.utils.StringUtils
import com.yuyan.imemodule.utils.isDarkMode
import com.yuyan.imemodule.view.preference.ManagedPreference
import splitties.bitflags.hasFlag
import splitties.dimensions.dp

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * Main class of the Pinyin input method. 输入法服务
 */
class ImeService : InputMethodService(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store
    private var isHardwareKeyboard = false
    private var showVirtualKeyboardOnPhysicalKeyboard = false
    private lateinit var  candidateView:CandidateView
    private lateinit var  inputView:InputView
    private lateinit var mInputView:IKeyboardView
    private val onThemeChangeListener = OnThemeChangeListener { _: Theme? -> if (::mInputView.isInitialized) mInputView.updateTheme()}
    private val clipboardUpdateContentListener = ManagedPreference.OnChangeListener<String> { _, value ->
        if (getInstance().clipboard.clipboardSuggestion.getValue()) {
            if (value.isNotBlank()) {
                val updateTime = getInstance().internal.clipboardUpdateTime.getValue()
                val timeoutSec = getInstance().clipboard.clipboardItemTimeout.getValue()
                val elapsedMs = System.currentTimeMillis() - updateTime
                if (elapsedMs <= (timeoutSec * 1000L)) {
                    if (KeyboardManager.instance.currentContainer is ClipBoardContainer
                        && (KeyboardManager.instance.currentContainer as ClipBoardContainer).getMenuMode() == SkbMenuMode.ClipBoard
                    ) {
                        (KeyboardManager.instance.currentContainer as ClipBoardContainer).showClipBoardView(SkbMenuMode.ClipBoard)
                    } else {
                        if (::mInputView.isInitialized) mInputView.showSymbols(arrayOf(value))
                    }
                }
            }
        }
    }
    private val showVirtualKeyboardOnPhysicalKeyboardListener = ManagedPreference.OnChangeListener<Boolean> { _, value ->
        showVirtualKeyboardOnPhysicalKeyboard = value
        handleHardwareKeyboard()
        updateInputViewShown()
    }
    private val candidatesObserver = Observer<Any?> { _ ->
        if (::mInputView.isInitialized) mInputView.onCandidateChanged()
    }

    override fun onCreate() {
        android.util.Log.w("ImeLifecycle", "ImeService onCreate")
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        addOnChangedListener(onThemeChangeListener)
        getInstance().input.showVirtualKeyboardOnPhysicalKeyboard.registerOnChangeListener(showVirtualKeyboardOnPhysicalKeyboardListener)
        getInstance().internal.clipboardUpdateContent.registerOnChangeListener(clipboardUpdateContentListener)
        handleHardwareKeyboard()
        DictDecoder.candidatesLiveData.observe(this, candidatesObserver)
        InputDispatcher.initImeSchema(getInstance().internal.pinyinModeRime.getValue(), force = true)
    }

    override fun onCreateInputView(): View {
        window?.window?.decorView?.let { decor ->
            decor.setViewTreeLifecycleOwner(this)
            decor.setViewTreeSavedStateRegistryOwner(this)
            decor.setViewTreeViewModelStoreOwner(this)
        }
        if (!::inputView.isInitialized) inputView = InputView(baseContext, this)
        mInputView = inputView
        return inputView
    }

    override fun onCreateCandidatesView(): View {
        if(!::candidateView.isInitialized)candidateView = CandidateView(baseContext, this)
        if(isHardwareKeyboard) mInputView = candidateView
        currentInputConnection?.requestCursorUpdates(InputConnection.CURSOR_UPDATE_MONITOR)
        return candidateView
    }

    override fun onEvaluateInputViewShown(): Boolean {
        super.onEvaluateInputViewShown()
        return showVirtualKeyboardOnPhysicalKeyboard || !isHardwareKeyboard
    }

    override fun onStartInput(editorInfo: EditorInfo?, restarting: Boolean) {
        android.util.Log.w("ImeLifecycle", "onStartInput restarting=$restarting, pkg=${editorInfo?.packageName}")
        val start = SystemClock.elapsedRealtime()
        try {
            super.onStartInput(editorInfo, restarting)
            if (editorInfo != null) {
                YuyanEmojiCompat.setEditorInfo(editorInfo)
                InputModeSwitcher.requestInputWithSkb(editorInfo)
            }
            if (isHardwareKeyboard) updateCandidatesViewShown(true)
        } catch (e: Exception) {
            android.util.Log.e("ImeService", "onStartInput error: ${e.message}")
        }
        android.util.Log.d("PerfProfile", "onStartInput took ${SystemClock.elapsedRealtime() - start}ms")
    }

    override fun onStartInputView(editorInfo: EditorInfo, restarting: Boolean) {
        android.util.Log.w("ImeLifecycle", "onStartInputView restarting=$restarting")
        val start = SystemClock.elapsedRealtime()
        try {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            if (!::mInputView.isInitialized) {
                onCreateInputView()
            }
            currentInputConnection?.finishComposingText()
            InputDispatcher.reset()
            mInputView.onStartInputView(editorInfo, restarting)
            super.onStartInputView(editorInfo, restarting)
        } catch (e: Exception) {
            android.util.Log.e("ImeService", "onStartInputView error: ${e.message}")
        }
        android.util.Log.d("PerfProfile", "onStartInputView took ${SystemClock.elapsedRealtime() - start}ms")
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        android.util.Log.w("ImeLifecycle", "onFinishInputView finishingInput=$finishingInput")
        try {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            if (::mInputView.isInitialized) {
                KeyboardManager.instance.switchKeyboardToMain()
            }
        } catch (_: Exception) {}
        super.onFinishInputView(finishingInput)
    }

    override fun onFinishInput() {
        android.util.Log.w("ImeLifecycle", "onFinishInput")
        YuyanEmojiCompat.clearEditorInfo()
        super.onFinishInput()
    }

    fun deleteSurroundingText(beforeLength: Int = 1, afterLength: Int = 0): Boolean {
        val ic = currentInputConnection ?: return false
        val selectedText = ic.getSelectedText(0)
        if (!selectedText.isNullOrEmpty()) {
            ic.commitText("", 1)
            return true
        }
        val beforeText = ic.getTextBeforeCursor(beforeLength, 0)
        val success = ic.deleteSurroundingText(beforeLength, afterLength)
        if (!success || (beforeLength == 1 && afterLength == 0 && ic.getTextBeforeCursor(1, 0) == beforeText)) {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
        }
        return true
    }

    override fun onDestroy() {
        android.util.Log.w("ImeLifecycle", "ImeService onDestroy")
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        super.onDestroy()
        DictDecoder.candidatesLiveData.removeObserver(candidatesObserver)
        getInstance().input.showVirtualKeyboardOnPhysicalKeyboard.unregisterOnChangeListener(showVirtualKeyboardOnPhysicalKeyboardListener)
        removeOnChangedListener(onThemeChangeListener)
        getInstance().internal.clipboardUpdateContent.unregisterOnChangeListener(clipboardUpdateContentListener)
        KeyboardManager.instance.release()
        try {
            if (spaceVoiceRecognitionHandlerLazy.isInitialized()) {
                spaceVoiceRecognitionHandlerLazy.value.release()
            }
        } catch (_: Exception) {}
    }

    @Volatile
    var isSpaceVoiceActive = false
        private set

    private val spaceVoiceRecognitionHandlerLazy = lazy {
        VoiceRecognitionHandler(
            context = this,
            onStateChanged = { state ->
                if (isSpaceVoiceActive) {
                    val inputViewInstance = mInputView as? InputView
                    when (state.voiceRecognitionState) {
                        com.yuyan.imemodule.speech.RecognitionState.LISTENING -> inputViewInstance?.showComposingTag("正在倾听...")
                        com.yuyan.imemodule.speech.RecognitionState.PROCESSING -> inputViewInstance?.showComposingTag("正在识别...")
                        else -> inputViewInstance?.hideComposingTag()
                    }
                }
            },
            getState = { VoiceState() },
            getInputConnection = { currentInputConnection },
            onVoiceComplete = {
                isSpaceVoiceActive = false
                val inputViewInstance = mInputView as? InputView
                inputViewInstance?.hideComposingTag()
                DictDecoder.reset()
                InputDispatcher.reset()
                inputViewInstance?.resetToIdleState()
                inputViewInstance?.updateCandidateBar()
                (KeyboardManager.instance.currentContainer as? com.yuyan.imemodule.keyboard.container.T9TextContainer)?.updateSymbolListView()
            },
            onAmplitudeChanged = {},
            onSpectrumChanged = {},
        ).apply { initialize() }
    }

    val spaceVoiceRecognitionHandler: VoiceRecognitionHandler get() = spaceVoiceRecognitionHandlerLazy.value

    fun startSpaceVoiceRecognition() {
        if (isSpaceVoiceActive) return
        val permission = com.hjq.permissions.Permission.RECORD_AUDIO
        DictDecoder.reset()
        InputDispatcher.reset()
        (mInputView as? InputView)?.resetToIdleState()
        if (com.hjq.permissions.XXPermissions.isGranted(this, permission)) {
            isSpaceVoiceActive = true
            com.yuyan.imemodule.utils.DevicesUtils.tryVibrate(mInputView as? View, isRelease = false)
            com.yuyan.imemodule.utils.DevicesUtils.tryPlayKeyDown(KeyEvent.KEYCODE_SPACE)
            spaceVoiceRecognitionHandler.startRecognition()
        } else {
            android.widget.Toast.makeText(this@ImeService, "语音输入需要麦克风权限，请允许录音权限", android.widget.Toast.LENGTH_LONG).show()
            com.hjq.permissions.XXPermissions.with(this)
                .permission(permission)
                .request(object : com.hjq.permissions.OnPermissionCallback {
                    override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                        if (allGranted) {
                            isSpaceVoiceActive = true
                            com.yuyan.imemodule.utils.DevicesUtils.tryVibrate(mInputView as? View, isRelease = false)
                            com.yuyan.imemodule.utils.DevicesUtils.tryPlayKeyDown(KeyEvent.KEYCODE_SPACE)
                            spaceVoiceRecognitionHandler.startRecognition()
                        }
                    }

                    override fun onDenied(permissions: MutableList<String>, doNotAskAgain: Boolean) {
                        android.widget.Toast.makeText(this@ImeService, "未获取麦克风权限，请在系统设置中允许录音权限", android.widget.Toast.LENGTH_SHORT).show()
                        com.hjq.permissions.XXPermissions.startPermissionActivity(this@ImeService, permissions)
                    }
                })
        }
    }

    fun stopSpaceVoiceRecognition() {
        if (isSpaceVoiceActive) {
            isSpaceVoiceActive = false
            com.yuyan.imemodule.utils.DevicesUtils.tryVibrate(mInputView as? View, isRelease = true)
            spaceVoiceRecognitionHandler.finishRecognition()
            val inputViewInstance = mInputView as? InputView
            inputViewInstance?.hideComposingTag()
            DictDecoder.reset()
            InputDispatcher.reset()
            inputViewInstance?.resetToIdleState()
            inputViewInstance?.updateCandidateBar()
            (KeyboardManager.instance.currentContainer as? com.yuyan.imemodule.keyboard.container.T9TextContainer)?.updateSymbolListView()
        }
    }

    /**
     * 横竖屏无缝无闪烁原绘切换 (0ms 延迟，原位无缝更新，媲美百度输入法)
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        handleHardwareKeyboard(newConfig)
        EnvironmentSingleton.instance.initData(this, newConfig)
        KeyboardLoaderUtil.instance.clearKeyboardMap()
        KeyboardManager.instance.currentContainer?.updateSkbLayout()
        if (::inputView.isInitialized) {
            inputView.initView(this)
        }
        if (::mInputView.isInitialized && mInputView !== inputView) {
            mInputView.setConfiguration(newConfig)
        }
        onSystemDarkModeChange(newConfig.isDarkMode())
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return if (0 != event.repeatCount || event.isShiftPressed || event.isMetaPressed) super.onKeyDown(keyCode, event)
        else if(event.isCtrlPressed && keyCode != KeyEvent.KEYCODE_SPACE)super.onKeyDown(keyCode, event)
        else if (::mInputView.isInitialized) mInputView.processKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
        else super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return if (0 != event.repeatCount || event.isShiftPressed || event.isMetaPressed) super.onKeyUp(keyCode, event)
        else if(event.isCtrlPressed && keyCode != KeyEvent.KEYCODE_SPACE)super.onKeyUp(keyCode, event)
        else if (::mInputView.isInitialized) mInputView.processKeyUp(event) || super.onKeyUp(keyCode, event)
        else super.onKeyUp(keyCode, event)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }



    override fun onEvaluateFullscreenMode(): Boolean = false //修复横屏之后输入框遮挡问题


    override fun onComputeInsets(outInsets: Insets) {
        val (x, y, width, height) = if (::mInputView.isInitialized) mInputView.getKeyboardRect() else intArrayOf(0, 0, 0,0)
        outInsets.apply {
            if(EnvironmentSingleton.instance.keyboardModeFloat) {
                contentTopInsets = EnvironmentSingleton.instance.mScreenHeight
                visibleTopInsets = EnvironmentSingleton.instance.mScreenHeight
                touchableInsets = Insets.TOUCHABLE_INSETS_REGION
                touchableRegion.set(x, y, x + width, y + height)
            } else {
                contentTopInsets = y
                touchableInsets = Insets.TOUCHABLE_INSETS_CONTENT
                touchableRegion.setEmpty()
                visibleTopInsets = y
            }
        }
    }

    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candidatesStart: Int, candidatesEnd: Int) {
       super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if (::mInputView.isInitialized) mInputView.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesEnd)
    }

    private val cursorAnchorPosition = FloatArray(2)
    override fun onUpdateCursorAnchorInfo(cursorAnchorInfo: CursorAnchorInfo?) {
        super.onUpdateCursorAnchorInfo(cursorAnchorInfo)
        if (!isHardwareKeyboard || cursorAnchorInfo == null) return
        cursorAnchorPosition[0] = cursorAnchorInfo.insertionMarkerHorizontal
        cursorAnchorPosition[1] = cursorAnchorInfo.insertionMarkerBottom
        val matrix = cursorAnchorInfo.matrix
        if (matrix != null) {
            matrix.mapPoints(cursorAnchorPosition)
        }
        if (cursorAnchorPosition[0].isNaN() || cursorAnchorPosition[1].isNaN()) return
        if (::mInputView.isInitialized) mInputView.updatePosition(cursorAnchorPosition)
    }

    override fun onWindowShown() {
        if (::mInputView.isInitialized) mInputView.onWindowShown()
        super.onWindowShown()
    }

    override fun onWindowHidden() {
        if (::mInputView.isInitialized) mInputView.onWindowHidden()
        super.onWindowHidden()
    }

    /**
     * 模拟Enter按键点击
     */
    fun sendEnterKeyEvent() {
        val inputConnection = currentInputConnection
        val info = currentInputEditorInfo ?: YuyanEmojiCompat.mEditorInfo
        if (info == null || inputConnection == null) {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
            return
        }
        info.run {
            if (inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_NULL || imeOptions.hasFlag(EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
                sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
            } else if (!actionLabel.isNullOrEmpty() && actionId != EditorInfo.IME_ACTION_UNSPECIFIED) {
                inputConnection.performEditorAction(actionId)
            } else when (val action = imeOptions and EditorInfo.IME_MASK_ACTION) {
                EditorInfo.IME_ACTION_UNSPECIFIED, EditorInfo.IME_ACTION_NONE -> sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
                else -> inputConnection.performEditorAction(action)
            }
        }
    }

    fun moveCursorSafely(direction: Int) {
        val ic = currentInputConnection ?: return
        if (direction < 0) {
            val textBefore = ic.getTextBeforeCursor(1, 0)
            if (textBefore.isNullOrEmpty()) return
            sendCombinationKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT)
        } else if (direction > 0) {
            val textAfter = ic.getTextAfterCursor(1, 0)
            if (textAfter.isNullOrEmpty()) return
            sendCombinationKeyEvents(KeyEvent.KEYCODE_DPAD_RIGHT)
        }
    }

    fun sendCombinationKeyEvents(keyEventCode: Int, alt: Boolean = false, ctrl: Boolean = false, shift: Boolean = false) {
        if (!alt && !ctrl && !shift && keyEventCode == KeyEvent.KEYCODE_DPAD_UP) {
            val ic = currentInputConnection
            if (ic != null) {
                val textBefore = ic.getTextBeforeCursor(1000, 0)?.toString() ?: ""
                val lastNewLine = textBefore.lastIndexOf('\n')
                if (lastNewLine >= 0) {
                    val currentColumn = textBefore.length - lastNewLine - 1
                    val prevText = textBefore.substring(0, lastNewLine)
                    val prevNewLine = prevText.lastIndexOf('\n')
                    val prevLineStart = if (prevNewLine >= 0) prevNewLine + 1 else 0
                    val targetPos = (prevLineStart + currentColumn).coerceAtMost(lastNewLine)
                    ic.setSelection(targetPos, targetPos)
                } else {
                    ic.setSelection(0, 0)
                }
                return
            }
        }
        if (!alt && !ctrl && !shift && keyEventCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            val ic = currentInputConnection
            if (ic != null) {
                val textAfter = ic.getTextAfterCursor(1000, 0)?.toString() ?: ""
                val nextNewLine = textAfter.indexOf('\n')
                if (nextNewLine >= 0) {
                    val textBefore = ic.getTextBeforeCursor(1000, 0)?.toString() ?: ""
                    val currentColumn = textBefore.length - (textBefore.lastIndexOf('\n') + 1)
                    val line2StartRel = nextNewLine + 1
                    val line2EndRel = textAfter.indexOf('\n', line2StartRel).let { if (it >= 0) it else textAfter.length }
                    val targetRel = (line2StartRel + currentColumn).coerceAtMost(line2EndRel)
                    val newPos = textBefore.length + targetRel
                    ic.setSelection(newPos, newPos)
                } else {
                    val textBefore = ic.getTextBeforeCursor(1000, 0)?.toString() ?: ""
                    val totalLen = textBefore.length + textAfter.length
                    ic.setSelection(totalLen, totalLen)
                }
                return
            }
        }

        var metaState = 0
        if (alt) metaState = KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
        if (ctrl) metaState = metaState or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        if (shift) metaState = metaState or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        val eventTime = SystemClock.uptimeMillis()
        if (alt) sendDownKeyEvent(eventTime, KeyEvent.KEYCODE_ALT_LEFT)
        if (ctrl) sendDownKeyEvent(eventTime, KeyEvent.KEYCODE_CTRL_LEFT)
        if (shift) sendDownKeyEvent(eventTime, KeyEvent.KEYCODE_SHIFT_LEFT)
        sendDownKeyEvent(eventTime, keyEventCode, metaState)
        sendUpKeyEvent(eventTime, keyEventCode, metaState)
        if (shift) sendUpKeyEvent(eventTime, KeyEvent.KEYCODE_SHIFT_LEFT)
        if (ctrl) sendUpKeyEvent(eventTime, KeyEvent.KEYCODE_CTRL_LEFT)
        if (alt) sendUpKeyEvent(eventTime, KeyEvent.KEYCODE_ALT_LEFT)
    }

    fun sendDownKeyEvent(eventTime: Long, keyEventCode: Int, metaState: Int = 0) {
        currentInputConnection?.sendKeyEvent(
            KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyEventCode, 0, metaState,
                KeyCharacterMap.VIRTUAL_KEYBOARD, keyEventCode, KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE)
        )
    }

    fun sendUpKeyEvent(eventTime: Long, keyEventCode: Int, metaState: Int = 0) {
        currentInputConnection?.sendKeyEvent(
            KeyEvent(eventTime, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, keyEventCode, 0, metaState,
                KeyCharacterMap.VIRTUAL_KEYBOARD, keyEventCode, KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE)
        )
    }

    /**
     * 向输入框提交预选词 (设置下划线，防范微信等第三方输入框留存拼音编码)
     */
    fun setComposingText(text: CharSequence) {
        val ic = currentInputConnection ?: return
        if (text.isNotEmpty()) {
            val spannable = android.text.SpannableString(text)
            val flag = android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            spannable.setSpan(android.text.style.UnderlineSpan(), 0, text.length, flag)
            ic.setComposingText(spannable, 1)
        } else {
            ic.finishComposingText()
        }
    }

    /**
     * 结束提交预选词
     */
    fun finishComposingText() {
        currentInputConnection?.finishComposingText()
    }

    /**
     * 发送字符串给编辑框 (安全空判断，防范 Activity/预览模式下白屏闪退)
     */
    fun commitText(text: String) {
        commitText(text, 1)
    }

    /**
     * 发送字符串给编辑框
     */
    fun commitText(text: String, newCursorPosition: Int) {
        val ic = currentInputConnection ?: run {
            SystemClock.sleep(10)
            currentInputConnection
        } ?: return
        ic.commitText(StringUtils.converted2FlowerTypeface(text), newCursorPosition)
    }

    fun getTextBeforeCursor(length:Int) : String {
        return currentInputConnection?.getTextBeforeCursor(length, 0)?.toString() ?: ""
    }

    fun getTextAfterCursor(length: Int, flags: Int = 0) : String {
        return currentInputConnection?.getTextAfterCursor(length, flags)?.toString() ?: ""
    }

    fun commitTextEditMenu(id:Int) {
        currentInputConnection?.performContextMenuAction(id)
    }



    fun performEditorAction(editorAction:Int) {
        currentInputConnection?.performEditorAction(editorAction)
    }

    fun deleteSurroundingText(length: Int) {
        deleteSurroundingText(length, 0)
    }

    @Suppress("unused")
    fun setSelection(start: Int, end: Int) {
        currentInputConnection?.setSelection(start, end)
    }

    fun handleHardwareKeyboard(newConfig: Configuration? = null): Boolean {
        val config = newConfig ?: resources.configuration
        val isHardKb = !showVirtualKeyboardOnPhysicalKeyboard && (config.keyboard != Configuration.KEYBOARD_NOKEYS && config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO)
        isHardwareKeyboard = isHardKb
        if (isHardKb) {
            if (::candidateView.isInitialized) mInputView = candidateView
        } else {
            if (::inputView.isInitialized) mInputView = inputView
        }
        return isHardKb
    }

    fun updateCandidatesViewShown(shown: Boolean) {
        setCandidatesViewShown(shown)
    }

}
