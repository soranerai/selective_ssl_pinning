package dev.soranerai.netprivacy

import android.util.Log

internal object NetPrivacyLog {
    private const val TAG = "SelectiveWebViewCA"

    fun info(message: String) {
        Log.i(TAG, message)
    }

    fun warn(message: String, error: Throwable? = null) {
        Log.w(TAG, message, error)
    }
}
