package com.yuyan.imemodule.utils

import android.view.KeyEvent
import com.yuyan.imemodule.application.CustomConstant
import com.yuyan.imemodule.entity.keyboard.SoftKey
import com.yuyan.imemodule.entity.keyboard.SoftKeyToggle
import com.yuyan.imemodule.entity.keyboard.SoftKeyboard
import com.yuyan.imemodule.entity.keyboard.ToggleState
import com.yuyan.imemodule.keyboard.KeyPreset
import com.yuyan.imemodule.keyboard.KeyboardData
import com.yuyan.imemodule.manager.InputModeSwitcher
import com.yuyan.imemodule.keyboard.KeyboardManager
import com.yuyan.imemodule.keyboard.container.InputBaseContainer
import com.yuyan.imemodule.prefs.AppPrefs
import com.yuyan.imemodule.prefs.behavior.DoublePinyinSchemaMode
import com.yuyan.imemodule.prefs.behavior.Lx17PrefixMode
import com.yuyan.imemodule.singleton.EnvironmentSingleton
import com.yuyan.imemodule.keyboard.doubleAbcMnemonicPreset
import com.yuyan.imemodule.keyboard.doubleFlyMnemonicPreset
import com.yuyan.imemodule.keyboard.doubleMSMnemonicPreset
import com.yuyan.imemodule.keyboard.doubleNaturalMnemonicPreset
import com.yuyan.imemodule.keyboard.doubleSogouMnemonicPreset
import com.yuyan.imemodule.keyboard.doubleZiguangMnemonicPreset
import com.yuyan.imemodule.keyboard.lx17MnemonicPreset
import com.yuyan.imemodule.prefs.behavior.SkbStyleMode
import java.util.LinkedList

/**
 * 键盘加载类  包括中文9键  中文26键 英文26键
 */
class KeyboardLoaderUtil private constructor() {
    private var rimeValue: String? = null
    private var mSkbValue: Int = 0
    private var numberLine: Boolean = false
    private var skbStyleMode: SkbStyleMode = SkbStyleMode.Yuyan
    fun clearKeyboardMap() {
        mSoftKeyboardMap.clear()
    }

