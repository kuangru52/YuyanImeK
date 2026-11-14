package com.yuyan.imemodule.prefs

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.view.HapticFeedbackConstants
import android.view.View
import com.yuyan.imemodule.application.Launcher
import com.yuyan.imemodule.utils.audioManager
import com.yuyan.imemodule.utils.vibrator

object InputFeedbacks {

    private val soundOnKeyPress by AppPrefs.getInstance().internal.soundOnKeyPress
    private val vibrationAmplitude by AppPrefs.getInstance().internal.vibrationAmplitude
    private val soundTimeRangeEnable by AppPrefs.getInstance().internal.soundTimeRangeEnable
    private val soundStartTime by AppPrefs.getInstance().internal.soundStartTime
    private val soundEndTime by AppPrefs.getInstance().internal.soundEndTime

    private fun isCurrentTimeInSoundRange(): Boolean {
        if (!soundTimeRangeEnable) return true
        return try {
            val calendar = java.util.Calendar.getInstance()
            val currentMinutes = calendar[java.util.Calendar.HOUR_OF_DAY] * 60 + calendar[java.util.Calendar.MINUTE]

            val startParts = soundStartTime.split(":")
            val startMinutes = startParts[0].toInt() * 60 + startParts[1].toInt()

            val endParts = soundEndTime.split(":")
            val endMinutes = endParts[0].toInt() * 60 + endParts[1].toInt()

            if (startMinutes <= endMinutes) {
                currentMinutes in startMinutes..endMinutes
            } else {
                currentMinutes >= startMinutes || currentMinutes <= endMinutes
            }
        } catch (_: Exception) {
            true
        }
    }

    private val soundEffectStandardPref = AppPrefs.getInstance().internal.soundEffectStandard
    private val soundEffectSpaceBarPref = AppPrefs.getInstance().internal.soundEffectSpaceBar
    private val soundEffectDeletePref = AppPrefs.getInstance().internal.soundEffectDelete
    private val soundEffectReturnPref = AppPrefs.getInstance().internal.soundEffectReturn

    private val vibrator = Launcher.instance.context.vibrator
    private val hasAmplitudeControl = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator.hasAmplitudeControl()
    private val audioManager = Launcher.instance.context.audioManager

    private val soundPool: SoundPool by lazy {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(audioAttributes)
            .build()
    }

    private val soundMap = mutableMapOf<String, Int>()

    init {
        // 监控并广播偏好项变更，当用户切换音效方案时自动预加载音效资源
        soundEffectStandardPref.registerOnChangeListener { _, _ -> preloadCurrentSounds() }
        soundEffectSpaceBarPref.registerOnChangeListener { _, _ -> preloadCurrentSounds() }
        soundEffectDeletePref.registerOnChangeListener { _, _ -> preloadCurrentSounds() }
        soundEffectReturnPref.registerOnChangeListener { _, _ -> preloadCurrentSounds() }
    }

    private fun loadSoundFromAssets(fileName: String): Int {
        return soundMap.getOrPut(fileName) {
            try {
                Launcher.instance.context.assets.openFd("sound/$fileName").use { afd ->
                    soundPool.load(afd, 1)
                }
            } catch (e: Exception) {
                0
            }
        }
    }

    /**
     * 预加载当前选中的音频文件，解决 SoundPool 首次异步加载导致的首击无声音延迟问题
     */
    fun preloadCurrentSounds() {
        val choices = listOf(
            soundEffectStandardPref.getValue(),
            soundEffectSpaceBarPref.getValue(),
            soundEffectDeletePref.getValue(),
            soundEffectReturnPref.getValue()
        )
        for (choice in choices) {
            val customFile = mapChoiceToFileName(choice)
            if (customFile != null) {
                loadSoundFromAssets(customFile)
            }
        }
    }

    private fun mapChoiceToFileName(choice: String): String? {
        return when (choice) {
            "微信点击", "微信默认点击" -> "wx_click.wav"
            "微信空格", "微信空格音效" -> "wx_click.wav"
            "微信删除", "微信删除音效" -> "wx_delete.wav"
            "微信回车", "微信回车/动作音效" -> "wx_action.wav"

            "百度红轴" -> "bd_axis_red.ogg"
            "百度青轴" -> "bd_axis_green.ogg"
            "百度茶轴" -> "bd_axis_tea.ogg"
            "百度机械" -> "bd_axis_mechanical.ogg"
            "百度黑莓" -> "bd_device_blackberry.ogg"
            "百度轻敲" -> "bd_device_tap.ogg"
            "百度可爱" -> "bd_style_cute.ogg"
            "百度卡通" -> "bd_style_cartoon.ogg"
            "百度快板" -> "bd_style_allegro.ogg"
            "百度经典" -> "bd_classic_click.ogg"
            "百度动作" -> "bd_classic_func.ogg"
            else -> null
        }
    }

    fun hapticFeedback(view: View, isRelease: Boolean = false) {
        val duration = when (vibrationAmplitude) {
            0 -> {
                @Suppress("DEPRECATION")
                val flags = HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING or HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                val hfc = if (isRelease && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    HapticFeedbackConstants.KEYBOARD_RELEASE
                } else if (isRelease) {
                    HapticFeedbackConstants.KEYBOARD_TAP
                } else {
                    HapticFeedbackConstants.LONG_PRESS
                }
                view.performHapticFeedback(hfc, flags)
                0L
            }
            1 -> 0L
            2 -> if (isRelease) 8L else 18L
            3 -> if (isRelease) 18L else 35L
            else -> if (isRelease) 30L else 55L
        }
        if (duration != 0L) {
            if (hasAmplitudeControl) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, if (isRelease) 100 else -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        }
    }

    enum class SoundEffect {
        Standard, SpaceBar, Delete, Return
    }

    fun soundEffect(effect: SoundEffect) {
        if (!isCurrentTimeInSoundRange()) return

        val baseVolume = when {
            soundOnKeyPress < 5 -> -1f
            soundOnKeyPress < 10 -> return
            else -> (soundOnKeyPress - 10) / 30f
        }

        val keyPressVolPercentage = AppPrefs.getInstance().internal.keyPressVolume.getValue().coerceIn(1, 100)
        val volMultiplier = keyPressVolPercentage / 100.0f

        val playVol = (if (baseVolume < 0f) 0.8f else baseVolume) * volMultiplier

        val choice = when (effect) {
            SoundEffect.Standard -> soundEffectStandardPref.getValue()
            SoundEffect.SpaceBar -> soundEffectSpaceBarPref.getValue()
            SoundEffect.Delete -> soundEffectDeletePref.getValue()
            SoundEffect.Return -> soundEffectReturnPref.getValue()
        }

        val customFile = mapChoiceToFileName(choice)

        if (customFile != null) {
            val soundId = loadSoundFromAssets(customFile)
            if (soundId != 0) {
                soundPool.play(soundId, playVol, playVol, 1, 0, 1.0f)
                return
            }
        }

        val fx = when (effect) {
            SoundEffect.Standard -> AudioManager.FX_KEYPRESS_STANDARD
            SoundEffect.SpaceBar -> AudioManager.FX_KEYPRESS_SPACEBAR
            SoundEffect.Delete -> AudioManager.FX_KEYPRESS_DELETE
            SoundEffect.Return -> AudioManager.FX_KEYPRESS_RETURN
        }
        audioManager.playSoundEffect(fx, playVol)
    }

}
