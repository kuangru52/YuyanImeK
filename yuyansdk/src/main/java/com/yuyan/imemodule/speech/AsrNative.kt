package com.yuyan.imemodule.speech

object AsrNative {
    init {
        try {
            System.loadLibrary("onnx_jni")
        } catch (e: Throwable) {
            android.util.Log.e("AsrNative", "Failed to load libonnx_jni.so", e)
        }
    }

    @JvmStatic
    external fun nativeCreate(encoder: String, decoder: String, joiner: String, tokens: String): Long

    @JvmStatic
    external fun nativeReset(handle: Long)

    @JvmStatic
    external fun nativeAcceptPcm(handle: Long, pcm: ByteArray)

    @JvmStatic
    external fun nativeGetPartial(handle: Long): String

    @JvmStatic
    external fun nativeFinalize(handle: Long): String

    @JvmStatic
    external fun nativeRelease(handle: Long)
}
