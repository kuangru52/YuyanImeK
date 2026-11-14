package com.yuyan.imemodule.speech

import android.content.Context

object AsrBackendFactory {
    fun create(context: Context): AsrBackend? = AsrSupport.create(context)

    fun getLocalName(context: Context): String? = AsrSupport.getLocalName(context)

    fun warmup(context: Context) = AsrSupport.warmup(context)

    fun releaseModel() = AsrSupport.releaseModel()
}