    private fun loadBaseSkb(skbValue: Int): SoftKeyboard {
        skbStyleMode = SkbStyleMode.Yuyan
        mSkbValue = skbValue
        // shift键状态
        // 直输状态
        val shiftToggleStates = LinkedList<ToggleState>()
        shiftToggleStates.add(ToggleState(0))
        shiftToggleStates.add(ToggleState(1))
        shiftToggleStates.add(ToggleState(2))
        // 拼写模式
        shiftToggleStates.add(ToggleState(3))
        shiftToggleStates.add(ToggleState(4))
        shiftToggleStates.add(ToggleState(5))

        val softKeyboard: SoftKeyboard?
        numberLine = AppPrefs.getInstance().keyboardSetting.abcNumberLine.getValue()
        val rows: MutableList<List<SoftKey>> = LinkedList()
        if (numberLine) {
            val qwertyKeys = createNumberLineKeys(arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0))
            rows.add(qwertyKeys.asList())
        }
        when(skbValue){
            InputModeSwitcher.MASK_SKB_LAYOUT_QWERTY_PINYIN -> {  // 1000  拼音全键
                rimeValue = AppPrefs.getInstance().internal.pinyinModeRime.getValue()
                var keyBeans = mutableListOf<SoftKey>()
                val keys = when (rimeValue) {
                    CustomConstant.SCHEMA_ZH_DOUBLE_FLYPY + DoublePinyinSchemaMode.mspy,
                    CustomConstant.SCHEMA_ZH_DOUBLE_FLYPY + DoublePinyinSchemaMode.sogou,
                    CustomConstant.SCHEMA_ZH_DOUBLE_FLYPY + DoublePinyinSchemaMode.ziguang -> arrayListOf(
                        arrayOf(45, 51, 33, 46, 48, 53, 49, 37, 43, 44),
                        arrayOf(29, 47, 32, 34, 35, 36, 38, 39, 40),
                        arrayOf(74, 54, 52, 31, 50, 30, 42, 41, KeyEvent.KEYCODE_DEL),)
                    else -> {
                        KeyboardData.layoutQwertyCn[skbStyleMode]!!
                    }
                }
                var qwertyKeys = createQwertyPYKeys(keys[0])
                keyBeans.addAll(qwertyKeys)
                rows.add(keyBeans)
                keyBeans = LinkedList()
                qwertyKeys = createQwertyPYKeys(keys[1])
                qwertyKeys.first().apply {
                    mLeftF = 0.06f
                }
                keyBeans.addAll(qwertyKeys)
                rows.add(keyBeans)
                keyBeans = LinkedList()
                qwertyKeys = createQwertyPYKeys(keys[2])
                qwertyKeys.first().apply {
                    widthF = 0.147f
                }
                qwertyKeys.last().apply {
                    widthF = 0.147f
                }
                keyBeans.addAll(qwertyKeys)
                rows.add(keyBeans)
                keyBeans = lastRows(skbValue)
                rows.add(keyBeans)
            }
            InputModeSwitcher.MASK_SKB_LAYOUT_T9_PINYIN -> {  // 2000  T9键键
                var keyBeans: MutableList<SoftKey> = LinkedList()
                val keys = KeyboardData.layoutT9Cn[skbStyleMode]!!
                var t9Key = createT9Keys(keys[0])
                t9Key.first().apply {
                    widthF = 0.18f
                    heightF = 0.75f
                }
                t9Key.last().widthF = 0.18f
                keyBeans.addAll(t9Key)
                rows.add(keyBeans)
                keyBeans = LinkedList()
                t9Key = createT9Keys(keys[1])
                t9Key.first().mLeftF = 0.185f
                t9Key.last().widthF = 0.18f
                keyBeans.addAll(t9Key)
                rows.add(keyBeans)
                keyBeans = LinkedList()
                t9Key = createT9Keys(keys[2])
                t9Key.first().mLeftF = 0.185f
                keyBeans.addAll(t9Key)
                rows.add(keyBeans)
                keyBeans = lastRows(skbValue)
                // 中文9键：让回车发送按键向上跨越融合第 3 行最右侧 (原来的 @ 键位置) 与第 4 行最右侧！
                val enterKey = keyBeans.lastOrNull()
                if (enterKey != null) {
                    enterKey.heightF = 0.50f
                    enterKey.mTopF = 0.50f
                }
                rows.add(keyBeans)

            }

            InputModeSwitcher.MASK_SKB_LAYOUT_QWERTY_ABC -> {// 4000 英文全键
                var keyBeans: MutableList<SoftKey> = LinkedList()
                val keys = KeyboardData.layoutQwertyEn[skbStyleMode]!!
                var qwertyKeys = createQwertyKeys(keys[0])
                keyBeans.addAll(qwertyKeys)
                rows.add(keyBeans)
                keyBeans = LinkedList()
                qwertyKeys = createQwertyKeys(keys[1])
                qwertyKeys.first().apply {
                    mLeftF = 0.06f
                }
                keyBeans.addAll(qwertyKeys)
                rows.add(keyBeans)
                keyBeans = LinkedList()
                val softKeyToggle = createKeyToggle(KeyEvent.KEYCODE_SHIFT_LEFT)
                softKeyToggle.widthF = 0.147f
                softKeyToggle.setToggleStates(shiftToggleStates)
                keyBeans.add(softKeyToggle)
                keyBeans.addAll(createQwertyKeys(keys[2]))
                keyBeans.last().apply {
                    widthF = 0.147f
                }
                rows.add(keyBeans)
                keyBeans = lastRows(skbValue)
                keyBeans[keyBeans.size -2].stateId = 1
                rows.add(keyBeans)
            }
            InputModeSwitcher.MASK_SKB_LAYOUT_NUMBER -> {  // 5000 数字键盘
                var keyBeans: MutableList<SoftKey> = LinkedList()
                val keys =  KeyboardData.layoutT9Number[skbStyleMode]!!
                var t9Keys = createT9NumberKeys(keys[0])
                t9Keys.first().apply {
                    widthF = 0.18f
                    heightF = 0.75f
                }
                t9Keys.last().widthF = 0.18f
                keyBeans.addAll(t9Keys)
                rows.add(keyBeans)

                keyBeans = LinkedList()
                t9Keys = createT9NumberKeys(keys[1])
                t9Keys.first().mLeftF = 0.185f
                t9Keys.last().widthF = 0.18f
                keyBeans.addAll(t9Keys)
                rows.add(keyBeans)

                keyBeans = LinkedList()
                t9Keys = createT9NumberKeys(keys[2])
                t9Keys.first().mLeftF = 0.185f
                t9Keys.last().widthF = 0.18f
                keyBeans.addAll(t9Keys)
                rows.add(keyBeans)
                keyBeans = lastRows(skbValue)
                rows.add(keyBeans)
            }
            InputModeSwitcher.MASK_SKB_LAYOUT_LX17 -> {     // 6000 乱序17键盘
                val leftDelete = AppPrefs.getInstance().keyboardSetting.lx17LeftDeleteEnable.getValue()
                val prefixMode = AppPrefs.getInstance().keyboardSetting.lx17PrefixMode.getValue()
                val currentContainer = KeyboardManager.instance.currentContainer
                val isExpanded = (currentContainer as? InputBaseContainer)?.isCandidatesExpanded() == true
                val centerSymbol = (prefixMode == Lx17PrefixMode.Center) && !isExpanded
                val withLeftPrefix = (prefixMode == Lx17PrefixMode.Left) || (prefixMode == Lx17PrefixMode.Center && isExpanded)

                if (!leftDelete && !centerSymbol && !withLeftPrefix) {
                    val keys = KeyboardData.layoutLX17Cn[skbStyleMode]!!
                    var keyBeans: MutableList<SoftKey> = LinkedList()
                    var lX17Keys = createLX17Keys(keys[0], 0.165f)
                    keyBeans.addAll(lX17Keys)
                    rows.add(keyBeans)
                    keyBeans = LinkedList()
                    lX17Keys = createLX17Keys(keys[1], 0.165f)
                    keyBeans.addAll(lX17Keys)
                    rows.add(keyBeans)
                    keyBeans = LinkedList()
                    lX17Keys = createLX17Keys(keys[2], 0.165f)
                    keyBeans.addAll(lX17Keys)
                    rows.add(keyBeans)
                    val lastRow = lastRows(skbValue)
                    rows.add(lastRow)
                } else if (centerSymbol) {
                    val rawKeys = KeyboardData.layoutLX17Cn[skbStyleMode]!!
                    val width = 1.0f / 7.0f
                    for (rowIndex in 0..2) {
                        val rowCodes = rawKeys[rowIndex].clone()
                        if (leftDelete && rowIndex == 2) {
                            val temp = rowCodes[0]
                            rowCodes[0] = rowCodes[rowCodes.size - 1]
                            rowCodes[rowCodes.size - 1] = temp
                        }

                        val keyBeans = LinkedList<SoftKey>()
                        if (rowIndex == 0) {
                            val list = rowCodes.toMutableList()
                            list.add(3, InputModeSwitcher.USER_KEYCODE_LEFT_SYMBOL)
                            val keys = createLX17Keys(list.toTypedArray(), width)
                            for (i in keys.indices) {
                                keys[i].mLeftF = i * width
                                if (i == 3) {
                                    keys[i].heightF = 0.75f
                                }
                            }
                            keyBeans.addAll(keys)
                        } else {
                            val keys = createLX17Keys(rowCodes, width)
                            for (i in 0..5) {
                                val targetCol = if (i >= 3) i + 1 else i
                                keys[i].mLeftF = targetCol * width
                                keyBeans.add(keys[i])
                            }
                        }
                        rows.add(keyBeans)
                    }
                    val lastRow = lastRows(skbValue)
                    rows.add(lastRow)
                } else {
                    val keys = if (withLeftPrefix) KeyboardData.layoutLX17CnWithLeftPrefix[skbStyleMode]!! else KeyboardData.layoutLX17Cn[skbStyleMode]!!
                    for (rowIndex in 0..2) {
                        val rowCodes = keys[rowIndex].clone()
                        if (leftDelete && rowIndex == 2) {
                            val temp = rowCodes[0]
                            rowCodes[0] = rowCodes[rowCodes.size - 1]
                            rowCodes[rowCodes.size - 1] = temp
                        }
                        val width = if (withLeftPrefix) 0.1416667f else if (rowCodes.size > 6) 0.125f else 0.165f
                        val lX17Keys = createLX17Keys(rowCodes, width)
                        if (rowIndex == 0 && withLeftPrefix && lX17Keys.isNotEmpty()) {
                            lX17Keys.first().apply {
                                widthF = 0.140f
                                heightF = 0.75f
                            }
                            if (lX17Keys.size > 1) lX17Keys[1].mLeftF = 0.145f
                        } else if (rowIndex > 0 && withLeftPrefix && lX17Keys.isNotEmpty()) {
                            lX17Keys.first().mLeftF = 0.145f
                        }
                        val keyBeans = LinkedList<SoftKey>().apply { addAll(lX17Keys) }
                        rows.add(keyBeans)
                    }
                    val lastRow = lastRows(skbValue)
                    rows.add(lastRow)
                }
            }
            InputModeSwitcher.MASK_SKB_LAYOUT_STROKE -> {  // 7000  笔画键盘
                var keyBeans: MutableList<SoftKey> = LinkedList()
                val keys =  KeyboardData.layoutStrokeCn[skbStyleMode]!!
                var t9Key = createT9Keys(keys[0])
                t9Key.first().apply {
                    widthF = 0.18f
                    heightF = 0.75f
                }
                t9Key.last().widthF = 0.18f
                keyBeans.addAll(t9Key)
                rows.add(keyBeans)
                keyBeans = LinkedList()
                t9Key = createT9Keys(keys[1])
                t9Key.first().mLeftF = 0.185f
                t9Key.last().widthF = 0.18f
                keyBeans.addAll(t9Key)
                rows.add(keyBeans)
                keyBeans = LinkedList()
                t9Key = createT9Keys(keys[2])
                t9Key.first().mLeftF = 0.185f
                keyBeans.addAll(t9Key)
                rows.add(keyBeans)
                keyBeans = lastRows(skbValue)
                // 笔画键盘：删掉 @ 键，让回车发送键向上跨越占据原来的 @ 键 + 发送键 2 行空间！
                val enterKey = keyBeans.lastOrNull()
                if (enterKey != null) {
                    enterKey.heightF = 0.50f
                    enterKey.mTopF = 0.50f
                }
                rows.add(keyBeans)
            }
            InputModeSwitcher.MASK_SKB_LAYOUT_TEXTEDIT -> {     // 8000 文本编辑键盘 (左侧大圆盘与内部按钮整体向上微微移动)
                var keyBeans: MutableList<SoftKey> = LinkedList()
                val keys =  KeyboardData.layoutTextEdit[skbStyleMode]!!

                // Row 0: ∧ [16%, 0.02f, 18%, 20%] | 全选 [54%, 0.03f, 20%, 28%] | 删除 [77%, 0.03f, 20%, 28%]
                var editKeys = createTextEditKeys(keys[0])
                editKeys[0].mLeftF = 0.16f
                editKeys[0].mTopF = 0.02f
                editKeys[0].widthF = 0.18f
                editKeys[0].heightF = 0.20f

                editKeys[1].mLeftF = 0.54f
                editKeys[1].mTopF = 0.03f
                editKeys[1].widthF = 0.20f
                editKeys[1].heightF = 0.28f

                editKeys[2].mLeftF = 0.77f
                editKeys[2].mTopF = 0.03f
                editKeys[2].widthF = 0.20f
                editKeys[2].heightF = 0.28f
                keyBeans.addAll(editKeys)
                rows.add(keyBeans)

                // Row 1: ＜ [2%, 0.22f, 15%, 26%] | 选择 [17%, 0.22f, 18%, 26%] | ＞ [35%, 0.22f, 15%, 26%] | 复制 [54%, 0.34f, 20%, 28%] | 回车 [77%, 0.34f, 20%, 28%]
                keyBeans = LinkedList()
                editKeys = createTextEditKeys(keys[1])
                editKeys[0].mLeftF = 0.02f
                editKeys[0].mTopF = 0.22f
                editKeys[0].widthF = 0.15f
                editKeys[0].heightF = 0.26f

                editKeys[1].mLeftF = 0.17f
                editKeys[1].mTopF = 0.22f
                editKeys[1].widthF = 0.18f
                editKeys[1].heightF = 0.26f

                editKeys[2].mLeftF = 0.35f
                editKeys[2].mTopF = 0.22f
                editKeys[2].widthF = 0.15f
                editKeys[2].heightF = 0.26f

                editKeys[3].mLeftF = 0.54f
                editKeys[3].mTopF = 0.34f
                editKeys[3].widthF = 0.20f
                editKeys[3].heightF = 0.28f

                editKeys[4].mLeftF = 0.77f
                editKeys[4].mTopF = 0.34f
                editKeys[4].widthF = 0.20f
                editKeys[4].heightF = 0.28f
                keyBeans.addAll(editKeys)
                rows.add(keyBeans)

                // Row 2: ∨ [18%, 0.42f, 16%, 0.22f] | |＜ [2%, 0.72f, 16%, 0.22f] | ＞| [34%, 0.72f, 16%, 0.22f] | 粘贴 [54%, 0.65f, 20%, 28%] | 剪贴板 [77%, 0.65f, 20%, 28%]
                keyBeans = LinkedList()
                editKeys = createTextEditKeys(keys[2])
                editKeys[0].mLeftF = 0.18f
                editKeys[0].mTopF = 0.42f
                editKeys[0].widthF = 0.16f
                editKeys[0].heightF = 0.22f

                editKeys[1].mLeftF = 0.02f
                editKeys[1].mTopF = 0.72f
                editKeys[1].widthF = 0.16f
                editKeys[1].heightF = 0.22f

                editKeys[2].mLeftF = 0.34f
                editKeys[2].mTopF = 0.72f
                editKeys[2].widthF = 0.16f
                editKeys[2].heightF = 0.22f

                editKeys[3].mLeftF = 0.54f
                editKeys[3].mTopF = 0.65f
                editKeys[3].widthF = 0.20f
                editKeys[3].heightF = 0.28f

                editKeys[4].mLeftF = 0.77f
                editKeys[4].mTopF = 0.65f
                editKeys[4].widthF = 0.20f
                editKeys[4].heightF = 0.28f
                keyBeans.addAll(editKeys)
                rows.add(keyBeans)
            }
        }
        val numberLineSkb = numberLine
        softKeyboard = getSoftKeyboard(rows, numberLineSkb)
        mSoftKeyboardMap[skbValue] = softKeyboard
        return softKeyboard
    }

    // 键盘最后一行（各键盘统一，数字键盘稍微不同）
    private fun lastRows(skbValue: Int): MutableList<SoftKey> {
        val enterToggleStates = listOf(ToggleState("去往", 2), ToggleState("搜索", 3), ToggleState("发送", 4),
            ToggleState("下一个", 5), ToggleState("完成", 6), ToggleState("上一个", 7))
        val softKeyToggle = createKeyToggle(KeyEvent.KEYCODE_ENTER)
        softKeyToggle.widthF = 0.18f
        softKeyToggle.stateId = 0
        softKeyToggle.setToggleStates(enterToggleStates)
        val keyBeans = mutableListOf<SoftKey>()
        val t9Keys = when(skbValue){
            InputModeSwitcher.MASK_SKB_LAYOUT_HANDWRITING -> {
                createT9Keys(arrayOf(InputModeSwitcher.USER_KEYCODE_SYMBOL, InputModeSwitcher.USER_KEYCODE_NUMBER, KeyEvent.KEYCODE_SPACE))
            }
            InputModeSwitcher.MASK_SKB_LAYOUT_T9_PINYIN, InputModeSwitcher.MASK_SKB_LAYOUT_STROKE ->{
                createT9Keys(arrayOf(InputModeSwitcher.USER_KEYCODE_SYMBOL, InputModeSwitcher.USER_KEYCODE_NUMBER,
                        KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_AT))
            }
            InputModeSwitcher.MASK_SKB_LAYOUT_QWERTY_ABC -> {
                createQwertyKeys(arrayOf(InputModeSwitcher.USER_KEYCODE_SYMBOL, InputModeSwitcher.USER_KEYCODE_NUMBER, KeyEvent.KEYCODE_SPACE))
            }
            InputModeSwitcher.MASK_SKB_LAYOUT_NUMBER -> {
                createT9NumberKeys(arrayOf(InputModeSwitcher.USER_KEYCODE_RETURN, InputModeSwitcher.USER_KEYCODE_SYMBOL, 7, KeyEvent.KEYCODE_SPACE))
            }
            InputModeSwitcher.MASK_SKB_LAYOUT_LX17 -> {
                createLX17Keys(arrayOf(InputModeSwitcher.USER_KEYCODE_SYMBOL, InputModeSwitcher.USER_KEYCODE_NUMBER, KeyEvent.KEYCODE_SPACE))
            }
            else -> { //0x1000 InputModeSwitcherManager.MASK_SKB_LAYOUT_QWERTY_PINYIN
                createQwertyPYKeys(arrayOf(InputModeSwitcher.USER_KEYCODE_SYMBOL, InputModeSwitcher.USER_KEYCODE_NUMBER,
                        InputModeSwitcher.USER_KEYCODE_LEFT_COMMA, KeyEvent.KEYCODE_SPACE))
            }
        }
        if (t9Keys.size == 3) {
            t9Keys[0].apply { mLeftF = 0.005f; widthF = 0.140f }
            t9Keys[1].apply { mLeftF = 0.145f; widthF = 0.1416667f }
            t9Keys[2].apply { mLeftF = 0.2866667f; widthF = 0.425f }
            softKeyToggle.apply { mLeftF = 0.7116667f; widthF = 0.2833333f }
        } else if (skbValue == InputModeSwitcher.MASK_SKB_LAYOUT_QWERTY_PINYIN) {
            // 小鹤双拼/拼音全键：横向做大发送键，合并占据 0.246f 横向宽度！
            softKeyToggle.widthF = 0.246f
            t9Keys[0].widthF = 0.147f; t9Keys[1].widthF = 0.099f
            t9Keys[2].widthF = 0.099f; t9Keys[3].widthF = 0.396f
        } else if (t9Keys.size == 4) {
            // 中文 9 键 / 笔画键盘：底栏为 [符号], [123], [空格/En], [@/0]，最右侧为纵向两行高大发送键！
            softKeyToggle.widthF = 0.18f
            t9Keys[0].widthF = 0.18f; t9Keys[1].widthF = 0.147f
            t9Keys[2].widthF = 0.336f; t9Keys[3].widthF = 0.147f
        } else {
            t9Keys[0].widthF = 0.18f;t9Keys[1].widthF = 0.147f
            t9Keys[2].widthF = 0.336f;t9Keys[3].widthF = 0.147f
        }
        keyBeans.addAll(t9Keys)
        keyBeans.add(softKeyToggle)
        return keyBeans
    }

    fun changeSKBNumberRow() {
        for (skbValue in mSoftKeyboardMap.keys) {
            loadBaseSkb(skbValue)
        }
    }

    fun getSoftKeyboard(skbValue: Int): SoftKeyboard {
        var softKeyboard = mSoftKeyboardMap[skbValue]
        if (softKeyboard == null) {
            softKeyboard = loadBaseSkb(skbValue)
        }
        return softKeyboard
    }

    /** 生成键盘布局，主要用于计算键盘边界 */
    private fun getSoftKeyboard(rows: List<List<SoftKey>>, isNumberRow: Boolean): SoftKeyboard {
        var lastKeyBottom = 0f
        var lastKeyRight: Float
        var lastKeyTop: Float
        for (rowBean in rows) {
            lastKeyTop = lastKeyBottom  // 新行top为上一行bottom
            lastKeyRight = 0.005f // 新行x从0.005开始
            for (keyBean in rowBean) {
                var keyXPos = keyBean.mLeftF
                var keyYPos = keyBean.mTopF
                val keyWidth = keyBean.widthF
                val keyHeight = keyBean.heightF
                if (keyXPos == -1f) keyXPos = lastKeyRight
                if (keyYPos == -1f) keyYPos = lastKeyTop
                if (isNumberRow) {
                    keyBean.setKeyDimensions(keyXPos, keyYPos/1.2f, keyHeight/1.2f)
                } else {
                    keyBean.setKeyDimensions(keyXPos, keyYPos)
                }
                keyBean.setSkbCoreSize(EnvironmentSingleton.instance.skbWidth, EnvironmentSingleton.instance.skbHeight)
                lastKeyRight = keyXPos + keyWidth
                lastKeyTop = keyYPos
                lastKeyBottom = keyYPos + keyHeight
            }
        }
        return SoftKeyboard(rows)
    }

    private fun createT9Keys(codes: Array<Int>): Array<SoftKey> {
        val softKeys = mutableListOf<SoftKey>()
        val keyPreset =  if(mSkbValue == 0x7000) KeyPreset.strokeKeyPreset else KeyPreset.t9PYKeyPreset
        for(code in codes){
            val labels = keyPreset[code]
            softKeys.add(SoftKey(code = code, label = labels?.getOrNull(0) ?: "", labelSmall = labels?.getOrNull(1)?: "").apply {
                widthF = 0.21f
            })
        }
        return softKeys.toTypedArray()
    }

    private fun createT9NumberKeys(codes: Array<Int>): Array<SoftKey> {
        val softKeys = mutableListOf<SoftKey>()
        val keyPreset = KeyPreset.t9NumberKeyPreset
        for(code in codes){
            val labels = keyPreset[code]
            softKeys.add(SoftKey(code = code, label = labels?.getOrNull(0) ?: "", labelSmall = labels?.getOrNull(1) ?: "").apply {
                widthF = 0.21f
            })
        }
        return softKeys.toTypedArray()
    }

    private fun createQwertyPYKeys(codes: Array<Int>): Array<SoftKey> {
        val keyMnemonicPreset = when (rimeValue) {
            CustomConstant.SCHEMA_ZH_DOUBLE_FLYPY + DoublePinyinSchemaMode.flypy -> doubleFlyMnemonicPreset
            CustomConstant.SCHEMA_ZH_DOUBLE_FLYPY + DoublePinyinSchemaMode.abc -> doubleAbcMnemonicPreset
            CustomConstant.SCHEMA_ZH_DOUBLE_FLYPY + DoublePinyinSchemaMode.mspy -> doubleMSMnemonicPreset
            CustomConstant.SCHEMA_ZH_DOUBLE_FLYPY + DoublePinyinSchemaMode.natural -> doubleNaturalMnemonicPreset
            CustomConstant.SCHEMA_ZH_DOUBLE_FLYPY + DoublePinyinSchemaMode.sogou -> doubleSogouMnemonicPreset
            CustomConstant.SCHEMA_ZH_DOUBLE_FLYPY + DoublePinyinSchemaMode.ziguang -> doubleZiguangMnemonicPreset
            else -> emptyMap()
        }
        val isLowercase = AppPrefs.getInstance().input.qwertyPinyinLowercase.getValue()
        val softKeys = mutableListOf<SoftKey>()
        val keyPreset = if(numberLine)KeyPreset.qwertyPYKeyPreset else KeyPreset.qwertyPYKeyNumberPreset
        for(code in codes){
            val labels = keyPreset[code]
            var mainLabel = labels?.getOrNull(0) ?: ""
            if (isLowercase) {
                if (code == KeyEvent.KEYCODE_V) {
                    mainLabel = "ü"
                } else if (mainLabel.length == 1 && mainLabel[0] in 'A'..'Z') {
                    mainLabel = mainLabel.lowercase()
                }
            }
            softKeys.add(SoftKey(code = code, label = mainLabel, labelSmall = labels?.getOrNull(1) ?: "", keyMnemonic = keyMnemonicPreset[code] ?: "").apply {
                widthF = 0.099f
            })
        }
        return softKeys.toTypedArray()
    }

    private fun createQwertyKeys(codes: Array<Int>): Array<SoftKey> {
        val softKeys = mutableListOf<SoftKey>()
        val keyPreset = if(numberLine)KeyPreset.qwertyKeyPreset else KeyPreset.qwertyKeyNumberPreset
        for(code in codes){
            val labels = keyPreset[code]
            softKeys.add(SoftKey(code = code, label = labels?.getOrNull(0) ?: "", labelSmall = labels?.getOrNull(1) ?: "", keyMnemonic = labels?.getOrNull(2) ?: "").apply {
                widthF = 0.099f
            })
        }
        return softKeys.toTypedArray()
    }



    private fun createNumberLineKeys(codes: Array<Int>): Array<SoftKey> {
        val softKeys = mutableListOf<SoftKey>()
        for(code in codes) {
            val softKey = SoftKey(label = code.toString()).apply {
                widthF = 0.099f
                heightF = 0.2f
            }
            softKeys.add(softKey)
        }
        return softKeys.toTypedArray()
    }

    private fun createLX17Keys(codes: Array<Int>, width: Float = 0.142f): Array<SoftKey> {
        val softKeys = mutableListOf<SoftKey>()
        val keyPreset = if(numberLine)KeyPreset.lx17PYKeyPreset else KeyPreset.lx17PYKeyNumberPreset
        for(code in codes){
            val labels = keyPreset[code]
            softKeys.add(SoftKey(code = code, label = labels?.getOrNull(0) ?: "", labelSmall = labels?.getOrNull(1) ?: "", keyMnemonic= lx17MnemonicPreset[code] ?: "").apply {
                widthF = width
            })
        }
        return softKeys.toTypedArray()
    }

    private fun createTextEditKeys(codes: Array<Int>): Array<SoftKey> {
        val softKeys = mutableListOf<SoftKey>()
        val keyPreset = KeyPreset.textEditKeyPreset
        for(code in codes){
            val labels = keyPreset[code]
            softKeys.add(SoftKey(code = code, label = labels?.getOrNull(0) ?: "").apply {
                widthF = 0.2475f
            })
        }
        return softKeys.toTypedArray()
    }

    private fun createKeyToggle(code: Int): SoftKeyToggle {
        return SoftKeyToggle(code)
    }

    companion object {
        private var mInstance: KeyboardLoaderUtil? = null
        private val mSoftKeyboardMap = HashMap<Int, SoftKeyboard?>() //缓存所有可用键盘
        @JvmStatic
        val instance: KeyboardLoaderUtil
            get() {
                if (null == mInstance) mInstance = KeyboardLoaderUtil()
                return mInstance!!
            }
    }
}