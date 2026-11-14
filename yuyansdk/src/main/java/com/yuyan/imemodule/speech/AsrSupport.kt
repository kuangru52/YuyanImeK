package com.yuyan.imemodule.speech

import android.content.Context

internal object AsrSupport {

    @Volatile
    private var warmBackend: OfflineAsrBackend? = null

    fun create(context: Context): AsrBackend? {
        val resident = warmBackend
        if (resident != null) {
            return resident
        }
        return OfflineAsrBackend(context)
    }

    fun getLocalName(context: Context): String? =
        AsrModelManager(context.applicationContext).getSelectedModelInfo()?.name

    fun warmup(context: Context) {
        synchronized(this) {
            if (warmBackend != null) return
            val backend = OfflineAsrBackend(context.applicationContext)
            if (!backend.initialize()) {
                return
            }
            warmBackend = backend
        }
    }

    fun releaseModel() {
        synchronized(this) {
            val backend = warmBackend
            warmBackend = null
            backend?.release()
        }
    }
}
